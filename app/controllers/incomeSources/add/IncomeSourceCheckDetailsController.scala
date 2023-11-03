/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.incomeSources.add

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Add, JourneyType}
import exceptions.MissingSessionKey
import models.incomeSourceDetails.AddIncomeSourceData.{dateStartedField, incomeSourcesAccountingMethodField}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
import play.api.Logger
import play.api.mvc._
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.IncomeSourceCheckDetails

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceCheckDetailsController @Inject()(val checkDetailsView: IncomeSourceCheckDetails,
                                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                                   val authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   val retrieveNino: NinoPredicate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val businessDetailsService: CreateBusinessDetailsService)
                                                  (implicit val ec: ExecutionContext,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val appConfig: FrontendAppConfig,
                                                   implicit val sessionService: SessionService,
                                                   implicit val itvcErrorHandler: ItvcErrorHandler,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController
  with IncomeSourcesUtils with FeatureSwitching {

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        incomeSourceType
      )
  }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true,
              incomeSourceType
            )
        }
  }

  private def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, incomeSourceType: IncomeSourceType)
                           (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = withIncomeSourcesFS {
    val backUrl: String = if (isAgent) controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(incomeSourceType).url
    else controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(incomeSourceType).url
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.submitAgent(incomeSourceType) else {
      controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.submit(incomeSourceType)
    }
    getDetails(incomeSourceType)(user).map {
      case Right(viewModel) =>
        Ok(checkDetailsView(
          viewModel,
          postAction = postAction,
          isAgent,
          backUrl = backUrl
        ))
      case Left(ex) =>
        Logger("application").error(
          s"[IncomeSourceCheckDetailsController][handleRequest] - Error: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    } recover {
      case ex: Exception =>
        Logger("application").error(
          s"[IncomeSourceCheckDetailsController][handleRequest] - Error: Unable to construct getCheckPropertyViewModel ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  private def getDetails(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Either[Throwable, CheckDetailsViewModel]] = {
    {
      if (incomeSourceType == SelfEmployment) getBusinessModel else getPropertyModel
    } map {
      case Right(checkBusinessViewModel: CheckBusinessDetailsViewModel) =>
        Right(checkBusinessViewModel)
      case Right(checkPropertyViewModel: CheckPropertyViewModel) =>
        Right(checkPropertyViewModel)
      case Left(ex) =>
        Left(new IllegalArgumentException(s"Missing required session data: ${ex.getMessage}"))
    }
  }

private def getPropertyModel(implicit user: MtdItUser[_]): Future[Either[Throwable, CheckPropertyViewModel]] = {
  sessionService.getMongoKeyTyped[LocalDate](dateStartedField, JourneyType(Add, ForeignProperty)).flatMap { startDate: Either[Throwable, Option[LocalDate]] =>
    sessionService.getMongoKeyTyped[String](incomeSourcesAccountingMethodField, JourneyType(Add, ForeignProperty)).map { accMethod: Either[Throwable, Option[String]] =>
      (startDate, accMethod) match {
        case (Right(dateMaybe), Right(methodMaybe)) =>
          (dateMaybe, methodMaybe) match {
            case (Some(date), Some(method)) =>
              Right(CheckPropertyViewModel(
                tradingStartDate = date,
                cashOrAccrualsFlag = method
              ))
            case (_,_) =>
              Left(new Error(s"Start date or accounting method not found in session. Start date: $dateMaybe, AccMethod: $methodMaybe"))
          }
//          for {
//            foreignPropertyStartDate <- dateMaybe
//            cashOrAccrualsFlag <- methodMaybe
//          } yield {
//            CheckPropertyViewModel(
//              tradingStartDate = foreignPropertyStartDate,
//              cashOrAccrualsFlag = cashOrAccrualsFlag)
//          }
        case (_, _) => Left(new Error(s"Error while retrieving date started or accounting method from session"))
      }
    }
  }
}

private def getBusinessModel(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Either[Throwable, CheckBusinessDetailsViewModel]] = {
  val userActiveBusinesses: List[BusinessDetailsModel] = user.incomeSources.businesses.filterNot(_.isCeased)
  val skipAccountingMethod: Boolean = userActiveBusinesses.isEmpty
  val errorTracePrefix = "[CheckBusinessDetailsController][getBusinessDetailsFromSession]:"
  sessionService.getMongo(JourneyType(Add, SelfEmployment).toString).map {
    case Right(Some(uiJourneySessionData)) =>
      uiJourneySessionData.addIncomeSourceData match {
        case Some(addIncomeSourceData) =>

          val address = addIncomeSourceData.address.getOrElse(throw MissingSessionKey(s"$errorTracePrefix address"))
          Right(CheckBusinessDetailsViewModel(
            businessName = addIncomeSourceData.businessName,
            businessStartDate = addIncomeSourceData.dateStarted,
            accountingPeriodEndDate = addIncomeSourceData.accountingPeriodEndDate
              .getOrElse(throw MissingSessionKey(s"$errorTracePrefix accountingPeriodEndDate")),
            businessTrade = addIncomeSourceData.businessTrade
              .getOrElse(throw MissingSessionKey(s"$errorTracePrefix businessTrade")),
            businessAddressLine1 = address.lines.headOption
              .getOrElse(throw MissingSessionKey(s"$errorTracePrefix businessAddressLine1")),
            businessAddressLine2 = address.lines.lift(1),
            businessAddressLine3 = address.lines.lift(2),
            businessAddressLine4 = address.lines.lift(3),
            businessPostalCode = address.postcode,
            businessCountryCode = addIncomeSourceData.countryCode,
            incomeSourcesAccountingMethod = addIncomeSourceData.incomeSourcesAccountingMethod,
            cashOrAccrualsFlag = addIncomeSourceData.incomeSourcesAccountingMethod
              .getOrElse(throw MissingSessionKey(s"$errorTracePrefix incomeSourcesAccountingMethod")),
            skippedAccountingMethod = skipAccountingMethod
          ))

        case None => throw new Exception(s"$errorTracePrefix failed to retrieve addIncomeSourceData")
      }
    case _ => throw new Exception(s"$errorTracePrefix failed to retrieve uiJourneySessionData ")
  }

}


//private def getErrors(startDate: Either[Throwable, Option[LocalDate]], accMethod: Either[Throwable, Option[String]]): Seq[String] = {
//  case class MissingKey(msg: String) //make versatile
//
//  Seq(
//    startDate match {
//      case Right(nameOpt) => nameOpt match {
//        case Some(name) => name
//        case None => Some(MissingKey("MissingKey: addForeignPropertyStartDate"))
//      }
//      case Left(_) => Some(MissingKey("MissingKey: addForeignPropertyStartDate"))
//    },
//    accMethod match {
//      case Right(nameOpt) => nameOpt match {
//        case Some(name) => name
//        case None => Some(MissingKey("MissingKey: addIncomeSourcesAccountingMethod"))
//      }
//      case Left(_) => Some(MissingKey("MissingKey: addIncomeSourcesAccountingMethod"))
//    }
//  ).collect {
//    case Some(MissingKey(msg)) => msg
//  }
//}

def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
  andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
  implicit user =>
    handleSubmit(isAgent = false, incomeSourceType)
}

def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
  implicit request =>
    implicit user =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
        implicit mtdItUser =>
          handleSubmit(isAgent = true, incomeSourceType)
      }
}

def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
  ???
}

}

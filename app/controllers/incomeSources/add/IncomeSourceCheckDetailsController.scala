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
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.AddIncomeSourceData.{dateStartedField, incomeSourcesAccountingMethodField}
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckPropertyViewModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.Logger
import play.api.mvc._
import services.{CreateBusinessDetailsService, IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.incomeSources.add.{CheckBusinessDetails, IncomeSourceCheckDetails}

import java.net.URI
import java.time.LocalDate
import javax.inject.{Inject, Singleton}
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
    getDetails(user).map {
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

  private def getDetails(implicit user: MtdItUser[_]): Future[Either[Throwable, CheckPropertyViewModel]] = {
//Make versatile
    sessionService.getMongoKeyTyped[LocalDate](dateStartedField, JourneyType(Add, ForeignProperty)).flatMap { startDate: Either[Throwable, Option[LocalDate]] =>
      sessionService.getMongoKeyTyped[String](incomeSourcesAccountingMethodField, JourneyType(Add, ForeignProperty)).map { accMethod: Either[Throwable, Option[String]] =>
        val errors: Seq[String] = getErrors(startDate, accMethod)
        val result: Option[CheckPropertyViewModel] = getResult(startDate, accMethod)

        result match {
          case Some(checkForeignPropertyViewModel) =>
            Right(checkForeignPropertyViewModel)
          case None =>
            Left(new IllegalArgumentException(s"Missing required session data: ${errors.map(x => x.mkString(" "))}"))
        }
      }
    }
  }

  private def getResult(startDate: Either[Throwable, Option[LocalDate]], accMethod: Either[Throwable, Option[String]]): Option[CheckPropertyViewModel] = {
    (startDate, accMethod) match { //make versatile
      case (Right(dateMaybe), Right(methodMaybe)) =>
        for {
          foreignPropertyStartDate <- dateMaybe
          cashOrAccrualsFlag <- methodMaybe
        } yield {
          CheckPropertyViewModel(
            tradingStartDate = foreignPropertyStartDate,
            cashOrAccrualsFlag = cashOrAccrualsFlag)
        }
      case (_, _) => None
    }
  }

  private def getErrors(startDate: Either[Throwable, Option[LocalDate]], accMethod: Either[Throwable, Option[String]]): Seq[String] = {
    case class MissingKey(msg: String) //make versatile

    Seq(
      startDate match {
        case Right(nameOpt) => nameOpt match {
          case Some(name) => name
          case None => Some(MissingKey("MissingKey: addForeignPropertyStartDate"))
        }
        case Left(_) => Some(MissingKey("MissingKey: addForeignPropertyStartDate"))
      },
      accMethod match {
        case Right(nameOpt) => nameOpt match {
          case Some(name) => name
          case None => Some(MissingKey("MissingKey: addIncomeSourcesAccountingMethod"))
        }
        case Left(_) => Some(MissingKey("MissingKey: addIncomeSourcesAccountingMethod"))
      }
    ).collect {
      case Some(MissingKey(msg)) => msg
    }
  }

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

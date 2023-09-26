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

package controllers.incomeSources.cease

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.utils.SessionKeys.{ceaseBusinessEndDate, ceaseBusinessIncomeSourceId, ceaseForeignPropertyEndDate, ceaseUKPropertyEndDate}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.CeaseCheckIncomeSourceDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CeaseCheckIncomeSourceDetailsController @Inject()(val authenticate: AuthenticationPredicate,
                                                        val authorisedFunctions: FrontendAuthorisedFunctions,
                                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                                        val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                        val retrieveBtaNavBar: NavBarPredicate,
                                                        val retrieveNino: NinoPredicate,
                                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                        val view: CeaseCheckIncomeSourceDetails,
                                                        val updateIncomeSourceservice: UpdateIncomeSourceService,
                                                        val sessionService: SessionService)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils {

//back/ change/ post/ redirect
//  private def getActions(isAgent: Boolean, incomeSourceType: String, id: Option[String]): Future[(Call, Call, Call, Call, IncomeSourceType)] = {
//    IncomeSourceType(incomeSourceType) match {
//      case Right(incomeSourceTypeValue) =>
//        Future.successful(
//          (incomeSourceTypeValue, isAgent) match {
//            case (SelfEmployment, true) =>
//              (routes.IncomeSourceEndDateController.showAgent(id = id, incomeSourceType = SelfEmployment.key),
//                routes.IncomeSourceEndDateController.showChangeAgent(id = id, incomeSourceType = SelfEmployment.key),
//                routes.CeaseCheckIncomeSourceDetailsController.submitAgent(),
//                routes.BusinessCeasedObligationsController.showAgent(),
//                SelfEmployment)
//            case (SelfEmployment, false) =>
//              (routes.IncomeSourceEndDateController.show(id = id, incomeSourceType = SelfEmployment.key),
//                routes.IncomeSourceEndDateController.showChange(id = id, incomeSourceType = SelfEmployment.key),
//                routes.CeaseCheckIncomeSourceDetailsController.submit(),
//                routes.BusinessCeasedObligationsController.show(),
//                SelfEmployment)
//            case (UkProperty, true) =>
//              (routes.IncomeSourceEndDateController.showAgent(id = id, incomeSourceType = UkProperty.key),
//                routes.IncomeSourceEndDateController.showChangeAgent(id = id, incomeSourceType = UkProperty.key),
//                routes.CeaseCheckIncomeSourceDetailsController.submitAgent(),
//                routes.UKPropertyCeasedObligationsController.showAgent(),
//                UkProperty)
//            case (UkProperty, false) =>
//              (routes.IncomeSourceEndDateController.show(id = id, incomeSourceType = UkProperty.key),
//                routes.IncomeSourceEndDateController.showChange(id = id, incomeSourceType = UkProperty.key),
//                routes.CeaseCheckIncomeSourceDetailsController.submit(),
//                routes.UKPropertyCeasedObligationsController.show(),
//                UkProperty)
//            case (ForeignProperty, true) =>
//              (routes.IncomeSourceEndDateController.showAgent(id = id, incomeSourceType = ForeignProperty.key),
//                routes.IncomeSourceEndDateController.showChangeAgent(id = id, incomeSourceType = ForeignProperty.key),
//                routes.CeaseCheckIncomeSourceDetailsController.submitAgent(),
//                routes.ForeignPropertyCeasedObligationsController.show(),
//                ForeignProperty)
//            case (ForeignProperty, false) =>
//              (routes.IncomeSourceEndDateController.show(id = id, incomeSourceType = ForeignProperty.key),
//                routes.IncomeSourceEndDateController.showChange(id = id, incomeSourceType = ForeignProperty.key),
//                routes.CeaseCheckIncomeSourceDetailsController.submit(),
//                routes.UKPropertyCeasedObligationsController.show(),
//                ForeignProperty)
//          })
//      case Left(exception) => Future.failed(exception)
//    }
//  }


  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, origin: Option[String] = None, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, request: Request[_]): Future[Result] = withIncomeSourcesFS {

    val messagesPrefix = incomeSourceType.ceaseCheckDetailsPrefix


    val sessionDataFuture = for {
      incomeSourceId <- sessionService.get(ceaseBusinessIncomeSourceId)
      cessationEndDate <- incomeSourceType match {
        case SelfEmployment => sessionService.get(ceaseBusinessEndDate)
        case UkProperty => sessionService.get(ceaseUKPropertyEndDate)
        case ForeignProperty => sessionService.get(ceaseForeignPropertyEndDate)
      }

    } yield (incomeSourceId, cessationEndDate)

    sessionDataFuture.flatMap {
      case (Right(Some(incomeSourceId)), Right(Some(cessationEndDate))) =>
        incomeSourceDetailsService.getCheckCeaseSelfEmploymentDetailsViewModel(sources, incomeSourceId, cessationEndDate) match {
          case Right(viewModel) =>
            Future.successful(Ok(view(
              viewModel = viewModel,
              isAgent = isAgent,
              changeUrl = controllers.routes.HomeController.show().url,
              backUrl = routes.CeaseIncomeSourceController.show().url,
              messagesPrefix = messagesPrefix)))
          case Left(ex) =>
            Future.failed(ex)
        }
      case (Right(None), Right(Some(cessationEndDate))) =>
        incomeSourceDetailsService.getCheckCeasePropertyIncomeSourceDetailsViewModel(sources, cessationEndDate, incomeSourceType) match {
          case Right(viewModel) =>
            Future.successful(Ok(view(
              viewModel = viewModel,
              isAgent = isAgent,
              changeUrl = routes.IncomeSourceNotCeasedController.show(isAgent, SelfEmployment.key).url,
              backUrl = routes.CeaseIncomeSourceController.show().url,
              messagesPrefix = messagesPrefix)))
          case Left(ex) =>
            Future.failed(ex)
        }
      case (Right(None), Right(None)) =>
        val errorMessage = "Both incomeSourceId and cessationEndDate are missing from the session."
        Future.failed(new Exception(errorMessage))
      case (Right(Some(_)), Right(None)) =>
        val errorMessage = s"CessationEndDate is missing from the session."
        Future.failed(new Exception(errorMessage))
      case (Left(ex), _) =>
        val errorMessage = s"Could not get incomeSourceId from session incomeSourceType = $incomeSourceType, ${ex.getMessage}"
        Future.failed(new Exception(errorMessage))
      case (_, Left(ex)) =>
        val errorMessage = s"Could not get cessation date from session incomeSourceType = $incomeSourceType, ${ex.getMessage}"
        Future.failed(new Exception(errorMessage))
    }

  } recover {
    case ex: Exception =>
      Logger("application").error(s"[CeaseCheckIncomeSourceDetailsController][handleRequest]${if (isAgent) "[Agent] "}" +
        s"Error getting CeaseCheckIncomeSourceDetails page: ${ex.getMessage}")
      Redirect(controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent, SelfEmployment.key))
  }


  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          sources = user.incomeSources,
          isAgent = false,
          None,
          incomeSourceType = incomeSourceType
        )
    }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true,
              incomeSourceType = incomeSourceType
            )
        }
  }

  def handleSubmitRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_], request: Request[_]): Future[Result] = withIncomeSourcesFS {

    val redirectAction: Call = (isAgent, incomeSourceType) match {
      case (true, SelfEmployment) => routes.BusinessCeasedObligationsController.showAgent()
      case (false, SelfEmployment) => routes.BusinessCeasedObligationsController.show()
      case (true, UkProperty) => routes.UKPropertyCeasedObligationsController.showAgent()
      case (false, UkProperty) => routes.UKPropertyCeasedObligationsController.show()
      case (true, ForeignProperty) => routes.ForeignPropertyCeasedObligationsController.showAgent()
      case (false, ForeignProperty) => routes.ForeignPropertyCeasedObligationsController.show() // Change to the appropriate route for ForeignProperty
      case _ => routes.IncomeSourceNotCeasedController.show(isAgent, incomeSourceType.key)
    }

    val sessionDataFuture = for {
      incomeSourceId <- sessionService.get(ceaseBusinessIncomeSourceId)
      cessationEndDate <- sessionService.get(ceaseBusinessEndDate)
    } yield (incomeSourceId, cessationEndDate)

    sessionDataFuture.flatMap {
      case (Right(Some(incomeSourceId)), Right(Some(cessationEndDate))) =>
        updateIncomeSourceservice
          .updateCessationDate(user.nino, incomeSourceId, cessationEndDate).flatMap {
          case Right(_) =>
            Future.successful(Redirect(redirectAction))
          case _ =>
            Logger("application").error(s"[CheckCeaseBusinessDetailsController][handleSubmitRequest]:" +
              s" Unsuccessful update response received")
            Future.successful {
              Redirect(controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent, SelfEmployment.key))
            }
        }
      case (Left(ex), Right(_)) =>
        val errorMessage = s" Could not get incomeSourceId from session. ${ex.getMessage}"
        Future.failed(new Exception(errorMessage))
      case (Right(_), Left(ex)) =>
        val errorMessage = s" Could not get ceaseBusinessEndDate from session. ${ex.getMessage}"
        Future.failed(new Exception(errorMessage))
      case (Right(None), Right(None)) =>
        val errorMessage = s" Could not get incomeSourceId from session"
        Future.failed(new Exception(errorMessage))
      case (Right(None), Right(ex)) =>
        val errorMessage = s" Could not get ceaseBusinessEndDate from session. ${ex}"
        Future.failed(new Exception(errorMessage))
      case (Right(ex), Right(None)) =>
        val errorMessage = s" Could not get ceaseBusinessEndDate from session. ${ex}"
        Future.failed(new Exception(errorMessage))
      case (Left(_), Left(ex)) =>
        val errorMessage = s"Both incomeSourceId and cessationEndDate are missing from the session. ${ex.getMessage}"
        Future.failed(new Exception(errorMessage))
    }
  } recover {
    case ex: Exception =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${
        if (isAgent) "[Agent]"
      }[CheckCeaseBusinessDetailsController][handleSubmitRequest] Error Submitting Cease Date : ${
        ex.getMessage
      }")
      errorHandler.showInternalServerError()
  }

  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType)
  }

  def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType)
        }
  }
}
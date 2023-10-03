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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.utils.SessionKeys.{ceaseBusinessEndDate, ceaseBusinessIncomeSourceId, ceaseForeignPropertyEndDate, ceaseUKPropertyEndDate}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
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

  def getSessionData(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[(Either[Throwable, Option[String]], Either[Throwable, Option[String]])] = {
    val incomeSourceIdFuture = sessionService.get(ceaseBusinessIncomeSourceId)
    val cessationEndDateFuture = incomeSourceType match {
      case SelfEmployment => sessionService.get(ceaseBusinessEndDate)
      case UkProperty => sessionService.get(ceaseUKPropertyEndDate)
      case ForeignProperty => sessionService.get(ceaseForeignPropertyEndDate)
    }

    for {
      incomeSourceId <- incomeSourceIdFuture
      cessationEndDate <- cessationEndDateFuture
    } yield (incomeSourceId, cessationEndDate)
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {

    val messagesPrefix = incomeSourceType.ceaseCheckDetailsPrefix


    val sessionDataFuture = getSessionData(incomeSourceType)

    sessionDataFuture.flatMap {
      case (Right(Some(incomeSourceId)), Right(Some(cessationEndDate))) =>
        incomeSourceDetailsService.getCheckCeaseSelfEmploymentDetailsViewModel(sources, incomeSourceId, cessationEndDate) match {
          case Right(viewModel) =>
            Future.successful(Ok(view(
              viewModel = viewModel,
              isAgent = isAgent,
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
      Redirect(controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent, SelfEmployment))
  }


  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          sources = user.incomeSources,
          isAgent = false,
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
      case (true, SelfEmployment) => routes.IncomeSourceCeasedObligationsController.showAgent(SelfEmployment)
      case (false, SelfEmployment) => routes.IncomeSourceCeasedObligationsController.show(SelfEmployment)
      case (true, UkProperty) => routes.IncomeSourceCeasedObligationsController.showAgent(UkProperty)
      case (false, UkProperty) => routes.IncomeSourceCeasedObligationsController.show(UkProperty)
      case (true, ForeignProperty) => routes.IncomeSourceCeasedObligationsController.showAgent(ForeignProperty)
      case (false, ForeignProperty) => routes.IncomeSourceCeasedObligationsController.show(ForeignProperty)
      case _ => routes.IncomeSourceNotCeasedController.show(isAgent, incomeSourceType)
    }

    val sessionDataFuture = getSessionData(incomeSourceType)

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
              Redirect(controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent, SelfEmployment))
            }
        }

      case (Right(None), Right(Some(cessationEndDate))) =>
        val propertyIncomeSources = (incomeSourceType) match {
          case (UkProperty) => user.incomeSources.properties.find(x => x.isUkProperty && !x.isCeased)
          case (ForeignProperty) => user.incomeSources.properties.find(x => x.isForeignProperty && !x.isCeased)
          case (SelfEmployment) => None
        }
        if (propertyIncomeSources.isEmpty) {
          val errorMessage = s"Failed to retrieve $propertyIncomeSources"
          Future.failed(new Exception(errorMessage))
        } else {
          val incomeSourceId = propertyIncomeSources.head.incomeSourceId
          updateIncomeSourceservice
            .updateCessationDate(user.nino, incomeSourceId, cessationEndDate).flatMap {
            case Right(_) =>
              Future.successful(Redirect(redirectAction))
            case _ =>
              Logger("application").error(s"[CheckCeaseIncomeSourceDetailsController][handleSubmitRequest]:" +
                s" Unsuccessful update response received")
              Future.successful {
                Redirect(controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent, incomeSourceType))
              }
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
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
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Cease, JourneyType}
import forms.utils.SessionKeys.{ceaseBusinessEndDate, ceaseBusinessIncomeSourceId, incomeSourceId}
import models.incomeSourceDetails.{CeaseIncomeSourceData, IncomeSourceDetailsModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.incomeSources.cease.CheckCeaseBusinessDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckCeaseBusinessDetailsController @Inject()(val authenticate: AuthenticationPredicate,
                                                    val authorisedFunctions: FrontendAuthorisedFunctions,
                                                    val checkSessionTimeout: SessionTimeoutPredicate,
                                                    val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                    val retrieveBtaNavBar: NavBarPredicate,
                                                    val retrieveNino: NinoPredicate,
                                                    val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                    val view: CheckCeaseBusinessDetails,
                                                    val updateIncomeSourceservice: UpdateIncomeSourceService,
                                                    val sessionService: SessionService)
                                                   (implicit val appConfig: FrontendAppConfig,
                                                    mcc: MessagesControllerComponents,
                                                    val ec: ExecutionContext,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils {

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, messages: Messages, request: Request[_]): Future[Result] = withIncomeSourcesFS {

    println("PPPPPPPP")
    val sessionDataFuture = for {
      incomeSourceId <- sessionService.getMongoKey(CeaseIncomeSourceData.incomeSourceIdField, JourneyType(Cease, SelfEmployment))
      cessationEndDate <- sessionService.getMongoKey(CeaseIncomeSourceData.dateCeasedField, JourneyType(Cease, SelfEmployment))
    } yield (incomeSourceId, cessationEndDate)

    Thread.sleep(1000)
    println("TTTTTTTTTTT")
    println(sessionDataFuture)

    sessionDataFuture.flatMap {
      case (Right(Some(incomeSourceId)), Right(Some(cessationEndDate))) =>
        incomeSourceDetailsService.getCheckCeaseBusinessDetailsViewModel(sources, incomeSourceId, cessationEndDate) match {
          case Right(viewModel) =>
            Future.successful(Ok(view(
              viewModel.get,
              isAgent = isAgent,
              origin = origin)(user, messages)))
          case Left(ex) =>
            Future.failed(ex)
        }
      case _ =>
        val errorMessage = s"Could not get incomeSourceId or ceaseBusinessEndDate from session"
        Future.failed(new Exception(errorMessage))
    }

  } recover {
    case ex: Exception =>
      Logger("application").error(s"[CheckCeaseBusinessDetailsController][handleRequest]${if (isAgent) "[Agent] "}" +
        s"Error getting CheckCeaseBusinessDetails page: ${ex.getMessage}")
      Redirect(controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent, SelfEmployment))
  }


  def show(): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          sources = user.incomeSources,
          isAgent = false,
          None
        )
    }

  def showAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true
            )
        }
  }

  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_], request: Request[_]): Future[Result] = withIncomeSourcesFS {
    val redirectAction = {
      if (isAgent)
        routes.IncomeSourceCeasedObligationsController.showAgent(SelfEmployment)
      else
        routes.IncomeSourceCeasedObligationsController.show(SelfEmployment)
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
            Future.successful(Redirect(redirectAction.url))
          case _ =>
            Logger("application").error(s"[CheckCeaseBusinessDetailsController][handleSubmitRequest]:" +
              s" Unsuccessful update response received")
            Future.successful {
              Redirect(controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent, SelfEmployment))
            }
        }
      case _ =>
        val errorMessage = s" Could not get incomeSourceId or ceaseBusinessEndDate from session"
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

  def submit(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(isAgent = false)
  }

  def submitAgent(): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true)
        }
  }
}
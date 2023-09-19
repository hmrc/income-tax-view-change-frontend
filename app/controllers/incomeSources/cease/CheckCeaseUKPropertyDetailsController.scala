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
import enums.IncomeSourceJourney.UkProperty
import exceptions.MissingSessionKey
import forms.utils.SessionKeys.ceaseUKPropertyEndDate
import models.updateIncomeSource.{UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{IncomeSourceDetailsService, SessionService, UpdateIncomeSourceService}
import utils.IncomeSourcesUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.CheckCeaseUKPropertyDetails

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckCeaseUKPropertyDetailsController @Inject()(val authenticate: AuthenticationPredicate,
                                                      val authorisedFunctions: FrontendAuthorisedFunctions,
                                                      val checkSessionTimeout: SessionTimeoutPredicate,
                                                      val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                      val retrieveBtaNavBar: NavBarPredicate,
                                                      val retrieveNino: NinoPredicate,
                                                      val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                      val view: CheckCeaseUKPropertyDetails,
                                                      val updateIncomeSourceService: UpdateIncomeSourceService,
                                                      val sessionService: SessionService)
                                                     (implicit val appConfig: FrontendAppConfig,
                                                      val ec: ExecutionContext,
                                                      implicit override val mcc: MessagesControllerComponents,
                                                      val itvcErrorHandler: ItvcErrorHandler,
                                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils {

  def handleRequest(isAgent: Boolean, origin: Option[String] = None)
                   (implicit user: MtdItUser[_], messages: Messages): Future[Result] = withIncomeSourcesFS {

    Future.successful(Ok(view(
      isAgent = isAgent,
      origin = origin)(user, messages)))

  } recover {
    case ex: Exception =>
      val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"[CheckCeaseUKPropertyDetailsController][handleRequest]${if (isAgent) "[Agent] "}" +
        s"Error getting CheckCeaseUKPropertyDetails page: ${ex.getMessage}")
      errorHandler.showInternalServerError()
  }


  def show(): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
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
              isAgent = true
            )
        }
  }

  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_], messages: Messages): Future[Result] = withIncomeSourcesFS {
    lazy val redirectAction = {
      if (isAgent)
        routes.UKPropertyCeasedObligationsController.showAgent()
      else
        routes.UKPropertyCeasedObligationsController.show()
    }

    lazy val incomeSourceNotCeasedShowAction = controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent, UkProperty.key)

    sessionService.get(ceaseUKPropertyEndDate).flatMap {
      case Right(Some(date)) =>
        val incomeSourceId: Option[String] = user.incomeSources.properties.filter(_.isUkProperty).map(_.incomeSourceId).headOption
        incomeSourceId match {
          case Some(id) =>
            updateIncomeSourceService.updateCessationDate(user.nino, id, date).flatMap {
              case Right(_) =>
                Future.successful(Redirect(redirectAction.url))
              case Left(error) =>
                Logger("application")
                  .error(s"${if (isAgent) "[Agent]"}[CheckCeaseUKPropertyDetailsController][submit]:${error.reason}")
                Future.successful(Redirect(incomeSourceNotCeasedShowAction))
            }
          case _ => Future.failed(new Exception("missing income source ID"))
        }
      case Right(None) =>  Future.failed(MissingSessionKey(ceaseUKPropertyEndDate))

      case Left(exception) => Future.failed(exception)
    }
  } recover {
    case ex: Exception =>
      val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${
        if (isAgent) "[Agent]"
      }[CheckCeaseUKPropertyDetailsController][submit] Error Submitting Cease Date : ${
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
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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.cease.IncomeSourceEndDateForm
import forms.utils.SessionKeys.ceaseBusinessIncomeSourceId
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import utils.IncomeSourcesUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.IncomeSourceEndDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceEndDateController @Inject()(val authenticate: AuthenticationPredicate,
                                              val authorisedFunctions: FrontendAuthorisedFunctions,
                                              val checkSessionTimeout: SessionTimeoutPredicate,
                                              val incomeSourceEndDateForm: IncomeSourceEndDateForm,
                                              val incomeSourceDetailsService: IncomeSourceDetailsService,
                                              val retrieveBtaNavBar: NavBarPredicate,
                                              val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                              val retrieveNino: NinoPredicate,
                                              val incomeSourceEndDate: IncomeSourceEndDate,
                                              val customNotFoundErrorView: CustomNotFoundError,
                                              val sessionService: SessionService)
                                             (implicit val appConfig: FrontendAppConfig,
                                              mcc: MessagesControllerComponents,
                                              val ec: ExecutionContext,
                                              val itvcErrorHandler: ItvcErrorHandler,
                                              val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils {


  private def getActions(isAgent: Boolean, incomeSourceType: String, id: Option[String]): Future[(Call, Call, Call, IncomeSourceType)] = {
    IncomeSourceType(incomeSourceType) match {
      case Right(incomeSourceTypeValue) =>
        Future.successful(
          (incomeSourceTypeValue, isAgent) match {
            case (UkProperty, true) =>
              (routes.CeaseUKPropertyController.showAgent(),
                routes.IncomeSourceEndDateController.submitAgent(id = id, incomeSourceType = UkProperty.key),
                routes.CheckCeaseUKPropertyDetailsController.showAgent(),
                UkProperty)
            case (UkProperty, false) =>
              (routes.CeaseUKPropertyController.show(),
                routes.IncomeSourceEndDateController.submit(id = id, incomeSourceType = UkProperty.key),
                routes.CheckCeaseUKPropertyDetailsController.show(),
                UkProperty)
            case (ForeignProperty, true) =>
              (routes.CeaseForeignPropertyController.showAgent(),
                routes.IncomeSourceEndDateController.submitAgent(id = id, incomeSourceType = ForeignProperty.key),
                routes.CheckCeaseForeignPropertyDetailsController.showAgent(),
                ForeignProperty)
            case (ForeignProperty, false) =>
              (routes.CeaseForeignPropertyController.show(),
                routes.IncomeSourceEndDateController.submit(id = id, incomeSourceType = ForeignProperty.key),
                routes.CheckCeaseForeignPropertyDetailsController.show(),
                ForeignProperty)
            case (SelfEmployment, true) =>
              (routes.CeaseIncomeSourceController.showAgent(),
                routes.IncomeSourceEndDateController.submitAgent(id = id, incomeSourceType = SelfEmployment.key),
                routes.CheckCeaseBusinessDetailsController.showAgent(),
                SelfEmployment)
            case (SelfEmployment, false) =>
              (routes.CeaseIncomeSourceController.show(),
                routes.IncomeSourceEndDateController.submit(id = id, incomeSourceType = SelfEmployment.key),
                routes.CheckCeaseBusinessDetailsController.show(),
                SelfEmployment)
          })
      case Left(exception) => Future.failed(exception)
    }
  }

  def show(id: Option[String], incomeSourceType: String): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType,
          id = id
        )
    }

  def showAgent(id: Option[String], incomeSourceType: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType,
              id = id
            )
        }
  }

  def handleRequest(id: Option[String], incomeSourceType: String, isAgent: Boolean)
                   (implicit user: MtdItUser[_], ec: ExecutionContext, messages: Messages): Future[Result] = withIncomeSourcesFS {

    getActions(isAgent, incomeSourceType, id).flatMap {
      actions =>
        val (backAction: Call, postAction: Call, _, incomeSourceTypeValue: IncomeSourceType) = actions
        (incomeSourceTypeValue, id) match {
          case (SelfEmployment, None) =>
            val errorMessage = "missing income source ID"
            Future.failed(new Exception(errorMessage))
          case _ =>
            Future.successful(
              Ok(incomeSourceEndDate(
                incomeSourceEndDateForm = incomeSourceEndDateForm(incomeSourceTypeValue, id),
                postAction = postAction,
                isAgent = isAgent,
                backUrl = backAction.url,
                incomeSourceType = incomeSourceTypeValue
              )(user, messages)))
        }
    }
  } recover {
    case ex: Exception =>
      val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"[IncomeSourceEndDateController][handleRequest]: Error getting IncomeSourceEndDate page: ${ex.getMessage}")
      errorHandler.showInternalServerError()
  }

  def submit(id: Option[String], incomeSourceType: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType,
        id = id)
  }

  def submitAgent(id: Option[String], incomeSourceType: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType,
              id = id)
        }
  }

  def handleSubmitRequest(id: Option[String], incomeSourceType: String, isAgent: Boolean)
                         (implicit user: MtdItUser[_], messages: Messages): Future[Result] = withIncomeSourcesFS {

    getActions(isAgent, incomeSourceType, id).flatMap { actions =>
      val (backAction, postAction, redirectAction, incomeSourceTypeValue) = actions
      incomeSourceEndDateForm.apply(incomeSourceTypeValue, id).bindFromRequest().fold(

        hasErrors => {
          Future.successful(
            BadRequest(incomeSourceEndDate(
              incomeSourceEndDateForm = hasErrors,
              postAction = postAction,
              backUrl = backAction.url,
              isAgent = isAgent,
              incomeSourceType = incomeSourceTypeValue
            )(user, messages)))
        },
        validatedInput => (incomeSourceTypeValue, id) match {
          case (SelfEmployment, None) =>
            val errorMessage = "missing income source ID"
            Future.failed(new Exception(errorMessage))

          case (SelfEmployment, Some(incomeSourceId)) =>
            val result = Redirect(redirectAction)
            sessionService.set(result,
              incomeSourceTypeValue.endDateSessionKey -> validatedInput.date.toString,
              ceaseBusinessIncomeSourceId -> incomeSourceId
            ).flatMap {
              case Right(result) => Future.successful(result)
              case Left(exception) => Future.failed(exception)
            }

          case _ =>
            val session = incomeSourceTypeValue.endDateSessionKey -> validatedInput.date.toString
            val result = Redirect(redirectAction)
            sessionService.set(result, session).flatMap {
              case Right(result) => Future.successful(result)
              case Left(exception) => Future.failed(exception)
            }

        })
    }


  } recover {
    case ex: Exception =>
      val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"[IncomeSourceEndDateController][handleSubmitRequest]: Error getting IncomeSourceEndDate page: ${ex.getMessage}")
      errorHandler.showInternalServerError()
  }

}

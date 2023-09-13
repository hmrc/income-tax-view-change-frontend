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
import forms.models.DateFormElement
import forms.utils.SessionKeys
import forms.utils.SessionKeys.ceaseBusinessIncomeSourceId
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import utils.IncomeSourcesUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.IncomeSourceEndDate

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

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
          id = id,
          isChange = false
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
              id = id,
              isChange = false
            )
        }
  }

  def showChange(id: Option[String], incomeSourceType: String): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType,
          id = id,
          isChange = false
        )
    }

  def showChangeAgent(id: Option[String], incomeSourceType: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType,
              id = id,
              isChange = false
            )
        }
  }

  def handleRequest(id: Option[String],  isAgent: Boolean, isChange: Boolean, incomeSourceType: String)
                   (implicit user: MtdItUser[_], ec: ExecutionContext, messages: Messages): Future[Result] = withIncomeSourcesFS {

    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    getActions(isAgent, incomeSourceType, id).map {
      actions =>
        val (backAction: Call, postAction: Call, _, incomeSourceTypeValue: IncomeSourceType) = actions
        (incomeSourceTypeValue, id) match {
          case (SelfEmployment, None) =>
            errorHandler.showInternalServerError()
          case _ =>
            getFilledForm(incomeSourceEndDateForm(incomeSourceTypeValue, id), incomeSourceTypeValue, isUpdate = true) match {
              case Right(form) =>
                Ok(
                  incomeSourceEndDate(
                    incomeSourceEndDateForm = form,
                    postAction = postAction,
                    isAgent = isAgent,
                    backUrl = backAction.url,
                    incomeSourceType = incomeSourceTypeValue
                  )(user, messages)
                )
              case Left(ex) =>
                Logger("application").error(s"${if (isAgent) "[Agent]"}" +
                  s"[IncomeSourceEndDateController][handleRequest]: Failed to get income source start date from session, reason: ${ex.getMessage}")
                errorHandler.showInternalServerError()
            }
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
        id = id,
        isChange = false)
  }

  def submitAgent(id: Option[String], incomeSourceType: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType,
              id = id,
              isChange = false)
        }
  }

  def handleSubmitRequest(id: Option[String], isAgent: Boolean, isChange:Boolean, incomeSourceType: String)
                         (implicit user: MtdItUser[_], messages: Messages): Future[Result] = withIncomeSourcesFS {

    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    getActions(isAgent, incomeSourceType, id).map { actions =>
      val (backAction, postAction, redirectAction, incomeSourceTypeValue) = actions
      incomeSourceEndDateForm.apply(incomeSourceTypeValue, id).bindFromRequest().fold(

        hasErrors => {
          BadRequest(incomeSourceEndDate(
            incomeSourceEndDateForm = hasErrors,
            postAction = postAction,
            backUrl = backAction.url,
            isAgent = isAgent,
            incomeSourceType = incomeSourceTypeValue
          )(user, messages))
        },

        validatedInput => (incomeSourceTypeValue, id) match {

          case (SelfEmployment, None) =>
            Logger("application").error(s"${if (isAgent) "[Agent]"}" +
              s"[IncomeSourceEndDateController][handleSubmitRequest]: missing income source ID.")
            errorHandler.showInternalServerError()

          case (SelfEmployment, Some(incomeSourceId)) =>
            Redirect(redirectAction)
              .addingToSession(incomeSourceTypeValue.endDateSessionKey -> validatedInput.date.toString)
              .addingToSession(ceaseBusinessIncomeSourceId -> incomeSourceId)

          case _ =>
            Redirect(redirectAction)
              .addingToSession(incomeSourceTypeValue.endDateSessionKey -> validatedInput.date.toString)
        })
    }


  } recover {
    case ex: Exception =>
      val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"[IncomeSourceEndDateController][handleSubmitRequest]: Error getting IncomeSourceEndDate page: ${ex.getMessage}")
      errorHandler.showInternalServerError()
  }

  private def getFilledForm(form: Form[DateFormElement],
                            incomeSourceType: IncomeSourceType,
                            isUpdate: Boolean)(implicit user: MtdItUser[_]): Either[Throwable, Form[DateFormElement]] = {

    val maybeStartDate = getEndDate(incomeSourceType)

    (maybeStartDate, isUpdate) match {
      case (Some(date), true) if Try(LocalDate.parse(date)).toOption.isDefined =>
        Right(
          form.fill(
            DateFormElement(
              LocalDate.parse(date)
            )
          )
        )
      case (Some(date), true) => Left(new Error(s"Could not parse $date as a LocalDate"))
      case _ => Right(form)
    }
  }

  private def getEndDate(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Option[String] = {
    user.session.get(incomeSourceType.endDateSessionKey)
  }

}

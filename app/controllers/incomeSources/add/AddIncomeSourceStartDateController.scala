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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.add.{AddIncomeSourceStartDateForm => form}
import forms.models.DateFormElement
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatterImpl
import play.api.Logger
import play.api.data.Form
import play.api.http.HttpVerbs
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddIncomeSourceStartDate

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class AddIncomeSourceStartDateController @Inject()(authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   checkSessionTimeout: SessionTimeoutPredicate,
                                                   retrieveNino: NinoPredicate,
                                                   val addIncomeSourceStartDate: AddIncomeSourceStartDate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val customNotFoundErrorView: CustomNotFoundError,
                                                   incomeSourceDetailsService: IncomeSourceDetailsService)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   implicit val itvcErrorHandler: ItvcErrorHandler,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                   implicit val dateFormatter: ImplicitDateFormatterImpl,
                                                   implicit val dateService: DateService,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def show(incomeSourceKey: String,
           isAgent: Boolean,
           isChange: Boolean
          ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>
    IncomeSourceType.get(incomeSourceKey) match {
      case Left(ex: Exception) => Logger("application").error(s"[AddIncomeSourceStartDateController][handleShowRequest]: " +
        s"Failed fulfil show request: ${ex.getMessage}")
        Future.successful(getErrorHandler(isAgent).showInternalServerError())
      case Right(value) =>
        handleShowRequest(
          incomeSourceType = value,
          isAgent = isAgent,
          isUpdate = isChange
        )
    }
  }

  def submit(incomeSourceKey: String,
             isAgent: Boolean,
             isChange: Boolean
            ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>
    IncomeSourceType.get(incomeSourceKey) match {
      case Left(ex: Exception) => Logger("application").error(s"[AddIncomeSourceStartDateController][handleShowRequest]: " +
        s"Failed fulfil submit request: ${ex.getMessage}")
        Future.successful(getErrorHandler(isAgent).showInternalServerError())
      case Right(value) =>
        handleSubmitRequest(
          incomeSourceType = value,
          isAgent = isAgent,
          isUpdate = isChange
        )
    }
  }

  private def handleShowRequest(incomeSourceType: IncomeSourceType,
                                isAgent: Boolean,
                                isUpdate: Boolean)
                               (implicit user: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.startDateMessagesPrefix

    Future.successful(
      if (isEnabled(IncomeSources)) {
        (getCalls(incomeSourceType, isAgent, isUpdate), getFilledForm(form(messagesPrefix), incomeSourceType, isUpdate)) match {
          case ((backCall, postAction, _), Right(form)) =>
            Ok(
              addIncomeSourceStartDate(
                form = form,
                isAgent = isAgent,
                backUrl = backCall.url,
                postAction = postAction,
                messagesPrefix = messagesPrefix
              )
            )
          case (_, Left(ex)) =>
            Logger("application").error(s"[AddIncomeSourceStartDateController][handleShowRequest]: " +
              s"Failed to get income source start date from session, reason: ${ex.getMessage}")
            getErrorHandler(isAgent).showInternalServerError()
        }
      } else Ok(customNotFoundErrorView())
    )
  }


  private def handleSubmitRequest(incomeSourceType: IncomeSourceType,
                                  isAgent: Boolean,
                                  isUpdate: Boolean)
                                 (implicit user: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.startDateMessagesPrefix

    Future.successful(
      if (isEnabled(IncomeSources)) {
        getCalls(incomeSourceType, isAgent, isUpdate) match {
          case (backCall, postAction, redirectCall) =>
            form(messagesPrefix).bindFromRequest().fold(
              formWithErrors =>
                BadRequest(
                  addIncomeSourceStartDate(
                    isAgent = isAgent,
                    form = formWithErrors,
                    backUrl = backCall.url,
                    postAction = postAction,
                    messagesPrefix = messagesPrefix
                  )
                ),
              formData =>
                Redirect(redirectCall)
                  .addingToSession(
                    (incomeSourceType match {
                      case SelfEmployment =>
                        SessionKeys.addBusinessStartDate
                      case UkProperty =>
                        SessionKeys.addUkPropertyStartDate
                      case ForeignProperty =>
                        SessionKeys.foreignPropertyStartDate
                    }) -> formData.date.toString
                  )
            )
        }
      } else Ok(customNotFoundErrorView())
    )
  }

  private def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent)
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              authenticatedCodeBlock(mtdItUser)
            }
      }
    else
      (checkSessionTimeout andThen authenticate andThen retrieveNino
        andThen retrieveIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }

  private def getErrorHandler(isAgent: Boolean): ShowInternalServerError = {
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler
  }

  private def getFilledForm(form: Form[DateFormElement],
                            incomeSourceType: IncomeSourceType,
                            isUpdate: Boolean)(implicit user: MtdItUser[_]): Either[Throwable, Form[DateFormElement]] = {

    val maybeStartDate = getStartDate(incomeSourceType)

    (maybeStartDate, isUpdate) match {
      case (Some(date), true) if Try(LocalDate.parse(date)).toOption.isDefined =>
        Right(
          form.fill(
            DateFormElement(
              LocalDate.parse(date)
            )
          )
        )
      case (Some(date), true) =>
        Left(new Error(s"Could not parse $date as a LocalDate"))
      case _ => Right(form)
    }
  }

  private def getStartDate(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Option[String] = {
    user.session.get(incomeSourceType.startDateSessionKey)
  }

  private def getCalls(incomeSourceType: IncomeSourceType,
                       isAgent: Boolean,
                       isChange: Boolean): (Call, Call, Call) = {

    val postAction = routes.AddIncomeSourceStartDateController.submit(incomeSourceType.key, isAgent, isChange)

    val successCall = routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType.key, isAgent, isChange)

    (
      getBackUrl(isAgent, isChange, incomeSourceType),
      postAction,
      successCall
    )
  }

  def getBackUrl(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Call = {
    if(isAgent)
      agentCalls(isChange)(incomeSourceType)
    else
      individualCalls(isChange)(incomeSourceType)
  }

  private def agentCalls(isChange: Boolean): Map[IncomeSourceType, Call] =
    if (isChange)
      Map(
        SelfEmployment -> routes.CheckBusinessDetailsController.showAgent(),
        UkProperty -> routes.CheckUKPropertyDetailsController.showAgent(),
        ForeignProperty -> routes.ForeignPropertyCheckDetailsController.showAgent()
      )
    else
      Map(
        SelfEmployment -> routes.AddBusinessNameController.showAgent(),
        UkProperty -> routes.AddIncomeSourceController.showAgent(),
        ForeignProperty -> routes.AddIncomeSourceController.showAgent()
      )

  private def individualCalls(isChange: Boolean): Map[IncomeSourceType, Call] = {
    if (isChange)
      Map(
        SelfEmployment -> routes.CheckBusinessDetailsController.show(),
        UkProperty -> routes.CheckUKPropertyDetailsController.show(),
        ForeignProperty -> routes.ForeignPropertyCheckDetailsController.show()
      )
    else
      Map(
        SelfEmployment -> routes.AddBusinessNameController.show(),
        UkProperty -> routes.AddIncomeSourceController.show(),
        ForeignProperty -> routes.AddIncomeSourceController.show()
      )
  }
}

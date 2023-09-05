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

  def show(isAgent: Boolean,
           isChange: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    handleShowRequest(
      incomeSourceType = incomeSourceType,
      isAgent = isAgent,
      isUpdate = isChange
    )
  }

  def submit(isAgent: Boolean,
             isChange: Boolean,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    handleSubmitRequest(
      incomeSourceType = incomeSourceType,
      isAgent = isAgent,
      isUpdate = isChange
    )
  }

  private def handleShowRequest(incomeSourceType: IncomeSourceType,
                                isAgent: Boolean,
                                isUpdate: Boolean)
                               (implicit user: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.startDateMessagesPrefix

    Future.successful(
      if (isEnabled(IncomeSources)) {
        getFilledForm(form(messagesPrefix), incomeSourceType, isUpdate) match {
          case Right(form) =>
            Ok(
              addIncomeSourceStartDate(
                form = form,
                isAgent = isAgent,
                messagesPrefix = messagesPrefix,
                backUrl = getBackUrl(incomeSourceType, isAgent, isUpdate),
                postAction = getPostAction(incomeSourceType, isAgent, isUpdate)
              )
            )
          case Left(ex) =>
            Logger("application").error(s"[AddIncomeSourceStartDateController][handleShowRequest]: " +
              s"Failed to get income source start date from session, reason: ${ex.getMessage}")
            showInternalServerError(isAgent)
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
        form(messagesPrefix).bindFromRequest().fold(
          formWithErrors =>
            BadRequest(
              addIncomeSourceStartDate(
                isAgent = isAgent,
                form = formWithErrors,
                backUrl = getBackUrl(incomeSourceType, isAgent, isUpdate),
                postAction = getPostAction(incomeSourceType, isAgent, isUpdate),
                messagesPrefix = messagesPrefix
              )
            ),
          formData =>
            Redirect(getSuccessUrl(incomeSourceType, isAgent, isUpdate))
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

  private def showInternalServerError(isAgent: Boolean)(implicit user: MtdItUser[_]): Result = {
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler).showInternalServerError()
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
      case (Some(date), true) => Left(new Error(s"Could not parse $date as a LocalDate"))
      case _ => Right(form)
    }
  }

  private def getStartDate(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Option[String] = {
    user.session.get(incomeSourceType.startDateSessionKey)
  }

  private def getPostAction(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): Call = {
    routes.AddIncomeSourceStartDateController.submit(isAgent, isChange, incomeSourceType)
  }

  private def getSuccessUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): Call = {
    routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange, incomeSourceType)
  }

  private def getBackUrl(incomeSourceType: IncomeSourceType,
                         isAgent: Boolean,
                         isChange: Boolean): String = {

    ((isAgent, isChange, incomeSourceType) match {
      case (false, false, SelfEmployment) => routes.AddBusinessNameController.show()
      case (false, true, SelfEmployment) => routes.CheckBusinessDetailsController.show()
      case (true, false, SelfEmployment) => routes.AddBusinessNameController.showAgent()
      case (true, true, SelfEmployment) => routes.CheckBusinessDetailsController.showAgent()
      case (false, false, _) => routes.AddIncomeSourceController.show()
      case (true, false, _) => routes.AddIncomeSourceController.showAgent()
      case (false, true, UkProperty) => routes.CheckUKPropertyDetailsController.show()
      case (true, true, UkProperty) => routes.CheckUKPropertyDetailsController.showAgent()
      case (false, true, ForeignProperty) => routes.ForeignPropertyCheckDetailsController.show()
      case (true, true, ForeignProperty) => routes.ForeignPropertyCheckDetailsController.showAgent()
    }).url
  }
}
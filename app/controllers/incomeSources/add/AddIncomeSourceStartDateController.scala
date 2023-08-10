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
import controllers.routes
import forms.incomeSources.add.{AddBusinessStartDateForm, AddForeignPropertyStartDateForm, AddUKPropertyStartDateForm}
import forms.models.DateFormElement
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatterImpl
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddIncomeSourceStartDate

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddIncomeSourceStartDateController @Inject()(authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   checkSessionTimeout: SessionTimeoutPredicate,
                                                   retrieveNino: NinoPredicate,
                                                   val addIncomeSourceStartDate: AddIncomeSourceStartDate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   addForeignPropertyStartDateForm: AddForeignPropertyStartDateForm,
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

  def showSoleTraderBusiness: Action[AnyContent] = show(SoleTraderBusiness)
  def showSoleTraderBusinessAgent: Action[AnyContent] = showAgent(SoleTraderBusiness)
  def showUKProperty: Action[AnyContent] = show(UKProperty)
  def showUKPropertyAgent: Action[AnyContent] = showAgent(UKProperty)
  def showForeignProperty: Action[AnyContent] = show(ForeignProperty)
  def showForeignPropertyAgent: Action[AnyContent] = showAgent(ForeignProperty)

  def submitSoleTraderBusiness: Action[AnyContent] = submit(SoleTraderBusiness)
  def submitSoleTraderBusinessAgent: Action[AnyContent] = submitAgent(SoleTraderBusiness)
  def submitUKProperty: Action[AnyContent] = submit(UKProperty)
  def submitUKPropertyAgent: Action[AnyContent] = submitAgent(UKProperty)
  def submitForeignProperty: Action[AnyContent] = submit(ForeignProperty)
  def submitForeignPropertyAgent: Action[AnyContent] = submitAgent(ForeignProperty)

  private def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType
      )
  }

  private def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType
            )
        }
  }

  private def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType
      )
  }

  private def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType
            )
        }
  }

  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] = {

    (isEnabled(IncomeSources), getCallsAndMessagesKey(isAgent, incomeSourceType)) match {
      case (false, _) =>
        Future(
          Ok(
            customNotFoundErrorView()
          )
        )
      case (_, (backCall, postAction, _, messagesPrefix, form)) =>
        Future(
          Ok(
            addIncomeSourceStartDate(
              messagesPrefix = messagesPrefix,
              form = form,
              postAction = postAction,
              backUrl = backCall.url,
              isAgent = isAgent
            )
          )
        )
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateController][handleRequest] - Error: ${ex.getMessage}")
      if(isAgent) itvcErrorHandlerAgent.showInternalServerError()
      else itvcErrorHandler.showInternalServerError()
  }

  private def handleSubmitRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                         (implicit user: MtdItUser[_]): Future[Result] = {

    (isEnabled(IncomeSources), getCallsAndMessagesKey(isAgent, incomeSourceType)) match {
      case (false, _) =>
        Future(
          Ok(
            customNotFoundErrorView()
          )
        )
      case (_, (backCall, postAction, redirectCall, messagesPrefix, form)) =>
        form.bindFromRequest().fold(
          formWithErrors =>
            Future(
              BadRequest(
                addIncomeSourceStartDate(
                  messagesPrefix = messagesPrefix,
                  form = formWithErrors,
                  postAction = postAction,
                  backUrl = backCall.url,
                  isAgent = isAgent
                )
              )
            ),
          formData =>
            Future.successful(
              Redirect(redirectCall)
                .addingToSession(
                  (formData, incomeSourceType) match {
                    case (d: DateFormElement, SoleTraderBusiness) =>
                      SessionKeys.addBusinessStartDate -> d.date.toString
                    case (d: DateFormElement, UKProperty) =>
                      SessionKeys.addUkPropertyStartDate -> d.date.toString
                    case (d: DateFormElement, ForeignProperty) =>
                      SessionKeys.foreignPropertyStartDate -> d.date.toString
                  }
                )
            )
        )
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateController][handleSubmitRequest] - Error: ${ex.getMessage}")
      if (isAgent) itvcErrorHandlerAgent.showInternalServerError()
      else itvcErrorHandler.showInternalServerError()
  }

  private def getCallsAndMessagesKey(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                                    (implicit user: MtdItUser[_]): (Call, Call, Call, String, Form[_]) = {

    (isAgent, incomeSourceType) match {
      case (false, SoleTraderBusiness) =>
        (
          controllers.incomeSources.add.routes.AddBusinessNameController.show(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusiness,
          controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.show(),
          soleTraderBusinessMessagesPrefix,
          AddBusinessStartDateForm()
        )
      case (false, UKProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.show(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKProperty,
          controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.show(),
          uKPropertyMessagesPrefix,
          AddUKPropertyStartDateForm()
        )
      case (false, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.show(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignProperty,
          controllers.incomeSources.add.routes.ForeignPropertyStartDateCheckController.show(),
          foreignPropertyMessagesPrefix,
          addForeignPropertyStartDateForm(user, implicitly)
        )
      case (true, SoleTraderBusiness) =>
        (
          controllers.incomeSources.add.routes.AddBusinessNameController.showAgent(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusinessAgent,
          controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.showAgent(),
          soleTraderBusinessMessagesPrefix,
          AddBusinessStartDateForm()
        )
      case (true, UKProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKPropertyAgent,
          controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.showAgent(),
          uKPropertyMessagesPrefix,
          AddUKPropertyStartDateForm()
        )
      case (true, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignPropertyAgent,
          controllers.incomeSources.add.routes.ForeignPropertyStartDateCheckController.showAgent(),
          foreignPropertyMessagesPrefix,
          addForeignPropertyStartDateForm(user, implicitly)
        )
    }
  }

  private lazy val soleTraderBusinessMessagesPrefix = "add-business-start-date"
  private lazy val uKPropertyMessagesPrefix = "incomeSources.add.UKPropertyStartDate"
  private lazy val foreignPropertyMessagesPrefix = "incomeSources.add.foreignProperty.startDate"

  private sealed trait IncomeSourceType
  private case object UKProperty extends IncomeSourceType
  private case object ForeignProperty extends IncomeSourceType
  private case object SoleTraderBusiness extends IncomeSourceType

  def changeBusinessStartDate(): Action[AnyContent] = Action(Ok("Change Business Start Date WIP"))
  def changeBusinessStartDateAgent(): Action[AnyContent] = Action(Ok("Agent Change Business Start Date WIP"))
  def changeUKPropertyStartDate(): Action[AnyContent] = Action(Ok("Change UK Property Start Date WIP"))
  def changeUKPropertyStartDateAgent(): Action[AnyContent] = Action(Ok("Agent Change UK Property Start Date WIP"))
  def changeForeignPropertyStartDate(): Action[AnyContent] = Action(Ok("Change Foreign Property Start Date WIP"))
  def changeForeignPropertyStartDateAgent(): Action[AnyContent] = Action(Ok("Agent Change Foreign Property Start Date WIP"))

}

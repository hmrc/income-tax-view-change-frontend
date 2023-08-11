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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.incomeSources.add.AddIncomeSourceStartDateForm
import forms.models.DateFormElement
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatterImpl
import play.api.Logger
import play.api.i18n.I18nSupport
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

  def showUKProperty: Action[AnyContent] = handleRequestMethod(UKProperty)(isGet = true)
  def showUKPropertyAgent: Action[AnyContent] = handleRequestMethodAgent(UKProperty)(isGet = true)
  def showForeignProperty: Action[AnyContent] = handleRequestMethod(ForeignProperty)(isGet = true)
  def showForeignPropertyAgent: Action[AnyContent] = handleRequestMethodAgent(ForeignProperty)(isGet = true)
  def showSoleTraderBusiness: Action[AnyContent] = handleRequestMethod(SoleTraderBusiness)(isGet = true)
  def showSoleTraderBusinessAgent: Action[AnyContent] = handleRequestMethodAgent(SoleTraderBusiness)(isGet = true)

  def submitUKProperty: Action[AnyContent] = handleRequestMethod(UKProperty)(isGet = false)
  def submitUKPropertyAgent: Action[AnyContent] = handleRequestMethodAgent(UKProperty)(isGet = false)
  def submitForeignProperty: Action[AnyContent] = handleRequestMethod(ForeignProperty)(isGet = false)
  def submitForeignPropertyAgent: Action[AnyContent] = handleRequestMethodAgent(ForeignProperty)(isGet = false)
  def submitSoleTraderBusiness: Action[AnyContent] = handleRequestMethod(SoleTraderBusiness)(isGet = false)
  def submitSoleTraderBusinessAgent: Action[AnyContent] = handleRequestMethodAgent(SoleTraderBusiness)(isGet = false)

  private def handleRequestMethod(incomeSourceType: IncomeSourceType)
                                 (isGet: Boolean): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      if(isGet) {
        handleRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType
        )
      } else {
        handleSubmitRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType
        )
      }
  }

  private def handleRequestMethodAgent(incomeSourceType: IncomeSourceType)
                                      (isGet: Boolean): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            if(isGet) {
              handleRequest(
                isAgent = true,
                incomeSourceType = incomeSourceType
              )
            } else {
              handleSubmitRequest(
                isAgent = false,
                incomeSourceType = incomeSourceType
              )
            }
        }
  }

  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] = {

    (isEnabled(IncomeSources), getCallsAndMessagesKeyPrefix(isAgent, incomeSourceType)) match {
      case (false, _) =>
        Future(
          Ok(
            customNotFoundErrorView()
          )
        )
      case (_, (backCall, postAction, _, messagesPrefix)) =>
        Future(
          Ok(
            addIncomeSourceStartDate(
              form = AddIncomeSourceStartDateForm(messagesPrefix),
              isAgent = isAgent,
              backUrl = backCall.url,
              postAction = postAction,
              messagesPrefix = messagesPrefix
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

    (isEnabled(IncomeSources), getCallsAndMessagesKeyPrefix(isAgent, incomeSourceType)) match {
      case (false, _) =>
        Future(
          Ok(
            customNotFoundErrorView()
          )
        )
      case (_, (backCall, postAction, redirectCall, messagesPrefix)) =>
        AddIncomeSourceStartDateForm(messagesPrefix).bindFromRequest().fold(
          formWithErrors =>
            Future(
              BadRequest(
                addIncomeSourceStartDate(
                  isAgent = isAgent,
                  form = formWithErrors,
                  backUrl = backCall.url,
                  postAction = postAction,
                  messagesPrefix = messagesPrefix
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

  private def getCallsAndMessagesKeyPrefix(isAgent: Boolean, incomeSourceType: IncomeSourceType): (Call, Call, Call, String) = {

    (isAgent, incomeSourceType) match {
      case (false, SoleTraderBusiness) =>
        (
          controllers.incomeSources.add.routes.AddBusinessNameController.show(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusiness,
          controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.show(),
          soleTraderBusinessMessagesPrefix
        )
      case (false, UKProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.show(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKProperty,
          controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.show(),
          uKPropertyMessagesPrefix
        )
      case (false, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.show(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignProperty,
          controllers.incomeSources.add.routes.ForeignPropertyStartDateCheckController.show(),
          foreignPropertyMessagesPrefix
        )
      case (true, SoleTraderBusiness) =>
        (
          controllers.incomeSources.add.routes.AddBusinessNameController.showAgent(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusinessAgent,
          controllers.incomeSources.add.routes.AddBusinessStartDateCheckController.showAgent(),
          soleTraderBusinessMessagesPrefix
        )
      case (true, UKProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKPropertyAgent,
          controllers.incomeSources.add.routes.CheckUKPropertyStartDateController.showAgent(),
          uKPropertyMessagesPrefix
        )
      case (true, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignPropertyAgent,
          controllers.incomeSources.add.routes.ForeignPropertyStartDateCheckController.showAgent(),
          foreignPropertyMessagesPrefix
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

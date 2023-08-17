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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.add.AddIncomeSourceStartDateForm
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

  def showUKProperty: Action[AnyContent] = show(UkProperty)
  def showUKPropertyAgent: Action[AnyContent] = showAgent(UkProperty)
  def showForeignProperty: Action[AnyContent] = show(ForeignProperty)
  def showForeignPropertyAgent: Action[AnyContent] = showAgent(ForeignProperty)
  def showSoleTraderBusiness: Action[AnyContent] = show(SelfEmployment)
  def showSoleTraderBusinessAgent: Action[AnyContent] = showAgent(SelfEmployment)
  def submitUKProperty: Action[AnyContent] = submit(UkProperty)
  def submitUKPropertyAgent: Action[AnyContent] = submitAgent(UkProperty)
  def submitForeignProperty: Action[AnyContent] = submit(ForeignProperty)
  def submitForeignPropertyAgent: Action[AnyContent] = submitAgent(ForeignProperty)
  def submitSoleTraderBusiness: Action[AnyContent] = submit(SelfEmployment)
  def submitSoleTraderBusinessAgent: Action[AnyContent] = submitAgent(SelfEmployment)

  private def show(incomeSourceType: IncomeSourceType): Action[AnyContent] = {
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType
        )
    }
  }

  private def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = {
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleSubmitRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType
        )
    }
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

  private def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType
            )
        }
  }


  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.addIncomeSourceStartDateMessagesPrefix

    if(isEnabled(IncomeSources)) {
      getCalls(isAgent, incomeSourceType) match {
        case (backCall, postAction, _) =>
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
    } else {
      Future(
        Ok(
          customNotFoundErrorView()
        )
      )
    } recover {
      case ex: Exception =>
        Logger("application").error(s"[AddIncomeSourceStartDateController][handleRequest] - Error: ${ex.getMessage}")
        if (isAgent) itvcErrorHandlerAgent.showInternalServerError()
        else itvcErrorHandler.showInternalServerError()
    }
  }

  private def handleSubmitRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                         (implicit user: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.addIncomeSourceStartDateMessagesPrefix

    if(isEnabled(IncomeSources)) {
      getCalls(isAgent, incomeSourceType) match {
        case (backCall, postAction, redirectCall) =>
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
          )
      }
    } else {
      Future(
        Ok(
          customNotFoundErrorView()
        )
      )
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateController][handleSubmitRequest] - Error: ${ex.getMessage}")
      if (isAgent) itvcErrorHandlerAgent.showInternalServerError()
      else itvcErrorHandler.showInternalServerError()
  }

  private def getCalls(isAgent: Boolean, incomeSourceType: IncomeSourceType): (Call, Call, Call) = {

    (isAgent, incomeSourceType) match {
      case (false, SelfEmployment) =>
        (
          controllers.incomeSources.add.routes.AddBusinessNameController.show(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusiness,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showSoleTraderBusiness
        )
      case (false, UkProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.show(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKProperty,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showUKProperty
        )
      case (false, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.show(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignProperty,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showForeignProperty
        )
      case (true, SelfEmployment) =>
        (
          controllers.incomeSources.add.routes.AddBusinessNameController.showAgent(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitSoleTraderBusinessAgent,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showSoleTraderBusinessAgent
        )
      case (true, UkProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitUKPropertyAgent,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showUKPropertyAgent
        )
      case (true, ForeignProperty) =>
        (
          controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent(),
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.submitForeignPropertyAgent,
          controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.showForeignPropertyAgent
        )
    }
  }

  def changeBusinessStartDate(): Action[AnyContent] = Action(Ok("Change Business Start Date WIP"))
  def changeBusinessStartDateAgent(): Action[AnyContent] = Action(Ok("Agent Change Business Start Date WIP"))
  def changeUKPropertyStartDate(): Action[AnyContent] = Action(Ok("Change UK Property Start Date WIP"))
  def changeUKPropertyStartDateAgent(): Action[AnyContent] = Action(Ok("Agent Change UK Property Start Date WIP"))
  def changeForeignPropertyStartDate(): Action[AnyContent] = Action(Ok("Change Foreign Property Start Date WIP"))
  def changeForeignPropertyStartDateAgent(): Action[AnyContent] = Action(Ok("Agent Change Foreign Property Start Date WIP"))
}

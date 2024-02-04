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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{InitialPage, SelfEmployment}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.BusinessNameForm
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.add.AddBusinessName

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessNameController @Inject()(val authorisedFunctions: AuthorisedFunctions,
                                          val addBusinessView: AddBusinessName,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val sessionService: SessionService,
                                          auth: AuthenticatorPredicate)
                                         (implicit val appConfig: FrontendAppConfig,
                                          implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          implicit override val mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyChecker {

  private def getBackUrl(isAgent: Boolean, isChange: Boolean): String = {
    ((isAgent, isChange) match {
      case (false, false) => routes.AddIncomeSourceController.show()
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, false) => routes.AddIncomeSourceController.showAgent()
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  private def getPostAction(isAgent: Boolean, isChange: Boolean): Call = {
    (isAgent, isChange) match {
      case (false, false) => routes.AddBusinessNameController.submit()
      case (false, _) => routes.AddBusinessNameController.submitChange()
      case (_, false) => routes.AddBusinessNameController.submitAgent()
      case (_, _) => routes.AddBusinessNameController.submitChangeAgent()
    }
  }

  private def getRedirect(isAgent: Boolean, isChange: Boolean): Call = {
    (isAgent, isChange) match {
      case (_, false) => routes.AddIncomeSourceStartDateController.show(isAgent, isChange = false, SelfEmployment)
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }
  }

  def show(): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleRequest(
        isAgent = false,
        backUrl = getBackUrl(isAgent = false, isChange = false),
        isChange = false
      )
  }

  def showAgent(): Action[AnyContent] =
    auth.authenticatedAction(isAgent = true) {
      implicit mtdItUser =>
        handleRequest(
          isAgent = true,
          backUrl = getBackUrl(isAgent = true, isChange = false),
          isChange = false
        )
    }

  def handleRequest(isAgent: Boolean, backUrl: String, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withSessionData(JourneyType(Add, SelfEmployment), journeyState = InitialPage) { sessionData =>
      val businessNameOpt: Option[String] = sessionData.addIncomeSourceData.flatMap(_.businessName)
      val filledForm: Form[BusinessNameForm] = businessNameOpt.fold(BusinessNameForm.form)(name =>
        BusinessNameForm.form.fill(BusinessNameForm(name)))
      val submitAction: Call = getPostAction(isAgent, isChange)

      Future.successful {
        Ok(addBusinessView(filledForm, isAgent, submitAction, backUrl, useFallbackLink = true))
      }
    }
  }.recover {
    case ex =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"[AddBusinessNameController][handleRequest] ${ex.getMessage} - ${ex.getCause}")
      errorHandler.showInternalServerError()
  }

  def submit: Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit request =>
      handleSubmitRequest(isAgent = false, isChange = false)
  }

  def submitAgent: Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleSubmitRequest(isAgent = true, isChange = false)
  }

  def submitChange: Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit request =>
      handleSubmitRequest(isAgent = false, isChange = true)
  }

  def submitChangeAgent: Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleSubmitRequest(isAgent = true, isChange = true)

  }

  def handleSubmitRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withSessionData(JourneyType(Add, SelfEmployment), InitialPage) { sessionData =>

      val businessTradeOpt: Option[String] = sessionData.addIncomeSourceData.flatMap(_.businessTrade)

      BusinessNameForm.checkBusinessNameWithTradeName(BusinessNameForm.form.bindFromRequest(), businessTradeOpt).fold(
        formWithErrors =>
          Future.successful {
            BadRequest(addBusinessView(formWithErrors,
              isAgent,
              getPostAction(isAgent, isChange),
              getBackUrl(isAgent, isChange),
              useFallbackLink = true))
          },
        formData => {
          sessionService.setMongoData(
            sessionData.addIncomeSourceData match {
              case Some(_) =>
                sessionData.copy(
                  addIncomeSourceData =
                    sessionData.addIncomeSourceData.map(
                      _.copy(
                        businessName = Some(formData.name)
                      )
                    )
                )
              case None =>
                sessionData.copy(
                  addIncomeSourceData =
                    Some(
                      AddIncomeSourceData(
                        businessName = Some(formData.name)
                      )
                    )
                )
            }
          ) flatMap {
            case true  => Future.successful(Redirect(getRedirect(isAgent, isChange)))
            case false => Future.failed(new Exception("Mongo update call was not acknowledged"))
          }
        }
      )
    }
  }.recover {
  case ex =>
    Logger("application")
      .error(s"[AddBusinessNameController][handleSubmitRequest] - ${ex.getMessage} - ${ex.getCause}")
    val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    errorHandler.showInternalServerError()
}

def changeBusinessName(): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
  implicit user =>
    handleRequest(
      isAgent = false,
      backUrl = getBackUrl(isAgent = false, isChange = true),
      isChange = true
    )
}

def changeBusinessNameAgent(): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
  implicit mtdItUser =>
    handleRequest(
      isAgent = true,
      backUrl = getBackUrl(isAgent = true, isChange = true),
      isChange = true
    )
}

}

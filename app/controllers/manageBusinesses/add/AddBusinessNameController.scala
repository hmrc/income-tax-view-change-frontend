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

package controllers.manageBusinesses.add

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.SelfEmployment
import enums.InitialPage
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.manageBusinesses.add.BusinessNameForm
import models.core.{Mode, NormalMode}
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.add.AddBusinessName

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessNameController @Inject()(val authActions: AuthActions,
                                          val addBusinessView: AddBusinessName,
                                          val sessionService: SessionService)
                                         (implicit val appConfig: FrontendAppConfig,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          val mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  private def getBackUrl(isAgent: Boolean, mode: Mode): String = {
    ((isAgent, mode) match {
      case (false, NormalMode) => controllers.manageBusinesses.routes.ManageYourBusinessesController.show()
      case (_, NormalMode) => controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent()
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  private def getPostAction(isAgent: Boolean, mode: Mode): Call = {
    if(isAgent) {
      routes.AddBusinessNameController.submitAgent(mode)
    } else {
      routes.AddBusinessNameController.submit(mode)

    }
    }

  private def getRedirect(isAgent: Boolean, mode: Mode): Call = {
    (isAgent, mode) match {
      case (_, NormalMode) => routes.AddIncomeSourceStartDateController.show(isAgent, mode = NormalMode, SelfEmployment)
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }
  }

  def show(mode: Mode): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(
        isAgent = false,
        backUrl = getBackUrl(false, mode),
        mode = mode
      )
  }

  def showAgent(mode: Mode): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit user =>
      handleRequest(
        isAgent = true,
        backUrl = getBackUrl(true, mode),
        mode = mode
      )
  }

  def handleRequest(isAgent: Boolean, backUrl: String, mode: Mode)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, SelfEmployment), journeyState = InitialPage) { sessionData =>
      val businessNameOpt: Option[String] = sessionData.addIncomeSourceData.flatMap(_.businessName)
      val filledForm: Form[BusinessNameForm] = businessNameOpt.fold(BusinessNameForm.form)(name =>
        BusinessNameForm.form.fill(BusinessNameForm(name)))
      val submitAction: Call = getPostAction(isAgent, mode)

      Future.successful {
        Ok(addBusinessView(filledForm, isAgent, submitAction, backUrl))
      }
    }
  }.recover {
    case ex =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      errorHandler.showInternalServerError()
  }

  def submit(mode: Mode): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit request =>
      handleSubmitRequest(false, mode)(implicitly, itvcErrorHandler)
  }

  def submitAgent(mode: Mode): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit request =>
      handleSubmitRequest(true, mode)(implicitly, itvcErrorHandlerAgent)
  }

  def handleSubmitRequest(isAgent: Boolean, mode: Mode)(implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, SelfEmployment), InitialPage) { sessionData =>

      val businessTradeOpt: Option[String] = sessionData.addIncomeSourceData.flatMap(_.businessTrade)

      BusinessNameForm.checkBusinessNameWithTradeName(BusinessNameForm.form.bindFromRequest(), businessTradeOpt).fold(
        formWithErrors =>
          Future.successful {
            BadRequest(addBusinessView(formWithErrors,
              isAgent,
              getPostAction(isAgent, mode),
              getBackUrl(isAgent, mode)))
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
            case true  => Future.successful(Redirect(getRedirect(isAgent, mode)))
            case false => Future.failed(new Exception("Mongo update call was not acknowledged"))
          }
        }
      )
    }
  }.recover {
  case ex =>
    Logger("application")
      .error(s"${ex.getMessage} - ${ex.getCause}")
    errorHandler.showInternalServerError()
}

}

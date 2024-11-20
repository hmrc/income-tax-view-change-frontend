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
import enums.IncomeSourceJourney.{InitialPage, SelfEmployment}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.BusinessNameForm
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

  private def getBackUrl(isAgent: Boolean, isChange: Boolean): String = {
    ((isAgent, isChange) match {
      case (false, false) => controllers.manageBusinesses.routes.ManageYourBusinessesController.show()
      case (_, false) => controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent()
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  private def getPostAction(isAgent: Boolean, isChange: Boolean): Call = {
    if(isAgent) {
      routes.AddBusinessNameController.submitAgent(isChange)
    } else {
      routes.AddBusinessNameController.submit(isChange)

    }
    }

  private def getRedirect(isAgent: Boolean, isChange: Boolean): Call = {
    (isAgent, isChange) match {
      case (_, false) => routes.AddIncomeSourceStartDateController.show(isAgent, isChange = false, SelfEmployment)
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }
  }

  def show(isChange: Boolean): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(
        isAgent = false,
        backUrl = getBackUrl(false, isChange),
        isChange = isChange
      )
  }

  def showAgent(isChange: Boolean): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit user =>
      handleRequest(
        isAgent = true,
        backUrl = getBackUrl(true, isChange),
        isChange = isChange
      )
  }

  def handleRequest(isAgent: Boolean, backUrl: String, isChange: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    withSessionData(JourneyType(Add, SelfEmployment), journeyState = InitialPage) { sessionData =>
      val businessNameOpt: Option[String] = sessionData.addIncomeSourceData.flatMap(_.businessName)
      val filledForm: Form[BusinessNameForm] = businessNameOpt.fold(BusinessNameForm.form)(name =>
        BusinessNameForm.form.fill(BusinessNameForm(name)))
      val submitAction: Call = getPostAction(isAgent, isChange)

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

  def submit(isChange: Boolean): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit request =>
      handleSubmitRequest(false, isChange)(implicitly, itvcErrorHandler)
  }

  def submitAgent(isChange: Boolean): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit request =>
      handleSubmitRequest(true, isChange)(implicitly, itvcErrorHandlerAgent)
  }

  def handleSubmitRequest(isAgent: Boolean, isChange: Boolean)(implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
    withSessionData(JourneyType(Add, SelfEmployment), InitialPage) { sessionData =>

      val businessTradeOpt: Option[String] = sessionData.addIncomeSourceData.flatMap(_.businessTrade)

      BusinessNameForm.checkBusinessNameWithTradeName(BusinessNameForm.form.bindFromRequest(), businessTradeOpt).fold(
        formWithErrors =>
          Future.successful {
            BadRequest(addBusinessView(formWithErrors,
              isAgent,
              getPostAction(isAgent, isChange),
              getBackUrl(isAgent, isChange)))
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
      .error(s"${ex.getMessage} - ${ex.getCause}")
    errorHandler.showInternalServerError()
}

}

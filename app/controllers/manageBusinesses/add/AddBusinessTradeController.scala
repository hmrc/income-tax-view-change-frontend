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
import enums.BeforeSubmissionPage
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.manageBusinesses.add.BusinessTradeForm
import models.core.{Mode, NormalMode}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.add.AddBusinessTrade

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AddBusinessTradeController @Inject()(val authActions: AuthActions,
                                           val addBusinessTradeView: AddBusinessTrade,
                                           val sessionService: SessionService)
                                          (implicit val appConfig: FrontendAppConfig,
                                           val itvcErrorHandler: ItvcErrorHandler,
                                           val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                           val mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  private def getBackURL(isAgent: Boolean, mode: Mode): String = {
    ((isAgent, mode) match {
      case (_, NormalMode) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, mode = NormalMode, SelfEmployment)
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  private def getSuccessURL(isAgent: Boolean, mode: Mode): String = {
    ((isAgent, mode) match {
      case (false, NormalMode) => routes.AddBusinessAddressController.show(mode)
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, NormalMode) => routes.AddBusinessAddressController.showAgent(mode)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  private def getPostAction(isAgent: Boolean, mode: Mode): Call = if(isAgent) {
    controllers.manageBusinesses.add.routes.AddBusinessTradeController.submitAgent(mode)
  } else {
    controllers.manageBusinesses.add.routes.AddBusinessTradeController.submit(mode)
  }

  def show(mode: Mode): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(false, mode)
  }

  def showAgent(mode: Mode): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit user =>
      handleRequest(true, mode)
  }

  def handleRequest(isAgent: Boolean, mode: Mode)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>

      val businessTradeOpt = sessionData.addIncomeSourceData.flatMap(_.businessTrade)
      val filledForm = businessTradeOpt.fold(BusinessTradeForm.form)(businessTrade =>
        BusinessTradeForm.form.fill(BusinessTradeForm(businessTrade)))
      val backURL = getBackURL(isAgent, mode)
      val postAction = getPostAction(isAgent, mode)

      Future.successful {
        Ok(addBusinessTradeView(filledForm, postAction, isAgent, backURL))
      }
    }
  }.recover {
    case ex =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
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
    withSessionData(IncomeSourceJourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>
      val businessNameOpt = sessionData.addIncomeSourceData.flatMap(_.businessName)

      BusinessTradeForm
        .checkBusinessTradeWithBusinessName(BusinessTradeForm.form.bindFromRequest(), businessNameOpt).fold(
          formWithErrors =>
            Future.successful {
              BadRequest(
                addBusinessTradeView(
                  businessTradeForm = formWithErrors,
                  postAction = getPostAction(isAgent, mode),
                  isAgent = isAgent,
                  backURL = getBackURL(isAgent, mode)
                )
              )
            },
          validForm =>
            sessionService.setMongoData(
              sessionData.copy(
                addIncomeSourceData =
                  sessionData.addIncomeSourceData.map(
                    _.copy(
                      businessTrade = Some(validForm.trade)
                    )
                  )
              )
            ) flatMap {
              case true  => Future.successful(Redirect(getSuccessURL(isAgent, mode)))
              case false => Future.failed(new Exception("Mongo update call was not acknowledged"))
            }
        )
    }
  }.recover {
    case ex =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      errorHandler.showInternalServerError()
  }
}

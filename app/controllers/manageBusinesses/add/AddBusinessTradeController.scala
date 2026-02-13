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
import views.html.manageBusinesses.add.AddBusinessTradeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class AddBusinessTradeController @Inject()(val authActions: AuthActions,
                                           val addBusinessTradeView: AddBusinessTradeView,
                                           val sessionService: SessionService)
                                          (implicit val appConfig: FrontendAppConfig,
                                           val itvcErrorHandler: ItvcErrorHandler,
                                           val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                           val mcc: MessagesControllerComponents,
                                           val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  private def getBackURL(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean): String = {
    ((isAgent, mode) match {
      case (_, NormalMode) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, mode = NormalMode, SelfEmployment, isTriggeredMigration)
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment, isTriggeredMigration)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment, isTriggeredMigration)
    }).url
  }

  private def getSuccessURL(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean): String = {
    ((isAgent, mode) match {
      case (false, NormalMode) => routes.AddBusinessAddressController.show(mode, isTriggeredMigration)
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment, isTriggeredMigration)
      case (_, NormalMode) => routes.AddBusinessAddressController.showAgent(mode, isTriggeredMigration)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment, isTriggeredMigration)
    }).url
  }

  private def getPostAction(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean): Call = if(isAgent) {
    controllers.manageBusinesses.add.routes.AddBusinessTradeController.submitAgent(mode, isTriggeredMigration)
  } else {
    controllers.manageBusinesses.add.routes.AddBusinessTradeController.submit(mode, isTriggeredMigration)
  }

  def show(mode: Mode, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDIndividual(isTriggeredMigration).async {
    implicit user =>
      handleRequest(isAgent = false, mode, isTriggeredMigration)
  }

  def showAgent(mode: Mode, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient(isTriggeredMigration).async  {
    implicit user =>
      handleRequest(isAgent = true, mode, isTriggeredMigration)
  }

  def handleRequest(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>

      val businessTradeOpt = sessionData.addIncomeSourceData.flatMap(_.businessTrade)
      val filledForm = businessTradeOpt.fold(BusinessTradeForm.form)(businessTrade =>
        BusinessTradeForm.form.fill(BusinessTradeForm(businessTrade)))
      val backURL = getBackURL(isAgent, mode, isTriggeredMigration)
      val postAction = getPostAction(isAgent, mode, isTriggeredMigration)

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

  def submit(mode: Mode, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDIndividual(isTriggeredMigration).async {
    implicit request =>
      handleSubmitRequest(isAgent = false, mode, isTriggeredMigration)(implicitly, itvcErrorHandler)
  }

  def submitAgent(mode: Mode, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient(isTriggeredMigration).async  {
    implicit request =>
      handleSubmitRequest(isAgent = true, mode, isTriggeredMigration)(implicitly, itvcErrorHandlerAgent)
  }

  def handleSubmitRequest(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean)(implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>
      val businessNameOpt = sessionData.addIncomeSourceData.flatMap(_.businessName)

      BusinessTradeForm
        .checkBusinessTradeWithBusinessName(BusinessTradeForm.form.bindFromRequest(), businessNameOpt).fold(
          formWithErrors =>
            Future.successful {
              BadRequest(
                addBusinessTradeView(
                  businessTradeForm = formWithErrors,
                  postAction = getPostAction(isAgent, mode, isTriggeredMigration),
                  isAgent = isAgent,
                  backURL = getBackURL(isAgent, mode, isTriggeredMigration)
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
              case true  => Future.successful(Redirect(getSuccessURL(isAgent, mode, isTriggeredMigration)))
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

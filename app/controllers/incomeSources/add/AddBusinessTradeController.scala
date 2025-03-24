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
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.{BeforeSubmissionPage, SelfEmployment}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.incomeSources.add.BusinessTradeForm
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.add.AddBusinessTrade

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
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyChecker {

  private def getBackURL(isAgent: Boolean, isChange: Boolean): String = {
    ((isAgent, isChange) match {
      case (_, false) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange = false, SelfEmployment)
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  def getPostAction(isAgent: Boolean, isChange: Boolean) = if(isAgent) {
    controllers.incomeSources.add.routes.AddBusinessTradeController.submitAgent(isChange)
  } else {
    controllers.incomeSources.add.routes.AddBusinessTradeController.submit(isChange)
  }

  private def getSuccessURL(isAgent: Boolean, isChange: Boolean): String = {
    ((isAgent, isChange) match {
      case (false, false) => routes.AddBusinessAddressController.show(isChange)
      case (false, _) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, false) => routes.AddBusinessAddressController.showAgent(isChange)
      case (_, _) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
    }).url
  }

  def show(isChange: Boolean): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(false, isChange)
  }

  def showAgent(isChange: Boolean): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
    implicit user =>
      handleRequest(true, isChange)
  }

  def handleRequest(isAgent: Boolean,
                    isChange: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>
      val businessTradeOpt = sessionData.addIncomeSourceData.flatMap(_.businessTrade)
      val filledForm = businessTradeOpt.fold(BusinessTradeForm.form)(businessTrade =>
        BusinessTradeForm.form.fill(BusinessTradeForm(businessTrade)))
      val backURL = getBackURL(isAgent, isChange)

      Future.successful {
        Ok(addBusinessTradeView(filledForm, getPostAction(isAgent, isChange), isAgent, backURL))
      }
    }
  }.recover {
    case ex =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
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

  def handleSubmitRequest(isAgent: Boolean, isChange: Boolean)
                         (implicit user: MtdItUser[_], errorHandler: ShowInternalServerError): Future[Result] = {
    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Add, SelfEmployment), BeforeSubmissionPage) { sessionData =>
      val businessNameOpt = sessionData.addIncomeSourceData.flatMap(_.businessName)

      BusinessTradeForm
        .checkBusinessTradeWithBusinessName(BusinessTradeForm.form.bindFromRequest(), businessNameOpt).fold(
          formWithErrors =>
            Future.successful {
              BadRequest(
                addBusinessTradeView(
                  businessTradeForm = formWithErrors,
                  postAction = getPostAction(isAgent, isChange),
                  isAgent = isAgent,
                  backURL = getBackURL(isAgent, isChange)
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
              case true  => Future.successful(Redirect(getSuccessURL(isAgent, isChange)))
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

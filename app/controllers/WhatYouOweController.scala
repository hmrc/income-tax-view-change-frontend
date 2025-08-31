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

package controllers

import auth.MtdItUser
import auth.authV2.AuthActions
import config._
import config.featureswitch._
import controllers.routes.{ChargeSummaryController, CreditAndRefundController, NotMigratedUserController, PaymentController, TaxYearSummaryController}
import enums.GatewayPage.WhatYouOwePage
import forms.utils.SessionKeys.gatewayPage
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateServiceInterface, WhatYouOweService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.WhatYouOwe

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatYouOweController @Inject()(val authActions: AuthActions,
                                     val whatYouOweService: WhatYouOweService,
                                     val itvcErrorHandler: ItvcErrorHandler,
                                     val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                     implicit val dateService: DateServiceInterface,
                                     whatYouOwe: WhatYouOwe
                                    )(implicit val appConfig: FrontendAppConfig,
                                      val mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with FeatureSwitching {

  def handleRequest(backUrl: String,
                    itvcErrorHandler: ShowInternalServerError,
                    isAgent: Boolean,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    whatYouOweService.createWhatYouOweViewModel(backUrl, origin, getCreditAndRefundUrl, getTaxYearSummaryUrl(origin), getAdjustPoaUrl, getChargeSummaryUrl, getPaymentHandOffUrl(origin)) map {
      case Some(viewModel) =>
        Ok(whatYouOwe(viewModel, origin))
          .addingToSession(gatewayPage -> WhatYouOwePage.name)
      case None =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" + "Failed to create WhatYouOweViewModel")
        itvcErrorHandler.showInternalServerError()
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"Error received while getting WhatYouOwe page details: ${ex.getMessage} - ${ex.getCause}")
      itvcErrorHandler.showInternalServerError()
  }

  def show(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(
        backUrl = controllers.routes.HomeController.show(origin).url,
        itvcErrorHandler = itvcErrorHandler,
        isAgent = false,
        origin = origin
      )
  }

  def showAgent: Action[AnyContent] = authActions.asMTDPrimaryAgent.async {
    implicit mtdItUser =>
      handleRequest(
        backUrl = controllers.routes.HomeController.showAgent().url,
        itvcErrorHandler = itvcErrorHandlerAgent,
        isAgent = true
      )
  }

  private def getCreditAndRefundUrl(implicit user: MtdItUser[_]): String = (user.isAgent() match {
    case true if user.incomeSources.yearOfMigration.isDefined  => CreditAndRefundController.showAgent()
    case true                                                  => NotMigratedUserController.showAgent()
    case false if user.incomeSources.yearOfMigration.isDefined => CreditAndRefundController.show()
    case false                                                 => NotMigratedUserController.show()
  }).url

  private def getTaxYearSummaryUrl(origin: Option[String])(implicit user: MtdItUser[_]): Int => String = {
    if (user.isAgent()) TaxYearSummaryController.renderAgentTaxYearSummaryPage(_).url
    else                TaxYearSummaryController.renderTaxYearSummaryPage(_, origin).url
  }

  private def getAdjustPoaUrl(implicit user: MtdItUser[_]): String = controllers.claimToAdjustPoa.routes.AmendablePoaController.show(user.isAgent()).url

  private def getChargeSummaryUrl(implicit user: MtdItUser[_]): (Int, String, Boolean, Option[String]) => String = (taxYearEnd: Int, transactionId: String, isInterest: Boolean, origin: Option[String]) => {
    if (user.isAgent()) ChargeSummaryController.showAgent(taxYearEnd, transactionId, isInterest).url
    else                ChargeSummaryController.show(taxYearEnd, transactionId, isInterest, origin).url
  }

  private def getPaymentHandOffUrl(origin: Option[String]): Long => String = PaymentController.paymentHandoff(_, origin).url
}

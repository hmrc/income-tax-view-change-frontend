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

package financials.controllers

import common.auth.{AuthActions, MtdItUser}
import common.config.featureswitch.FeatureSwitching
import common.config.*
import common.enums.GatewayPage.WhatYouOwePage
import common.models.admin.SelfServeTimeToPayR17
import common.services.DateServiceInterface
import financials.controllers.claimToAdjustPoa.routes as claimToAdjustPoaRoutes
import financials.services.WhatYouOweService
import financials.forms.utils.SessionKeys.gatewayPage
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import financials.views.html.WhatYouOweView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class WhatYouOweController @Inject()(val authActions: AuthActions,
                                     val whatYouOweService: WhatYouOweService,
                                     val itvcErrorHandler: ItvcErrorHandler,
                                     val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                     implicit val dateService: DateServiceInterface,
                                     whatYouOwe: WhatYouOweView
                                    )(implicit val appConfig: FrontendAppConfig,
                                      val mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with FeatureSwitching {

  def handleRequest(backUrl: String,
                    itvcErrorHandler: ShowInternalServerError,
                    isAgent: Boolean,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    whatYouOweService.createWhatYouOweViewModel(backUrl, getMoneyInYourAccountUrl, getTaxYearSummaryUrl(origin), getAdjustPoaUrl, getChargeSummaryUrl, getPaymentHandOffUrl(origin)) map {
      case Some(viewModel) =>
        Ok(whatYouOwe(viewModel, origin, isEnabled(SelfServeTimeToPayR17)))
          .addingToSession(gatewayPage -> WhatYouOwePage.name)
      case None =>
        Logger("application").error(s"${if (isAgent) "[Agent]" else ""}" + "Failed to create WhatYouOweViewModel")
        itvcErrorHandler.showInternalServerError()
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"${if (isAgent) "[Agent]" else ""}" +
        s"Error received while getting WhatYouOwe page details: ${ex.getMessage} - ${ex.getCause}")
      itvcErrorHandler.showInternalServerError()
  }

  def show(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      handleRequest(
        backUrl = appConfig.individualHomeUrlWithOrigin(origin),
        itvcErrorHandler = itvcErrorHandler,
        isAgent = false,
        origin = origin
      )
  }

  def showAgent: Action[AnyContent] = authActions.asMTDPrimaryAgent().async {
    implicit mtdItUser =>
      handleRequest(
        backUrl = appConfig.homePageUrl(isAgent = true),
        itvcErrorHandler = itvcErrorHandlerAgent,
        isAgent = true
      )
  }

  private def getMoneyInYourAccountUrl(implicit user: MtdItUser[_]): String = (user.isAgent match {
    case true if user.incomeSources.yearOfMigration.isDefined  => routes.MoneyInYourAccountController.showAgent()
    case true                                                  => routes.NotMigratedUserController.showAgent()
    case false if user.incomeSources.yearOfMigration.isDefined => routes.MoneyInYourAccountController.show()
    case false                                                 => routes.NotMigratedUserController.show()
  }).url

  private def getTaxYearSummaryUrl(origin: Option[String])(implicit user: MtdItUser[_]): Int => String = {
    //ToDo update this when the ReturnsFrontend feature switch is built
    val returnsFrontendEnabled: Boolean = false
    if (user.isAgent) appConfig.returnsTaxYearSummaryAgentUrl(_, None, returnsFrontendEnabled)
    else appConfig.returnsTaxYearSummaryIndividualUrl(_, origin, None, returnsFrontendEnabled)
  }

  private def getAdjustPoaUrl(implicit user: MtdItUser[_]): String = claimToAdjustPoaRoutes.AmendablePoaController.show(user.isAgent).url

  private def getChargeSummaryUrl(implicit user: MtdItUser[_]): (Int, String, Boolean, Option[String]) => String = (taxYearEnd: Int, transactionId: String, isInterest: Boolean, origin: Option[String]) => {
    if (user.isAgent) routes.ChargeSummaryController.showAgent(taxYearEnd, transactionId, isInterest).url
    else                routes.ChargeSummaryController.show(taxYearEnd, transactionId, isInterest, origin).url
  }

  private def getPaymentHandOffUrl(origin: Option[String]): Long => String = routes.PaymentController.makingPayment(_, origin).url
}

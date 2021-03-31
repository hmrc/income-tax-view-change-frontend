/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers.agent

import config.featureswitch.{AgentViewer, FeatureSwitching, NewFinancialDetailsApi, Payment}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.NotFoundException
import controllers.agent.utils.SessionKeys
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.financialDetails.{Charge, FinancialDetailsModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.FinancialDetailsService
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.agent.ChargeSummary

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChargeSummaryController @Inject()(chargeSummary: ChargeSummary,
                                        val authorisedFunctions: AuthorisedFunctions,
                                        financialDetailsService: FinancialDetailsService
                                        )(implicit val appConfig: FrontendAppConfig,
                                          val languageUtils: LanguageUtils,
                                          mcc: MessagesControllerComponents,
                                          dateFormatter: ImplicitDateFormatterImpl,
                                          implicit val ec: ExecutionContext,
                                          val itvcErrorHandler: ItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  private def view(charge: Charge, backLocation: Option[String], taxYear: Int)(implicit request: Request[_]): Html = {
    chargeSummary.apply(
      charge = charge,
      implicitDateFormatter = dateFormatter,
      paymentEnabled = isEnabled(Payment),
      backUrl = backUrl(backLocation, taxYear)
    )
  }

  def showChargeSummary(taxYear: Int, chargeId: String): Action[AnyContent] =
    Authenticated.async { implicit request =>
      implicit user =>
        if (isEnabled(AgentViewer)) {
          if (isEnabled(NewFinancialDetailsApi)) {
            financialDetailsService.getFinancialDetails(taxYear, getClientNino).map {
              case success: FinancialDetailsModel if success.financialDetails.exists(_.transactionId == chargeId) =>
                val backLocation = request.session.get(SessionKeys.chargeSummaryBackPage)
                Ok(view(success.financialDetails.find(_.transactionId == chargeId).get, backLocation, taxYear))
              //Should not happen unless url is changed manually so redirect to home
              case _: FinancialDetailsModel =>
                Logger.warn(s"[ChargeSummaryController][showChargeSummary] Transaction id not found for tax year $taxYear")
                Redirect(controllers.agent.routes.HomeController.show())
              case _ =>
                Logger.warn("[ChargeSummaryController][showChargeSummary] Invalid response from financial transactions")
                itvcErrorHandler.showInternalServerError()
            }
          }
          else {
            Future.successful(Redirect(controllers.agent.routes.HomeController.show().url))
          }
        } else {
          Future.failed(new NotFoundException("[HomeController][home] - Agent viewer is disabled"))
        }
    }

  def backUrl(backLocation: Option[String], taxYear: Int): String = backLocation match {

      //need to be uncommented after tax year overview page payment tab be built
//    case Some("taxYearOverview") => controllers.agent.routes.CalculationController.renderTaxYearOverviewPage(taxYear).url + "#payments"
    //need to be uncommented after what you owe page be built
//    case Some("paymentDue") => controllers.agent.routes.PaymentDueController.viewPaymentsDue().url
    case _ => controllers.agent.routes.HomeController.show().url
  }

}

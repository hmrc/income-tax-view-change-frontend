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

package controllers

import config.featureswitch.{FeatureSwitching, NewFinancialDetailsApi, Payment}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.financialDetails.{DocumentDetail, FinancialDetailsModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.FinancialDetailsService
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.chargeSummary

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChargeSummaryController @Inject()(authenticate: AuthenticationPredicate,
                                        checkSessionTimeout: SessionTimeoutPredicate,
                                        retrieveNino: NinoPredicate,
                                        retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                        financialDetailsService: FinancialDetailsService,
                                        itvcErrorHandler: ItvcErrorHandler)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val languageUtils: LanguageUtils,
                                        mcc: MessagesControllerComponents,
                                        val executionContext: ExecutionContext,
                                        dateFormatter: ImplicitDateFormatterImpl)
  extends BaseController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  private def view(documentDetail: DocumentDetail, dueDate: Option[LocalDate], backLocation: Option[String], taxYear: Int)(implicit request: Request[_]) = {
    chargeSummary(documentDetail, dueDate, dateFormatter, isEnabled(Payment), backUrl(backLocation, taxYear))
  }

  def showChargeSummary(taxYear: Int, id: String): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
      implicit user =>
        if (isEnabled(NewFinancialDetailsApi)) {
          financialDetailsService.getFinancialDetails(taxYear, user.nino).map {
            case success: FinancialDetailsModel if success.documentDetails.exists(_.transactionId == id) =>
              val backLocation = user.session.get(SessionKeys.chargeSummaryBackPage)
              val documentDetail = success.documentDetails.find(_.transactionId == id).get
              Ok(view(
                documentDetail = documentDetail,
                dueDate = success.getDueDateFor(documentDetail),
                backLocation = backLocation,
                taxYear = taxYear
              ))
            //Should not happen unless url is changed manually so redirect to home
            case _: FinancialDetailsModel =>
              Logger.warn(s"[ChargeSummaryController][showChargeSummary] Transaction id not found for tax year $taxYear")
              Redirect(controllers.routes.HomeController.home().url)
            case _ =>
              Logger.warn("[ChargeSummaryController][showChargeSummary] Invalid response from financial transactions")
              itvcErrorHandler.showInternalServerError()
          }
        }
        else Future.successful(Redirect(controllers.routes.HomeController.home().url))
    }

  def backUrl(backLocation: Option[String], taxYear: Int): String = backLocation match {
    case Some("taxYearOverview") => controllers.routes.CalculationController.renderTaxYearOverviewPage(taxYear).url + "#payments"
    case Some("paymentDue") => controllers.routes.PaymentDueController.viewPaymentsDue().url
    case _ => controllers.routes.HomeController.home().url
  }
}

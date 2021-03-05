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

import config.{FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, NewFinancialDetailsApi, Payment}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import javax.inject.Inject
import models.financialDetails.{Charge, FinancialDetailsModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import services.FinancialDetailsService
import uk.gov.hmrc.play.language.LanguageUtils
import views.html.chargeSummary

import scala.concurrent.{ExecutionContext, Future}

class ChargeSummaryController@Inject()(authenticate: AuthenticationPredicate,
																			 checkSessionTimeout: SessionTimeoutPredicate,
																			 retrieveNino: NinoPredicate,
																			 retrieveIncomeSources: IncomeSourceDetailsPredicate,
																			 financialDetailsService: FinancialDetailsService,
																			 itvcErrorHandler: ItvcErrorHandler)(implicit val appConfig: FrontendAppConfig,
																																					 val languageUtils: LanguageUtils,
																																					 mcc: MessagesControllerComponents,
																																					 val executionContext: ExecutionContext,
																																					 dateFormatter: ImplicitDateFormatterImpl)
	extends BaseController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

	private def view(charge: Charge)(implicit request: Request[_]) = {
		chargeSummary(charge, dateFormatter, isEnabled(Payment))
	}

	def showChargeSummary(taxYear: Int, chargeId: String): Action[AnyContent] =
		(checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
			implicit user =>
				if (isEnabled(NewFinancialDetailsApi)) {
					financialDetailsService.getFinancialDetails(taxYear).map {
						case success: FinancialDetailsModel if success.financialDetails.exists(_.transactionId == chargeId) =>
								Ok(view(success.financialDetails.find(_.transactionId == chargeId).get))
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
}

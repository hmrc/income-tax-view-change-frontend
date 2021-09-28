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

import audit.AuditingService
import audit.models.WhatYouOweResponseAuditModel
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{FeatureSwitching, TxmEventsApproved}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.agent.utils.SessionKeys
import models.financialDetails.WhatYouOweChargesList
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import services.{IncomeSourceDetailsService, WhatYouOweService}
import views.html.agent.WhatYouOwe

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class WhatYouOweController @Inject()(whatYouOweView: WhatYouOwe,
                                     whatYouOweService: WhatYouOweService,
                                     incomeSourceDetailsService: IncomeSourceDetailsService,
                                     auditingService: AuditingService,
                                     implicit val appConfig: FrontendAppConfig,
                                     val authorisedFunctions: FrontendAuthorisedFunctions
                                    )(implicit mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext,
                                      itvcErrorHandler: ItvcErrorHandler
                                    ) extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private def view(charge: WhatYouOweChargesList, taxYear: Int)(implicit user: MtdItUser[_]): Html = {
    whatYouOweView.apply(
      chargesList = charge,
      currentTaxYear = taxYear,
      backUrl = backUrl,
      user.saUtr
    )
  }

  def show: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
						whatYouOweService.getWhatYouOweChargesList().map {
							whatYouOweChargesList => {
								if (isEnabled(TxmEventsApproved)) {
									auditingService.extendedAudit(WhatYouOweResponseAuditModel(mtdItUser, whatYouOweChargesList))
								}

								Ok(view(whatYouOweChargesList, mtdItUser.incomeSources.getCurrentTaxEndYear)
								).addingToSession(SessionKeys.chargeSummaryBackPage -> "paymentDue")
							}
						} recover {
							case ex: Exception =>
								Logger.error(s"Error received while getting agent what you page details: ${ex.getMessage}")
								itvcErrorHandler.showInternalServerError()
						}
        }
  }

  lazy val backUrl: String = controllers.agent.routes.HomeController.show().url

}

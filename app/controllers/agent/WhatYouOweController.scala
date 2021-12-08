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
import config.featureswitch.{CodingOut, FeatureSwitching, TxmEventsApproved, WhatYouOweTotals}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.agent.utils.SessionKeys
import models.financialDetails.WhatYouOweChargesList
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import services.{IncomeSourceDetailsService, WhatYouOweService}
import views.html.WhatYouOwe

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYouOweController @Inject()(whatYouOweView: WhatYouOwe,
                                     whatYouOweService: WhatYouOweService,
                                     incomeSourceDetailsService: IncomeSourceDetailsService,
                                     auditingService: AuditingService,
                                     implicit val appConfig: FrontendAppConfig,
                                     val authorisedFunctions: FrontendAuthorisedFunctions
                                    )(implicit mcc: MessagesControllerComponents,
                                      implicit val ec: ExecutionContext,
                                      itvcErrorHandler: ItvcErrorHandler
                                    ) extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private def view(charge: WhatYouOweChargesList, taxYear: Int,codingOutEnabled: Boolean, displayTotals: Boolean,
                   hasLpiWithDunningBlock: Boolean, dunningLock: Boolean)(implicit user: MtdItUser[_]): Html = {
    whatYouOweView.apply(
      chargesList = charge,
      currentTaxYear = taxYear,
      hasLpiWithDunningBlock = hasLpiWithDunningBlock,
      backUrl = backUrl,
      utr = user.saUtr,
      dunningLock = dunningLock,
      codingOutEnabled = codingOutEnabled,
      displayTotals = displayTotals,
      isAgent = true
    )
  }

  def show: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
//        implicit val mtdUser = getUserWithNino()
//        incomeSourceDetailsService.getIncomeSourceDetails(cacheKey = Some("key")).flatMap(
//          res => Future.successful(itvcErrorHandler.showInternalServerError())
//        )
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
          implicit mtdItUser =>
						whatYouOweService.getWhatYouOweChargesList().map {
							whatYouOweChargesList => {
								if (isEnabled(TxmEventsApproved)) {
									auditingService.extendedAudit(WhatYouOweResponseAuditModel(mtdItUser, whatYouOweChargesList))
								}
                val codingOutEnabled = isEnabled(CodingOut)
                val displayTotals = isEnabled(WhatYouOweTotals)
								Ok(view(whatYouOweChargesList, mtdItUser.incomeSources.getCurrentTaxEndYear,codingOutEnabled = codingOutEnabled,
                  displayTotals = displayTotals, hasLpiWithDunningBlock = whatYouOweChargesList.hasLpiWithDunningBlock,
                  dunningLock = whatYouOweChargesList.hasDunningLock)
								).addingToSession(SessionKeys.chargeSummaryBackPage -> "paymentDue")
							}
						} recover {
							case ex: Exception =>
								Logger("application").error(s"Error received while getting agent what you page details: ${ex.getMessage}")
								itvcErrorHandler.showInternalServerError()
						}
        }
  }

  lazy val backUrl: String = controllers.agent.routes.HomeController.show().url

}

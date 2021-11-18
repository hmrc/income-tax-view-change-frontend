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

import audit.AuditingService
import audit.models.WhatYouOweResponseAuditModel
import config.featureswitch.{CodingOut, FeatureSwitching, TxmEventsApproved, WhatYouOweTotals}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import models.financialDetails.{DocumentDetail, FinancialDetailsErrorModel, FinancialDetailsResponseModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.WhatYouOweService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.WhatYouOweUnified
import javax.inject.Inject

import scala.concurrent.ExecutionContext

class WhatYouOweController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                     val authenticate: AuthenticationPredicate,
                                     val retrieveNino: NinoPredicate,
                                     val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                     val whatYouOweService: WhatYouOweService,
                                     val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                     val itvcErrorHandler: ItvcErrorHandler,
                                     val auditingService: AuditingService,
                                     implicit val appConfig: FrontendAppConfig,
                                     mcc: MessagesControllerComponents,
                                     implicit val ec: ExecutionContext,
                                     whatYouOwe: WhatYouOweUnified
                                    ) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def hasFinancialDetailsError(financialDetails: List[FinancialDetailsResponseModel]): Boolean = {
    financialDetails.exists(_.isInstanceOf[FinancialDetailsErrorModel])
  }

  val viewPaymentsDue: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
        whatYouOweService.getWhatYouOweChargesList().map {
          whatYouOweChargesList =>
            if (isEnabled(TxmEventsApproved)) {
              auditingService.extendedAudit(WhatYouOweResponseAuditModel(user, whatYouOweChargesList))
            }

            val codingOutEnabled = isEnabled(CodingOut)
            val displayTotals = isEnabled(WhatYouOweTotals)

            Ok(whatYouOwe(chargesList = whatYouOweChargesList, hasLpiWithDunningBlock = whatYouOweChargesList.hasLpiWithDunningBlock ,
              currentTaxYear = user.incomeSources.getCurrentTaxEndYear, backUrl = backUrl, user.saUtr,
              dunningLock = whatYouOweChargesList.hasDunningLock, codingOutEnabled = codingOutEnabled, displayTotals = displayTotals)
            ).addingToSession(SessionKeys.chargeSummaryBackPage -> "whatYouOwe")
        } recover {
          case ex: Exception =>
            Logger("application").error(s"Error received while getting what you page details: ${ex.getMessage}")
            itvcErrorHandler.showInternalServerError()
        }
  }


  lazy val backUrl: String = controllers.routes.HomeController.home().url

}

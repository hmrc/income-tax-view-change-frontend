/*
 * Copyright 2022 HM Revenue & Customs
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
import auth.MtdItUser
import config.featureswitch.{CodingOut, FeatureSwitch, FeatureSwitching, TxmEventsApproved, WhatYouOweTotals}
import config.{FrontendAppConfig, ShowInternalServerError, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate,
  SessionTimeoutPredicate, BtaNavBarPredicate}
import forms.utils.SessionKeys
import models.financialDetails.{FinancialDetailsErrorModel, FinancialDetailsResponseModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.api.mvc.Results._
import services.WhatYouOweService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.WhatYouOwe

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait WhatYouOweRequestHandler {
  def handleRequest(whatYouOweService: WhatYouOweService,
                    auditingService: AuditingService,
                    whatYouOwe: WhatYouOwe,
                    backUrl: String,
                    itvcErrorHandler: ShowInternalServerError,
                    isEnabled: (FeatureSwitch) => Boolean,
                    isAgent: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    whatYouOweService.getWhatYouOweChargesList().map {
      whatYouOweChargesList =>
        if (isEnabled(TxmEventsApproved)) {
          auditingService.extendedAudit(WhatYouOweResponseAuditModel(user, whatYouOweChargesList))
        }

        val codingOutEnabled = isEnabled(CodingOut)
        val displayTotals = isEnabled(WhatYouOweTotals)

        Ok(whatYouOwe(chargesList = whatYouOweChargesList, hasLpiWithDunningBlock = whatYouOweChargesList.hasLpiWithDunningBlock,
          currentTaxYear = user.incomeSources.getCurrentTaxEndYear, backUrl = backUrl, utr = user.saUtr,
          btaNavPartial = user.btaNavPartial,
          dunningLock = whatYouOweChargesList.hasDunningLock, codingOutEnabled = codingOutEnabled, displayTotals = displayTotals, isAgent = isAgent)
        ).addingToSession(SessionKeys.chargeSummaryBackPage -> "whatYouOwe")
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error received while getting WhatYouOwe page details: ${ex.getMessage}")
        itvcErrorHandler.showInternalServerError()
    }
  }
}

class WhatYouOweController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                     val authenticate: AuthenticationPredicate,
                                     val retrieveNino: NinoPredicate,
                                     val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                     val whatYouOweService: WhatYouOweService,
                                     val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                     val itvcErrorHandler: ItvcErrorHandler,
                                     val retrieveBtaNavBar: BtaNavBarPredicate,
                                     val auditingService: AuditingService,
                                     implicit val appConfig: FrontendAppConfig,
                                     mcc: MessagesControllerComponents,
                                     implicit val ec: ExecutionContext,
                                     whatYouOwe: WhatYouOwe
                                    ) extends FrontendController(mcc) with I18nSupport with FeatureSwitching with WhatYouOweRequestHandler {

  def hasFinancialDetailsError(financialDetails: List[FinancialDetailsResponseModel]): Boolean = {
    financialDetails.exists(_.isInstanceOf[FinancialDetailsErrorModel])
  }

  val viewPaymentsDue: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar ).async {
    implicit user =>
      handleRequest(
        whatYouOweService = whatYouOweService,
        auditingService = auditingService,
        whatYouOwe = whatYouOwe,
        backUrl = backUrl,
        itvcErrorHandler = itvcErrorHandler,
        isEnabled = isEnabled,
        isAgent = false
      )
  }


  lazy val backUrl: String = controllers.routes.HomeController.home().url

}

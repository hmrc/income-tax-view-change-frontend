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
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{CodingOut, FeatureSwitch, FeatureSwitching, TxmEventsApproved, WhatYouOweTotals}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, BtaNavBarPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{IncomeSourceDetailsService, WhatYouOweService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.WhatYouOwe

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatYouOweController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                     val authenticate: AuthenticationPredicate,
                                     val retrieveNino: NinoPredicate,
                                     val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                     val whatYouOweService: WhatYouOweService,
                                     val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                     val itvcErrorHandler: ItvcErrorHandler,
                                     implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                     val retrieveBtaNavBar: BtaNavBarPredicate,
                                     val authorisedFunctions: FrontendAuthorisedFunctions,
                                     val auditingService: AuditingService,
                                     val incomeSourceDetailsService: IncomeSourceDetailsService,
                                     implicit val appConfig: FrontendAppConfig,
                                     implicit override val mcc: MessagesControllerComponents,
                                     implicit val ec: ExecutionContext,
                                     whatYouOwe: WhatYouOwe
                                    ) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

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
          dunningLock = whatYouOweChargesList.hasDunningLock,
          codingOutEnabled = codingOutEnabled,
          displayTotals = displayTotals,
          isAgent = isAgent)(user, user, messages)
        ).addingToSession(SessionKeys.chargeSummaryBackPage -> "whatYouOwe")
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error received while getting WhatYouOwe page details: ${ex.getMessage}")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def viewWhatYouOwe: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        whatYouOweService = whatYouOweService,
        auditingService = auditingService,
        whatYouOwe = whatYouOwe,
        backUrl = controllers.routes.HomeController.home().url,
        itvcErrorHandler = itvcErrorHandler,
        isEnabled = isEnabled,
        isAgent = false
      )
  }

  def viewWhatYouOweAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
          implicit mtdItUser =>
            handleRequest(
              whatYouOweService = whatYouOweService,
              auditingService = auditingService,
              whatYouOwe = whatYouOwe,
              backUrl = controllers.agent.routes.HomeController.show().url,
              itvcErrorHandler = itvcErrorHandlerAgent,
              isEnabled = isEnabled,
              isAgent = true
            )
        }
  }
}

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

import audit.AuditingService
import audit.models.WhatYouOweResponseAuditModel
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{CodingOut, CreditsRefundsRepay, CutOverCredits, FeatureSwitching, MFACreditsAndDebits, TimeMachineAddYear, WhatYouOweCreditAmount}
import config._
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.utils.SessionKeys.gatewayPage
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DateService, IncomeSourceDetailsService, WhatYouOweService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.WhatYouOwe

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import enums.GatewayPage.WhatYouOwePage

class WhatYouOweController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                     val authenticate: AuthenticationPredicate,
                                     val retrieveNino: NinoPredicate,
                                     val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                     val whatYouOweService: WhatYouOweService,
                                     val itvcErrorHandler: ItvcErrorHandler,
                                     implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                     val retrieveBtaNavBar: NavBarPredicate,
                                     val authorisedFunctions: FrontendAuthorisedFunctions,
                                     val auditingService: AuditingService,
                                     val dateService: DateService,
                                     val incomeSourceDetailsService: IncomeSourceDetailsService,
                                     implicit val appConfig: FrontendAppConfig,
                                     implicit override val mcc: MessagesControllerComponents,
                                     implicit val ec: ExecutionContext,
                                     whatYouOwe: WhatYouOwe
                                    ) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def handleRequest(backUrl: String,
                    itvcErrorHandler: ShowInternalServerError,
                    isAgent: Boolean,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    whatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isEnabled(TimeMachineAddYear)) flatMap {
      whatYouOweChargesList =>
        auditingService.extendedAudit(WhatYouOweResponseAuditModel(user, whatYouOweChargesList, dateService))

        val codingOutEnabled = isEnabled(CodingOut)

        whatYouOweService.getCreditCharges().map {
          creditCharges =>

            Ok(whatYouOwe(
              currentDate = dateService.getCurrentDate(isEnabled(TimeMachineAddYear)),
              creditCharges,
              whatYouOweChargesList = whatYouOweChargesList, hasLpiWithDunningBlock = whatYouOweChargesList.hasLpiWithDunningBlock,
              currentTaxYear = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear)), backUrl = backUrl, utr = user.saUtr,
              btaNavPartial = user.btaNavPartial,
              dunningLock = whatYouOweChargesList.hasDunningLock,
              codingOutEnabled = codingOutEnabled,
              MFADebitsEnabled = isEnabled(MFACreditsAndDebits),
              isAgent = isAgent,
              whatYouOweCreditAmountEnabled = isEnabled(WhatYouOweCreditAmount),
              isUserMigrated = user.incomeSources.yearOfMigration.isDefined,
              creditAndRefundEnabled = isEnabled(CreditsRefundsRepay),
              origin = origin)(user, user, messages)
            ).addingToSession(gatewayPage -> WhatYouOwePage.name)
        }
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error received while getting WhatYouOwe page details: ${ex.getMessage}")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def show(origin: Option[String] = None): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        backUrl = controllers.routes.HomeController.show(origin).url,
        itvcErrorHandler = itvcErrorHandler,
        isAgent = false,
        origin = origin
      )
  }

  def showAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true).flatMap {
          implicit mtdItUser =>
            handleRequest(
              backUrl = controllers.routes.HomeController.showAgent.url,
              itvcErrorHandler = itvcErrorHandlerAgent,
              isAgent = true
            )
        }
  }
}

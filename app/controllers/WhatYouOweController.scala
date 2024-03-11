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
import config._
import config.featureswitch._
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicateV2, IncomeSourceDetailsPredicate, NavBarPredicate, SessionTimeoutPredicate}
import enums.GatewayPage.WhatYouOwePage
import forms.utils.SessionKeys.gatewayPage
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, ActionBuilder, AnyContent, MessagesControllerComponents, Result}
import services.{DateServiceInterface, WhatYouOweService}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier
import utils.AuthenticatorPredicate
import views.html.WhatYouOwe

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatYouOweController @Inject()(val whatYouOweService: WhatYouOweService,
                                     val checkSessionTimeout: SessionTimeoutPredicate,
                                     val authenticate: AuthenticationPredicateV2,
                                     val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                     val retrievebtaNavPartial: NavBarPredicate,
                                     val itvcErrorHandler: ItvcErrorHandler,
                                     implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                     val authorisedFunctions: FrontendAuthorisedFunctions,
                                     val auditingService: AuditingService,
                                     val dateService: DateServiceInterface,
                                     implicit val appConfig: FrontendAppConfig,
                                     implicit override val mcc: MessagesControllerComponents,
                                     implicit val ec: ExecutionContext,
                                     whatYouOwe: WhatYouOwe,
                                     val auth: AuthenticatorPredicate
                                    ) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  val action: ActionBuilder[MtdItUser, AnyContent] =
    checkSessionTimeout andThen authenticate andThen retrieveNinoWithIncomeSources andThen retrievebtaNavPartial

  def handleRequest(backUrl: String,
                    itvcErrorHandler: ShowInternalServerError,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
    val isTimeMachineEnabled: Boolean = isEnabled(TimeMachineAddYear)
    whatYouOweService.getWhatYouOweChargesList(isEnabled(CodingOut), isEnabled(MFACreditsAndDebits), isTimeMachineEnabled).flatMap {
      whatYouOweChargesList =>
        auditingService.extendedAudit(WhatYouOweResponseAuditModel(user, whatYouOweChargesList, dateService))

        val codingOutEnabled = isEnabled(CodingOut)

        whatYouOweService.getCreditCharges().map {
          creditCharges =>

            Ok(whatYouOwe(
              currentDate = dateService.getCurrentDate(isTimeMachineEnabled),
              creditCharges,
              whatYouOweChargesList = whatYouOweChargesList, hasLpiWithDunningBlock = whatYouOweChargesList.hasLpiWithDunningBlock,
              currentTaxYear = dateService.getCurrentTaxYearEnd(isTimeMachineEnabled), backUrl = backUrl, utr = user.saUtr,
              btaNavPartial = user.btaNavPartial,
              dunningLock = whatYouOweChargesList.hasDunningLock,
              codingOutEnabled = codingOutEnabled,
              MFADebitsEnabled = isEnabled(MFACreditsAndDebits),
              isAgent = user.userType.contains(Agent),
              whatYouOweCreditAmountEnabled = isEnabled(WhatYouOweCreditAmount),
              isUserMigrated = user.incomeSources.yearOfMigration.isDefined,
              creditAndRefundEnabled = isEnabled(CreditsRefundsRepay),
              origin = origin)(user, user, messages)
            ).addingToSession(gatewayPage -> WhatYouOwePage.name)
        }
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (user.userType.contains(Agent)) "[Agent]"}" +
          s"Error received while getting WhatYouOwe page details: ${ex.getMessage} - ${ex.getCause}")
        itvcErrorHandler.showInternalServerError()
    }
  }

  def show(origin: Option[String] = None): Action[AnyContent] = action.async {
    implicit user =>
      handleRequest(
        backUrl = controllers.routes.HomeController.show(origin).url,
        itvcErrorHandler = itvcErrorHandler,
        origin = origin
      )
  }

  def showAgent: Action[AnyContent] = action.async {
    implicit mtdItUser =>
      handleRequest(
        backUrl = controllers.routes.HomeController.showAgent.url,
        itvcErrorHandler = itvcErrorHandlerAgent,
      )
  }
}

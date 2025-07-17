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
import auth.MtdItUser
import auth.authV2.AuthActions
import config._
import config.featureswitch._
import enums.GatewayPage.WhatYouOwePage
import forms.utils.SessionKeys.gatewayPage
import models.admin._
import models.core.Nino
import models.financialDetails.{ChargeItem, SecondLatePaymentPenalty, WhatYouOweViewModel}
import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ClaimToAdjustService, DateServiceInterface, SelfServeTimeToPayService, WhatYouOweService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.WhatYouOwe
import controllers.routes._
import implicits.ImplicitCurrencyFormatter.CurrencyFormatter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class WhatYouOweController @Inject()(val authActions: AuthActions,
                                     val whatYouOweService: WhatYouOweService,
                                     val selfServeTimeToPayService: SelfServeTimeToPayService,
                                     val claimToAdjustService: ClaimToAdjustService,
                                     val itvcErrorHandler: ItvcErrorHandler,
                                     val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                     val auditingService: AuditingService,
                                     implicit val dateService: DateServiceInterface,
                                     whatYouOwe: WhatYouOwe
                                    )(implicit val appConfig: FrontendAppConfig,
                                      val mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with FeatureSwitching {

  def handleRequest(backUrl: String,
                    itvcErrorHandler: ShowInternalServerError,
                    isAgent: Boolean,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    for {
      whatYouOweChargesList <- whatYouOweService.getWhatYouOweChargesList(isEnabled(ReviewAndReconcilePoa),
        isEnabled(FilterCodedOutPoas),
        isEnabled(PenaltiesAndAppeals),
        mainChargeIsNotPaidFilter)
      selfServeTimeToPayUrl <- selfServeTimeToPayService.startSelfServeTimeToPayJourney()
      ctaViewModel <- claimToAdjustViewModel(Nino(user.nino))
    } yield {

      auditingService.extendedAudit(WhatYouOweResponseAuditModel(user, whatYouOweChargesList, dateService))

      val hasOverdueCharges: Boolean = whatYouOweChargesList.chargesList.exists(_.isOverdue()(dateService))
      val hasAccruingInterestReviewAndReconcileCharges: Boolean = whatYouOweChargesList.chargesList.exists(_.isNotPaidAndNotOverduePoaReconciliationDebit()(dateService))
      (selfServeTimeToPayUrl, getLPP2Link(whatYouOweChargesList.chargesList, isAgent)) match {
        case (Left(ex), _) =>
          Logger("application").error(s"Unable to retrieve selfServeTimeToPayStartUrl: ${ex.getMessage} - ${ex.getCause}")
          itvcErrorHandler.showInternalServerError()
        case (Right(startUrl), Some(lpp2Url)) =>

          val wyoViewModel: WhatYouOweViewModel = WhatYouOweViewModel(
            currentDate = dateService.getCurrentDate,
            hasOverdueOrAccruingInterestCharges = hasOverdueCharges || hasAccruingInterestReviewAndReconcileCharges,
            whatYouOweChargesList = whatYouOweChargesList,
            hasLpiWithDunningLock = whatYouOweChargesList.hasLpiWithDunningLock,
            currentTaxYear = dateService.getCurrentTaxYearEnd,
            backUrl = backUrl,
            utr = user.saUtr,
            dunningLock = whatYouOweChargesList.hasDunningLock,
            reviewAndReconcileEnabled = isEnabled(ReviewAndReconcilePoa),
            creditAndRefundUrl = (user.isAgent() match {
              case true if user.incomeSources.yearOfMigration.isDefined  => CreditAndRefundController.showAgent()
              case true                                                  => NotMigratedUserController.showAgent()
              case false if user.incomeSources.yearOfMigration.isDefined => CreditAndRefundController.show()
              case false                                                 => NotMigratedUserController.show()
            }).url,
            creditAndRefundEnabled = isEnabled(CreditsRefundsRepay),
            taxYearSummaryUrl = {
              if (user.isAgent()) TaxYearSummaryController.renderAgentTaxYearSummaryPage(_).url
              else                TaxYearSummaryController.renderTaxYearSummaryPage(_, origin).url
            },
            claimToAdjustViewModel = ctaViewModel,
            lpp2Url = lpp2Url,
            adjustPoaUrl = controllers.claimToAdjustPoa.routes.AmendablePoaController.show(user.isAgent()).url,
            chargeSummaryUrl = (taxYearEnd: Int, transactionId: String, isInterest: Boolean, origin: Option[String]) => {
              if (user.isAgent()) ChargeSummaryController.showAgent(taxYearEnd, transactionId, isInterest).url
              else                ChargeSummaryController.show(taxYearEnd, transactionId, isInterest, origin).url
            },
            paymentHandOffUrl = PaymentController.paymentHandoff(_, origin).url,
            selfServeTimeToPayStartUrl = startUrl
          )
          Ok(whatYouOwe(
            viewModel = wyoViewModel,
            origin = origin)(user, user, messages, dateService))
            .addingToSession(gatewayPage -> WhatYouOwePage.name)
        case (_, None) =>
          Logger("application").error("No chargeReference supplied with second late payment penalty. Hand-off url could not be formulated")
          itvcErrorHandler.showInternalServerError()
      }
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"Error received while getting WhatYouOwe page details: ${ex.getMessage} - ${ex.getCause}")
      itvcErrorHandler.showInternalServerError()
  }

  private def getLPP2Link(chargeItems: List[ChargeItem], isAgent: Boolean): Option[String] = {
    val LPP2 = chargeItems.find(_.transactionType == SecondLatePaymentPenalty)
    LPP2 match {
      case Some(charge) => charge.chargeReference match {
        case Some(value) if isAgent => Some(appConfig.incomeTaxPenaltiesFrontendLPP2CalculationAgent(value))
        case Some(value) => Some(appConfig.incomeTaxPenaltiesFrontendLPP2Calculation(value))
        case None => None
      }
      case None => Some("")
    }
  }

  private def claimToAdjustViewModel(nino: Nino)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[WYOClaimToAdjustViewModel] = {
    if (isEnabled(AdjustPaymentsOnAccount)) {
      claimToAdjustService.getPoaTaxYearForEntryPoint(nino).flatMap {
        case Right(value) => Future.successful(WYOClaimToAdjustViewModel(isEnabled(AdjustPaymentsOnAccount), value))
        case Left(ex: Throwable) => Future.failed(ex)
      }
    } else {
      Future.successful(WYOClaimToAdjustViewModel(isEnabled(AdjustPaymentsOnAccount), None))
    }
  }

  def show(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual.async {
    implicit user =>
      handleRequest(
        backUrl = controllers.routes.HomeController.show(origin).url,
        itvcErrorHandler = itvcErrorHandler,
        isAgent = false,
        origin = origin
      )
  }

  def showAgent: Action[AnyContent] = authActions.asMTDPrimaryAgent.async {
    implicit mtdItUser =>
      handleRequest(
        backUrl = controllers.routes.HomeController.showAgent().url,
        itvcErrorHandler = itvcErrorHandlerAgent,
        isAgent = true
      )
  }

  private def mainChargeIsNotPaidFilter: PartialFunction[ChargeItem, ChargeItem]  = {
    case x if x.remainingToPayByChargeOrInterest > 0 => x
  }
}

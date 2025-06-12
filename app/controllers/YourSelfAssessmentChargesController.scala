/*
 * Copyright 2025 HM Revenue & Customs
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
import enums.GatewayPage.YourSelfAssessmentChargeSummaryPage
import forms.utils.SessionKeys.gatewayPage
import models.admin._
import models.core.Nino
import models.financialDetails.{ChargeItem, YourSelfAssessmentChargesViewModel}
import models.incomeSourceDetails.TaxYear
import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel
import models.taxYearAmount.EarliestDueCharge
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ClaimToAdjustService, DateServiceInterface, SelfServeTimeToPayService, WhatYouOweService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.YourSelfAssessmentCharges

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class YourSelfAssessmentChargesController @Inject()(val authActions: AuthActions,
                                                    val whatYouOweService: WhatYouOweService,
                                                    val claimToAdjustService: ClaimToAdjustService,
                                                    val selfServeTimeToPayService: SelfServeTimeToPayService,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                    val auditingService: AuditingService,
                                                    implicit val dateService: DateServiceInterface,
                                                    view: YourSelfAssessmentCharges
                                    )(implicit val appConfig: FrontendAppConfig,
                                      val mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with FeatureSwitching {

  def handleRequest(backUrl: String,
                    itvcErrorHandler: ShowInternalServerError,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    for {
      whatYouOweChargesList <- whatYouOweService.getWhatYouOweChargesList(isEnabled(ReviewAndReconcilePoa),
        isEnabled(FilterCodedOutPoas),
        isEnabled(PenaltiesAndAppeals),
        mainChargeIsPaidFilter)
      selfServeTimeToPayStartUrl <- selfServeTimeToPayService.startSelfServeTimeToPayJourney()
      ctaViewModel <- claimToAdjustViewModel(Nino(user.nino))
    } yield {

      selfServeTimeToPayStartUrl match {
        case Left(ex) =>
          Logger("application").error(s"Unable to retrieve selfServeTimeToPayStartUrl: ${ex.getMessage} - ${ex.getCause}")
          itvcErrorHandler.showInternalServerError()
        case Right(startUrl) =>
          auditingService.extendedAudit(WhatYouOweResponseAuditModel(user, whatYouOweChargesList, dateService))

          val hasOverdueCharges: Boolean = whatYouOweChargesList.chargesList.exists(_.isOverdue()(dateService))
          val hasAccruingInterestReviewAndReconcileCharges: Boolean = whatYouOweChargesList.chargesList
            .exists(_.isNotPaidAndNotOverduePoaReconciliationDebit()(dateService))
          val earliestTaxYearAndAmount: Option[EarliestDueCharge] =
            whatYouOweChargesList.getEarliestTaxYearAndAmountByDueDate
              .map { case (year, amount) => EarliestDueCharge(TaxYear.forYearEnd(year), amount) }

          val viewModel: YourSelfAssessmentChargesViewModel = YourSelfAssessmentChargesViewModel(
            hasOverdueOrAccruingInterestCharges = hasOverdueCharges || hasAccruingInterestReviewAndReconcileCharges,
            whatYouOweChargesList = whatYouOweChargesList, hasLpiWithDunningLock = whatYouOweChargesList.hasLpiWithDunningLock,
            backUrl = backUrl,
            dunningLock = whatYouOweChargesList.hasDunningLock,
            reviewAndReconcileEnabled = isEnabled(ReviewAndReconcilePoa),
            penaltiesEnabled = isEnabled(PenaltiesAndAppeals),
            LPP2Url = appConfig.incomeTaxPenaltiesFrontendCalculation,
            creditAndRefundEnabled = isEnabled(CreditsRefundsRepay),
            earliestTaxYearAndAmountByDueDate = earliestTaxYearAndAmount,
            selfServeTimeToPayStartUrl = startUrl,
            claimToAdjustViewModel = ctaViewModel
          )
          Ok(view(
            viewModel = viewModel,
            origin = origin)(user, user, messages, dateService)
          ).addingToSession(gatewayPage -> YourSelfAssessmentChargeSummaryPage.name)
      }
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"${if (user.isAgent()) "[Agent]"}" +
        s"Error received while getting WhatYouOwe page details: ${ex.getMessage} - ${ex.getCause}")
      itvcErrorHandler.showInternalServerError()
  }

  private def claimToAdjustViewModel(nino: Nino)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[WYOClaimToAdjustViewModel] = {
    if (isEnabled(AdjustPaymentsOnAccount)) {
      claimToAdjustService.getPoaTaxYearForEntryPoint(nino).flatMap {
        case Right(value) => Future.successful(WYOClaimToAdjustViewModel(isEnabled(AdjustPaymentsOnAccount), value))
        case Left(ex: Throwable) => Logger("application").error(s"Unable to create WYOClaimToAdjustViewModel: ${ex.getMessage} - ${ex.getCause}")
          Future.failed(ex)
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
        origin = origin
      )
  }

  def showAgent: Action[AnyContent] = authActions.asMTDPrimaryAgent.async {
    implicit mtdItUser =>
      handleRequest(
        backUrl = controllers.routes.HomeController.showAgent().url,
        itvcErrorHandler = itvcErrorHandlerAgent
      )
  }

  private def mainChargeIsPaidFilter: PartialFunction[ChargeItem, ChargeItem]  = {
    case x if x.remainingToPayByChargeOrInterestWhenChargeIsPaid  => x
  }
}

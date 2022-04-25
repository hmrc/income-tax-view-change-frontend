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
import audit.models.PaymentAllocationsResponseAuditModel
import auth.MtdItUser
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.GatewayPage.{GatewayPage, PaymentHistoryPage, TaxYearSummaryPage, WhatYouOwePage}
import forms.utils.SessionKeys.gatewayPage
import implicits.ImplicitDateFormatterImpl
import models.core.Nino
import models.paymentAllocationCharges.{PaymentAllocationError, PaymentAllocationViewModel}
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.mvc.Http
import services.{IncomeSourceDetailsService, PaymentAllocationsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.PaymentAllocation

import javax.inject.{Inject, Singleton}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentAllocationsController @Inject()(val paymentAllocationView: PaymentAllocation,
                                             val checkSessionTimeout: SessionTimeoutPredicate,
                                             val authenticate: AuthenticationPredicate,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             val retrieveNino: NinoPredicate,
                                             val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                             val incomeSourceDetailsService: IncomeSourceDetailsService,
                                             itvcErrorHandler: ItvcErrorHandler,
                                             implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             paymentAllocations: PaymentAllocationsService,
                                             val retrieveBtaNavBar: NavBarPredicate,
                                             auditingService: AuditingService)
                                            (implicit override val mcc: MessagesControllerComponents,
                                             val ec: ExecutionContext,
                                             val implicitDateFormatter: ImplicitDateFormatterImpl,
                                             val appConfig: FrontendAppConfig) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  private lazy val redirectUrlIndividual: String = controllers.errors.routes.NotFoundDocumentIDLookupController.show().url
  private lazy val redirectUrlAgent: String = controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show().url


  def handleRequest(itvcErrorHandler: ShowInternalServerError,
                    documentNumber: String,
                    redirectUrl: String,
                    isAgent: Boolean,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val sessionGatewayPage = user.session.get(gatewayPage).map(GatewayPage(_))
    paymentAllocations.getPaymentAllocation(Nino(user.nino), documentNumber) map {
      case Right(paymentAllocations: PaymentAllocationViewModel) =>
        val taxYearOpt = paymentAllocations.originalPaymentAllocationWithClearingDate.headOption.flatMap(_.allocationDetail.map(_.getTaxYear))
        val backUrl = getBackUrl(sessionGatewayPage, taxYearOpt, origin, isAgent)
        if (!isEnabled(CutOverCredits) && paymentAllocations.paymentAllocationChargeModel.documentDetails.exists(_.credit.isDefined)) {
          Logger("application").warn(s"[PaymentAllocationsController][handleRequest] CutOverCredits is disabled and redirected to not found page")
          Redirect(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url)
        } else {
          auditingService.extendedAudit(PaymentAllocationsResponseAuditModel(user, paymentAllocations))
          Ok(paymentAllocationView(paymentAllocations, backUrl = backUrl, user.saUtr,
            CutOverCreditsEnabled = isEnabled(CutOverCredits), btaNavPartial = user.btaNavPartial,
            isAgent = isAgent, origin = origin, gatewayPage = sessionGatewayPage)(implicitly, messages))
        }

      case Left(PaymentAllocationError(Some(Http.Status.NOT_FOUND))) =>
        Redirect(redirectUrl)
      case _ => itvcErrorHandler.showInternalServerError()
    }
  }

  def viewPaymentAllocation(documentNumber: String, origin: Option[String] = None): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        if (isEnabled(PaymentAllocation)) {
          handleRequest(
            itvcErrorHandler = itvcErrorHandler,
            documentNumber = documentNumber,
            redirectUrl = redirectUrlIndividual,
            isAgent = false,
            origin = origin
          )
        } else Future.successful(Redirect(redirectUrlIndividual))
    }

  def viewPaymentAllocationAgent(documentNumber: String): Action[AnyContent] = {
    Authenticated.async { implicit request =>
      implicit agent =>
        if (isEnabled(PaymentAllocation)) {
          getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = true) flatMap { implicit mtdItUser =>
            handleRequest(
              itvcErrorHandler = itvcErrorHandlerAgent,
              documentNumber = documentNumber,
              redirectUrl = redirectUrlAgent,
              isAgent = true
            )
          }
        } else Future.successful(Redirect(redirectUrlAgent))
    }
  }

  private def getBackUrl(gatewayPageOpt: Option[GatewayPage], taxYearOpt: Option[Int], origin: Option[String], isAgent: Boolean): String = (gatewayPageOpt, taxYearOpt) match {
    case (Some(TaxYearSummaryPage), Some(taxYear)) =>
      if (isAgent) controllers.agent.routes.TaxYearSummaryController.show(taxYear).url + "#payments"
      else controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(taxYear, origin).url + "#payments"
    case (Some(TaxYearSummaryPage), None) =>
      if (isAgent) controllers.routes.HomeController.showAgent().url
      else controllers.routes.HomeController.show(origin).url
    case (Some(WhatYouOwePage), _) =>
      if (isAgent) controllers.routes.WhatYouOweController.showAgent().url
      else controllers.routes.WhatYouOweController.show(origin).url
    case (Some(PaymentHistoryPage), _) =>
      if (isAgent) controllers.routes.PaymentHistoryController.showAgent().url
      else controllers.routes.PaymentHistoryController.show(origin).url
    case _ =>
      if (isAgent) controllers.routes.HomeController.showAgent().url
      else controllers.routes.HomeController.show(origin).url
  }
}

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
import audit.models.PaymentAllocationsResponseAuditModel
import auth.MtdItUser
import config.featureswitch._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import enums.GatewayPage.GatewayPage
import forms.utils.SessionKeys.gatewayPage
import implicits.ImplicitDateFormatterImpl
import models.admin.{CreditsRefundsRepay, CutOverCredits, PaymentAllocation}
import models.core.Nino
import models.paymentAllocationCharges.{PaymentAllocationError, PaymentAllocationViewModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import play.mvc.Http
import services.{IncomeSourceDetailsService, PaymentAllocationsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, FallBackBackLinks}
import views.html.PaymentAllocation

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentAllocationsController @Inject()(val paymentAllocationView: PaymentAllocation,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             val incomeSourceDetailsService: IncomeSourceDetailsService,
                                             itvcErrorHandler: ItvcErrorHandler,
                                             implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             paymentAllocations: PaymentAllocationsService,
                                             auditingService: AuditingService,
                                             val auth: AuthenticatorPredicate)
                                            (implicit override val mcc: MessagesControllerComponents,
                                             val ec: ExecutionContext,
                                             val implicitDateFormatter: ImplicitDateFormatterImpl,
                                             val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with I18nSupport with FeatureSwitching with FallBackBackLinks {

  private lazy val redirectUrlIndividual: String = controllers.errors.routes.NotFoundDocumentIDLookupController.show().url
  private lazy val redirectUrlAgent: String = controllers.agent.errors.routes.AgentNotFoundDocumentIDLookupController.show.url

  def viewPaymentAllocation(documentNumber: String, origin: Option[String] = None): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
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

  def handleRequest(itvcErrorHandler: ShowInternalServerError,
                    documentNumber: String,
                    redirectUrl: String,
                    isAgent: Boolean,
                    origin: Option[String] = None)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val sessionGatewayPage = user.session.get(gatewayPage).map(GatewayPage(_))
    paymentAllocations.getPaymentAllocation(Nino(user.nino), documentNumber) map {
      case Right(paymentAllocations: PaymentAllocationViewModel) =>
        val taxYearOpt = paymentAllocations.originalPaymentAllocationWithClearingDate.headOption.flatMap(_.allocationDetail.flatMap(_.getTaxYearOpt))
        val backUrl = getPaymentAllocationBackUrl(sessionGatewayPage, taxYearOpt, origin, isAgent)
        if (!isEnabled(CutOverCredits) && paymentAllocations.paymentAllocationChargeModel.documentDetails.exists(_.credit.isDefined)) {
          Logger("application").warn("CutOverCredits is disabled and redirected to not found page")
          Redirect(controllers.errors.routes.NotFoundDocumentIDLookupController.show().url)
        } else {
          auditingService.extendedAudit(PaymentAllocationsResponseAuditModel(user, paymentAllocations))
          val dueDate: Option[LocalDate] = paymentAllocations.paymentAllocationChargeModel.financialDetails.headOption
            .flatMap(_.items.flatMap(_.headOption.flatMap(_.dueDate)))
          val outstandingAmount: Option[BigDecimal] = paymentAllocations.paymentAllocationChargeModel.documentDetails.headOption.map(_.outstandingAmount)
          Ok(paymentAllocationView(paymentAllocations, backUrl = backUrl, user.saUtr,
            CutOverCreditsEnabled = isEnabled(CutOverCredits), btaNavPartial = user.btaNavPartial,
            isAgent = isAgent, origin = origin, gatewayPage = sessionGatewayPage,
            creditsRefundsRepayEnabled = isEnabled(CreditsRefundsRepay), dueDate = dueDate,
            outstandingAmount = outstandingAmount)(implicitly, messages))
        }

      case Left(PaymentAllocationError(Some(Http.Status.NOT_FOUND))) =>
        Redirect(redirectUrl)
      case _ => itvcErrorHandler.showInternalServerError()
    }
  }

  def viewPaymentAllocationAgent(documentNumber: String): Action[AnyContent] = {
    auth.authenticatedAction(isAgent = true) {
      implicit mtdItUser =>
        if (isEnabled(PaymentAllocation)(mtdItUser)) {
          handleRequest(
            itvcErrorHandler = itvcErrorHandlerAgent,
            documentNumber = documentNumber,
            redirectUrl = redirectUrlAgent,
            isAgent = true
          )
        } else
          Future.successful(Redirect(redirectUrlAgent))
    }
  }

}

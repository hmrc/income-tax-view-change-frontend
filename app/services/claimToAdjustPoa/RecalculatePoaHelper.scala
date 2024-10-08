/*
 * Copyright 2024 HM Revenue & Customs
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

package services.claimToAdjustPoa

import auth.MtdItUser
import audit.AuditingService
import audit.models.AdjustPaymentsOnAccountAuditModel
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.routes.HomeController
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.{PaymentOnAccountViewModel, PoAAmendmentData}
import models.core.Nino
import play.api.Logger
import play.api.i18n.{Lang, Messages,LangImplicits}
import play.api.i18n.MessagesApi
import play.api.mvc.Result
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait RecalculatePoaHelper extends ClientConfirmedController with FeatureSwitching with LangImplicits{

  private def dataFromSession(poaSessionService: PaymentOnAccountSessionService)(implicit hc: HeaderCarrier, ec: ExecutionContext)
  : Future[PoAAmendmentData] = {
    poaSessionService.getMongo(hc, ec).flatMap {
      case Right(Some(newPoaData: PoAAmendmentData)) =>
        Future.successful(newPoaData)
      case _ =>
        Future.failed(new Exception(s"Failed to retrieve session data"))
    }
  }

  private def handlePoaAndOtherData(poa: PaymentOnAccountViewModel,
                                      otherData: PoAAmendmentData, isAgent: Boolean, nino: Nino, ctaCalculationService: ClaimToAdjustPoaCalculationService, auditingService: AuditingService)
                                     (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext,
                                      itvcErrorHandler: ItvcErrorHandler, itvcErrorHandlerAgent: AgentItvcErrorHandler): Future[Result] = {
    implicit val lang : Lang = Lang("en")
    otherData match {
      case PoAAmendmentData(Some(poaAdjustmentReason), Some(amount), _) =>
        ctaCalculationService.recalculate(nino, poa.taxYear, amount, poaAdjustmentReason) map {
          case Left(ex) =>
            Logger("application").error(s"POA recalculation request failed: ${ex.getMessage}")
            auditingService.extendedAudit(AdjustPaymentsOnAccountAuditModel(
              isSuccessful = false,
              previousPaymentOnAccountAmount = poa.totalAmountOne,
              requestedPaymentOnAccountAmount = amount,
              adjustmentReasonCode = poaAdjustmentReason.code,
              adjustmentReasonDescription = Messages(poaAdjustmentReason.messagesKey)(lang2Messages),
              isDecreased = amount < poa.totalAmountOne
            ))
            Redirect(controllers.claimToAdjustPoa.routes.ApiFailureSubmittingPoaController.show(isAgent))
          case Right(_) =>
            auditingService.extendedAudit(AdjustPaymentsOnAccountAuditModel(
              isSuccessful = true,
              previousPaymentOnAccountAmount = poa.totalAmountOne,
              requestedPaymentOnAccountAmount = amount,
              adjustmentReasonCode = poaAdjustmentReason.code,
              adjustmentReasonDescription = Messages(poaAdjustmentReason.messagesKey,lang)(lang2Messages),
              isDecreased = amount < poa.totalAmountOne
            ))
            Redirect(controllers.claimToAdjustPoa.routes.PoaAdjustedController.show(isAgent))
        }
      case PoAAmendmentData(_, _, _) =>
        Future.successful(showInternalServerError(isAgent))
    }
  }

  protected def handleSubmitPoaData(claimToAdjustService: ClaimToAdjustService, ctaCalculationService: ClaimToAdjustPoaCalculationService,
                                    poaSessionService: PaymentOnAccountSessionService, isAgent: Boolean, auditingService: AuditingService)
                                   (implicit user: MtdItUser[_], itvcErrorHandler: ItvcErrorHandler, itvcErrorHandlerAgent: AgentItvcErrorHandler): Future[Result] = {
    if (isEnabled(AdjustPaymentsOnAccount)) {
      {
        for {
          poaMaybe <- claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino))
        } yield poaMaybe match {
          case Right(Some(poa)) =>
            dataFromSession(poaSessionService).flatMap(otherData =>
              handlePoaAndOtherData(poa, otherData, isAgent, Nino(user.nino), ctaCalculationService, auditingService)
            )
          case Right(None) =>
            Logger("application").error(s"Failed to create PaymentOnAccount model")
            Future.successful(showInternalServerError(isAgent))
          case Left(ex) =>
            Logger("application").error(s"Exception: ${ex.getMessage} - ${ex.getCause}.")
            Future.successful(showInternalServerError(isAgent))
        }
      }.flatten
    } else {
      Future.successful(
        Redirect(if (isAgent) HomeController.showAgent else HomeController.show())
      )
    }
  }

}

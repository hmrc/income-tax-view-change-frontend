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

import audit.AuditingService
import audit.models.AdjustPaymentsOnAccountAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import models.claimToAdjustPoa.{ClaimToAdjustNrsPayload, PaymentOnAccountViewModel, PoaAmendmentData, SelectYourReason}
import models.core.Nino
import models.nrs.{NrsMetadata, NrsSubmission, RawPayload, SearchKeys}
import play.api.Logger
import play.api.i18n.{Lang, LangImplicits, Messages}
import play.api.libs.Files.logger
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.{ClaimToAdjustService, NrsService, PaymentOnAccountSessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.AuditExtensions
import utils.ErrorRecovery
import controllers.claimToAdjustPoa.routes._
import models.admin.SubmitClaimToAdjustToNrs

import java.security.MessageDigest
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

trait RecalculatePoaHelper extends FeatureSwitching with LangImplicits with ErrorRecovery {
  private def dataFromSession(poaSessionService: PaymentOnAccountSessionService)(implicit hc: HeaderCarrier, ec: ExecutionContext)
  : Future[PoaAmendmentData] = {
    poaSessionService.getMongo(hc, ec).flatMap {
      case Right(Some(newPoaData: PoaAmendmentData)) =>
        Future.successful(newPoaData)
      case _ =>
        Future.failed(new Exception(s"Failed to retrieve session data"))
    }
  }

  private def handlePoaAndOtherData(poa: PaymentOnAccountViewModel,
                                    otherData: PoaAmendmentData, nino: Nino, ctaCalculationService: ClaimToAdjustPoaCalculationService, auditingService: AuditingService, nrsService: NrsService)
                                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    implicit val lang: Lang = Lang("en")
    otherData match {
      case PoaAmendmentData(Some(poaAdjustmentReason), Some(amount), _) =>
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
            Redirect(ApiFailureSubmittingPoaController.show(user.isAgent()))
          case Right(_) =>
            auditingService.extendedAudit(AdjustPaymentsOnAccountAuditModel(
              isSuccessful = true,
              previousPaymentOnAccountAmount = poa.totalAmountOne,
              requestedPaymentOnAccountAmount = amount,
              adjustmentReasonCode = poaAdjustmentReason.code,
              adjustmentReasonDescription = Messages(poaAdjustmentReason.messagesKey, lang)(lang2Messages),
              isDecreased = amount < poa.totalAmountOne
            ))

            if (isEnabled(SubmitClaimToAdjustToNrs))
              submitClaimToAdjustToNrs(nrsService, amount, poa, poaAdjustmentReason)

            Redirect(PoaAdjustedController.show(user.isAgent()))
        }
      case PoaAmendmentData(_, _, _) =>
        Future.successful(logAndRedirect("Missing poaAdjustmentReason and/or amount"))
    }
  }

  protected def handleSubmitPoaData(claimToAdjustService: ClaimToAdjustService, ctaCalculationService: ClaimToAdjustPoaCalculationService,
                                    poaSessionService: PaymentOnAccountSessionService, nrsService: NrsService, auditingService: AuditingService)
                                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
      {
        for {
          poaMaybe <- claimToAdjustService.getPoaForNonCrystallisedTaxYear(Nino(user.nino))
        } yield poaMaybe match {
          case Right(Some(poa)) =>
            dataFromSession(poaSessionService).flatMap(otherData =>
              handlePoaAndOtherData(poa, otherData, Nino(user.nino), ctaCalculationService, auditingService, nrsService)
            )
          case Right(None) =>
            Future.successful(logAndRedirect("Failed to create PaymentOnAccount model"))
          case Left(ex) =>
            Future.successful(logAndRedirect(s"Exception: ${ex.getMessage} - ${ex.getCause}."))
        }
      }.flatten
  }

  private def submitClaimToAdjustToNrs(nrsService: NrsService,
                                       amount: BigDecimal,
                                       poa: PaymentOnAccountViewModel,
                                       poaAdjustmentReason: SelectYourReason)(
                                       implicit user: MtdItUser[_],
                                       lang: Lang, hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] = {
    val now = Instant.now()

    val auditTags = AuditExtensions.auditHeaderCarrier(hc).toAuditTags("adjust-payments-on-account", user.path)

    val payload = ClaimToAdjustNrsPayload(
      credId                          = user.credId,
      saUtr                           = user.saUtr,
      nino                            = user.nino,
      mtditId                         = user.mtditid,
      userType                        = user.userType.map(_.toString),
      generatedAt                     = now.toString,
      isDecreased                     = amount < poa.totalAmountOne,
      previousPaymentOnAccountAmount  = poa.totalAmountOne,
      requestedPaymentOnAccountAmount = amount,
      adjustmentReasonCode            = poaAdjustmentReason.code,
      adjustmentReasonDescription     = Messages(poaAdjustmentReason.messagesKey, lang)(lang2Messages)
    )

    val jsonBytes = Json.toBytes(Json.toJson(payload).as[JsObject])

    val checksum = MessageDigest.getInstance("SHA-256").digest(jsonBytes).map("%02x".format(_)).mkString

    val baseMetadata = NrsMetadata(
      request = user,
      userSubmissionTimestamp = now,
      searchKeys = SearchKeys(user.credId, user.saUtr, user.nino),
      checkSum = checksum
    )

    val metadata: NrsMetadata = {

      val currentHeaderData: Map[String, String] = baseMetadata.headerData
      val tagsMap: Map[String, Map[String, String]] = Map("tags" -> auditTags)

      val combinedValid: Map[String, String] = currentHeaderData ++ tagsMap.flatMap {
        case (k: String, v) => v.map(y => s"${k}.${y._1}" -> y._2)
      }
      baseMetadata.copy(headerData = combinedValid)
    }

    val submission = NrsSubmission(RawPayload(jsonBytes, user.charset), metadata)

    nrsService.submit(submission).map {
      case Some(resp) => logger.info(s"NRS submission accepted: ${resp.nrSubmissionId}")
      case None       => logger.error("NRS submission failed or was not accepted")
    }
  }

}

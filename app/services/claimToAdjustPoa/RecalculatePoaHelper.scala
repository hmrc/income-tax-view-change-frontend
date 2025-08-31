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
import controllers.routes.HomeController
import models.admin.SubmitClaimToAdjustToNrs
import models.claimToAdjustPoa.{ClaimToAdjustNrsPayload, PaymentOnAccountViewModel, PoaAmendmentData}
import models.core.Nino
import models.nrs.{IdentityData, NrsMetadata, NrsSubmission, RawPayload, SearchKeys}
import play.api.Logger
import play.api.i18n.{Lang, LangImplicits, Messages}
import play.api.libs.Files.logger
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import services.{ClaimToAdjustService, NrsService, PaymentOnAccountSessionService}
import uk.gov.hmrc.auth.core.ConfidenceLevel
import uk.gov.hmrc.auth.core.retrieve.{AgentInformation, Credentials, LoginTimes, MdtpInformation}
import uk.gov.hmrc.http.HeaderCarrier
import utils.ErrorRecovery

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Right

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
            Redirect(controllers.claimToAdjustPoa.routes.ApiFailureSubmittingPoaController.show(user.isAgent()))

          case Right(_) if isEnabled(SubmitClaimToAdjustToNrs) =>
            auditingService.extendedAudit(AdjustPaymentsOnAccountAuditModel(
              isSuccessful = true,
              previousPaymentOnAccountAmount = poa.totalAmountOne,
              requestedPaymentOnAccountAmount = amount,
              adjustmentReasonCode = poaAdjustmentReason.code,
              adjustmentReasonDescription = Messages(poaAdjustmentReason.messagesKey, lang)(lang2Messages),
              isDecreased = amount < poa.totalAmountOne
            ))

            val now = Instant.now()

            val payload = ClaimToAdjustNrsPayload(
              credId = user.credId,
              saUtr = user.saUtr,
              nino = user.nino,
              clientIP = user.headers.get("True-Client-IP"),
              deviceCookie = user.headers.get("deviceID"),
              sessionId = user.headers.get("X-Session-ID"),
              userType = user.userType.map(_.toString),
              generatedAt = now.toString,
              sessionCookie = user.headers.get("mdtp"),
              isDecreased = amount < poa.totalAmountOne,
              previousPaymentOnAccountAmount = poa.totalAmountOne,
              requestedPaymentOnAccountAmount = amount,
              adjustmentReasonCode = poaAdjustmentReason.code,
              adjustmentReasonDescription = Messages(poaAdjustmentReason.messagesKey, lang)(lang2Messages),
              mtditId = user.mtditid
            )

            val jsonBytes = Json.toBytes(Json.toJson(payload))

            val checksum = {
              val md = MessageDigest.getInstance("SHA-256")
              md.digest(jsonBytes).map("%02x".format(_)).mkString
            }

            val identity = IdentityData(
              internalId = None,
              externalId = None,
              agentCode = None,
              credentials = user.credId.map(id => Credentials(id, "GovernmentGateway")),
              confidenceLevel = ConfidenceLevel.L200,
              nino = Some(user.nino),
              saUtr = user.saUtr,
              name = user.userName,
              dateOfBirth = None,
              email = None,
              agentInformation = AgentInformation(user.arn, None, None),
              groupIdentifier = None,
              credentialRole = None,
              mdtpInformation = Some(MdtpInformation(payload.deviceCookie.getOrElse(""), payload.sessionId.getOrElse(""))),
              itmpName = None,
              itmpDateOfBirth = None,
              itmpAddress = None,
              affinityGroup = user.userType,
              credentialStrength = None,
              enrolments = user.authUserDetails.enrolments,
              loginTimes = LoginTimes(now, None)
            )

            val metadata = NrsMetadata(
              request = user,
              userSubmissionTimestamp = now,
              identityData = identity,
              searchKeys = SearchKeys(
                credId = user.credId,
                saUtr = user.saUtr,
                nino = Some(user.nino)
              ),
              checkSum = checksum
            )

            val submission = NrsSubmission(
              rawPayload = RawPayload(jsonBytes, user.charset),
              metadata = metadata
            )

            nrsService.submit(submission).map {
              case Some(resp) => logger.info(s"NRS submission accepted: ${resp.nrsSubmissionId}")
              case None => logger.warn("NRS submission failed or was not accepted")
            }

            Redirect(controllers.claimToAdjustPoa.routes.PoaAdjustedController.show(user.isAgent()))

          case Right(_) =>
            auditingService.extendedAudit(AdjustPaymentsOnAccountAuditModel(
              isSuccessful = true,
              previousPaymentOnAccountAmount = poa.totalAmountOne,
              requestedPaymentOnAccountAmount = amount,
              adjustmentReasonCode = poaAdjustmentReason.code,
              adjustmentReasonDescription = Messages(poaAdjustmentReason.messagesKey, lang)(lang2Messages),
              isDecreased = amount < poa.totalAmountOne
            ))
            Redirect(controllers.claimToAdjustPoa.routes.PoaAdjustedController.show(user.isAgent()))
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
}

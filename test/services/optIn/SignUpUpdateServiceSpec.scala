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

package services.optIn

import audit.AuditingService
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ErrorItem, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import enums.JourneyType.{Opt, OptInJourney}
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.Voluntary
import models.optin.{OptInContextData, OptInSessionData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, OneInstancePerTest}
import repositories.UIJourneySessionDataRepository
import services.optIn.core.{CurrentOptInTaxYear, NextOptInTaxYear, OptInProposition}
import testConstants.BaseTestConstants.testSessionId
import testUtils.{TestSupport, UnitSpec}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.Future


class SignUpUpdateServiceSpec extends UnitSpec
  with BeforeAndAfter
  with OneInstancePerTest
  with TestSupport with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val mockAuditingService: AuditingService = mock(classOf[AuditingService])
  val mockOptInService: OptInService = mock(classOf[OptInService])
  val mockUiJourneySessionDataRepository: UIJourneySessionDataRepository = mock(classOf[UIJourneySessionDataRepository])
  val mockItsaStatusUpdateConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])

  val service: SignUpUpdateService =
    new SignUpUpdateService(mockAuditingService, mockItsaStatusUpdateConnector, mockOptInService, mockUiJourneySessionDataRepository)

  "SignUpUpdateService" when {

    ".getOptInSessionData()" when {

      "there is data pre-existing in db" should {

        "return the correct session data" in {

          val optInContextData =
            OptInContextData(
              currentTaxYear = "2025-2026",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val selectedOptInYear = Some("2025-2026")
          val optInSessionData = OptInSessionData(Some(optInContextData), selectedOptInYear = selectedOptInYear)

          val retrievedUiSessionData =
            UIJourneySessionData(
              sessionId = hc.sessionId.get.value,
              journeyType = Opt(OptInJourney).toString,
              optInSessionData = Some(optInSessionData)
            )

          when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney)))
            .thenReturn(Future(Some(retrievedUiSessionData)))

          val actual = service.getOptInSessionData()
          whenReady(actual) { result => result shouldBe Some(optInSessionData) }
        }
      }

      "there is NO data present in db" should {

        "return None" in {

          when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney)))
            .thenReturn(Future(None))

          val actual = service.getOptInSessionData()
          whenReady(actual) { result => result shouldBe None }
        }
      }
    }

    ".triggerOptInRequest()" when {

      "the user has selected a tax year" should {

        "successfully make a Sign up request" in {

          val optInContextData =
            OptInContextData(
              currentTaxYear = "2025-2026",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val selectedOptInYear = Some("2025-2026")
          val optInSessionData = OptInSessionData(Some(optInContextData), selectedOptInYear = selectedOptInYear)

          val retrievedUiSessionData =
            UIJourneySessionData(
              sessionId = hc.sessionId.get.value,
              journeyType = Opt(OptInJourney).toString,
              optInSessionData = Some(optInSessionData)
            )

          val currentOptInTaxYear =
            CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026))

          val nextOptInTaxYear =
            NextOptInTaxYear(Voluntary, TaxYear(2026, 2027), currentOptInTaxYear)

          val optInProposition =
            OptInProposition(currentOptInTaxYear, nextOptInTaxYear)

          when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney)))
            .thenReturn(Future(Some(retrievedUiSessionData)))

          when(mockOptInService.fetchOptInProposition()(any(), any(), any()))
            .thenReturn(Future(optInProposition))

          when(mockItsaStatusUpdateConnector.optIn(any(), any())(any()))
            .thenReturn(Future(ITSAStatusUpdateResponseSuccess()))

          when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
            .thenReturn(Future(()))

          val actual = service.triggerOptInRequest()

          whenReady(actual) { result => result shouldBe ITSAStatusUpdateResponseSuccess(204) } // 204 No Content
        }
      }

      "the user has not selected a tax year" should {

        "not make a request to connector and return default error" in {

          val optInContextData =
            OptInContextData(
              currentTaxYear = "2025-2026",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val selectedOptInYear = None
          val optInSessionData = OptInSessionData(Some(optInContextData), selectedOptInYear = selectedOptInYear)

          val retrievedUiSessionData =
            UIJourneySessionData(
              sessionId = hc.sessionId.get.value,
              journeyType = Opt(OptInJourney).toString,
              optInSessionData = Some(optInSessionData)
            )

          val currentOptInTaxYear =
            CurrentOptInTaxYear(Voluntary, TaxYear(2025, 2026))

          val nextOptInTaxYear =
            NextOptInTaxYear(Voluntary, TaxYear(2026, 2027), currentOptInTaxYear)

          val optInProposition =
            OptInProposition(currentOptInTaxYear, nextOptInTaxYear)

          when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney)))
            .thenReturn(Future(Some(retrievedUiSessionData)))

          when(mockOptInService.fetchOptInProposition()(any(), any(), any()))
            .thenReturn(Future(optInProposition))

          val actual = service.triggerOptInRequest()

          whenReady(actual) { result => result shouldBe ITSAStatusUpdateResponseFailure(List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to unknown reason"))) }
        }
      }
    }
  }
}
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

package services.reportingObligations.signUp

import audit.AuditingService
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ErrorItem, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import enums.JourneyType.{Opt, SignUpJourney}
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, Voluntary}
import models.reportingObligations.signUp.{SignUpContextData, SignUpSessionData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, OneInstancePerTest}
import play.mvc.Http.Status.NO_CONTENT
import repositories.UIJourneySessionDataRepository
import services.DateService
import services.reportingObligations.signUp.{SignUpService, SignUpSubmissionService}
import services.reportingObligations.signUp.core.{CurrentSignUpTaxYear, NextSignUpTaxYear, SignUpProposition}
import testConstants.BaseTestConstants.testSessionId
import testUtils.{TestSupport, UnitSpec}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.Future


class SignUpSubmissionServiceSpec extends UnitSpec
  with BeforeAndAfter
  with OneInstancePerTest
  with TestSupport with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val mockDateService: DateService = mock(classOf[DateService])
  val mockAuditingService: AuditingService = mock(classOf[AuditingService])
  val mockOptInService: SignUpService = mock(classOf[SignUpService])
  val mockUiJourneySessionDataRepository: UIJourneySessionDataRepository = mock(classOf[UIJourneySessionDataRepository])
  val mockItsaStatusUpdateConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])

  val service: SignUpSubmissionService =
    new SignUpSubmissionService(
      auditingService = mockAuditingService,
      dateService = mockDateService,
      itsaStatusUpdateConnector = mockItsaStatusUpdateConnector,
      optInService = mockOptInService,
      uiJourneySessionDataRepository = mockUiJourneySessionDataRepository
    )

  "SignUpUpdateService" when {

    ".getOptInSessionData()" when {

      "there is data pre-existing in db" should {

        "return the correct session data" in {

          val optInContextData =
            SignUpContextData(
              currentTaxYear = "2025-2026",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val selectedOptInYear = Some("2025-2026")
          val optInSessionData = SignUpSessionData(Some(optInContextData), selectedSignUpYear = selectedOptInYear)

          val retrievedUiSessionData =
            UIJourneySessionData(
              sessionId = hc.sessionId.get.value,
              journeyType = Opt(SignUpJourney).toString,
              signUpSessionData = Some(optInSessionData)
            )

          when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(SignUpJourney)))
            .thenReturn(Future(Some(retrievedUiSessionData)))

          val request = service.getOptInSessionData()
          whenReady(request) { result => result shouldBe Some(optInSessionData) }
        }
      }

      "there is NO data present in db" should {

        "return None" in {

          when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(SignUpJourney)))
            .thenReturn(Future(None))

          val request = service.getOptInSessionData()
          whenReady(request) { result => result shouldBe None }
        }
      }
    }

    "getAllSignUpTaxYears()" when {
      "the user has selected CY as the sign up tax year and both years are Annual" should {

        "return both tax years" in {

          val currentTaxYear = TaxYear(2025, 2026)

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(currentTaxYear)

          val result = service.getAllSignUpTaxYears(Annual, Annual, Some(currentTaxYear))

          result shouldBe Seq(TaxYear(2025, 2026), TaxYear(2026, 2027))
        }
      }

      "the user has selected CY as the sign up tax year and only current year is Annual" should {

        "return only current tax year" in {

          val currentTaxYear = TaxYear(2025, 2026)

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(currentTaxYear)

          val result = service.getAllSignUpTaxYears(Annual, Voluntary, Some(currentTaxYear))

          result shouldBe Seq(TaxYear(2025, 2026))
        }
      }

      "the user has selected CY+1 as the sign up tax year and only next year is Annual" should {

        "return only next tax year" in {

          val currentTaxYear = TaxYear(2025, 2026)

          when(mockDateService.getCurrentTaxYear)
            .thenReturn(currentTaxYear)

          val result = service.getAllSignUpTaxYears(Voluntary, Annual, Some(currentTaxYear.nextYear))

          result shouldBe Seq(TaxYear(2026, 2027))
        }
      }
    }

    ".triggerSignUpRequest()" when {

      "single tax year - CY" when {

        "the user has selected CY as the sign up tax year" should {

          "successfully make a Sign Up request" in {

            val optInContextData =
              SignUpContextData(
                currentTaxYear = "2025-2026",
                currentYearITSAStatus = "Annual",
                nextYearITSAStatus = "MTD Voluntary"
              )

            val currentTaxYear = TaxYear(2025, 2026)

            val selectedOptInYear = Some("2025-2026")
            val optInSessionData = SignUpSessionData(Some(optInContextData), selectedSignUpYear = selectedOptInYear)

            val retrievedUiSessionData =
              UIJourneySessionData(
                sessionId = hc.sessionId.get.value,
                journeyType = Opt(SignUpJourney).toString,
                signUpSessionData = Some(optInSessionData)
              )

            val currentOptInTaxYear =
              CurrentSignUpTaxYear(Annual, TaxYear(2025, 2026))

            val nextOptInTaxYear =
              NextSignUpTaxYear(Voluntary, TaxYear(2026, 2027), currentOptInTaxYear)

            val optInProposition =
              SignUpProposition(currentOptInTaxYear, nextOptInTaxYear)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(SignUpJourney)))
              .thenReturn(Future(Some(retrievedUiSessionData)))

            when(mockOptInService.fetchSignUpProposition()(any(), any(), any()))
              .thenReturn(Future(optInProposition))

            when(mockItsaStatusUpdateConnector.signUp(any(), any())(any()))
              .thenReturn(Future(ITSAStatusUpdateResponseSuccess(NO_CONTENT)))

            when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
              .thenReturn(Future(()))

            val request = service.triggerSignUpRequest()

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseSuccess(204) }
          }
        }
      }

      "single tax year - CY+1" when {

        "the user has selected CY+1 as the sign up tax year" should {

          "successfully make a Sign up request" in {

            val optInContextData =
              SignUpContextData(
                currentTaxYear = "2025-2026",
                currentYearITSAStatus = "MTD Voluntary",
                nextYearITSAStatus = "Annual"
              )

            val currentTaxYear = TaxYear(2025, 2026)

            val selectedOptInYear = Some("2026-2027")
            val optInSessionData = SignUpSessionData(Some(optInContextData), selectedSignUpYear = selectedOptInYear)

            val retrievedUiSessionData =
              UIJourneySessionData(
                sessionId = hc.sessionId.get.value,
                journeyType = Opt(SignUpJourney).toString,
                signUpSessionData = Some(optInSessionData)
              )

            val currentOptInTaxYear =
              CurrentSignUpTaxYear(Voluntary, TaxYear(2025, 2026))

            val nextOptInTaxYear =
              NextSignUpTaxYear(Annual, TaxYear(2026, 2027), currentOptInTaxYear)

            val optInProposition =
              SignUpProposition(currentOptInTaxYear, nextOptInTaxYear)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(SignUpJourney)))
              .thenReturn(Future(Some(retrievedUiSessionData)))

            when(mockOptInService.fetchSignUpProposition()(any(), any(), any()))
              .thenReturn(Future(optInProposition))

            when(mockItsaStatusUpdateConnector.signUp(any(), any())(any()))
              .thenReturn(Future(ITSAStatusUpdateResponseSuccess()))

            when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
              .thenReturn(Future(()))

            val request = service.triggerSignUpRequest()

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseSuccess(204) }
          }
        }
      }


      "multiple tax years -  CY & CY+1" when {

        "the user has selected CY as the desired tax year - success scenario" should {

          "return a success response - 204" in {

            val optInContextData =
              SignUpContextData(
                currentTaxYear = "2025-2026",
                currentYearITSAStatus = "Annual",
                nextYearITSAStatus = "Annual"
              )

            val currentTaxYear = TaxYear(2025, 2026)

            val selectedOptInYear = Some("2025-2026")
            val optInSessionData = SignUpSessionData(Some(optInContextData), selectedSignUpYear = selectedOptInYear)

            val retrievedUiSessionData =
              UIJourneySessionData(
                sessionId = hc.sessionId.get.value,
                journeyType = Opt(SignUpJourney).toString,
                signUpSessionData = Some(optInSessionData)
              )

            val currentOptInTaxYear =
              CurrentSignUpTaxYear(Annual, TaxYear(2025, 2026))

            val nextOptInTaxYear =
              NextSignUpTaxYear(Annual, TaxYear(2026, 2027), currentOptInTaxYear)

            val optInProposition =
              SignUpProposition(currentOptInTaxYear, nextOptInTaxYear)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(SignUpJourney)))
              .thenReturn(Future(Some(retrievedUiSessionData)))

            when(mockItsaStatusUpdateConnector.signUp(any(), any())(any()))
              .thenReturn(Future(ITSAStatusUpdateResponseSuccess()))

            when(mockOptInService.fetchSignUpProposition()(any(), any(), any()))
              .thenReturn(Future(optInProposition))

            when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
              .thenReturn(Future(()))

            val request = service.triggerSignUpRequest()

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseSuccess(204) }
          }
        }

        "the user has selected CY+1 as the desired tax year - success scenario" should {

          "return a success response - 204" in {

            val optInContextData =
              SignUpContextData(
                currentTaxYear = "2025-2026",
                currentYearITSAStatus = "Annual",
                nextYearITSAStatus = "Annual"
              )

            val currentTaxYear = TaxYear(2025, 2026)

            val selectedOptInYear = Some("2026-2027") // user did not pick a tax year but should have
            val optInSessionData = SignUpSessionData(Some(optInContextData), selectedSignUpYear = selectedOptInYear)

            val retrievedUiSessionData =
              UIJourneySessionData(
                sessionId = hc.sessionId.get.value,
                journeyType = Opt(SignUpJourney).toString,
                signUpSessionData = Some(optInSessionData)
              )

            val currentOptInTaxYear =
              CurrentSignUpTaxYear(Annual, TaxYear(2025, 2026))

            val nextOptInTaxYear =
              NextSignUpTaxYear(Annual, TaxYear(2026, 2027), currentOptInTaxYear)

            val optInProposition =
              SignUpProposition(currentOptInTaxYear, nextOptInTaxYear)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(SignUpJourney)))
              .thenReturn(Future(Some(retrievedUiSessionData)))

            when(mockItsaStatusUpdateConnector.signUp(any(), any())(any()))
              .thenReturn(Future(ITSAStatusUpdateResponseFailure.defaultFailure()))

            when(mockOptInService.fetchSignUpProposition()(any(), any(), any()))
              .thenReturn(Future(optInProposition))

            when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
              .thenReturn(Future(()))

            val request = service.triggerSignUpRequest()

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseFailure(List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to unknown reason"))) }
          }
        }
      }
    }


    ".sendSignUpRequest" when {

      "there's no selected tax year" should {

        "return an error response" in {
          val emptyTaxYearSeq = Seq.empty

          val actual = service.sendSignUpRequest(signUpTaxYears = emptyTaxYearSeq, nino = "AB123456A")

          whenReady(actual) { result =>
            result shouldBe ITSAStatusUpdateResponseFailure.defaultFailure()
          }
        }
      }
    }
  }
}
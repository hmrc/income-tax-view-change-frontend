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
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ErrorItem, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import connectors.itsastatus.{ITSAStatusUpdateConnector, ITSAStatusUpdateConnectorModel}
import enums.JourneyType.{Opt, OptInJourney}
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, Voluntary}
import models.optin.{OptInContextData, OptInSessionData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach, OneInstancePerTest}
import play.mvc.Http.Status.NO_CONTENT
import repositories.UIJourneySessionDataRepository
import services.DateService
import services.optIn.core.{CurrentOptInTaxYear, NextOptInTaxYear, OptInProposition}
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
  val mockOptInService: OptInService = mock(classOf[OptInService])
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

          val request = service.getOptInSessionData()
          whenReady(request) { result => result shouldBe Some(optInSessionData) }
        }
      }

      "there is NO data present in db" should {

        "return None" in {

          when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney)))
            .thenReturn(Future(None))

          val request = service.getOptInSessionData()
          whenReady(request) { result => result shouldBe None }
        }
      }
    }

    ".makeUpdateRequest()" when {

      "selected tax year == CY" when {

        "both CY & CY+1 are Annual" should {

          "return success response with No Content - 204" in {

            val currentTaxYear = TaxYear(2025, 2026)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockItsaStatusUpdateConnector.optIn(any(), any())(any()))
              .thenReturn(Future.successful(ITSAStatusUpdateResponseSuccess(NO_CONTENT)))

            val request: Future[ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponse] =
              service.makeUpdateRequest(
                selectedSignUpYear = Some(currentTaxYear),
                currentYearItsaStatus = Some(Annual),
                nextYearItsaStatus = Some(Annual)
              )

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseSuccess(204) } // 204 No Content
          }
        }

        "only CY is Annual - single tax year scenario, user is unable to select a yax year" should {

          "return success response with No Content - 204" in {

            val currentTaxYear = TaxYear(2025, 2026)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockItsaStatusUpdateConnector.optIn(any(), any())(any()))
              .thenReturn(Future.successful(ITSAStatusUpdateResponseSuccess(NO_CONTENT)))

            val request: Future[ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponse] =
              service.makeUpdateRequest(
                selectedSignUpYear = None,
                currentYearItsaStatus = Some(Annual),
                nextYearItsaStatus = Some(Voluntary)
              )

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseSuccess(204) } // 204 No Content
          }
        }

        "only CY+1 is Annual - single tax year scenario, user is unable to select a yax year" should {

          "return success response with No Content - 204" in {

            val currentTaxYear = TaxYear(2025, 2026)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockItsaStatusUpdateConnector.optIn(any(), any())(any()))
              .thenReturn(Future.successful(ITSAStatusUpdateResponseSuccess(NO_CONTENT)))

            val request: Future[ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponse] =
              service.makeUpdateRequest(
                selectedSignUpYear = None,
                currentYearItsaStatus = Some(Voluntary),
                nextYearItsaStatus = Some(Annual)
              )

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseSuccess(204) } // 204 No Content
          }
        }

        "Neither CY or CY+1 are Annual - invalid sign up scenario" should {

          "return success response with No Content - 204" in {

            val currentTaxYear = TaxYear(2025, 2026)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockItsaStatusUpdateConnector.optIn(any(), any())(any()))
              .thenReturn(Future.successful(ITSAStatusUpdateResponseSuccess(NO_CONTENT)))

            val request: Future[ITSAStatusUpdateConnectorModel.ITSAStatusUpdateResponse] =
              service.makeUpdateRequest(
                selectedSignUpYear = None,
                currentYearItsaStatus = Some(Voluntary),
                nextYearItsaStatus = Some(Voluntary)
              )

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseFailure.defaultFailure() }
          }
        }
      }
    }

    ".triggerOptInRequest()" when {

      "single tax year - CY" when {

        "the user has selected CY as the sign up tax year" should {

          "successfully make a Sign Up request" in {

            val optInContextData =
              OptInContextData(
                currentTaxYear = "2025-2026",
                currentYearITSAStatus = "Annual",
                nextYearITSAStatus = "MTD Voluntary"
              )

            val currentTaxYear = TaxYear(2025, 2026)

            val selectedOptInYear = None // user cannot select tax year for a single tax year scenario
            val optInSessionData = OptInSessionData(Some(optInContextData), selectedOptInYear = selectedOptInYear)

            val retrievedUiSessionData =
              UIJourneySessionData(
                sessionId = hc.sessionId.get.value,
                journeyType = Opt(OptInJourney).toString,
                optInSessionData = Some(optInSessionData)
              )

            val currentOptInTaxYear =
              CurrentOptInTaxYear(Annual, TaxYear(2025, 2026))

            val nextOptInTaxYear =
              NextOptInTaxYear(Voluntary, TaxYear(2026, 2027), currentOptInTaxYear)

            val optInProposition =
              OptInProposition(currentOptInTaxYear, nextOptInTaxYear)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney)))
              .thenReturn(Future(Some(retrievedUiSessionData)))

            when(mockOptInService.fetchOptInProposition()(any(), any(), any()))
              .thenReturn(Future(optInProposition))

            when(mockItsaStatusUpdateConnector.optIn(any(), any())(any()))
              .thenReturn(Future(ITSAStatusUpdateResponseSuccess(NO_CONTENT)))

            when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
              .thenReturn(Future(()))

            val request = service.triggerSignUpRequest()

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseSuccess(204) } // 204 No Content
          }
        }
      }

      "single tax year - CY+1" when {

        "the user has selected CY+1 as the sign up tax year" should {

          "successfully make a Sign up request" in {

            val optInContextData =
              OptInContextData(
                currentTaxYear = "2025-2026",
                currentYearITSAStatus = "MTD Voluntary",
                nextYearITSAStatus = "Annual"
              )

            val currentTaxYear = TaxYear(2025, 2026)

            val selectedOptInYear = None // user cannot select tax year for a single tax year scenario
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
              NextOptInTaxYear(Annual, TaxYear(2026, 2027), currentOptInTaxYear)

            val optInProposition =
              OptInProposition(currentOptInTaxYear, nextOptInTaxYear)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney)))
              .thenReturn(Future(Some(retrievedUiSessionData)))

            when(mockOptInService.fetchOptInProposition()(any(), any(), any()))
              .thenReturn(Future(optInProposition))

            when(mockItsaStatusUpdateConnector.optIn(any(), any())(any()))
              .thenReturn(Future(ITSAStatusUpdateResponseSuccess()))

            when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
              .thenReturn(Future(()))

            val request = service.triggerSignUpRequest()

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseSuccess(204) } // 204 No Content
          }
        }
      }


      "multiple tax years -  CY & CY+1" when {

        "the user has selected CY as the desired tax year - success scenario" should {

          "return a success response - 204" in {

            val optInContextData =
              OptInContextData(
                currentTaxYear = "2025-2026",
                currentYearITSAStatus = "Annual",
                nextYearITSAStatus = "Annual"
              )

            val currentTaxYear = TaxYear(2025, 2026)

            val selectedOptInYear = Some("2025-2026") // user did not pick a tax year but should have
            val optInSessionData = OptInSessionData(Some(optInContextData), selectedOptInYear = selectedOptInYear)

            val retrievedUiSessionData =
              UIJourneySessionData(
                sessionId = hc.sessionId.get.value,
                journeyType = Opt(OptInJourney).toString,
                optInSessionData = Some(optInSessionData)
              )

            val currentOptInTaxYear =
              CurrentOptInTaxYear(Annual, TaxYear(2025, 2026))

            val nextOptInTaxYear =
              NextOptInTaxYear(Annual, TaxYear(2026, 2027), currentOptInTaxYear)

            val optInProposition =
              OptInProposition(currentOptInTaxYear, nextOptInTaxYear)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney)))
              .thenReturn(Future(Some(retrievedUiSessionData)))

            when(mockItsaStatusUpdateConnector.optIn(any(), any())(any()))
              .thenReturn(Future(ITSAStatusUpdateResponseFailure.defaultFailure()))

            when(mockOptInService.fetchOptInProposition()(any(), any(), any()))
              .thenReturn(Future(optInProposition))

            when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
              .thenReturn(Future(()))

            val request = service.triggerSignUpRequest()

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseFailure(List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to unknown reason"))) }
          }
        }

        "the user has selected CY+1 as the desired tax year - success scenario" should {

          "return a success response - 204" in {

            val optInContextData =
              OptInContextData(
                currentTaxYear = "2025-2026",
                currentYearITSAStatus = "Annual",
                nextYearITSAStatus = "Annual"
              )

            val currentTaxYear = TaxYear(2025, 2026)

            val selectedOptInYear = Some("2026-2027") // user did not pick a tax year but should have
            val optInSessionData = OptInSessionData(Some(optInContextData), selectedOptInYear = selectedOptInYear)

            val retrievedUiSessionData =
              UIJourneySessionData(
                sessionId = hc.sessionId.get.value,
                journeyType = Opt(OptInJourney).toString,
                optInSessionData = Some(optInSessionData)
              )

            val currentOptInTaxYear =
              CurrentOptInTaxYear(Annual, TaxYear(2025, 2026))

            val nextOptInTaxYear =
              NextOptInTaxYear(Annual, TaxYear(2026, 2027), currentOptInTaxYear)

            val optInProposition =
              OptInProposition(currentOptInTaxYear, nextOptInTaxYear)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney)))
              .thenReturn(Future(Some(retrievedUiSessionData)))

            when(mockItsaStatusUpdateConnector.optIn(any(), any())(any()))
              .thenReturn(Future(ITSAStatusUpdateResponseFailure.defaultFailure()))

            when(mockOptInService.fetchOptInProposition()(any(), any(), any()))
              .thenReturn(Future(optInProposition))

            when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
              .thenReturn(Future(()))

            val request = service.triggerSignUpRequest()

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseFailure(List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to unknown reason"))) }
          }
        }

        "the user has not selected a tax year - error scenario" should {

          "not make a request to connector and return default error" in {

            val optInContextData =
              OptInContextData(
                currentTaxYear = "2025-2026",
                currentYearITSAStatus = "Annual",
                nextYearITSAStatus = "Annual"
              )

            val currentTaxYear = TaxYear(2025, 2026)

            val selectedOptInYear = None // user did not pick a tax year but should have
            val optInSessionData = OptInSessionData(Some(optInContextData), selectedOptInYear = selectedOptInYear)

            val retrievedUiSessionData =
              UIJourneySessionData(
                sessionId = hc.sessionId.get.value,
                journeyType = Opt(OptInJourney).toString,
                optInSessionData = Some(optInSessionData)
              )

            val currentOptInTaxYear =
              CurrentOptInTaxYear(Annual, TaxYear(2025, 2026))

            val nextOptInTaxYear =
              NextOptInTaxYear(Annual, TaxYear(2026, 2027), currentOptInTaxYear)

            val optInProposition =
              OptInProposition(currentOptInTaxYear, nextOptInTaxYear)

            when(mockDateService.getCurrentTaxYear)
              .thenReturn(currentTaxYear)

            when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptInJourney)))
              .thenReturn(Future(Some(retrievedUiSessionData)))

            when(mockItsaStatusUpdateConnector.optIn(any(), any())(any()))
              .thenReturn(Future(ITSAStatusUpdateResponseFailure.defaultFailure()))

            when(mockOptInService.fetchOptInProposition()(any(), any(), any()))
              .thenReturn(Future(optInProposition))

            when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
              .thenReturn(Future(()))

            val request = service.triggerSignUpRequest()

            whenReady(request) { result => result shouldBe ITSAStatusUpdateResponseFailure(List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to unknown reason"))) }
          }
        }
      }
    }
  }
}
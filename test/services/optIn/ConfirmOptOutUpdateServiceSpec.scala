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

import audit.mocks.MockAuditingService
import audit.models.{OptOutCompleteAuditModel, Outcome}
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import enums.JourneyType.{Opt, OptOutJourney}
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockITSAStatusUpdateConnector}
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import models.optout.{OptOutSessionData, OptOutYearToUpdate}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.{BeforeAndAfter, OneInstancePerTest}
import repositories.{OptOutContextData, UIJourneySessionDataRepository}
import services.optout.ConfirmOptOutUpdateService
import testConstants.BaseTestConstants.testSessionId
import testUtils.UnitSpec
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.Future

class ConfirmOptOutUpdateServiceSpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with OneInstancePerTest
  with MockITSAStatusUpdateConnector
  with MockAuditingService {

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))

  val mockItsaStatusUpdateConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val mockUiJourneySessionDataRepository: UIJourneySessionDataRepository = mock(classOf[UIJourneySessionDataRepository])

  val service: ConfirmOptOutUpdateService =
    new ConfirmOptOutUpdateService(mockAuditingService, mockItsaStatusUpdateConnector, mockUiJourneySessionDataRepository)

  val currentTaxYear = TaxYear(2025, 2026)
  val previousTaxYear = currentTaxYear.previousYear
  val nextTaxYear = currentTaxYear.nextYear

  "ConfirmOptOutUpdateService" when {

    ".getOptOutSessionData()" when {

      "there is data pre-existing in db" should {

        "return the correct session data" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = true,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = Some("2025-2026"))

          val retrievedUiSessionData =
            UIJourneySessionData(
              sessionId = hc.sessionId.get.value,
              journeyType = Opt(OptOutJourney).toString,
              optOutSessionData = Some(optOutSessionData)
            )

          when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptOutJourney))).thenReturn(Future(Some(retrievedUiSessionData)))

          val actual = service.getOptOutSessionData()
          whenReady(actual) { result => result shouldBe Some(optOutSessionData) }
        }
      }

      "there is NO data present in db" should {

        "return None" in {

          when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptOutJourney))).thenReturn(Future(None))

          val actual = service.getOptOutSessionData()
          whenReady(actual) { result => result shouldBe None }
        }
      }
    }

    ".optOutResponseOutcomes()" when {

      "given a list of successful ITSA responses" should {

        "return the correct list of Outcomes with only isSuccessful == true" in {

          val listOfSuccessResponses = List(ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess())
          val result = service.optOutResponseOutcomes(listOfSuccessResponses)
          val expected = List(Outcome(true, None, None), Outcome(true, None, None), Outcome(true, None, None))

          result shouldBe expected
        }
      }

      "given a list of a mix of success and failure ITSA responses" should {

        "return the correct list with a mix of success and failure Outcomes - List(fail - success - fail)" in {

          val listOfSuccessResponses = List(ITSAStatusUpdateResponseFailure.defaultFailure(), ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseFailure.defaultFailure())
          val result = service.optOutResponseOutcomes(listOfSuccessResponses)
          val expected = List(Outcome(false, Some("INTERNAL_SERVER_ERROR"), Some("Request failed due to unknown reason")), Outcome(true, None, None), Outcome(false, Some("INTERNAL_SERVER_ERROR"), Some("Request failed due to unknown reason")))
          result shouldBe expected
        }

        "return the correct list with a mix of success and failure Outcomes - List(fail - success - success)" in {

          val listOfSuccessResponses = List(ITSAStatusUpdateResponseFailure.defaultFailure(), ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess())
          val result = service.optOutResponseOutcomes(listOfSuccessResponses)
          val expected = List(Outcome(false, Some("INTERNAL_SERVER_ERROR"), Some("Request failed due to unknown reason")), Outcome(true, None, None), Outcome(true, None, None))
          result shouldBe expected
        }
      }

      "given a list of only failure ITSA responses" should {

        "return the correct list of Outcomes with failures" in {

          val listOfSuccessResponses = List(ITSAStatusUpdateResponseFailure.defaultFailure(), ITSAStatusUpdateResponseFailure.defaultFailure(), ITSAStatusUpdateResponseFailure.defaultFailure())
          val result = service.optOutResponseOutcomes(listOfSuccessResponses)
          val expected = List(Outcome(false, Some("INTERNAL_SERVER_ERROR"), Some("Request failed due to unknown reason")), Outcome(false, Some("INTERNAL_SERVER_ERROR"), Some("Request failed due to unknown reason")), Outcome(false, Some("INTERNAL_SERVER_ERROR"), Some("Request failed due to unknown reason")))
          result shouldBe expected
        }
      }
    }

    ".getOptOutYearsToUpdateWithStatuses()" when {

      "there is OptOutContextData given for a user account" should {

        "return the correct list of tax years to update for the opt out scenario - (CY-1, CY & CY+1)" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = true,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = Some("2025-2026"))

          val retrievedUiSessionData =
            UIJourneySessionData(
              sessionId = hc.sessionId.get.value,
              journeyType = Opt(OptOutJourney).toString,
              optOutSessionData = Some(optOutSessionData)
            )

          when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptOutJourney))).thenReturn(Future(Some(retrievedUiSessionData)))

          val result = service.getOptOutYearsToUpdateWithStatuses(Some(optOutContextData))
          result shouldBe List(
            OptOutYearToUpdate(TaxYear(2024, 2025), Voluntary),
            OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
            OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
          )
        }
      }

      "there is NO OptOutContextData given for a user account" should {

        "return an empty List" in {

          val noOptOutContextData = None
          val result = service.getOptOutYearsToUpdateWithStatuses(noOptOutContextData)

          result shouldBe List.empty
        }
      }
    }

    ".correctYearsToUpdateBasedOnUserSelection()" when {

      "the user selects the Previous TaxYear to update - CY-1 && crystallisationStatus = true - (scenario should not be possible, but testing regardless)" should {

        "return the full list" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = true,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val userSelection = Some("2024-2025")
          val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = userSelection)

          val allYearsToUpdate =
            List(
              OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
              OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val expected = List.empty

          val result = service.correctYearsToUpdateBasedOnUserSelection(Some(optOutSessionData), allYearsToUpdate)
          result shouldBe expected
        }
      }

      "the user selects the Previous TaxYear to update - CY-1 && crystallisationStatus = false" should {

        "return all 3 years to opt out from" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = false,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val userSelection = Some("2024-2025")
          val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = userSelection)

          val allYearsToUpdate =
            List(
              OptOutYearToUpdate(TaxYear(2024, 2025), Voluntary),
              OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val expected = allYearsToUpdate

          val result = service.correctYearsToUpdateBasedOnUserSelection(Some(optOutSessionData), allYearsToUpdate)
          result shouldBe expected
        }
      }

      "the user selects the Current TaxYear to update - CY" should {

        "return only CY onwards to opt out from" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = true,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val userSelection = Some("2025-2026")
          val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = userSelection)

          val allYearsToUpdate =
            List(
              OptOutYearToUpdate(TaxYear(2024, 2025), Voluntary),
              OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val expected =
            List(
              OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val result = service.correctYearsToUpdateBasedOnUserSelection(Some(optOutSessionData), allYearsToUpdate)
          result shouldBe expected
        }
      }

      "the user selects the Next TaxYear to update - CY+1" should {

        "return only CY+1 to opt out from" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = true,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val userSelection = Some("2026-2027")
          val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = userSelection)

          val allYearsToUpdate =
            List(
              OptOutYearToUpdate(TaxYear(2024, 2025), Voluntary),
              OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val expected =
            List(
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val result = service.correctYearsToUpdateBasedOnUserSelection(Some(optOutSessionData), allYearsToUpdate)
          result shouldBe expected
        }
      }

      "the user does not select a tax year to OptOut" should {

        "return all tax years" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = true,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "No Status",
              nextYearITSAStatus = "No Status"
            )

          val allYearsToUpdate =
            List(
              OptOutYearToUpdate(TaxYear(2024, 2025), NoStatus),
              OptOutYearToUpdate(TaxYear(2025, 2026), NoStatus),
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = None)
          val result = service.correctYearsToUpdateBasedOnUserSelection(Some(optOutSessionData), allYearsToUpdate)

          result shouldBe allYearsToUpdate
        }
      }

      "there is NO OptOutContextData given for a user account" should {

        "return an empty List" in {

          val optOutContextData =  None
          val allYearsToUpdate = service.getOptOutYearsToUpdateWithStatuses(optOutContextData)

          val optOutSessionData = OptOutSessionData(None, selectedOptOutYear = Some("2024-2025"))

          val result = service.correctYearsToUpdateBasedOnUserSelection(Some(optOutSessionData), allYearsToUpdate)

          result shouldBe List.empty
        }
      }

      "there is NO choice to select a tax year given for a user account - single year opt out scenario" should {

        "return the full List - we filter later on in .updateTaxYearsITSAStatusRequest() for the desire status" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = false,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val allYearsToUpdate =
            List(
              OptOutYearToUpdate(TaxYear(2024, 2025), Voluntary),
              OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = None)

          val result = service.correctYearsToUpdateBasedOnUserSelection(Some(optOutSessionData), allYearsToUpdate)
          val expected = allYearsToUpdate

          result shouldBe expected
        }
      }

    }

    ".createAuditEvent()" when {

      "successfully made an update request and received a successful update response back from connector" should {

        "return the OptOutSubmissionAuditEvent with Outcomes with success" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = true,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val userSelection = Some("2024-2025")
          val listOfSuccessResponses = List(ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess())

          val allYearsToUpdate =
            List(
              OptOutYearToUpdate(TaxYear(2024, 2025), Voluntary),
              OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val expectedAuditEvent =
            OptOutCompleteAuditModel(
              saUtr = Some("1234567890"),
              credId = Some("testCredId"),
              userType = Some(Individual),
              agentReferenceNumber = None,
              mtditid = "XAIT0000123456",
              nino = "AB123456C",
              outcome =
                List(
                  Outcome(isSuccessful = true, None, None),
                  Outcome(isSuccessful = true, None, None),
                  Outcome(isSuccessful = true, None, None)
                ),
              optOutRequestedFromTaxYear = "2024-2025",
              currentYear = "25-26",
              `beforeITSAStatusCurrentYear-1` = Voluntary,
              beforeITSAStatusCurrentYear = Voluntary,
              `beforeITSAStatusCurrentYear+1` = Voluntary,
              `afterAssumedITSAStatusCurrentYear-1` = Some(Annual),
              afterAssumedITSAStatusCurrentYear = Some(Annual),
              `afterAssumedITSAStatusCurrentYear+1` = Some(Annual),
              `currentYear-1Crystallised` = true
            )

          val result = service.createAuditEvent(
            mayBeSelectedTaxYear = userSelection,
            mayBeOptOutContextData = Some(optOutContextData),
            filteredTaxYearsForDesiredItsaStatus = allYearsToUpdate,
            updateRequestsForEachYearResponse = listOfSuccessResponses
          )

          result shouldBe expectedAuditEvent
        }
      }

      "successfully made an update request and received a failure update response back from connector" should {

        "return the OptOutSubmissionAuditEvent model with failure Outcomes" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = true,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val userSelection = Some("2024-2025")
          val listOfSuccessResponses = List(ITSAStatusUpdateResponseFailure.defaultFailure(), ITSAStatusUpdateResponseFailure.defaultFailure(), ITSAStatusUpdateResponseFailure.defaultFailure())

          val allYearsToUpdate =
            List(
              OptOutYearToUpdate(TaxYear(2024, 2025), Voluntary),
              OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val expectedAuditEvent =
            OptOutCompleteAuditModel(
              saUtr = Some("1234567890"),
              credId = Some("testCredId"),
              userType = Some(Individual),
              agentReferenceNumber = None,
              mtditid = "XAIT0000123456",
              nino = "AB123456C",
              outcome =
                List(
                  Outcome(isSuccessful = false, Some("INTERNAL_SERVER_ERROR"), Some("Request failed due to unknown reason")),
                  Outcome(isSuccessful = false, Some("INTERNAL_SERVER_ERROR"), Some("Request failed due to unknown reason")),
                  Outcome(isSuccessful = false, Some("INTERNAL_SERVER_ERROR"), Some("Request failed due to unknown reason"))
                ),
              optOutRequestedFromTaxYear = "2024-2025",
              currentYear = "25-26",
              `beforeITSAStatusCurrentYear-1` = Voluntary,
              beforeITSAStatusCurrentYear = Voluntary,
              `beforeITSAStatusCurrentYear+1` = Voluntary,
              `afterAssumedITSAStatusCurrentYear-1` = Some(Annual),
              afterAssumedITSAStatusCurrentYear = Some(Annual),
              `afterAssumedITSAStatusCurrentYear+1` = Some(Annual),
              `currentYear-1Crystallised` = true
            )

          val result = service.createAuditEvent(
            mayBeSelectedTaxYear = userSelection,
            mayBeOptOutContextData = Some(optOutContextData),
            filteredTaxYearsForDesiredItsaStatus = allYearsToUpdate,
            updateRequestsForEachYearResponse = listOfSuccessResponses
          )

          result shouldBe expectedAuditEvent
        }
      }

      "for a successful request - if the user selects the CURRENT tax year, update only CY & CY+1" should {

        "return the OptOutSubmissionAuditEvent with Outcomes with success" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = true,
              previousYearITSAStatus = "MTD Mandated",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val userSelection = Some("2025-2026")
          val listOfSuccessResponses = List(ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess())

          val allYearsToUpdate =
            List(
              OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val expectedAuditEvent =
            OptOutCompleteAuditModel(
              saUtr = Some("1234567890"),
              credId = Some("testCredId"),
              userType = Some(Individual),
              agentReferenceNumber = None,
              mtditid = "XAIT0000123456",
              nino = "AB123456C",
              outcome =
                List(
                  Outcome(isSuccessful = true, None, None),
                  Outcome(isSuccessful = true, None, None)
                ),
              optOutRequestedFromTaxYear = "2025-2026",
              currentYear = "25-26",
              `beforeITSAStatusCurrentYear-1` = Mandated,
              beforeITSAStatusCurrentYear = Voluntary,
              `beforeITSAStatusCurrentYear+1` = Voluntary,
              `afterAssumedITSAStatusCurrentYear-1` = Some(Mandated),
              afterAssumedITSAStatusCurrentYear = Some(Annual),
              `afterAssumedITSAStatusCurrentYear+1` = Some(Annual),
              `currentYear-1Crystallised` = true
            )

          val result = service.createAuditEvent(
            mayBeSelectedTaxYear = userSelection,
            mayBeOptOutContextData = Some(optOutContextData),
            filteredTaxYearsForDesiredItsaStatus = allYearsToUpdate,
            updateRequestsForEachYearResponse = listOfSuccessResponses
          )

          result shouldBe expectedAuditEvent
        }
      }

      "for a successful request - if the user selects the PREVIOUS tax year && crystallisationStatus = false, update all tax years CY-1, CY & CY+1" should {

        "return the OptOutSubmissionAuditEvent with Outcomes with success" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = false,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val userSelection = Some("2024-2025")
          val listOfSuccessResponses = List(ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess())

          val allYearsToUpdate =
            List(
              OptOutYearToUpdate(TaxYear(2024, 2025), Voluntary),
              OptOutYearToUpdate(TaxYear(2025, 2026), Voluntary),
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val expectedAuditEvent =
            OptOutCompleteAuditModel(
              saUtr = Some("1234567890"),
              credId = Some("testCredId"),
              userType = Some(Individual),
              agentReferenceNumber = None,
              mtditid = "XAIT0000123456",
              nino = "AB123456C",
              outcome =
                List(
                  Outcome(isSuccessful = true, None, None),
                  Outcome(isSuccessful = true, None, None),
                  Outcome(isSuccessful = true, None, None)
                ),
              optOutRequestedFromTaxYear = "2024-2025",
              currentYear = "25-26",
              `beforeITSAStatusCurrentYear-1` = Voluntary,
              beforeITSAStatusCurrentYear = Voluntary,
              `beforeITSAStatusCurrentYear+1` = Voluntary,
              `afterAssumedITSAStatusCurrentYear-1` = Some(Annual),
              afterAssumedITSAStatusCurrentYear = Some(Annual),
              `afterAssumedITSAStatusCurrentYear+1` = Some(Annual),
              `currentYear-1Crystallised` = false
            )

          val result = service.createAuditEvent(
            mayBeSelectedTaxYear = userSelection,
            mayBeOptOutContextData = Some(optOutContextData),
            filteredTaxYearsForDesiredItsaStatus = allYearsToUpdate,
            updateRequestsForEachYearResponse = listOfSuccessResponses
          )

          result shouldBe expectedAuditEvent
        }
      }

      "if the user selects the PREVIOUS tax year && crystallisationStatus = true - (should not be possible)" should {

        "return the OptOutSubmissionAuditEvent model with NO updates to the account ITSA statuses & no response outcomes, since no requests should have been made" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = true,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val userSelection = Some("2024-2025")
          val listOfSuccessResponses = List()

          val allYearsToUpdate = List()

          val expectedAuditEvent =
            OptOutCompleteAuditModel(
              saUtr = Some("1234567890"),
              credId = Some("testCredId"),
              userType = Some(Individual),
              agentReferenceNumber = None,
              mtditid = "XAIT0000123456",
              nino = "AB123456C",
              outcome = List(),
              optOutRequestedFromTaxYear = "2024-2025",
              currentYear = "25-26",
              `beforeITSAStatusCurrentYear-1` = Voluntary,
              beforeITSAStatusCurrentYear = Voluntary,
              `beforeITSAStatusCurrentYear+1` = Voluntary,
              `afterAssumedITSAStatusCurrentYear-1` = Some(Voluntary),
              afterAssumedITSAStatusCurrentYear = Some(Voluntary),
              `afterAssumedITSAStatusCurrentYear+1` = Some(Voluntary),
              `currentYear-1Crystallised` = true
            )

          val result = service.createAuditEvent(
            mayBeSelectedTaxYear = userSelection,
            mayBeOptOutContextData = Some(optOutContextData),
            filteredTaxYearsForDesiredItsaStatus = allYearsToUpdate,
            updateRequestsForEachYearResponse = listOfSuccessResponses
          )

          result shouldBe expectedAuditEvent
        }
      }

      "for a successful request - if the user selects the NEXT tax year, update only CY+1" should {

        "return the OptOutSubmissionAuditEvent with Outcomes with success" in {

          val optOutContextData =
            OptOutContextData(
              currentYear = "2025-2026",
              crystallisationStatus = true,
              previousYearITSAStatus = "MTD Voluntary",
              currentYearITSAStatus = "MTD Voluntary",
              nextYearITSAStatus = "MTD Voluntary"
            )

          val userSelection = Some("2026-2027")
          val listOfSuccessResponses = List(ITSAStatusUpdateResponseSuccess())

          val allYearsToUpdate =
            List(
              OptOutYearToUpdate(TaxYear(2026, 2027), Voluntary)
            )

          val expectedAuditEvent =
            OptOutCompleteAuditModel(
              saUtr = Some("1234567890"),
              credId = Some("testCredId"),
              userType = Some(Individual),
              agentReferenceNumber = None,
              mtditid = "XAIT0000123456",
              nino = "AB123456C",
              outcome =
                List(
                  Outcome(isSuccessful = true, None, None)
                ),
              optOutRequestedFromTaxYear = "2026-2027",
              currentYear = "25-26",
              `beforeITSAStatusCurrentYear-1` = Voluntary,
              beforeITSAStatusCurrentYear = Voluntary,
              `beforeITSAStatusCurrentYear+1` = Voluntary,
              `afterAssumedITSAStatusCurrentYear-1` = Some(Voluntary),
              afterAssumedITSAStatusCurrentYear = Some(Voluntary),
              `afterAssumedITSAStatusCurrentYear+1` = Some(Annual),
              `currentYear-1Crystallised` = true
            )

          val result = service.createAuditEvent(
            mayBeSelectedTaxYear = userSelection,
            mayBeOptOutContextData = Some(optOutContextData),
            filteredTaxYearsForDesiredItsaStatus = allYearsToUpdate,
            updateRequestsForEachYearResponse = listOfSuccessResponses
          )

          result shouldBe expectedAuditEvent
        }
      }
    }

    ".updateTaxYearsITSAStatusRequest()" when {

      "desired ITSA Statuses == Voluntary" when {

        "CY-1 is the chosen as the tax year to opt out from" when {

          "user account is of MTD Voluntary status only for all tax years && is NOT Crystallised" should {

            "return the correct responses of 3 NoContent - 204" in {

              val optOutContextData =
                OptOutContextData(
                  currentYear = "2025-2026",
                  crystallisationStatus = false,
                  previousYearITSAStatus = "MTD Voluntary",
                  currentYearITSAStatus = "MTD Voluntary",
                  nextYearITSAStatus = "MTD Voluntary"
                )

              val selectedOptOutYear = Some("2024-2025")
              val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = selectedOptOutYear)

              val retrievedUiSessionData =
                UIJourneySessionData(
                  sessionId = hc.sessionId.get.value,
                  journeyType = Opt(OptOutJourney).toString,
                  optOutSessionData = Some(optOutSessionData)
                )

              when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptOutJourney)))
                .thenReturn(
                  Future(Some(retrievedUiSessionData))
                )

              when(mockItsaStatusUpdateConnector.makeMultipleItsaStatusUpdateRequests(any(), any(), any())(any()))
                .thenReturn(
                  Future(List(ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess()))
                )

              when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
                .thenReturn(Future(()))

              val desiredStatus = Voluntary
              val actual = service.updateTaxYearsITSAStatusRequest(desiredStatus)

              whenReady(actual) { result =>
                result shouldBe List(ITSAStatusUpdateResponseSuccess(204), ITSAStatusUpdateResponseSuccess(204), ITSAStatusUpdateResponseSuccess(204))
              }

            }
          }

          "user account is of MTD Voluntary status only for all tax years && is Crystallised" should {

            "return the correct responses of 2 NoContent - 204" in {

              val optOutContextData =
                OptOutContextData(
                  currentYear = "2025-2026",
                  crystallisationStatus = false,
                  previousYearITSAStatus = "MTD Voluntary",
                  currentYearITSAStatus = "MTD Voluntary",
                  nextYearITSAStatus = "MTD Voluntary"
                )

              val selectedOptOutYear = Some("2024-2025")
              val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = selectedOptOutYear)

              val retrievedUiSessionData =
                UIJourneySessionData(
                  sessionId = hc.sessionId.get.value,
                  journeyType = Opt(OptOutJourney).toString,
                  optOutSessionData = Some(optOutSessionData)
                )

              when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptOutJourney)))
                .thenReturn(
                  Future(Some(retrievedUiSessionData))
                )

              when(mockItsaStatusUpdateConnector.makeMultipleItsaStatusUpdateRequests(any(), any(), any())(any()))
                .thenReturn(
                  Future(List(ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess()))
                )

              when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
                .thenReturn(Future(()))

              val desiredStatus = Voluntary
              val actual = service.updateTaxYearsITSAStatusRequest(desiredStatus)

              whenReady(actual) { result =>
                result shouldBe List(ITSAStatusUpdateResponseSuccess(204), ITSAStatusUpdateResponseSuccess(204))
              }

            }
          }

          "user account is of mixed ITSA statuses for each tax year but with 2 MTD Voluntary statuses" should {

            "return the correct responses of 2 NoContent - 204" in {

              val optOutContextData =
                OptOutContextData(
                  currentYear = "2025-2026",
                  crystallisationStatus = true,
                  previousYearITSAStatus = "MTD Mandated",
                  currentYearITSAStatus = "MTD Voluntary",
                  nextYearITSAStatus = "MTD Voluntary"
                )

              val selectedOptOutYear = Some("2024-2025")
              val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = selectedOptOutYear)

              val retrievedUiSessionData =
                UIJourneySessionData(
                  sessionId = hc.sessionId.get.value,
                  journeyType = Opt(OptOutJourney).toString,
                  optOutSessionData = Some(optOutSessionData)
                )

              when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptOutJourney)))
                .thenReturn(
                  Future(Some(retrievedUiSessionData))
                )

              when(mockItsaStatusUpdateConnector.makeMultipleItsaStatusUpdateRequests(any(), any(), any())(any()))
                .thenReturn(
                  Future(List(ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess()))
                )

              when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
                .thenReturn(Future(()))

              val desiredStatus = Voluntary
              val actual = service.updateTaxYearsITSAStatusRequest(desiredStatus)

              whenReady(actual) { result =>
                result shouldBe List(ITSAStatusUpdateResponseSuccess(204), ITSAStatusUpdateResponseSuccess(204))
              }

            }
          }
        }

        "CY is the chosen as the tax year to opt out from" when {

          "user account is of MTD Voluntary status only for CY & CY+1 && is NOT Crystallised" should {

            "return the correct responses of 2 NoContent - 204" in {

              val optOutContextData =
                OptOutContextData(
                  currentYear = "2025-2026",
                  crystallisationStatus = false,
                  previousYearITSAStatus = "Annual",
                  currentYearITSAStatus = "MTD Voluntary",
                  nextYearITSAStatus = "MTD Voluntary"
                )

              val selectedOptOutYear = Some("2025-2026")
              val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = selectedOptOutYear)

              val retrievedUiSessionData =
                UIJourneySessionData(
                  sessionId = hc.sessionId.get.value,
                  journeyType = Opt(OptOutJourney).toString,
                  optOutSessionData = Some(optOutSessionData)
                )

              when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptOutJourney)))
                .thenReturn(
                  Future(Some(retrievedUiSessionData))
                )

              when(mockItsaStatusUpdateConnector.makeMultipleItsaStatusUpdateRequests(any(), any(), any())(any()))
                .thenReturn(
                  Future(List(ITSAStatusUpdateResponseSuccess(), ITSAStatusUpdateResponseSuccess()))
                )

              when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
                .thenReturn(Future(()))

              val desiredStatus = Voluntary
              val actual = service.updateTaxYearsITSAStatusRequest(desiredStatus)

              whenReady(actual) { result =>
                result shouldBe List(ITSAStatusUpdateResponseSuccess(204), ITSAStatusUpdateResponseSuccess(204))
              }

            }
          }
        }

        "CY+1 is the chosen as the tax year to opt out from" when {

          "user account is of MTD Voluntary status only for CY+1 && is NOT Crystallised" should {

            "return the correct responses of 2 NoContent - 204" in {

              val optOutContextData =
                OptOutContextData(
                  currentYear = "2025-2026",
                  crystallisationStatus = false,
                  previousYearITSAStatus = "Annual",
                  currentYearITSAStatus = "Annual",
                  nextYearITSAStatus = "MTD Voluntary"
                )

              val selectedOptOutYear = Some("2025-2026")
              val optOutSessionData = OptOutSessionData(Some(optOutContextData), selectedOptOutYear = selectedOptOutYear)

              val retrievedUiSessionData =
                UIJourneySessionData(
                  sessionId = hc.sessionId.get.value,
                  journeyType = Opt(OptOutJourney).toString,
                  optOutSessionData = Some(optOutSessionData)
                )

              when(mockUiJourneySessionDataRepository.get(hc.sessionId.get.value, Opt(OptOutJourney)))
                .thenReturn(
                  Future(Some(retrievedUiSessionData))
                )

              when(mockItsaStatusUpdateConnector.makeMultipleItsaStatusUpdateRequests(any(), any(), any())(any()))
                .thenReturn(
                  Future(List(ITSAStatusUpdateResponseSuccess()))
                )

              when(mockAuditingService.extendedAudit(any(), any())(any(), any(), any()))
                .thenReturn(Future(()))

              val desiredStatus = Voluntary
              val actual = service.updateTaxYearsITSAStatusRequest(desiredStatus)

              whenReady(actual) { result =>
                result shouldBe List(ITSAStatusUpdateResponseSuccess(204))
              }

            }
          }
        }
      }

    }
  }
}

/*
 * Copyright 2026 HM Revenue & Customs
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

package services

import auth.MtdItUser
import authV2.AuthActionsTestData.{agentEnrolment, getAllEnrolmentsAgent, getAuthUserDetails, ninoEnrolment}
import config.featureswitch.FeatureSwitching
import connectors.{CalculationListConnector, IncomeTaxCalculationConnector}
import enums.{MTDIndividual, MTDPrimaryAgent}
import models.calculationList.{CalculationListErrorModel, CalculationListModel}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.liabilitycalculation._
import models.taxyearsummary._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{saEnrolment, testMtditid, testNino}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.Enrolments

import scala.concurrent.Future

class TaxYearSummaryServiceSpec extends TestSupport with FeatureSwitching {

  val mockCalculationListConnector: CalculationListConnector = mock[CalculationListConnector]
  val mockIncomeTaxCalculationConnector: IncomeTaxCalculationConnector = mock[IncomeTaxCalculationConnector]

  val service =
    new TaxYearSummaryService(
      calculationListConnector = mockCalculationListConnector,
      incomeTaxCalculationConnector = mockIncomeTaxCalculationConnector
    )

  "TaxYearSummaryService" when {

    ".determineCannotDisplayCalculationContentScenario()" when {

      "the tax year is after 2023" when {

        "no IRSA Enrolments and either api returns an error response" should {

          "return the NoIrsaAEnrolement view scenario" in {

            val nino = "fakeNino"
            val taxYear = TaxYear(2025, 2026)

            val calculationListModel =
              CalculationListModel(calculationId = "calc Id", calculationTimestamp = "calc timestamp", calculationType = "calc type", crystallised = Some(true))

            val liabilityCalculationError = LiabilityCalculationError(status = 404, message = "some error message")

            val noIRSAEnrolments =
              Enrolments(enrolments = Set(None, Some(ninoEnrolment), None).flatten)

            val individualUser =
              MtdItUser(
                mtditid = testMtditid,
                nino = testNino,
                usersRole = MTDIndividual,
                authUserDetails = getAuthUserDetails(Some(Individual), noIRSAEnrolments, hasUserName = true),
                clientDetails = None,
                incomeSources = IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty),
                btaNavPartial = None,
                featureSwitches = List()
              )(FakeRequest())

            when(mockCalculationListConnector.getLegacyCalculationList(any(), any())(any()))
              .thenReturn(Future(calculationListModel))

            when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any()))
              .thenReturn(Future(liabilityCalculationError))

            val actual = service.determineCannotDisplayCalculationContentScenario(nino, taxYear)(individualUser, headerCarrier, ec)
            val expected = NoIrsaAEnrolement

            whenReady(actual) { result =>
              result shouldBe expected
            }
          }
        }

        "user has IRSA Enrolments and either api returns an error response" should {

          "return the IrsaEnrolementHandedOff view scenario" in {

            val nino = "fakeNino"
            val taxYear = TaxYear(2025, 2026)

            val calculationListModel =
              CalculationListModel(calculationId = "calc Id", calculationTimestamp = "calc timestamp", calculationType = "calc type", crystallised = Some(true))

            val liabilityCalculationError = LiabilityCalculationError(status = 404, message = "some error message")



            val irsaEnrolments =
              Enrolments(enrolments = Set(None, Some(ninoEnrolment), Some(saEnrolment)).flatten)

            val individualUser =
              MtdItUser(
                mtditid = testMtditid,
                nino = testNino,
                usersRole = MTDIndividual,
                authUserDetails = getAuthUserDetails(Some(Individual), irsaEnrolments, hasUserName = true),
                clientDetails = None,
                incomeSources = IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty),
                btaNavPartial = None,
                featureSwitches = List()
              )(FakeRequest())

            when(mockCalculationListConnector.getLegacyCalculationList(any(), any())(any()))
              .thenReturn(Future(calculationListModel))

            when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any()))
              .thenReturn(Future(liabilityCalculationError))

            val actual = service.determineCannotDisplayCalculationContentScenario(nino, taxYear)(individualUser, headerCarrier, ec)
            val expected = IrsaEnrolementHandedOff

            whenReady(actual) { result =>
              result shouldBe expected
            }
          }
        }

        "both success responses" should {

          "return the Default view scenario" in {

            val nino = "fakeNino"
            val taxYear = TaxYear(2025, 2026)

            val calculationListModel =
              CalculationListModel(calculationId = "calc Id", calculationTimestamp = "calc timestamp", calculationType = "calc type", crystallised = Some(true))

            val liabilityCalculationResponse =
              LiabilityCalculationResponse(
                inputs = Inputs(PersonalInformation(taxRegime = "", class2VoluntaryContributions = Some(true))),
                metadata = Metadata(
                  calculationTimestamp = Some(""),
                  calculationType = ""
                ),
                messages = None,
                calculation = None,
              )

            when(mockCalculationListConnector.getLegacyCalculationList(any(), any())(any()))
              .thenReturn(Future(calculationListModel))

            when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any()))
              .thenReturn(Future(liabilityCalculationResponse))

            val actual = service.determineCannotDisplayCalculationContentScenario(nino, taxYear)
            val expected = Default
            whenReady(actual) { result =>
              result shouldBe expected
            }
          }
        }
      }

      "the tax year is before 2023" when {

        "both apis return success responses" should {

          "return the MtdSoftware view scenario" in {

            val nino = "fakeNino"
            val taxYear = TaxYear(2022, 2023)

            val calculationListModel =
              CalculationListModel(calculationId = "calc Id", calculationTimestamp = "calc timestamp", calculationType = "calc type", crystallised = Some(true))

            val liabilityCalculationResponse =
              LiabilityCalculationResponse(
                inputs = Inputs(PersonalInformation(taxRegime = "", class2VoluntaryContributions = Some(true))),
                metadata = Metadata(
                  calculationTimestamp = Some(""),
                  calculationType = ""
                ),
                messages = None,
                calculation = None,
              )

            when(mockCalculationListConnector.getLegacyCalculationList(any(), any())(any()))
              .thenReturn(Future(calculationListModel))

            when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any()))
              .thenReturn(Future(liabilityCalculationResponse))

            val actual = service.determineCannotDisplayCalculationContentScenario(nino, taxYear)
            val expected = MtdSoftware

            whenReady(actual) { result =>
              result shouldBe expected
            }
          }
        }

        ".getLegacyCalculationList() returns an error response" should {

          "return the LegacyAndCesa view scenario" in {

            val nino = "fakeNino"
            val taxYear = TaxYear(2022, 2023)

            val calculationListErrorModel =
              CalculationListErrorModel(code = 404, message = "some error message")

            val liabilityCalculationResponse =
              LiabilityCalculationResponse(
                inputs = Inputs(PersonalInformation(taxRegime = "", class2VoluntaryContributions = Some(true))),
                metadata = Metadata(
                  calculationTimestamp = Some(""),
                  calculationType = ""
                ),
                messages = None,
                calculation = None,
              )

            when(mockCalculationListConnector.getLegacyCalculationList(any(), any())(any()))
              .thenReturn(Future(calculationListErrorModel))

            when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any()))
              .thenReturn(Future(liabilityCalculationResponse))

            val actual = service.determineCannotDisplayCalculationContentScenario(nino, taxYear)
            val expected = LegacyAndCesa

            whenReady(actual) { result =>
              result shouldBe expected
            }
          }
        }

        ".getCalculationResponse() returns an error response" should {

          "return the LegacyAndCesa view scenario" in {

            val nino = "fakeNino"
            val taxYear = TaxYear(2022, 2023)

            val calculationListModel =
              CalculationListModel(calculationId = "calc Id", calculationTimestamp = "calc timestamp", calculationType = "calc type", crystallised = Some(true))

            val liabilityCalculationError = LiabilityCalculationError(status = 404, message = "some error message")

            val noIRSAEnrolments =
              Enrolments(enrolments = Set(Some(agentEnrolment), Some(ninoEnrolment), None).flatten)

            val individualUser =
              MtdItUser(
                mtditid = testMtditid,
                nino = testNino,
                usersRole = MTDIndividual,
                authUserDetails = getAuthUserDetails(Some(Individual), noIRSAEnrolments, hasUserName = true),
                clientDetails = None,
                incomeSources = IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty),
                btaNavPartial = None,
                featureSwitches = List()
              )(FakeRequest())

            when(mockCalculationListConnector.getLegacyCalculationList(any(), any())(any()))
              .thenReturn(Future(calculationListModel))

            when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any()))
              .thenReturn(Future(liabilityCalculationError))

            val actual = service.determineCannotDisplayCalculationContentScenario(nino, taxYear)(individualUser, headerCarrier, ec)
            val expected = LegacyAndCesa

            whenReady(actual) { result =>
              result shouldBe expected
            }
          }
        }

        "getLegacyCalculationList() or .getCalculationResponse() returns an error  - Agent user" should {

          "return the AgentBefore2023TaxYear view scenario" in {

            val nino = "fakeNino"
            val taxYear = TaxYear(2022, 2023)

            val calculationListModel =
              CalculationListModel(calculationId = "calc Id", calculationTimestamp = "calc timestamp", calculationType = "calc type", crystallised = Some(true))

            val liabilityCalculationError = LiabilityCalculationError(status = 404, message = "some error message")

            val agentUser =
              MtdItUser(
                mtditid = testMtditid,
                nino = testNino,
                usersRole = MTDPrimaryAgent,
                authUserDetails = getAuthUserDetails(Some(Agent), getAllEnrolmentsAgent(hasNino = true, hasSA = true), hasUserName = true),
                clientDetails = None,
                incomeSources = IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty),
                btaNavPartial = None,
                featureSwitches = List()
              )(FakeRequest())

            when(mockCalculationListConnector.getLegacyCalculationList(any(), any())(any()))
              .thenReturn(Future(calculationListModel))

            when(mockIncomeTaxCalculationConnector.getCalculationResponse(any(), any(), any(), any())(any(), any()))
              .thenReturn(Future(liabilityCalculationError))

            val actual = service.determineCannotDisplayCalculationContentScenario(nino, taxYear)(agentUser, headerCarrier, ec) // Agent user
            val expected = AgentBefore2023TaxYear

            whenReady(actual) { result =>
              result shouldBe expected
            }
          }
        }
      }
    }
  }
}

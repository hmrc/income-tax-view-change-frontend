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
import authV2.AuthActionsTestData.{getAllEnrolmentsAgent, getAuthUserDetails, ninoEnrolment}
import config.featureswitch.FeatureSwitching
import connectors.{CalculationListConnector, IncomeTaxCalculationConnector}
import enums.{MTDIndividual, MTDPrimaryAgent}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.liabilitycalculation.*
import models.taxyearsummary.*
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.test.FakeRequest
import testConstants.BaseTestConstants.{saEnrolment, testMtditid, testNino}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}
import uk.gov.hmrc.auth.core.Enrolments

class TaxYearSummaryServiceSpec extends TestSupport with FeatureSwitching {

  val mockCalculationListConnector: CalculationListConnector = mock[CalculationListConnector]
  val mockIncomeTaxCalculationConnector: IncomeTaxCalculationConnector = mock[IncomeTaxCalculationConnector]

  val service = new TaxYearSummaryService()

  "TaxYearSummaryService" when {

    ".checkSubmissionChannel()" when {

      "submission channel == IsLegacyWithCesa return the LegacyAndCesa view" in {

        val liabilityCalculationResponse =
          LiabilityCalculationResponse(
            inputs = Inputs(PersonalInformation(taxRegime = "", class2VoluntaryContributions = Some(true))),
            metadata = Metadata(
              calculationTimestamp = Some(""),
              calculationType = ""
            ),
            submissionChannel = Some(IsLegacyWithCesa),
            messages = None,
            calculation = None,
          )

        val actual = service.checkSubmissionChannel(Some(liabilityCalculationResponse))
        val expected = LegacyAndCesa

        actual shouldBe expected
      }

      "submission channel == IsMTD return the MtdSoftwareShowCalc view" in {

        val liabilityCalculationResponse =
          LiabilityCalculationResponse(
            inputs = Inputs(PersonalInformation(taxRegime = "", class2VoluntaryContributions = Some(true))),
            metadata = Metadata(
              calculationTimestamp = Some(""),
              calculationType = ""
            ),
            submissionChannel = Some(IsMTD),
            messages = None,
            calculation = None,
          )

        val actual = service.checkSubmissionChannel(Some(liabilityCalculationResponse))
        val expected = MtdSoftwareShowCalc

        actual shouldBe expected
      }
    }

    ".determineCannotDisplayCalculationContentScenario()" when {

      "the tax year is after 2023" when {

        "no IRSA Enrolments should check the liability response submissionChannel" should {

          "return the MtdSoftwareShowCalc view scenario when the submissionChannel == None" in {

            val taxYear = TaxYear(2023, 2024)

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

            val liabilityCalculationResponse =
              LiabilityCalculationResponse(
                inputs = Inputs(PersonalInformation(taxRegime = "", class2VoluntaryContributions = Some(true))),
                metadata = Metadata(
                  calculationTimestamp = Some(""),
                  calculationType = ""
                ),
                submissionChannel = None,
                messages = None,
                calculation = None,
              )

            val actual = service.determineCannotDisplayCalculationContentScenario(Some(liabilityCalculationResponse), taxYear)(individualUser)
            val expected = MtdSoftwareShowCalc

            actual shouldBe expected
          }

          "return the LegacyAndCesa view scenario when the submissionChannel == IsLegacyWithCesa" in {

            val taxYear = TaxYear(2023, 2024)

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

            val liabilityCalculationResponse =
              LiabilityCalculationResponse(
                inputs = Inputs(PersonalInformation(taxRegime = "", class2VoluntaryContributions = Some(true))),
                metadata = Metadata(
                  calculationTimestamp = Some(""),
                  calculationType = ""
                ),
                submissionChannel = Some(IsLegacyWithCesa),
                messages = None,
                calculation = None,
              )

            val actual = service.determineCannotDisplayCalculationContentScenario(Some(liabilityCalculationResponse), taxYear)(individualUser)
            val expected = LegacyAndCesa

            actual shouldBe expected
          }
        }

        "user has IRSA Enrolments and should check the liability response submissionChannel to determine the status" should {

          "return the MtdSoftwareShowCalc view scenario when the submissionChannel == None" in {

            val taxYear = TaxYear(2023, 2024)

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

            val liabilityCalculationResponse =
              LiabilityCalculationResponse(
                inputs = Inputs(PersonalInformation(taxRegime = "", class2VoluntaryContributions = Some(true))),
                metadata = Metadata(
                  calculationTimestamp = Some(""),
                  calculationType = ""
                ),
                submissionChannel = None,
                messages = None,
                calculation = None,
              )

            val actual = service.determineCannotDisplayCalculationContentScenario(Some(liabilityCalculationResponse), taxYear)(individualUser)
            val expected = MtdSoftwareShowCalc

            actual shouldBe expected
          }

          "return the LegacyAndCesa view scenario when the submissionChannel == IsLegacyWithCesa" in {

            val taxYear = TaxYear(2023, 2024)

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

            val liabilityCalculationResponse =
              LiabilityCalculationResponse(
                inputs = Inputs(PersonalInformation(taxRegime = "", class2VoluntaryContributions = Some(true))),
                metadata = Metadata(
                  calculationTimestamp = Some(""),
                  calculationType = ""
                ),
                submissionChannel = Some(IsLegacyWithCesa),
                messages = None,
                calculation = None,
              )

            val actual = service.determineCannotDisplayCalculationContentScenario(Some(liabilityCalculationResponse), taxYear)(individualUser)
            val expected = LegacyAndCesa

            actual shouldBe expected
          }
        }

        "user is an agent" should {

          "return the AgentCannotViewTaxCalc view scenario" in {

            val taxYear = TaxYear(2024, 2025)

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

            val liabilityCalculationResponse =
              LiabilityCalculationResponse(
                inputs = Inputs(PersonalInformation(taxRegime = "", class2VoluntaryContributions = Some(true))),
                metadata = Metadata(
                  calculationTimestamp = Some(""),
                  calculationType = ""
                ),
                submissionChannel = Some(IsLegacyWithCesa),
                messages = None,
                calculation = None,
              )


            val actual = service.determineCannotDisplayCalculationContentScenario(Some(liabilityCalculationResponse), taxYear)(agentUser) // Agent user
            val expected = AgentCannotViewTaxCalc

            actual shouldBe expected
          }
        }
      }

      "the tax year is before 2023" when {

        "the user is an Agent user" should {

          "return the AgentCannotViewTaxCalc view scenario" in {

            val taxYear = TaxYear(2022, 2023)

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

            val liabilityCalculationErrorResponse = LiabilityCalculationError(status = 404, message = "some error message")

            val actual = service.determineCannotDisplayCalculationContentScenario(Some(liabilityCalculationErrorResponse), taxYear)(agentUser) // Agent user
            val expected = AgentCannotViewTaxCalc

            actual shouldBe expected
          }
        }

        "no IRSA Enrolments should not check the liability response submissionChannel and" should {

          "return the NoIrsaAEnrolement view" in {

            val taxYear = TaxYear(2022, 2023)

            val irsaEnrolments =
              Enrolments(enrolments = Set(None, Some(ninoEnrolment), None).flatten)

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

            val liabilityCalculationErrorResponse = LiabilityCalculationError(status = 500, message = "some error message")
            val actual = service.determineCannotDisplayCalculationContentScenario(Some(liabilityCalculationErrorResponse), taxYear)(individualUser)
            val expected = NoIrsaAEnrolement

            actual shouldBe expected
          }
        }

        "has IRSA Enrolments should not check the liability response submissionChannel and" should {

          "return the IrsaEnrolementHandedOff view" in {

            val taxYear = TaxYear(2022, 2023)

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

            val liabilityCalculationErrorResponse = LiabilityCalculationError(status = 500, message = "some error message")

            val actual = service.determineCannotDisplayCalculationContentScenario(Some(liabilityCalculationErrorResponse), taxYear)(individualUser)
            val expected = IrsaEnrolementHandedOff

            actual shouldBe expected
          }
        }
      }
    }
  }
}

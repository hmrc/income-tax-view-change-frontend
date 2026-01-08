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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.{CesaSAReturn, MTDIndividual, MTDSupportingAgent}
import forms.utils.SessionKeys.calcPagesBackPage
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.MockCalculationService
import models.liabilitycalculation._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.{CalculationService, DateServiceInterface}
import testConstants.BaseTestConstants.testTaxYear
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessIncome2018and2019

import java.time.LocalDate
import scala.concurrent.Future

class FinalTaxCalculationControllerSpec extends MockAuthActions with MockCalculationService with ImplicitDateFormatter {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[CalculationService].toInstance(mockCalculationService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[FinalTaxCalculationController]

  val testCalcError: LiabilityCalculationError = LiabilityCalculationError(Status.OK, "Test message")
  val testCalcNOCONTENT: LiabilityCalculationError = LiabilityCalculationError(Status.NO_CONTENT, "Test message")

  val testCalcResponse: LiabilityCalculationResponse =
    LiabilityCalculationResponse(
      inputs = Inputs(personalInformation = PersonalInformation(taxRegime = "UK", class2VoluntaryContributions = None)),
      messages = None,
      calculation = None,
      metadata = Metadata(
        calculationTimestamp = Some("2019-02-15T09:35:15.094Z"),
        calculationType = "inYear",
        calculationReason = Some("customerRequest"),
        periodFrom = Some(LocalDate.of(2022, 1, 1)),
        periodTo = Some(LocalDate.of(2023, 1, 1)),
        calculationTrigger = Some(CesaSAReturn)
      ))
  val taxYear = 2018

  mtdAllRoles.foreach { mtdUserRole =>

    val isAgent = mtdUserRole != MTDIndividual

    s"show${if (isAgent) "Agent"}" when {
      val action = if (isAgent) testController.showAgent(testTaxYear) else testController.show(testTaxYear, None)
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the final tax calculation page" in {
            setupMockSuccess(mtdUserRole)
            mockItsaStatusRetrievalAction(businessIncome2018and2019)
            setupMockGetIncomeSourceDetails(businessIncome2018and2019)
            when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
              .thenReturn(Future.successful(testCalcResponse))
            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
            session(result).get(calcPagesBackPage) shouldBe Some("submission")
          }

          "render the error page" when {
            "calculation service fails" in {
              setupMockSuccess(mtdUserRole)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)
              when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
                .thenReturn(Future.successful(testCalcError))
              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole, false)(fakeRequest)
    }

    s"submit${if (isAgent) "Agent"}" when {

      val action = if (isAgent) testController.agentSubmit(testTaxYear) else testController.submit(testTaxYear)
      val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdUserRole)

      s"the $mtdUserRole is authenticated" should {

        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {

          "redirect to submissionFrontendFinalDeclaration" in {

            setupMockSuccess(mtdUserRole)
            mockItsaStatusRetrievalAction(businessIncome2018and2019)
            setupMockGetIncomeSourceDetails(businessIncome2018and2019)

            when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
              .thenReturn(Future.successful(testCalcResponse))
            val result = action(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result).get should include(s"/update-and-submit-income-tax-return/$taxYear/declaration")
          }

          "render the error page" when {
            "calculation service returns NO CONTENT" in {
              setupMockSuccess(mtdUserRole)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)
              when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
                .thenReturn(Future.successful(testCalcNOCONTENT))
              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
            "calculation service fails" in {
              setupMockSuccess(mtdUserRole)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)
              when(mockCalculationService.getLiabilityCalculationDetail(any(), any(), any())(any()))
                .thenReturn(Future.successful(testCalcError))
              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }
      }

      testMTDAuthFailuresForRole(action, mtdUserRole, false)(fakeRequest)
    }
  }
}

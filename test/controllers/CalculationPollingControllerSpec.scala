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

import enums.{MTDIndividual, MTDSupportingAgent}
import forms.utils.SessionKeys
import mocks.auth.MockAuthActions
import mocks.services.MockCalculationPollingService
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.CalculationPollingService
import testConstants.BaseTestConstants.testTaxYear

class CalculationPollingControllerSpec extends MockAuthActions with MockCalculationPollingService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[CalculationPollingService].toInstance(mockCalculationPollingService)
    )
    .build()

  lazy val testController = app.injector.instanceOf[CalculationPollingController]

  mtdAllRoles.foreach { mtdRole =>
    val isAgent     = mtdRole != MTDIndividual
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    s"calculationPoller${if (isAgent) "Agent"}" when {
      def action(isFinalCalc: Boolean = false) =
        if (isAgent) {
          testController.calculationPollerAgent(testTaxYear, isFinalCalc)
        } else {
          testController.calculationPoller(testTaxYear, isFinalCalc)
        }

      s"the user is authenticated as a $mtdRole" should {
        if (mtdRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action())(fakeRequest)
        } else {
          s"redirect to Tax summary controller" when {
            "the session key contains calculationId, request is not final calc" in {
              setupMockSuccess(mtdRole)
              mockBusinessIncomeSource()
              mockCalculationPollingSuccess()
              val result = action()(fakeRequest.addingToSession(SessionKeys.calculationId -> testCalcId))
              status(result) shouldBe Status.SEE_OTHER
              val expectedUrl = if (isAgent) {
                routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(testTaxYear).url
              } else {
                routes.TaxYearSummaryController.renderTaxYearSummaryPage(testTaxYear).url
              }
              redirectLocation(result) shouldBe Some(expectedUrl)
            }
          }

          s"redirect to Final Tax Calculation controller" when {
            "the session key contains calculationId, and request is final calc" in {
              setupMockSuccess(mtdRole)
              mockBusinessIncomeSource()
              mockCalculationPollingSuccess()
              val result = action(true)(fakeRequest.addingToSession(SessionKeys.calculationId -> testCalcId))
              status(result) shouldBe Status.SEE_OTHER
              val expectedUrl = if (isAgent) {
                routes.FinalTaxCalculationController.showAgent(testTaxYear).url
              } else {
                routes.FinalTaxCalculationController.show(testTaxYear).url
              }
              redirectLocation(result) shouldBe Some(expectedUrl)
            }
          }

          "render the error page" when {
            "the calculationId is missing from the session" in {
              setupMockSuccess(mtdRole)
              mockBusinessIncomeSource()

              val result = action()(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }

            "the calculation returned from the calculation service was not found" in {
              setupMockSuccess(mtdRole)
              mockBusinessIncomeSource()
              mockCalculationPollingRetryableError()
              val result = action(true)(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }

            "the calculation returned from the calculation service was an error" in {
              setupMockSuccess(mtdRole)
              mockBusinessIncomeSource()
              mockCalculationPollingNonRetryableError()
              val result = action(true)(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
              contentType(result) shouldBe Some("text/html")
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action(), mtdRole, false)(fakeRequest)
    }
  }
}

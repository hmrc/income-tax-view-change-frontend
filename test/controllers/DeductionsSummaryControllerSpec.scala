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
import mocks.auth.MockAuthActions
import mocks.services.MockCalculationService
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.CalculationService
import testConstants.BaseTestConstants.{testMtditid, testTaxYear}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessIncome2018and2019

class DeductionsSummaryControllerSpec extends MockAuthActions with MockCalculationService {

  val testYear: Int = 2020
  val title = messages("deduction_breakdown.heading")

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[CalculationService].toInstance(mockCalculationService)
    ).build()

  lazy val testController = app.injector.instanceOf[DeductionsSummaryController]

  mtdAllRoles.foreach { mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.showDeductionsSummaryAgent(testTaxYear) else testController.showDeductionsSummary(testTaxYear)
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    s"show${if (isAgent) "Agent"}" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the deductions income summary page" when {
            "all calc data available" in {
              setupMockSuccess(mtdUserRole)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)
              mockCalculationSuccessWithFlag(testMtditid)

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              lazy val document = result.toHtmlDocument
              val title = messages("deduction_breakdown.heading")
              document.title() shouldBe messages(
                if (isAgent) {
                  "htmlTitle.agent"
                } else {
                  "htmlTitle"
                }, title)
              document.getElementById("total-value").text() shouldBe "£12,500.00"
            }

            "no calc data available" in {
              setupMockSuccess(mtdUserRole)
              mockCalculationSuccessWithFlagMinimum(testMtditid)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              lazy val document = result.toHtmlDocument
              val title = messages("deduction_breakdown.heading")
              document.title() shouldBe messages(
                if (isAgent) {
                  "htmlTitle.agent"
                } else {
                  "htmlTitle"
                }, title)
              document.getElementById("total-value").text() shouldBe "£0.00"
            }
          }

          "render the error page" when {
            "there is a downstream error which return INTERNAL_SERVER_ERROR" in {
              setupMockSuccess(mtdUserRole)
              mockCalculationSuccessWithFlagError(testMtditid)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)
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

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

import connectors.ObligationsConnector
import enums.{MTDIndividual, MTDSupportingAgent}
import mocks.auth.MockAuthActions
import mocks.connectors.MockObligationsConnector
import mocks.services.MockCalculationService
import models.incomeSourceDetails.TaxYear
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.{CalculationService, NextUpdatesService}
import testConstants.BaseTestConstants.testTaxYear
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessIncome2018and2019

class TaxDueSummaryControllerSpec extends MockAuthActions with MockCalculationService with MockObligationsConnector {

  val testYear: Int = 2020

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[CalculationService].toInstance(mockCalculationService)
    ).build()

  lazy val testController = app.injector.instanceOf[TaxDueSummaryController]

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.showTaxDueSummaryAgent(testTaxYear) else testController.showTaxDueSummary(testTaxYear)
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    s"showTaxDueSummary${if (isAgent) "Agent"}" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the payment allocation page" when {
            "the tax year can be found in ETMP" in {
              setupMockSuccess(mtdUserRole)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)
              mockCalculationSuccessfulNew("XAIT0000123456")

              val testTaxYear: Int = 2018

              val testTaxYearModel = TaxYear.makeTaxYearWithEndYear(testTaxYear)
              println(s"${testTaxYearModel.toFinancialYearStart}, ${testTaxYearModel.toFinancialYearEnd}")

              val currentObligations: ObligationsModel = ObligationsModel(Seq(
                GroupedObligationsModel(
                  identification = "testId",
                  obligations = List(
                    SingleObligationModel(testTaxYearModel.toFinancialYearStart, testTaxYearModel.toFinancialYearEnd, testTaxYearModel.toFinancialYearStart, "Quarterly", None, "testPeriodKey", StatusFulfilled)
                  ))
              ))
              setupMockAllObligationsWithDates(testTaxYearModel.toFinancialYearStart, testTaxYearModel.toFinancialYearEnd)(currentObligations)


              val result = action(fakeRequest)
              status(result) shouldBe Status.OK

              lazy val document = result.toHtmlDocument
              contentType(result) shouldBe Some("text/html")
              charset(result) shouldBe Some("utf-8")
              document.title() shouldBe messages("htmlTitle" + {if(isAgent) ".agent" else ""}, messages("taxCal_breakdown.heading"))
            }
          }

          "render the error page" when {
            "given a tax year which can not be found in ETMP" in {
              setupMockSuccess(mtdUserRole)
              mockCalculationNotFoundNew("XAIT0000123456")
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)
              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "there is a downstream error" in {
              setupMockSuccess(mtdUserRole)
              mockCalculationErrorNew("XAIT0000123456")
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)
              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "obligations connector returns an error" in {
              setupMockAllObligationsWithDates(testTaxYearModel.toFinancialYearStart, testTaxYearModel.toFinancialYearEnd)(obligationsError)
              setupMockSuccess(mtdUserRole)
              mockCalculationSuccessfulNew("XAIT0000123456")
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

/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.optIn.oldJourney

import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockOptInService
import models.admin.ReportingFrequencyPage
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.optIn.OptInService
import services.optIn.core.OptInProposition.createOptInProposition
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

class OptInCompletedControllerSpec extends MockAuthActions
  with MockOptInService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptInService].toInstance(mockOptInService)
    ).build()

  lazy val testController = app.injector.instanceOf[OptInCompletedController]

  val endTaxYear = 2023
  val taxYear2023 = TaxYear.forYearEnd(endTaxYear)

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        s"render the optInCompleted page" that {
          "is for the current year" in {
            enable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val proposition = createOptInProposition(taxYear2023, ITSAStatus.Annual, ITSAStatus.Annual)
            mockFetchOptInProposition(Some(proposition))
            mockFetchSavedChosenTaxYear(Some(taxYear2023))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
          }

          "is for next year" in {
            enable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val proposition = createOptInProposition(taxYear2023, ITSAStatus.Annual, ITSAStatus.Annual)
            mockFetchOptInProposition(Some(proposition))
            mockFetchSavedChosenTaxYear(Some(taxYear2023.nextYear))

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
          }
        }
        "render the error page" when {
          "no proposition returned" in {
            enable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            mockFetchOptInProposition(None)
            mockFetchSavedChosenTaxYear(Some(taxYear2023))

            val result = action(fakeRequest)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }

          "FetchSavedChosenTaxYear fails" in {
            enable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val proposition = createOptInProposition(taxYear2023, ITSAStatus.Annual, ITSAStatus.Annual)
            mockFetchOptInProposition(Some(proposition))
            mockFetchSavedChosenTaxYear(None)

            val result = action(fakeRequest)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }

        "render the home page" when {
          "the ReportingFrequencyPage feature switch is disabled" in {
            disable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
            } else {
              "/report-quarterly/income-and-expenses/view"
            }

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}

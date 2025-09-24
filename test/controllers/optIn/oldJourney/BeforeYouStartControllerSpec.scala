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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.optIn.OptInService
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class BeforeYouStartControllerSpec extends MockAuthActions with MockOptInService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptInService].toInstance(mockOptInService)
    ).build()

  lazy val testController = app.injector.instanceOf[BeforeYouStartController]

  val endTaxYear = 2025
  val taxYear2024: TaxYear = TaxYear.forYearEnd(endTaxYear - 1)
  val taxYear2025: TaxYear = TaxYear.forYearEnd(endTaxYear)

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        s"render the before you start page" that {
          "contains a button to redirect to choose tax year" when {
            "there are multiple optIn tax years" in {
              enable(ReportingFrequencyPage)
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
                .thenReturn(Future.successful(Seq(taxYear2024, taxYear2025)))

              val buttonUrl = controllers.optIn.oldJourney.routes.ChooseYearController.show(isAgent).url

              val result = action(fakeRequest)
              val doc: Document = Jsoup.parse(contentAsString(result))
              doc.getElementById("start-button").attr("href") shouldBe buttonUrl
              status(result) shouldBe Status.OK
            }
          }

          "contains a button to redirect to confirm tax year page" when {
            "there is one optIn tax year" in {
              enable(ReportingFrequencyPage)
              setupMockSuccess(mtdRole)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
                .thenReturn(Future.successful(Seq(taxYear2024)))

              val buttonUrl = controllers.optIn.oldJourney.routes.SingleTaxYearOptInWarningController.show(isAgent).url

              val result = action(fakeRequest)
              val doc: Document = Jsoup.parse(contentAsString(result))
              doc.getElementById("start-button").attr("href") shouldBe buttonUrl
              status(result) shouldBe Status.OK
            }
          }
        }
        "render the error page" when {
          "the call to get available optInTaxYear fails" in {
            enable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

            when(mockOptInService.availableOptInTaxYear()(any(), any(), any()))
              .thenReturn(Future.failed(new Exception("Some error")))

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
              "/report-quarterly/income-and-expenses/view/agents"
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

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
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus.{Annual, Mandated, Voluntary}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.{Action, AnyContent}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.optIn.OptInService
import services.optIn.core.OptInProposition
import testConstants.BaseTestConstants._
import testConstants.BusinessDetailsTestConstants.business1
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncome
import views.html.optIn.oldJourney.OptInCancelledView

import scala.concurrent.Future


class OptInCancelledControllerSpec extends MockAuthActions with MockOptInService with MockitoSugar {

  def config: Map[String, Object] = Map(
    "feature-switches.read-from-mongo" -> "false"
  )

  override def fakeApplication(): Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[OptInService].toInstance(mockOptInService)
    )
    .configure(config)
    .build()

  val testController = fakeApplication().injector.instanceOf[OptInCancelledController]
  val optInCancelledView = fakeApplication().injector.instanceOf[OptInCancelledView]

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      def action: Action[AnyContent] = if (isAgent) testController.showAgent() else testController.show()
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      s"the user is authenticated as a $mtdRole" should {
        s"render the opt in cancelled page" in {
          enable(ReportingFrequencyPage)
          val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
          setupMockSuccess(mtdRole)
          when(
            mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
          ).thenReturn(Future(singleBusinessIncome))

          when(mockOptInService.fetchOptInProposition()(any(), any(), any())).thenReturn(
            Future(
              OptInProposition.createOptInProposition(
                currentYear = TaxYear(2024, 2025),
                currentYearItsaStatus = Annual,
                nextYearItsaStatus = Mandated
              )
            )
          )

          val result = action(fakeRequest)

          status(result) shouldBe OK
          contentAsString(result) shouldBe
            optInCancelledView(
              isAgent = isAgent,
              currentTaxYearStart = "2024",
              currentTaxYearEnd = "2025"
            ).toString
        }

        "show the Error Template view" when {
          "and return Internal Server Error - 500" in {
            enable(ReportingFrequencyPage)
            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)

            setupMockSuccess(mtdRole)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))

            when(mockOptInService.fetchOptInProposition()(any(), any(), any())).thenReturn(
              Future(
                OptInProposition.createOptInProposition(
                  currentYear = TaxYear(2024, 2025),
                  currentYearItsaStatus = Voluntary,
                  nextYearItsaStatus = Voluntary
                )
              )
            )
            val result = action(fakeRequest)

            status(result) shouldBe INTERNAL_SERVER_ERROR
            contentAsString(result).contains("Sorry, there is a problem with the service") shouldBe true
          }
        }

        "render the home page" when {
          "the ReportingFrequencyPage feature switch is disabled" in {
            disable(ReportingFrequencyPage)
            setupMockSuccess(mtdRole)
            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
            } else {
              "/report-quarterly/income-and-expenses/view"
            }

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }
  }
}

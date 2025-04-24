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

package controllers.manageBusinesses.add

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.{MockDateService, MockIncomeSourceRFService, MockSessionService}
import models.admin.IncomeSourcesNewJourney
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.manageBusinesses.IncomeSourceRFService
import services.{DateService, SessionService}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

class IncomeSourceRFCheckDetailsControllerSpec
  extends MockAuthActions
  with MockDateService
  with MockSessionService
  with MockIncomeSourceRFService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[IncomeSourceRFService].toInstance(mockIncomeSourceRFService),
      api.inject.bind[DateService].toInstance(mockDateService),
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[IncomeSourceRFCheckDetailsController]

  mtdAllRoles.foreach { mtdRole =>
    List(SelfEmployment, UkProperty, ForeignProperty).foreach { incomeSource =>
      val isAgent = mtdRole != MTDIndividual
      s"show(isAgent = $isAgent, incomeSourceType = $incomeSource)" when {
        val action = testController.show(isAgent, incomeSource)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "render the check your answers page" in {
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockRedirectChecksForIncomeSourceRF()

            enable(IncomeSourcesNewJourney)

            setupMockGetMongo(Right(Some(UIJourneySessionData("", ""))))

            val result = action(fakeRequest)
            status(result) shouldBe OK
          }
        }
      }

      s"submit(isAgent = $isAgent, incomeSourceType = $incomeSource)" when {
        val action = testController.submit(isAgent, incomeSource)
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        s"the user is authenticated as a $mtdRole" should {
          "render the check your answers page" in {
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockRedirectChecksForIncomeSourceRF()

            enable(IncomeSourcesNewJourney)

            setupMockGetMongo(Right(Some(UIJourneySessionData("", "", Some(AddIncomeSourceData(incomeSourceId = Some("ID")))))))

            val result = action(fakeRequest)
            status(result) shouldBe SEE_OTHER

            val redirectUrlCentre = if (isAgent) "/agents" else ""
            val addedUrl = incomeSource match {
              case SelfEmployment => "business-added"
              case UkProperty => "uk-property-added"
              case ForeignProperty => "foreign-property-added"
            }

            val redirectUrl = s"/report-quarterly/income-and-expenses/view$redirectUrlCentre/income-sources/add/$addedUrl"

            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
    }
  }
}

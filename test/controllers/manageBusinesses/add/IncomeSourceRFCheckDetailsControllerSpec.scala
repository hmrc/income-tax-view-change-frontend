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
import models.UIJourneySessionData
import models.incomeSourceDetails.{AddIncomeSourceData, IncomeSourceReportingFrequencySourceData}
import models.updateIncomeSource.UpdateIncomeSourceResponseError
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.manageBusinesses.IncomeSourceRFService
import services.{DateService, SessionService, UpdateIncomeSourceService}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class IncomeSourceRFCheckDetailsControllerSpec
  extends MockAuthActions
  with MockDateService
  with MockSessionService
  with MockIncomeSourceRFService {

  lazy val mockUpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[IncomeSourceRFService].toInstance(mockIncomeSourceRFService),
      api.inject.bind[DateService].toInstance(mockDateService),
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[UpdateIncomeSourceService].toInstance(mockUpdateIncomeSourceService)
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
            setupMockGetCurrentTaxYearEnd(2025)

            setupMockGetMongo(Right(Some(UIJourneySessionData("", "", Some(AddIncomeSourceData(incomeSourceId = Some("ID")))))))

            val result = action(fakeRequest)
            status(result) shouldBe SEE_OTHER

            val redirectUrlCentre = if (isAgent) "/agents" else ""
            val addedUrl = incomeSource match {
              case SelfEmployment => "add-sole-trader/business-added"
              case UkProperty => "add-uk-property/uk-property-added"
              case ForeignProperty => "add-foreign-property/foreign-property-added"
            }

            val redirectUrl = s"/report-quarterly/income-and-expenses/view$redirectUrlCentre/manage-your-businesses/$addedUrl"

            redirectLocation(result) shouldBe Some(redirectUrl)
          }

          "return an error if the session data contains errors for updating the income source" in {
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockRedirectChecksForIncomeSourceRF()
            setupMockGetCurrentTaxYearEnd(2025)

            setupMockGetMongo(Right(Some(UIJourneySessionData(
              "",
              "",
              Some(AddIncomeSourceData(incomeSourceId = Some("ID"))),
              incomeSourceReportingFrequencyData = Some(IncomeSourceReportingFrequencySourceData(true, true, true, true))))
            ))

            when(mockUpdateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any())).thenReturn(Future.successful(UpdateIncomeSourceResponseError("", "")))

            val result = action(fakeRequest)
            status(result) shouldBe SEE_OTHER

            val redirectUrl =
              if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showAgent(incomeSource).url
              else routes.IncomeSourceReportingMethodNotSavedController.show(incomeSource).url

            redirectLocation(result) shouldBe Some(redirectUrl)
          }

          "return an error if the session data doesn't retrieve the income source id" in {
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)
            mockRedirectChecksForIncomeSourceRF()

            setupMockGetMongo(Right(Some(UIJourneySessionData("", "", Some(AddIncomeSourceData(incomeSourceId = None))))))

            val redirectUrl =
              if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showAgent(incomeSource).url
              else routes.IncomeSourceReportingMethodNotSavedController.show(incomeSource).url

            val result = action(fakeRequest)
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
    }
  }
}

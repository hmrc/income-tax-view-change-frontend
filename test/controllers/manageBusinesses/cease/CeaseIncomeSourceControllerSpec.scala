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

package controllers.manageBusinesses.cease

import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import enums.MTDIndividual
import exceptions.MissingFieldException
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesNewJourney
import models.incomeSourceDetails.viewmodels.CeaseIncomeSourcesViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.SessionService
import testConstants.BusinessDetailsTestConstants.{ceaseBusinessDetailsViewModel, ceaseBusinessDetailsViewModel2}
import testConstants.PropertyDetailsTestConstants.{ceaseForeignPropertyDetailsViewModel, ceaseUkPropertyDetailsViewModel}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.notCompletedUIJourneySessionData

import scala.concurrent.Future

class CeaseIncomeSourceControllerSpec extends MockAuthActions
  with MockSessionService {

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testCeaseIncomeSourceController = app.injector.instanceOf[CeaseIncomeSourceController]

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    s"show${if (mtdRole != MTDIndividual) "Agent"}" when {
      val action = if (mtdRole == MTDIndividual) testCeaseIncomeSourceController.show() else testCeaseIncomeSourceController.showAgent()
      s"the user is authenticated as a $mtdRole" should {
        "redirect user back to the home page" when {
          "income source is disabled" in {
            setupMockSuccess(mtdRole)
            disable(IncomeSourcesNewJourney)
            mockSingleBISWithCurrentYearAsMigrationYear()

            val result = action(fakeRequest)

            val expectedRedirectUrl = if (mtdRole == MTDIndividual) {
              controllers.routes.HomeController.show().url
            } else {
              controllers.routes.HomeController.showAgent().url
            }

            status(result) shouldBe Status.SEE_OTHER
            redirectLocation(result) shouldBe Some(expectedRedirectUrl)
          }
        }
        "Render the cease an income source page" when {
          "income source is enabled" in {
            setupMockSuccess(mtdRole)
            enable(IncomeSourcesNewJourney)
            mockBothIncomeSources()
            setupMockCreateSession(true)
            setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Cease, SelfEmployment)))))
            setupMockDeleteSession(true)

            when(mockIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(any(), any()))
              .thenReturn(Right(CeaseIncomeSourcesViewModel(
                soleTraderBusinesses = List(ceaseBusinessDetailsViewModel, ceaseBusinessDetailsViewModel2),
                ukProperty = Some(ceaseUkPropertyDetailsViewModel),
                foreignProperty = Some(ceaseForeignPropertyDetailsViewModel),
                ceasedBusinesses = Nil,
                displayStartDate = true)))

            val result = action(fakeRequest)

            status(result) shouldBe Status.OK
          }
        }
        "show error page" when {
          "income source is enabled" in {
            setupMockSuccess(mtdRole)
            enable(IncomeSourcesNewJourney)
            mockBothIncomeSources()

            when(mockIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(any(), any()))
              .thenReturn(Left(MissingFieldException("Trading Name")))

            val result: Future[Result] = action(fakeRequest)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }
    }
  }
}

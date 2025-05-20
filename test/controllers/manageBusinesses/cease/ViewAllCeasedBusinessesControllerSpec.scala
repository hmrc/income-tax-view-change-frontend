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

package controllers.manageBusinesses.cease

import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import enums.MTDIndividual
import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesNewJourney
import models.incomeSourceDetails.viewmodels.CeaseIncomeSourcesViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.SessionService
import testConstants.BusinessDetailsTestConstants._
import testConstants.PropertyDetailsTestConstants.{ceaseForeignPropertyDetailsViewModel, ceaseUkPropertyDetailsViewModel}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.notCompletedUIJourneySessionData

import scala.concurrent.Future

class ViewAllCeasedBusinessesControllerSpec extends MockAuthActions
  with ImplicitDateFormatter
  with MockSessionService {

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[ViewAllCeasedBusinessesController]

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    s"show($isAgent)" when {
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        "render the view all ceased businesses page" in {
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
              ceasedBusinesses = List(ceasedBusinessDetailsViewModel, ceasedForeignPropertyDetailsViewModel, ceasedUkPropertyDetailsViewModel),
              displayStartDate = true)))

          val result: Future[Result] = action(fakeRequest)
          status(result) shouldBe Status.OK
        }

        "show error page" when {
          "get incomeSourceCeased details returns an error" in {
            setupMockSuccess(mtdRole)
            enable(IncomeSourcesNewJourney)
            mockBothIncomeSources()

            when(mockIncomeSourceDetailsService.getCeaseIncomeSourceViewModel(any(), any()))
              .thenReturn(Left(MissingFieldException("Trading Name")))

            val result: Future[Result] = action(fakeRequest)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}

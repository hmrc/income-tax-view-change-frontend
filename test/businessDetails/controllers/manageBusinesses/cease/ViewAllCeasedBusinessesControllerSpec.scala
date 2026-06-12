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

package businessDetails.controllers.manageBusinesses.cease

import businessDetails.mocks.services.MockIncomeSourceDetailsService
import businessDetails.services.{IncomeSourceDetailsService, SessionService}
import common.connectors.ITSAStatusConnector
import common.enums.IncomeSourceJourney.SelfEmployment
import common.enums.JourneyType.{Cease, IncomeSourceJourneyType}
import common.enums.MTDIndividual
import common.exceptions.MissingFieldException
import common.implicits.ImplicitDateFormatter
import common.mocks.auth.MockAuthActions
import common.mocks.services.MockSessionService
import common.services.DateServiceInterface
import models.incomeSourceDetails.viewmodels.CeaseIncomeSourcesViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import businessDetails.testConstants.BusinessDetailsTestConstants.*
import businessDetails.testConstants.PropertyDetailsTestConstants.{ceaseForeignPropertyDetailsViewModel, ceaseUkPropertyDetailsViewModel}
import common.testConstants.IncomeSourceDetailsTestConstants.{businessesAndPropertyIncome, notCompletedUIJourneySessionData}

import scala.concurrent.Future

class ViewAllCeasedBusinessesControllerSpec extends MockAuthActions with ImplicitDateFormatter
  with MockSessionService
  with MockIncomeSourceDetailsService {

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface),
      api.inject.bind[IncomeSourceDetailsService].toInstance(mockIncomeSourceDetailsService)
    ).build()

  lazy val testController = app.injector.instanceOf[ViewAllCeasedBusinessesController]

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    s"show($isAgent)" when {
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      val action = testController.show(isAgent, false)
      s"the user is authenticated as a $mtdRole" should {
        "render the view all ceased businesses page" in {
          setupMockSuccess(mtdRole)
          mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
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
            mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
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

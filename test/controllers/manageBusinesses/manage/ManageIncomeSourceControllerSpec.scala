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

package controllers.manageBusinesses.manage

import enums.MTDIndividual
import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.admin.IncomeSourcesNewJourney
import models.incomeSourceDetails.viewmodels.ViewIncomeSourcesViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.SessionService
import testConstants.BusinessDetailsTestConstants.viewBusinessDetailsViewModel
import testConstants.PropertyDetailsTestConstants.viewUkPropertyDetailsViewModel

import scala.concurrent.Future

class ManageIncomeSourceControllerSpec extends MockAuthActions
  with ImplicitDateFormatter
  with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val testController = app.injector.instanceOf[ManageIncomeSourceController]

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    s"show(isAgent = $isAgent)" when {
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
      val action = testController.show(isAgent)
      s"the user is authenticated as a $mtdRole" should {
        "render the manage income sources page" when {
          "the user has a sole trader business and a UK property" in {
            enable(IncomeSourcesNewJourney)
            setupMockSuccess(mtdRole)
            mockBothIncomeSources()

            setupMockCreateSession(true)
            setupMockDeleteSession(true)

            when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any(), any()))
              .thenReturn(
                Right(
                  ViewIncomeSourcesViewModel(
                    viewSoleTraderBusinesses = List(viewBusinessDetailsViewModel),
                    viewUkProperty = Some(viewUkPropertyDetailsViewModel),
                    viewForeignProperty = None,
                    viewCeasedBusinesses = Nil,
                    displayStartDate = true
                  )
                )
              )

            val result = action(fakeRequest)
            status(result) shouldBe Status.OK
          }
        }
        "redirect to the home page" when {
          "IncomeSources FS is disabled" in {
            setupMockSuccess(mtdRole)
            disable(IncomeSourcesNewJourney)
            mockBusinessIncomeSource()
            val result: Future[Result] = action(fakeRequest)
            status(result) shouldBe Status.SEE_OTHER
            val homeUrl = if (isAgent) {
              controllers.routes.HomeController.showAgent().url
            } else {
              controllers.routes.HomeController.show().url
            }
            redirectLocation(result) shouldBe Some(homeUrl)
          }
        }

        "render the error page" when {
          "error response from service" in {
            enable(IncomeSourcesNewJourney)
            setupMockSuccess(mtdRole)
            mockBothIncomeSources()

            setupMockCreateSession(true)
            setupMockDeleteSession(true)

            when(mockIncomeSourceDetailsService.getViewIncomeSourceViewModel(any(), any()))
              .thenReturn(Left(MissingFieldException("Trading Name")))

            val result = action(fakeRequest)
            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }
  }
}

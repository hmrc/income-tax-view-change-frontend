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

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.http.Status
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{SessionService, UpdateIncomeSourceService}
import testConstants.BaseTestConstants.testSelfEmploymentId

import scala.concurrent.Future

class ReportingMethodChangeErrorControllerSpec
  extends MockAuthActions with MockSessionService {

  lazy val mockUpdateIncomeSourcesService = mock(classOf[UpdateIncomeSourceService])

  override lazy val app = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[UpdateIncomeSourceService].toInstance(mockUpdateIncomeSourcesService)
    ).build()

  lazy val testController = app.injector.instanceOf[ReportingMethodChangeErrorController]

  val incomeSourceTypes = List(SelfEmployment, UkProperty, ForeignProperty)

  mtdAllRoles.foreach { mtdRole =>
    incomeSourceTypes.foreach { incomeSourceType =>
      val isAgent = mtdRole != MTDIndividual
      s"show($isAgent, $incomeSourceType)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = testController.show(isAgent, incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          "render the reporting method change error page" when {
            "using the manage businesses journey" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              if (incomeSourceType == SelfEmployment)
                when(mockSessionService.getMongoKey(any(), any())(any(), any()))
                  .thenReturn(
                    Future(
                      Right(Some(testSelfEmploymentId))
                    )
                  )

              val result = action(fakeRequest)

              status(result) shouldBe Status.OK
              val document = Jsoup.parse(contentAsString(result))
              val optSelfEmploymentId = if (incomeSourceType == SelfEmployment) Some(testSelfEmploymentId) else None
              document.getElementById("reportingMethodError.p2-link").attr("href") shouldBe
                controllers.manageBusinesses.manage.routes
                  .ManageIncomeSourceDetailsController.show(isAgent, incomeSourceType, optSelfEmploymentId).url
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}

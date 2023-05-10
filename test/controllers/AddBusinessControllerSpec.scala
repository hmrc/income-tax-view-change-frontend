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

package controllers

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockClientDetailsService
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import play.mvc.Http.Status
import testConstants.BaseTestConstants
import testConstants.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.AddBusiness

import scala.concurrent.Future


class AddBusinessControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with FeatureSwitching {

  object TestAddBusinessNameController
    extends AddBusinessController(
      MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      addBusinessView = app.injector.instanceOf[AddBusiness],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      ec = ec
    )

  "AddBusinessController" should {

    "redirect a user back to the custom error page" when {

      "the user is not authenticated" should {
        "redirect them to sign in" in {
          setupMockAuthorisationException()

          val result = TestAddBusinessNameController.show()(fakeRequestWithActiveSession)
         // status(result) shouldBe SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn.url)
        }

        "the user is  authenticated" in {
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          //val user: MtdItUser[_] = MtdItUser("1234567890", "test", Some("test"), incomeSources = mockIncomeSourceDetailsService, Some("test"), Some("test"), None, None, false, false)

          val result: Future[Result] = TestAddBusinessNameController.show()(fakeRequestWithActiveSession)

          status(result) shouldBe Status.OK

          //        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess, withClientPredicate = false)
          //       mockEnterClientsUTR(HtmlFormat.empty)

          //        val result = TestEnterClientsUTRController.show()(fakeRequestWithActiveSession)


          //        contentType(result) shouldBe Some(HTML)
          //        verify(mockAuthService, times(1)).authorised(ArgumentMatchers.eq(EmptyPredicate))
          //        verify(mockAuthService, times(0)).authorised(ArgumentMatchers.any(Enrolment.apply("").getClass))
        }
      }



      //    "return a redirect to the business date page when form is submitted successfully" in {
      //      val user: MtdItUser[_] = MtdItUser("1234567890", "test", Some("test"), incomeSources = mockIncomeSourceDetailsService, Some("test"), Some("test"), None, None, false, false)
      //      val form = BusinessNameForm.form.bind(Map("name" -> "Test Business"))
      //
      //    }

      //    "return a bad request when form submission has errors" in {
      //
      //
      //    }
    }
  }
}

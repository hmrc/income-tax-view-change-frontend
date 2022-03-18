/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.agent

import controllers.NextUpdatesController
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment}
import config.{FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitching
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockIncomeSourceDetailsService, MockNextUpdatesService}
import mocks.views.agent.MockNextUpdates
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, NextUpdatesResponseModel, ObligationsModel}
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import java.time.LocalDate

import audit.AuditingService
import controllers.predicates.{BtaNavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicateNoCache}
import views.html.{NextUpdates, NoNextUpdates}

import scala.concurrent.{ExecutionContext, Future}

class NextUpdatesControllerSpec extends MockAuthenticationPredicate with MockFrontendAuthorisedFunctions with MockItvcErrorHandler
  with MockIncomeSourceDetailsPredicateNoCache with MockIncomeSourceDetailsService with MockNextUpdates with MockNextUpdatesService with FeatureSwitching {

  trait Setup {
    val controller = new controllers.NextUpdatesController(
      app.injector.instanceOf[NoNextUpdates],
      app.injector.instanceOf[NextUpdates],
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicateNoCache,
      app.injector.instanceOf[services.IncomeSourceDetailsService],
      app.injector.instanceOf[AuditingService],
      mockNextUpdatesService,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[BtaNavBarPredicate],
      appConfig,
      app.injector.instanceOf[FrontendAuthorisedFunctions],
    )(
      app.injector.instanceOf[MessagesControllerComponents],
      mockItvcErrorHandler,
      ec
    )
  }

/*
  trait Setup {
    val controller = new controllers.NextUpdatesController(
      nextUpdates,
      mockIncomeSourceDetailsService,
      mockNextUpdatesService,
      app.injector.instanceOf[FrontendAppConfig],
      mockAuthService
    )(languageUtils,
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ExecutionContext],
      mockItvcErrorHandler)

  }
*/
  val isAgent: Boolean = true
  val date: LocalDate = LocalDate.now

  val obligationsModel = ObligationsModel(Seq(
    NextUpdatesModel(BaseTestConstants.testSelfEmploymentId, List(NextUpdateModel(date, date, date, "Quarterly", Some(date), "#001"))),
    NextUpdatesModel(BaseTestConstants.testPropertyIncomeId, List(NextUpdateModel(date, date, date, "EOPS", Some(date), "EOPS")))
  ))

  def mockObligations: OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    when(mockNextUpdatesService.getNextUpdates(matches(false))(any(), any()))
      .thenReturn(Future.successful(obligationsModel))
  }

  def mockNoObligations: OngoingStubbing[Future[NextUpdatesResponseModel]] = {
    when(mockNextUpdatesService.getNextUpdates(matches(false))(any(), any()))
      .thenReturn(Future.successful(ObligationsModel(Seq())))
  }

  "The NextUpdatesController.getNextUpdatesAgent function" when {

    "the user is not authenticated" should {
      "redirect them to sign in" in new Setup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = controller.getNextUpdatesAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in new Setup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = controller.getNextUpdatesAgent()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result: Future[Result] = controller.getNextUpdatesAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

    "the user has all correct details" should {
      "return Status OK (200) when we have obligations" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()
        mockObligations
        mockNextUpdates(obligationsModel, controllers.agent.routes.HomeController.show().url, isAgent)(HtmlFormat.empty)

        val result: Future[Result] = controller.getNextUpdatesAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some(HTML)
      }
      "return Status INTERNAL_SERVER_ERROR (500) when we have no obligations" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()
        mockNoObligations
        mockShowInternalServerError()

        val result: Future[Result] = controller.getNextUpdatesAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}

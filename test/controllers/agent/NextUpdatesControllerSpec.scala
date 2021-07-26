/*
 * Copyright 2021 HM Revenue & Customs
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

import assets.BaseTestConstants
import assets.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment}
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockIncomeSourceDetailsService, MockReportDeadlinesService}
import mocks.views.agent.MockNextUpdates
import models.reportDeadlines.{ObligationsModel, ReportDeadlineModel, ReportDeadlinesModel, ReportDeadlinesResponseModel}
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
import scala.concurrent.{ExecutionContext, Future}

class NextUpdatesControllerSpec extends TestSupport with MockFrontendAuthorisedFunctions with MockItvcErrorHandler
  with MockIncomeSourceDetailsService with MockNextUpdates with MockReportDeadlinesService with FeatureSwitching {

  trait Setup {
    val controller = new NextUpdatesController(
      agentNextUpdates,
      mockIncomeSourceDetailsService,
      mockReportDeadlinesService,
      app.injector.instanceOf[FrontendAppConfig],
      mockAuthService
    )(languageUtils,
      app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[ExecutionContext],
      mockItvcErrorHandler)
  }

  val date: LocalDate = LocalDate.now

  val obligationsModel = ObligationsModel(Seq(
    ReportDeadlinesModel(BaseTestConstants.testSelfEmploymentId, List(ReportDeadlineModel(date, date, date, "Quarterly", Some(date), "#001"))),
    ReportDeadlinesModel(BaseTestConstants.testPropertyIncomeId, List(ReportDeadlineModel(date, date, date, "EOPS", Some(date), "EOPS")))
  ))

  def mockObligations: OngoingStubbing[Future[ReportDeadlinesResponseModel]] = {
    when(mockReportDeadlinesService.getReportDeadlines(matches(false))(any(), any()))
      .thenReturn(Future.successful(obligationsModel))
  }

  def mockNoObligations: OngoingStubbing[Future[ReportDeadlinesResponseModel]] = {
    when(mockReportDeadlinesService.getReportDeadlines(matches(false))(any(), any()))
      .thenReturn(Future.successful(ObligationsModel(Seq())))
  }

  "The NextUpdatesController.getReportDeadlines function" when {

    "the user is not authenticated" should {
      "redirect them to sign in" in new Setup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = controller.getNextUpdates()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in new Setup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = controller.getNextUpdates()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result: Future[Result] = controller.getNextUpdates()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }

    "the user has all correct details" should {
      "return Status OK (200) when we have obligations" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()
        mockObligations
        mockAgentNextUpdates(obligationsModel, controllers.agent.routes.HomeController.show().url)(HtmlFormat.empty)

        val result: Future[Result] = controller.getNextUpdates()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        contentType(result) shouldBe Some(HTML)
      }
      "return Status INTERNAL_SERVER_ERROR (500) when we have no obligations" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockSingleBusinessIncomeSource()
        mockNoObligations
        mockShowInternalServerError()

        val result: Future[Result] = controller.getNextUpdates()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

}

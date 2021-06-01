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

import assets.BaseTestConstants.{testAgentAuthRetrievalSuccess, testAgentAuthRetrievalSuccessNoEnrolment}
import audit.mocks.MockAuditingService
import config.FrontendAppConfig
import config.featureswitch._
import controllers.Assets.{NOT_FOUND, OK, SEE_OTHER}
import implicits.ImplicitDateFormatterImpl
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.services.{MockFinancialDetailsService, MockIncomeSourceDetailsService, MockReportDeadlinesService}
import mocks.views.MockHome
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{HTML, contentType, defaultAwaitTimeout, redirectLocation}
import play.twirl.api.HtmlFormat
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.BearerTokenExpired
import uk.gov.hmrc.http.InternalServerException

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class HomeControllerSpec extends TestSupport
  with MockIncomeSourceDetailsService
  with MockFrontendAuthorisedFunctions
  with MockItvcErrorHandler
  with MockReportDeadlinesService
  with MockFinancialDetailsService
  with MockAuditingService
  with MockHome
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentViewer)
  }

  trait Setup {
    val controller = new HomeController(
      app.injector.instanceOf[views.html.agent.Home],
      mockReportDeadlinesService,
      mockFinancialDetailsService,
      mockIncomeSourceDetailsService,
      mockAuditingService,
      mockAuthService
    )(app.injector.instanceOf[MessagesControllerComponents],
      app.injector.instanceOf[FrontendAppConfig],
      mockItvcErrorHandler,
      app.injector.instanceOf[ExecutionContext],
      app.injector.instanceOf[ImplicitDateFormatterImpl])
  }

  "show" when {
    "the user is not authenticated" should {
      "redirect them to sign in" in new Setup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.SignInController.signIn().url)
      }
    }
    "the user has timed out" should {
      "redirect to the session timeout page" in new Setup {
        setupMockAgentAuthorisationException(exception = BearerTokenExpired())

        val result: Future[Result] = controller.show()(fakeRequestWithClientDetails)

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.timeout.routes.SessionTimeoutController.timeout().url)
      }
    }
    "the user does not have an agent reference number" should {
      "return Ok with technical difficulties" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccessNoEnrolment, withClientPredicate = false)
        mockShowOkTechnicalDifficulties()

        val result: Future[Result] = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentType(result) shouldBe Some(HTML)
      }
    }
    "the agent viewer feature switch is disabled" should {
      "return Not Found" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        mockNotFound()

        val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

        status(result) shouldBe NOT_FOUND
        contentType(result) shouldBe Some(HTML)
      }
    }
    "the agent viewer feature switch is enabled" when {
      "the call to retrieve income sources for the client returns an error" should {
        "return an internal server exception" in new Setup {
          enable(AgentViewer)

          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockErrorIncomeSource()

          val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

          intercept[InternalServerException](await(result))
            .message shouldBe "[ClientConfirmedController][getMtdItUserWithIncomeSources] IncomeSourceDetailsModel not created"
        }
      }
      "the call to retrieve income sources for the client is successful" when {
        "retrieving their obligation due date details had a failure" should {
          "return an internal server exception" in new Setup {
            enable(AgentViewer)

            setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
            mockSingleBusinessIncomeSource()
            mockGetObligationDueDates(Future.failed(new InternalServerException("obligation test exception")))

            val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

            intercept[InternalServerException](await(result))
              .message shouldBe "obligation test exception"
          }
        }
        "retrieving their obligation due date details was successful" when {
          "retrieving their charge due date details had a failure" should {
            "return an internal server exception" in new Setup {
              enable(AgentViewer)

              setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
              mockSingleBusinessIncomeSource()
              mockGetObligationDueDates(Future.successful(Right(2)))
              mockGetChargeDueDates(Future.failed(new InternalServerException("charge test exception")))

              val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

              intercept[InternalServerException](await(result))
                .message shouldBe "charge test exception"
            }
          }
          "retrieving their charge due date details was successful" should {
            "display the home page with those details" in new Setup {
              enable(AgentViewer)

              setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
              mockSingleBusinessIncomeSource()
              mockGetObligationDueDates(Future.successful(Right(2)))
              mockGetChargeDueDates(Future.successful(Some(Left(LocalDate.now -> true))))
              mockHome(Some(Left(LocalDate.now -> true)), Right(2))(HtmlFormat.empty)

              val result: Future[Result] = controller.show()(fakeRequestConfirmedClient())

              status(result) shouldBe OK
              contentType(result) shouldBe Some(HTML)
            }
          }
        }
      }
    }
  }

}

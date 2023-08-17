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

package controllers.incomeSources.cease

import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import connectors.IncomeTaxViewChangeConnector
import controllers.predicates.{IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import forms.incomeSources.cease.UKPropertyEndDateForm
import forms.utils.SessionKeys.ceaseUKPropertyEndDate
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.updateIncomeSource.{UpdateIncomeSourceRequestModel, UpdateIncomeSourceResponseModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, verify, when}
import play.api.http.Status
import play.api.http.Status.OK
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.UpdateIncomeSourceService
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testConstants.UpdateIncomeSourceTestConstants
import testConstants.UpdateIncomeSourceTestConstants.{cessationDate, successHttpResponse, successResponse, successResponseJson}
import testUtils.TestSupport
import uk.gov.hmrc.http.{HttpClient, HttpResponse}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.{CheckCeaseUKPropertyDetails, UKPropertyEndDate}

import scala.concurrent.Future

class CheckCeaseUKPropertyDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with FeatureSwitching {

  val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])
  val mockIncomeTaxViewChangeConnector: IncomeTaxViewChangeConnector = mock(classOf[IncomeTaxViewChangeConnector])
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  object TestCheckCeaseUKPropertyDetailsController extends CheckCeaseUKPropertyDetailsController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NavBarPredicate],
    app.injector.instanceOf[NinoPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[CheckCeaseUKPropertyDetails],
    mockUpdateIncomeSourceService,
    app.injector.instanceOf[CustomNotFoundError])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.ceaseUKProperty.checkDetails.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.ceaseUKProperty.checkDetails.heading"))}"
    val heading: String = messages("incomeSources.ceaseUKProperty.checkDetails.heading")

  }

  "Individual - CheckCeaseUKPropertyDetailsController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockUKPropertyIncomeSource()
        val result: Future[Result] = TestCheckCeaseUKPropertyDetailsController.show()(fakeRequestWithCeaseUKPropertyDate(cessationDate))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestCheckCeaseUKPropertyDetailsController.title
        document.select("h1").text shouldBe TestCheckCeaseUKPropertyDetailsController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestCheckCeaseUKPropertyDetailsController.show()(fakeRequestWithActiveSession)
        val expectedContent: String = TestCheckCeaseUKPropertyDetailsController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestCheckCeaseUKPropertyDetailsController.show()(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - CheckCeaseUKPropertyDetailsController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.UKPropertyCeasedObligationsController.show().url}" when {
      "submitted" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())).thenReturn(Future.successful(Right(successResponse)))

        lazy val result: Future[Result] = {
          TestCheckCeaseUKPropertyDetailsController.submit()(fakeRequestWithCeaseUKPropertyDate(cessationDate))
        }
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.UKPropertyCeasedObligationsController.show().url)
      }
    }
  }

  "Agent - CheckCeaseUKPropertyDetailsController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        val result: Future[Result] = TestCheckCeaseUKPropertyDetailsController.showAgent()(fakeRequestConfirmedClient()
          .withSession(forms.utils.SessionKeys.ceaseUKPropertyEndDate -> cessationDate))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestCheckCeaseUKPropertyDetailsController.titleAgent
        document.select("h1").text shouldBe TestCheckCeaseUKPropertyDetailsController.heading
      }
    }
    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestCheckCeaseUKPropertyDetailsController.showAgent()(fakeRequestConfirmedClient())
        val expectedContent: String = TestCheckCeaseUKPropertyDetailsController.customNotFoundErrorView().toString()
        status(result) shouldBe Status.OK
        contentAsString(result) shouldBe expectedContent
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestCheckCeaseUKPropertyDetailsController.showAgent()(fakeRequestConfirmedClient())
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - CheckCeaseUKPropertyDetailsController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.UKPropertyEndDateController.showAgent().url}" when {
      "form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockUKPropertyIncomeSource()

        when(mockIncomeTaxViewChangeConnector.updateCessationDate(any(), any(), any())(any())).thenReturn(Future.successful(successResponse))

        lazy val result: Future[Result] = {
          TestCheckCeaseUKPropertyDetailsController.submitAgent()(fakeRequestConfirmedClient()
            .withSession(forms.utils.SessionKeys.ceaseUKPropertyEndDate -> cessationDate)
            .withMethod("POST"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.UKPropertyCeasedObligationsController.showAgent().url)
      }
    }
  }


}

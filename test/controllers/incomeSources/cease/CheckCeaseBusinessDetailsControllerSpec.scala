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
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.SelfEmployment
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.Results.Redirect
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{SessionService, UpdateIncomeSourceService, UpdateIncomeSourceSuccess}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testSelfEmploymentId}
import testConstants.BusinessDetailsTestConstants.businessIncomeSourceId
import testConstants.UpdateIncomeSourceTestConstants.cessationDate
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.checkCeaseBusinessDetailsModel
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.cease.CheckCeaseBusinessDetails

import scala.concurrent.Future

class CheckCeaseBusinessDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with FeatureSwitching {

  val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])
  val mockIncomeTaxViewChangeConnector: IncomeTaxViewChangeConnector = mock(classOf[IncomeTaxViewChangeConnector])
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  object TestCheckCeaseBusinessDetailsController extends CheckCeaseBusinessDetailsController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NavBarPredicate],
    app.injector.instanceOf[NinoPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[CheckCeaseBusinessDetails],
    mockUpdateIncomeSourceService,
    sessionService = app.injector.instanceOf[SessionService])(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec, app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    val title: String = s"${messages("htmlTitle", messages("incomeSources.ceaseBusiness.checkDetails.heading"))}"
    val titleAgent: String = s"${messages("htmlTitle.agent", messages("incomeSources.ceaseBusiness.checkDetails.heading"))}"
    val heading: String = messages("incomeSources.ceaseBusiness.checkDetails.heading")

  }

  "Individual - CheckCeaseBusinessDetailsController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()

        when(mockIncomeSourceDetailsService.getCheckCeaseBusinessDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(Some(checkCeaseBusinessDetailsModel)))

        val result: Future[Result] = TestCheckCeaseBusinessDetailsController.show()(fakeRequestWithCeaseBusinessDetails(cessationDate, businessIncomeSourceId))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestCheckCeaseBusinessDetailsController.title
        document.select("h1").text shouldBe TestCheckCeaseBusinessDetailsController.heading
      }
    }

    "return Internal Server Error" when {
      "navigating to the page with no IncomeSourceId & End date values in session" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()

        when(mockIncomeSourceDetailsService.getCheckCeaseBusinessDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(Some(checkCeaseBusinessDetailsModel)))

        val result: Future[Result] = TestCheckCeaseBusinessDetailsController.show()(fakeRequestWithNinoAndOrigin("pta"))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent = false, SelfEmployment.key).url)
      }
    }

    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockBusinessIncomeSource()

        val result: Future[Result] = TestCheckCeaseBusinessDetailsController.show()(fakeRequestWithCeaseBusinessDetails(cessationDate, businessIncomeSourceId))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestCheckCeaseBusinessDetailsController.show()(fakeRequestWithCeaseBusinessDetails(cessationDate, businessIncomeSourceId))
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - CheckCeaseBusinessDetailsController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.BusinessCeasedObligationsController.show().url}" when {
      "submitted" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockBusinessIncomeSource()

        when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(businessIncomeSourceId))))

        lazy val result: Future[Result] = {
          TestCheckCeaseBusinessDetailsController.submit()(fakeRequestWithCeaseBusinessDetails(cessationDate, businessIncomeSourceId))
        }
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.BusinessCeasedObligationsController.show().url)
      }
    }
  }

  "Agent - CheckCeaseBusinessDetailsController.show" should {
    "return 200 OK" when {
      "navigating to the page with FS Enabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()

        when(mockIncomeSourceDetailsService.getCheckCeaseBusinessDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(Some(checkCeaseBusinessDetailsModel)))

        val result: Future[Result] = TestCheckCeaseBusinessDetailsController.showAgent()(fakeRequestConfirmedClient()
          .withSession(forms.utils.SessionKeys.ceaseBusinessEndDate -> cessationDate)
          .withSession(forms.utils.SessionKeys.ceaseBusinessIncomeSourceId -> businessIncomeSourceId)
        )
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.title shouldBe TestCheckCeaseBusinessDetailsController.titleAgent
        document.select("h1").text shouldBe TestCheckCeaseBusinessDetailsController.heading
      }
    }

    "return Internal Server Error" when {
      "navigating to the page with no IncomeSourceId & End date values in session" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()

        when(mockIncomeSourceDetailsService.getCheckCeaseBusinessDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(Some(checkCeaseBusinessDetailsModel)))

        val result: Future[Result] = TestCheckCeaseBusinessDetailsController.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceNotCeasedController.show(isAgent = true, SelfEmployment.key).url)
      }
    }

    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        disable(IncomeSources)
        mockBusinessIncomeSource()

        when(mockIncomeSourceDetailsService.getCheckCeaseBusinessDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(Some(checkCeaseBusinessDetailsModel)))

        val result: Future[Result] = TestCheckCeaseBusinessDetailsController.showAgent()(fakeRequestConfirmedClient()
          .withSession(forms.utils.SessionKeys.ceaseBusinessEndDate -> cessationDate)
          .withSession(forms.utils.SessionKeys.ceaseBusinessIncomeSourceId -> businessIncomeSourceId)
        )

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent.url)
      }
      "called with an unauthenticated user" in {
        setupMockAgentAuthorisationException()
        val result: Future[Result] = TestCheckCeaseBusinessDetailsController.showAgent()(fakeRequestConfirmedClient()
          .withSession(forms.utils.SessionKeys.ceaseBusinessEndDate -> cessationDate)
          .withSession(forms.utils.SessionKeys.ceaseBusinessIncomeSourceId -> businessIncomeSourceId)
        )
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - CheckCeaseBusinessDetailsController.submit" should {
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showAgent(Some(testSelfEmploymentId), SelfEmployment.key).url}" when {
      "form is completed successfully" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockBusinessIncomeSource()

        when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(businessIncomeSourceId))))

        lazy val result: Future[Result] = {
          TestCheckCeaseBusinessDetailsController.submitAgent()(fakeRequestConfirmedClient()
            .withSession(forms.utils.SessionKeys.ceaseBusinessEndDate -> cessationDate)
            .withSession(forms.utils.SessionKeys.ceaseBusinessIncomeSourceId -> businessIncomeSourceId)
            .withMethod("POST"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.BusinessCeasedObligationsController.showAgent().url)
      }
    }
  }

}

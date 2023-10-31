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
import connectors.UpdateIncomeSourceConnector
import controllers.incomeSources.add.routes
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.utils.SessionKeys
import forms.utils.SessionKeys.ceaseBusinessIncomeSourceId
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Results.Redirect
import play.api.mvc.{Call, MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{UpdateIncomeSourceService, UpdateIncomeSourceSuccess}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testMtditid, testPropertyIncomeId, testSelfEmploymentId}
import testConstants.BusinessDetailsTestConstants.businessIncomeSourceId
import testConstants.UpdateIncomeSourceTestConstants.{cessationDate, successResponse}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{checkCeaseBusinessDetailsModel, checkCeaseForeignPropertyDetailsModel, checkCeaseUkPropertyDetailsModel}
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.cease.CeaseCheckIncomeSourceDetails

import scala.concurrent.Future

class CheckCeaseIncomeSourceDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with FeatureSwitching with MockSessionService {

  val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])
  val mockUpdateIncomeSourceConnector: UpdateIncomeSourceConnector = mock(classOf[UpdateIncomeSourceConnector])
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  val validCeaseDate: String = "01-01-2022"

  val checkDetailsHeading: String = messages("incomeSources.ceaseBusiness.checkDetails.heading")

  object TestCeaseCheckIncomeSourceDetailsController extends CeaseCheckIncomeSourceDetailsController(
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[NavBarPredicate],
    app.injector.instanceOf[NinoPredicate],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[CeaseCheckIncomeSourceDetails],
    mockUpdateIncomeSourceService,
    sessionService = mockSessionService)(appConfig,
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler]) {

    def heading(incomeSourceType: IncomeSourceType): String = {
      incomeSourceType match {
        case SelfEmployment => messages("incomeSources.ceaseBusiness.checkDetails.caption")
        case UkProperty => messages("incomeSources.ceaseUKProperty.checkDetails.caption")
        case ForeignProperty => messages("incomeSources.ceaseForeignProperty.checkDetails.caption")
      }
    }

    def title(incomeSourceType: IncomeSourceType, isAgent: Boolean): String = {
      if (isAgent)
        s"${messages("htmlTitle.agent", heading(incomeSourceType))}"
      else
        s"${messages("htmlTitle", heading(incomeSourceType))}"
    }

  }

  "Individual - CheckCeaseIncomeSourceDetailsController.show" should {
    def stage(): Unit = {
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
      disableAllSwitches()
      enable(IncomeSources)
      mockBothPropertyBothBusiness()
      mockBusinessIncomeSource()
      setupMockGetSessionKeyMongoTyped[String](Right(Some("2022-08-27")))
    }

    "return 200 OK" when {
      "navigating to the page with FS Enabled with income source type as Self Employment" in {
        stage()

        when(mockIncomeSourceDetailsService.getCheckCeaseSelfEmploymentDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(checkCeaseBusinessDetailsModel))

        val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.show(SelfEmployment)(fakeRequestWithNinoAndOrigin("pta"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(
          TestCeaseCheckIncomeSourceDetailsController.heading(SelfEmployment))
        document.select("h1").text shouldBe checkDetailsHeading
        verifyMockGetMongoKeyTypedResponse[String](2)
      }

      "navigating to the page with FS Enabled with income source type as Foreign Property" in {
        stage()

        when(mockIncomeSourceDetailsService.getCheckCeasePropertyIncomeSourceDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(checkCeaseForeignPropertyDetailsModel))

        val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.show(ForeignProperty)(fakeRequestWithNinoAndOrigin("pta"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(
          TestCeaseCheckIncomeSourceDetailsController.heading(ForeignProperty))
        document.select("h1").text shouldBe checkDetailsHeading
        verifyMockGetMongoKeyTypedResponse[String](1)
      }

      "navigating to the page with FS Enabled with income source type as UK Property" in {
        stage()

        when(mockIncomeSourceDetailsService.getCheckCeasePropertyIncomeSourceDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(checkCeaseUkPropertyDetailsModel))

        val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.show(UkProperty)(fakeRequestWithNinoAndOrigin("pta"))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(
          TestCeaseCheckIncomeSourceDetailsController.heading(UkProperty))
        document.select("h1").text shouldBe checkDetailsHeading
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }

    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.show(SelfEmployment)(fakeRequestWithNinoAndOrigin("BTA"))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()
        val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.show(SelfEmployment)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Individual - CheckCeaseIncomeSourceDetailsController.submit" should {
    def stage(): Unit = {
      setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
      disableAllSwitches()
      enable(IncomeSources)
      mockBothPropertyBothBusiness()
      setupMockGetSessionKeyMongoTyped[String](Right(Some("2022-08-27")))
    }
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(SelfEmployment).url}" when {
      "submitted and Income Source Type = Self Employment" in {
        stage()

        when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testMtditid))))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submit(SelfEmployment)(fakeRequestWithNinoAndOrigin("pta"))
        }
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(SelfEmployment).url)
        verifyMockGetMongoKeyTypedResponse[String](2)
      }
    }

    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(UkProperty).url}" when {
      "submitted and Income Source Type = UK Property" in {
        stage()

        when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testMtditid))))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submit(UkProperty)(fakeRequestWithNinoAndOrigin("pta"))
        }
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(UkProperty).url)
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }
    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(ForeignProperty).url}" when {
      "submitted Income Source Type = Foreign Property" in {
        stage()

        when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testMtditid))))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submit(ForeignProperty)(fakeRequestWithNinoAndOrigin("pta"))
        }
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(ForeignProperty).url)
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }

    s"return 500 INTERNAL_SERVER_ERROR" when {
      "user has no Self Employment sources" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockGetSessionKeyMongoTyped[String](Right(None))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submit(SelfEmployment)(fakeRequestConfirmedClient()
            .withMethod("POST"))
        }

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        verifyMockGetMongoKeyTypedResponse[String](2)
      }
    }

    s"return 500 INTERNAL_SERVER_ERROR" when {
      "user has no foreign property income sources" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockGetSessionKeyMongoTyped[String](Right(None))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submit(ForeignProperty)(fakeRequestConfirmedClient()
            .withMethod("POST"))
        }

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }

    s"return 500 INTERNAL_SERVER_ERROR" when {
      "user has no UK property income sources" in {
        setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockGetSessionKeyMongoTyped[String](Right(None))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submit(UkProperty)(fakeRequestConfirmedClient()
            .withMethod("POST"))
        }

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }
  }

  "Agent - CheckCeaseIncomeSourceDetailsController.show" should {
    def stage(): Unit = {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      disableAllSwitches()
      enable(IncomeSources)
      mockBothPropertyBothBusiness()
      setupMockGetSessionKeyMongoTyped[String](Right(Some("2022-08-27")))
    }

    "return 200 OK" when {
      "return 200 OK" when {
        "navigating to the page with FS Enabled with income source type as Self Employment" in {
          stage()

          when(mockIncomeSourceDetailsService.getCheckCeaseSelfEmploymentDetailsViewModel(any(), any(), any()))
            .thenReturn(Right(checkCeaseBusinessDetailsModel))

          val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.showAgent(SelfEmployment)(fakeRequestConfirmedClient())
          val document: Document = Jsoup.parse(contentAsString(result))

          status(result) shouldBe Status.OK
          document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(
            TestCeaseCheckIncomeSourceDetailsController.heading(SelfEmployment))
          document.select("h1").text shouldBe checkDetailsHeading
          verifyMockGetMongoKeyTypedResponse[String](2)
        }
      }

      "navigating to the page with FS Enabled with income source type as Foreign Property" in {
        stage()

        when(mockIncomeSourceDetailsService.getCheckCeasePropertyIncomeSourceDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(checkCeaseForeignPropertyDetailsModel))

        val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.showAgent(ForeignProperty)(fakeRequestConfirmedClient()
          .withSession(forms.utils.SessionKeys.ceaseForeignPropertyEndDate -> cessationDate))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(
          TestCeaseCheckIncomeSourceDetailsController.heading(ForeignProperty))
        document.select("h1").text shouldBe checkDetailsHeading
        verifyMockGetMongoKeyTypedResponse[String](1)
      }

      "navigating to the page with FS Enabled with income source type as UK Property" in {
        stage()

        when(mockIncomeSourceDetailsService.getCheckCeasePropertyIncomeSourceDetailsViewModel(any(), any(), any()))
          .thenReturn(Right(checkCeaseUkPropertyDetailsModel))

        val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.showAgent(UkProperty)(fakeRequestConfirmedClient()
          .withSession(forms.utils.SessionKeys.ceaseUKPropertyEndDate -> cessationDate))
        val document: Document = Jsoup.parse(contentAsString(result))

        status(result) shouldBe Status.OK
        document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(
          TestCeaseCheckIncomeSourceDetailsController.heading(UkProperty))
        document.select("h1").text shouldBe checkDetailsHeading
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }

    "return 303 SEE_OTHER and redirect to custom not found error page" when {
      "navigating to the page with FS Disabled" in {
        disable(IncomeSources)
        mockPropertyIncomeSource()

        val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.show(SelfEmployment)(fakeRequestWithNinoAndOrigin("BTA"))
        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
      }
      "called with an unauthenticated user" in {
        setupMockAuthorisationException()

        val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.show(SelfEmployment)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

  "Agent - CheckCeaseIncomeSourceDetailsController.submit" should {
    def stage(): Unit = {
      setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
      disableAllSwitches()
      enable(IncomeSources)
      mockBothPropertyBothBusiness()
      setupMockGetSessionKeyMongoTyped[String](Right(Some("2022-08-27")))
    }

    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(SelfEmployment).url}" when {
      "form is completed successfully with income source type = Self Employment" in {
        stage()

        when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(businessIncomeSourceId))))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submitAgent(SelfEmployment)(fakeRequestConfirmedClient()
            .withSession(forms.utils.SessionKeys.ceaseBusinessEndDate -> cessationDate)
            .withSession(forms.utils.SessionKeys.ceaseBusinessIncomeSourceId -> businessIncomeSourceId)
            .withMethod("POST"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(SelfEmployment).url)
        verifyMockGetMongoKeyTypedResponse[String](2)
      }
    }

    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(ForeignProperty).url}" when {
      "form is completed successfully with income source type = Foreign Property" in {
        stage()

        when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testPropertyIncomeId))))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submitAgent(ForeignProperty)(fakeRequestConfirmedClient()
            .withSession(forms.utils.SessionKeys.ceaseForeignPropertyEndDate -> cessationDate)
            .withMethod("POST"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(ForeignProperty).url)
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }

    s"return 303 SEE_OTHER and redirect to ${controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(UkProperty).url}" when {
      "form is completed successfully" in {
        stage()

        when(mockUpdateIncomeSourceConnector.updateCessationDate(any(), any(), any())(any())).thenReturn(Future.successful(successResponse))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submitAgent(UkProperty)(fakeRequestConfirmedClient()
            .withSession(forms.utils.SessionKeys.ceaseUKPropertyEndDate -> cessationDate)
            .withMethod("POST"))
        }

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(UkProperty).url)
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }

    s"return 500 INTERNAL_SERVER_ERROR" when {
      "user has no Self Employment sources" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockGetSessionKeyMongoTyped[String](Right(None))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submitAgent(SelfEmployment)(fakeRequestConfirmedClient()
            .withMethod("POST"))
        }

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        verifyMockGetMongoKeyTypedResponse[String](2)
      }
    }

    s"return 500 INTERNAL_SERVER_ERROR" when {
      "user has no foreign property income sources" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockGetSessionKeyMongoTyped[String](Right(None))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submitAgent(ForeignProperty)(fakeRequestConfirmedClient()
            .withMethod("POST"))
        }

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }

    s"return 500 INTERNAL_SERVER_ERROR" when {
      "user has no UK property income sources" in {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        enable(IncomeSources)
        mockNoIncomeSources()
        setupMockGetSessionKeyMongoTyped[String](Right(None))

        lazy val result: Future[Result] = {
          TestCeaseCheckIncomeSourceDetailsController.submitAgent(UkProperty)(fakeRequestConfirmedClient()
            .withMethod("POST"))
        }

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        verifyMockGetMongoKeyTypedResponse[String](1)
      }
    }
  }
}

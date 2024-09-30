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

package controllers.manageBusinesses.cease

import audit.models.CeaseIncomeSourceAuditModel
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import connectors.UpdateIncomeSourceConnector
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import models.admin.IncomeSources
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.Assertion
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{UpdateIncomeSourceService, UpdateIncomeSourceSuccess}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse, testMtditid, testPropertyIncomeId, testSelfEmploymentId}
import testConstants.UpdateIncomeSourceTestConstants
import testConstants.UpdateIncomeSourceTestConstants.failureResponse
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.manageBusinesses.cease.CeaseCheckIncomeSourceDetails

import java.time.LocalDate
import scala.concurrent.Future

class CheckCeaseIncomeSourceDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate
  with FeatureSwitching with MockSessionService {

  val mockUpdateIncomeSourceService: UpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])
  val mockUpdateIncomeSourceConnector: UpdateIncomeSourceConnector = mock(classOf[UpdateIncomeSourceConnector])
  val mockHttpClient: HttpClient = mock(classOf[HttpClient])

  val validCeaseDate: String = LocalDate.of(2022, 10, 10).toString

  val checkDetailsHeading: String = messages("cease-check-answers.title")

  object TestCeaseCheckIncomeSourceDetailsController extends CeaseCheckIncomeSourceDetailsController(
    mockAuthService,
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[CeaseCheckIncomeSourceDetails],
    mockUpdateIncomeSourceService,
    sessionService = mockSessionService,
    auditingService = mockAuditingService,
    testAuthenticator)(appConfig,
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

  val individual: Boolean = false
  val agent: Boolean = true


  "CheckCeaseIncomeSourceDetailsController.show" should {
    "return 200 OK" when {
      def stage(isAgent: Boolean): Unit = {
        setupMockAuthorisationSuccess(isAgent)
        disableAllSwitches()
        enable(IncomeSources)
        mockBothPropertyBothBusiness()
      }

      def testCheckCeaseIncomeSourcePage(isAgent: Boolean, incomeSourceType: IncomeSourceType): Unit = {
        stage(isAgent)
        setupMockCreateSession(true)
        setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))

        incomeSourceType match {
          case SelfEmployment =>
            when(mockIncomeSourceDetailsService.getCheckCeaseSelfEmploymentDetailsViewModel(any(), IncomeSourceId(any()), any()))
              .thenReturn(Right(checkCeaseBusinessDetailsModel))
          case UkProperty =>
            when(mockIncomeSourceDetailsService.getCheckCeasePropertyIncomeSourceDetailsViewModel(any(), any(), any()))
              .thenReturn(Right(checkCeaseUkPropertyDetailsModel))
          case ForeignProperty =>
            when(mockIncomeSourceDetailsService.getCheckCeasePropertyIncomeSourceDetailsViewModel(any(), any(), any()))
              .thenReturn(Right(checkCeaseForeignPropertyDetailsModel))
        }

        val result: Future[Result] = {
          if (isAgent) TestCeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
          else TestCeaseCheckIncomeSourceDetailsController.show(incomeSourceType)(fakeRequestWithNinoAndOrigin("pta"))
        }

        val document: Document = Jsoup.parse(contentAsString(result))
        status(result) shouldBe Status.OK
        document.getElementsByClass("hmrc-caption govuk-caption-l").text().contains(
          TestCeaseCheckIncomeSourceDetailsController.heading(incomeSourceType))
        document.select("h1").text shouldBe checkDetailsHeading
      }

      "navigating to the page with FS Enabled with income source type as Self Employment" when {
        "user is an Individual" in {
          testCheckCeaseIncomeSourcePage(isAgent = individual, incomeSourceType = SelfEmployment)
        }
        "user is an Agent" in {
          testCheckCeaseIncomeSourcePage(isAgent = agent, incomeSourceType = SelfEmployment)
        }
      }

      "navigating to the page with FS Enabled with income source type as Foreign Property" when {
        "user is an Individual" in {
          testCheckCeaseIncomeSourcePage(isAgent = individual, incomeSourceType = ForeignProperty)
        }
        "user is an Agent" in {
          testCheckCeaseIncomeSourcePage(isAgent = agent, incomeSourceType = ForeignProperty)
        }
      }

      "navigating to the page with FS Enabled with income source type as UK Property" when {
        "user is an Individual" in {
          testCheckCeaseIncomeSourcePage(isAgent = individual, incomeSourceType = UkProperty)
        }
        "user is an Agent" in {
          testCheckCeaseIncomeSourcePage(isAgent = agent, incomeSourceType = UkProperty)
        }
      }
    }
    "return 303 SEE_OTHER and redirect to Home page" when {
      "navigating to the page with FS Disabled" when {
        "user is an Individual" in {
          disable(IncomeSources)
          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          mockPropertyIncomeSource()
          val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.show(SelfEmployment)(fakeRequestWithNinoAndOrigin("BTA"))
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }
        "user is an Agent" in {
          disable(IncomeSources)
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          mockPropertyIncomeSource()
          val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.showAgent(SelfEmployment)(fakeRequestConfirmedClient())
          status(result) shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.showAgent().url)
        }
      }
      "called with an unauthenticated user" when {
        "user is an Individual" in {
          setupMockAuthorisationException()
          val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.show(SelfEmployment)(fakeRequestWithActiveSession)
          status(result) shouldBe Status.SEE_OTHER
        }
        "user is an Agent" in {
          setupMockAgentAuthorisationException()
          val result: Future[Result] = TestCeaseCheckIncomeSourceDetailsController.showAgent(SelfEmployment)(fakeRequestConfirmedClient())
          status(result) shouldBe Status.SEE_OTHER
        }
      }
    }

    "redirect to the Cannot Go Back page" when {
      def setupCompletedCeaseJourney(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
        setupMockAuthorisationSuccess(isAgent)
        disableAllSwitches()
        enable(IncomeSources)
        mockBothPropertyBothBusiness()
        setupMockCreateSession(true)
        setupMockGetMongo(Right(Some(completedUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))

        val result = if (isAgent) {
          TestCeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType)(fakeRequestConfirmedClient())
        } else {
          TestCeaseCheckIncomeSourceDetailsController.show(incomeSourceType)(fakeRequestWithActiveSession)
        }

        val expectedRedirectUrl = if (isAgent) {
          routes.IncomeSourceCeasedBackErrorController.showAgent(incomeSourceType).url
        } else {
          routes.IncomeSourceCeasedBackErrorController.show(incomeSourceType).url
        }

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(expectedRedirectUrl)
      }

      "UK Property journey is complete - Individual" in {
        setupCompletedCeaseJourney(isAgent = false, UkProperty)
      }
      "UK Property journey is complete - Agent" in {
        setupCompletedCeaseJourney(isAgent = true, UkProperty)
      }
      "Foreign Property journey is complete - Individual" in {
        setupCompletedCeaseJourney(isAgent = false, ForeignProperty)
      }
      "Foreign Property journey is complete - Agent" in {
        setupCompletedCeaseJourney(isAgent = true, ForeignProperty)
      }
      "Self Employment journey is complete - Individual" in {
        setupCompletedCeaseJourney(isAgent = false, SelfEmployment)
      }
      "Self Employment journey is complete - Agent" in {
        setupCompletedCeaseJourney(isAgent = true, SelfEmployment)
      }
    }

  }
  "CheckCeaseIncomeSourceDetailsController.submit" should {
    def stage(isAgent: Boolean): Unit = {
      setupMockAuthorisationSuccess(isAgent)
      disableAllSwitches()
      enable(IncomeSources)
      mockBothPropertyBothBusiness()
    }

    def testSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType, hasIncomeSourceUpdateFailed: Boolean = false): Unit = {
      stage(isAgent)
      setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))

      if (hasIncomeSourceUpdateFailed) {
        when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Left(UpdateIncomeSourceTestConstants.failureResponse)))
      } else {
        when(mockUpdateIncomeSourceService.updateCessationDate(any(), any(), any())(any(), any()))
          .thenReturn(Future.successful(Right(UpdateIncomeSourceSuccess(testMtditid))))
      }

      val redirectResult = (isAgent, hasIncomeSourceUpdateFailed) match {
        case (true, false) => controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.showAgent(incomeSourceType).url
        case (false, false) => controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(incomeSourceType).url
        case (_, true) => controllers.manageBusinesses.cease.routes.IncomeSourceNotCeasedController.show(isAgent, incomeSourceType).url
      }

      lazy val result: Future[Result] = {
        if (isAgent) TestCeaseCheckIncomeSourceDetailsController.submitAgent(incomeSourceType)(fakeRequestConfirmedClient().withMethod("POST"))
        else TestCeaseCheckIncomeSourceDetailsController.submit(incomeSourceType)(fakeRequestWithNinoAndOrigin("pta"))
      }

      status(result) shouldBe Status.SEE_OTHER
      redirectLocation(result) shouldBe Some(redirectResult)

      (incomeSourceType, hasIncomeSourceUpdateFailed) match {
        case (SelfEmployment, false) =>
          verifyExtendedAudit(CeaseIncomeSourceAuditModel(incomeSourceType, validCeaseDate, mkIncomeSourceId(testSelfEmploymentId), None))
        case (UkProperty, false) =>
          verifyExtendedAudit(CeaseIncomeSourceAuditModel(incomeSourceType, validCeaseDate, mkIncomeSourceId(testPropertyIncomeId), None))
        case (ForeignProperty, false) =>
          verifyExtendedAudit(CeaseIncomeSourceAuditModel(incomeSourceType, validCeaseDate, mkIncomeSourceId(testPropertyIncomeId), None))
        case (SelfEmployment, true) | _ =>
          verifyExtendedAudit(CeaseIncomeSourceAuditModel(incomeSourceType, validCeaseDate, mkIncomeSourceId(testSelfEmploymentId), Some(failureResponse)))
      }
    }

    def testMissingIncomeSourceOnSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType): Unit = {
      enable(IncomeSources)
      mockNoIncomeSources()
      setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Cease, incomeSourceType)))))

      lazy val result: Future[Result] = {
        if (isAgent) {
          setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
          TestCeaseCheckIncomeSourceDetailsController.submitAgent(incomeSourceType)(fakeRequestConfirmedClient()
            .withMethod("POST"))
        }
        else {
          setupMockAuthRetrievalSuccess(testIndividualAuthSuccessWithSaUtrResponse())
          TestCeaseCheckIncomeSourceDetailsController.submit(incomeSourceType)(fakeRequestWithNinoAndOrigin("pta")
            .withMethod("POST"))
        }
      }

      status(result) shouldBe Status.INTERNAL_SERVER_ERROR
    }

    s"return 303 SEE_OTHER and redirect to ${controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(SelfEmployment).url}" when {
      "submitted and Income Source Type = Self Employment" when {
        "user is an Individual" in {
          testSubmit(isAgent = individual, SelfEmployment)
        }
        "user is an Agent" in {
          testSubmit(isAgent = agent, SelfEmployment)
        }
      }
    }
    s"return 303 SEE_OTHER and redirect to ${controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(UkProperty).url}" when {
      "submitted and Income Source Type = UK Property" when {
        "user is an Individual" in {
          testSubmit(isAgent = individual, UkProperty)
        }
        "user is an Agent" in {
          testSubmit(isAgent = agent, UkProperty)
        }
      }
    }
    s"return 303 SEE_OTHER and redirect to ${controllers.manageBusinesses.cease.routes.IncomeSourceCeasedObligationsController.show(ForeignProperty).url}" when {
      "submitted Income Source Type = Foreign Property" when {
        "user is an Individual" in {
          testSubmit(isAgent = individual, ForeignProperty)
        }
        "user is an Agent" in {
          testSubmit(isAgent = agent, ForeignProperty)
        }
      }
    }
    s"return 500 INTERNAL_SERVER_ERROR" when {
      "user has no Self Employment sources" when {
        "user is an Individual" in {
          testMissingIncomeSourceOnSubmit(isAgent = individual, SelfEmployment)
        }
        "user is an Agent" in {
          testMissingIncomeSourceOnSubmit(isAgent = agent, SelfEmployment)
        }
      }
      "user has no foreign property income sources" when {
        "user is an Individual" in {
          testMissingIncomeSourceOnSubmit(isAgent = individual, ForeignProperty)
        }
        "user is an Agent" in {
          testMissingIncomeSourceOnSubmit(isAgent = agent, ForeignProperty)
        }
      }
      "user has no UK property income sources" when {
        "user is an Individual" in {
          testMissingIncomeSourceOnSubmit(isAgent = individual, UkProperty)
        }
        "user is an Agent" in {
          testMissingIncomeSourceOnSubmit(isAgent = agent, UkProperty)
        }
      }
    }
    s"return 303 SEE_OTHER and redirect to ${controllers.manageBusinesses.cease.routes.IncomeSourceNotCeasedController.show(true, SelfEmployment).url}" when {
      "update income source fails" in {
        testSubmit(isAgent = true, incomeSourceType = SelfEmployment, hasIncomeSourceUpdateFailed = true)
      }
    }
  }
}

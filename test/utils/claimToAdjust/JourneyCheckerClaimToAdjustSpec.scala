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

package utils.claimToAdjust

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.{AfterSubmissionPage, BeforeSubmissionPage, CannotGoBackPage, InitialPage}
import mocks.services.MockPaymentOnAccountSessionService
import models.claimToAdjustPoa.PoaAmendmentData
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{spy, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.mvc.Results.{Ok, Redirect}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.PaymentOnAccountSessionService
import testConstants.BaseTestConstants.{testNino, testUserTypeAgent, testUserTypeIndividual}
import testConstants.claimToAdjustPoa.ClaimToAdjustPoaTestConstants.whatYouNeedToKnowViewModel
import testUtils.TestSupport
import views.html.claimToAdjustPoa.WhatYouNeedToKnow

import scala.concurrent.{ExecutionContext, Future}

class JourneyCheckerClaimToAdjustSpec extends TestSupport with MockPaymentOnAccountSessionService {

  val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val TestJourneyCheckerClaimToAdjust: JourneyCheckerClaimToAdjust = new JourneyCheckerClaimToAdjust {
    val appConfig: FrontendAppConfig = mockAppConfig
    override val poaSessionService: PaymentOnAccountSessionService = mockPaymentOnAccountSessionService
    override val individualErrorHandler: ItvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
    override val agentErrorHandler: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]
    override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  }

  val TestJourneyCheckerClaimToAdjustSpy: JourneyCheckerClaimToAdjust = spy(new JourneyCheckerClaimToAdjust {
    val appConfig: FrontendAppConfig = mockAppConfig
    override val poaSessionService: PaymentOnAccountSessionService = mockPaymentOnAccountSessionService
    override val individualErrorHandler: ItvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
    override val agentErrorHandler: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]
    override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  })

  override lazy val tsTestUser: MtdItUser[_] =
    defaultMTDITUser(Some(testUserTypeIndividual), IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty))

  override lazy val tsTestUserAgent: MtdItUser[_] =
    defaultMTDITUser(Some(testUserTypeAgent), IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty))

  val whatYouNeedToKnowView: WhatYouNeedToKnow = app.injector.instanceOf[WhatYouNeedToKnow]

  def successfulFutureOk: PoaAmendmentData => Future[Result] = _ => {
    Future.successful(Ok(whatYouNeedToKnowView(isAgent = false, whatYouNeedToKnowViewModel(false, false))))
}
  def successfulFutureOkAgent: PoaAmendmentData => Future[Result] = _ => {
    Future.successful(Ok(whatYouNeedToKnowView(isAgent = true, whatYouNeedToKnowViewModel(true, true))))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "JourneyCheckerClaimToAdjust.redirectToYouCannotGoBackPage" should {
    "redirect to the You Cannot Go Back page" when {
      "user is an agent" in {

        val res = TestJourneyCheckerClaimToAdjust.redirectToYouCannotGoBackPage(tsTestUserAgent)

        res shouldBe Redirect(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(true).url)

      }
      "user is an individual" in {

        val res = TestJourneyCheckerClaimToAdjust.redirectToYouCannotGoBackPage(tsTestUser)

        res shouldBe Redirect(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(false).url)

      }
    }
  }

  "JourneyCheckerClaimToAdjust.showCannotGoBackErrorPage" should {
    "return true" when {
      "journeyCompleted = true, journeyState = BeforeSubmissionPage" in {

        val res = TestJourneyCheckerClaimToAdjust.showCannotGoBackErrorPage(journeyCompleted = true, journeyState = BeforeSubmissionPage)

        res shouldBe true

      }
      "journeyCompleted = true, journeyState = InitialPage" in {

        val res = TestJourneyCheckerClaimToAdjust.showCannotGoBackErrorPage(journeyCompleted = true, journeyState = InitialPage)

        res shouldBe true

      }
    }
    "return false" when {

      "journeyCompleted = true, journeyState = CannotGoBackPage" in {

        val res = TestJourneyCheckerClaimToAdjust.showCannotGoBackErrorPage(journeyCompleted = true, journeyState = CannotGoBackPage)

        res shouldBe false

      }
      "journeyCompleted = true, journeyState = AfterSubmissionPage" in {

        val res = TestJourneyCheckerClaimToAdjust.showCannotGoBackErrorPage(journeyCompleted = true, journeyState = AfterSubmissionPage)

        res shouldBe false

      }
      "journeyCompleted = false, journeyState = CannotGoBackPage" in {

        val res = TestJourneyCheckerClaimToAdjust.showCannotGoBackErrorPage(journeyCompleted = false, journeyState = CannotGoBackPage)

        res shouldBe false

      }
      "journeyCompleted = false, journeyState = AfterSubmissionPage" in {

        val res = TestJourneyCheckerClaimToAdjust.showCannotGoBackErrorPage(journeyCompleted = false, journeyState = AfterSubmissionPage)

        res shouldBe false

      }
      "journeyCompleted = false, journeyState = BeforeSubmissionPage" in {

        val res = TestJourneyCheckerClaimToAdjust.showCannotGoBackErrorPage(journeyCompleted = false, journeyState = BeforeSubmissionPage)

        res shouldBe false

      }
      "journeyCompleted = false, journeyState = InitialPage" in {

        val res = TestJourneyCheckerClaimToAdjust.showCannotGoBackErrorPage(journeyCompleted = false, journeyState = InitialPage)

        res shouldBe false

      }
    }
  }

  "JourneyCheckerClaimToAdjust.withSessionDataAndOldIncomeSourceFS when not on the initial page" should {
    "redirect to the You Cannot Go Back error page" when {
      "showCannotGoBackErrorPage returns true" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))

        when(TestJourneyCheckerClaimToAdjustSpy.showCannotGoBackErrorPage(ArgumentMatchers.eq(true), ArgumentMatchers.eq(BeforeSubmissionPage))).thenReturn(true)

        val res = TestJourneyCheckerClaimToAdjustSpy.withSessionData(journeyState = BeforeSubmissionPage)(successfulFutureOk)(tsTestUser, headerCarrier)
        val resAgent = TestJourneyCheckerClaimToAdjustSpy.withSessionData(journeyState = BeforeSubmissionPage)(successfulFutureOkAgent)(tsTestUserAgent, headerCarrier)

        status(res) shouldBe SEE_OTHER
        redirectLocation(res) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(false).url)
        status(resAgent) shouldBe SEE_OTHER
        redirectLocation(resAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(true).url)
      }
    }
    "run the code block and go to the what you need to know page" when {
      "showCannotGoBackErrorPage returns false" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))

        when(TestJourneyCheckerClaimToAdjustSpy.showCannotGoBackErrorPage(ArgumentMatchers.eq(false), ArgumentMatchers.eq(CannotGoBackPage))).thenReturn(false)

        val res = TestJourneyCheckerClaimToAdjustSpy.withSessionData(journeyState = CannotGoBackPage)(successfulFutureOk)(tsTestUser, headerCarrier)
        val resAgent = TestJourneyCheckerClaimToAdjustSpy.withSessionData(journeyState = CannotGoBackPage)(successfulFutureOkAgent)(tsTestUserAgent, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe OK
        doc.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
        status(resAgent) shouldBe OK
        docAgent.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
      }
    }
    "return an internal server error" when {
      "getMongo returns None so there is not an active session" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))

        val res = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = CannotGoBackPage)(successfulFutureOk)(tsTestUser, headerCarrier)
        val resAgent = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = CannotGoBackPage)(successfulFutureOkAgent)(tsTestUserAgent, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo returns an exception" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Left(new Exception("Error"))))

        val res = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = CannotGoBackPage)(successfulFutureOk)(tsTestUser, headerCarrier)
        val resAgent = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = CannotGoBackPage)(successfulFutureOkAgent)(tsTestUserAgent, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
    }
  }
  "JourneyCheckerClaimToAdjust.withSessionDataAndOldIncomeSourceFS when on the initial page" should {
    "run the code block and go to the what you need to know page" when {
      "getMongo returns a right containing PoA session data but the journeyComplete flag is set to false" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))

        val res = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = InitialPage)(successfulFutureOk)(tsTestUser, headerCarrier)
        val resAgent = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = InitialPage)(successfulFutureOkAgent)(tsTestUserAgent, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe OK
        doc.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
        status(resAgent) shouldBe OK
        docAgent.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
      }
      "getMongo returns a right containing None" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))
        setupMockPaymentOnAccountSessionServiceCreateSession(Future.successful(Right((): Unit)))

        val res = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = InitialPage)(successfulFutureOk)(tsTestUser, headerCarrier)
        val resAgent = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = InitialPage)(successfulFutureOkAgent)(tsTestUserAgent, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe OK
        doc.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
        status(resAgent) shouldBe OK
        docAgent.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
      }
      "getMongo returns a right containing PoA session data, the journeyComplete flag is set to true and createSession returns a Right containing a Unit" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))
        setupMockPaymentOnAccountSessionServiceCreateSession(Future.successful(Right((): Unit)))

        val res = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = InitialPage)(successfulFutureOk)(tsTestUser, headerCarrier)
        val resAgent = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = InitialPage)(successfulFutureOkAgent)(tsTestUserAgent, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe OK
        doc.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
        status(resAgent) shouldBe OK
        docAgent.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
      }
    }
    "return an internal server error" when {
      "journeyComplete flag is set to true and createSession returns an exception" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))
        setupMockPaymentOnAccountSessionServiceCreateSession(Future.successful(Left(new Exception("Error"))))

        val res = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = InitialPage)(successfulFutureOk)(tsTestUser, headerCarrier)
        val resAgent = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = InitialPage)(successfulFutureOkAgent)(tsTestUserAgent, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"

        status(res) shouldBe INTERNAL_SERVER_ERROR
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo returns an exception" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Left(new Exception("Error"))))

        val res = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = InitialPage)(successfulFutureOk)(tsTestUser, headerCarrier)
        val resAgent = TestJourneyCheckerClaimToAdjust.withSessionData(journeyState = InitialPage)(successfulFutureOkAgent)(tsTestUserAgent, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
    }
  }

}

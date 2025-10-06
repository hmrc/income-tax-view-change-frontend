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
import cats.data.EitherT
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.{BeforeSubmissionPage, CannotGoBackPage, InitialPage}
import mocks.services.{MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.claimToAdjustPoa.{PaymentOnAccountViewModel, PoaAmendmentData}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, spy, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import testConstants.BaseTestConstants.{testNino, testUserTypeAgent, testUserTypeIndividual}
import testConstants.claimToAdjustPoa.ClaimToAdjustPoaTestConstants.whatYouNeedToKnowViewModel
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import testUtils.TestSupport
import views.html.claimToAdjustPoa.WhatYouNeedToKnow

import scala.concurrent.{ExecutionContext, Future}

class WithSessionAndPoaSpec extends TestSupport with MockPaymentOnAccountSessionService with MockClaimToAdjustService {

  val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val TestWithSessionAndPoa: WithSessionAndPoa = new WithSessionAndPoa {
    val appConfig: FrontendAppConfig = mockAppConfig
    override val poaSessionService: PaymentOnAccountSessionService = mockPaymentOnAccountSessionService
    override val individualErrorHandler: ItvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
    override val agentErrorHandler: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]
    override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    override val claimToAdjustService: ClaimToAdjustService = mockClaimToAdjustService
  }

  lazy val TestWithSessionAndPoaSpy: WithSessionAndPoa = spy(new WithSessionAndPoa {
    val appConfig: FrontendAppConfig = mockAppConfig
    override val poaSessionService: PaymentOnAccountSessionService = mockPaymentOnAccountSessionService
    override val individualErrorHandler: ItvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
    override val agentErrorHandler: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]
    override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    override val claimToAdjustService: ClaimToAdjustService = mockClaimToAdjustService
  })

  val whatYouNeedToKnowView: WhatYouNeedToKnow = app.injector.instanceOf[WhatYouNeedToKnow]

  def successfulFutureOk: (PoaAmendmentData, PaymentOnAccountViewModel) => EitherT[Future, Throwable, Result] = (_, _) => {
    EitherT.rightT(Ok(whatYouNeedToKnowView(isAgent = false, whatYouNeedToKnowViewModel(false, false))))
  }

  def successfulFutureOkAgent: (PoaAmendmentData, PaymentOnAccountViewModel) => EitherT[Future, Throwable, Result] = (_, _) => {
    EitherT.rightT(Ok(whatYouNeedToKnowView(isAgent = true, whatYouNeedToKnowViewModel(true, true))))
  }

  override implicit val individualUser: MtdItUser[_] =
    defaultMTDITUser(Some(testUserTypeIndividual), IncomeSourceDetailsModel(testNino, "test", None, List.empty, List.empty))

  val agentUser: MtdItUser[_] =
    defaultMTDITUser(Some(testUserTypeAgent), businessAndPropertyAligned, FakeRequest())

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(TestWithSessionAndPoaSpy)
  }

  "WithSessionAndPoa.withSessionDataAndPoa when not on the initial page" should {
    "redirect to the You Cannot Go Back error page" when {
      "showCannotGoBackErrorPage returns true and getPoaForNonCrystallisedTaxYear call is successful" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))
        setupMockGetPaymentsOnAccount()

        when(TestWithSessionAndPoaSpy.showCannotGoBackErrorPage(ArgumentMatchers.eq(true), ArgumentMatchers.eq(BeforeSubmissionPage))).thenReturn(true)

        val res = TestWithSessionAndPoaSpy.withSessionDataAndPoa(journeyState = BeforeSubmissionPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoaSpy.withSessionDataAndPoa(journeyState = BeforeSubmissionPage)(successfulFutureOkAgent)(agentUser, headerCarrier)

        status(res) shouldBe SEE_OTHER
        redirectLocation(res) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(false).url)
        status(resAgent) shouldBe SEE_OTHER
        redirectLocation(resAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(true).url)
      }
    }

    "run the code block and go to the what you need to know page" when {
      "showCannotGoBackErrorPage returns false and getPoaForNonCrystallisedTaxYear call is successful" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))
        setupMockGetPaymentsOnAccount()

        when(TestWithSessionAndPoaSpy.showCannotGoBackErrorPage(ArgumentMatchers.eq(false), ArgumentMatchers.eq(CannotGoBackPage))).thenReturn(false)

        val res = TestWithSessionAndPoaSpy.withSessionDataAndPoa(journeyState = CannotGoBackPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoaSpy.withSessionDataAndPoa(journeyState = CannotGoBackPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe OK
        doc.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
        status(resAgent) shouldBe OK
        docAgent.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
      }
    }
    "return an internal server error" when {
      "getMongo returns None so there is not an active session and getPoaForNonCrystallisedTaxYear call is successful" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))
        setupMockGetPaymentsOnAccount()

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = CannotGoBackPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = CannotGoBackPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo returns an exception and getPoaForNonCrystallisedTaxYear call is successful" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Left(new Exception("Error"))))
        setupMockGetPaymentsOnAccount()

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = CannotGoBackPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = CannotGoBackPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo call is successful but getPoaForNonCrystallisedTaxYear returns a None" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))
        setupMockGetPaymentsOnAccount(None)

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = CannotGoBackPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = CannotGoBackPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo call is successful but getPoaForNonCrystallisedTaxYear returns an exception" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))
        setupMockGetPaymentsOnAccountBuildFailure()

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = CannotGoBackPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = CannotGoBackPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
    }
  }
  "WithSessionAndPoa.withSessionDataAndPoa when on the initial page" should {
    "run the code block and go to the what you need to know page" when {
      "getMongo returns a right containing PoA session data but the journeyComplete flag is set to false and getPoaForNonCrystallisedTaxYear call is successful" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))
        setupMockGetPaymentsOnAccount()

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe OK
        doc.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
        status(resAgent) shouldBe OK
        docAgent.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
      }
      "getMongo returns a right containing PoA session data, getPoaForNonCrystallisedTaxYear call is successful, the journeyComplete flag is set to true and createSession returns a Right containing a Unit" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))
        setupMockGetPaymentsOnAccount()
        setupMockPaymentOnAccountSessionServiceCreateSession(Future.successful(Right((): Unit)))

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe OK
        doc.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
        status(resAgent) shouldBe OK
        docAgent.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
      }
      "getMongo returns a right containing None and getPoaForNonCrystallisedTaxYear call is successful and createSession returns a Right containing a Unit" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))
        setupMockGetPaymentsOnAccount()
        setupMockPaymentOnAccountSessionServiceCreateSession(Future.successful(Right((): Unit)))

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe OK
        doc.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
        status(resAgent) shouldBe OK
        docAgent.title() shouldBe "What you need to know - Manage your Self Assessment - GOV.UK"
      }
    }
    "return an internal server error" when {
      "journeyComplete flag is set to true and createSession returns an exception and getPoaForNonCrystallisedTaxYear call is successful" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData(None, None, journeyCompleted = true)))))
        setupMockGetPaymentsOnAccount()
        setupMockPaymentOnAccountSessionServiceCreateSession(Future.successful(Left(new Exception("Error"))))

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"

        status(res) shouldBe INTERNAL_SERVER_ERROR
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo returns an exception and getPoaForNonCrystallisedTaxYear call is successful" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Left(new Exception("Error"))))
        setupMockGetPaymentsOnAccount()

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo call is successful and getPoaForNonCrystallisedTaxYear returns an exception" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))
        setupMockGetPaymentsOnAccountBuildFailure()

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo call is successful and getPoaForNonCrystallisedTaxYear returns a None" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoaAmendmentData()))))
        setupMockGetPaymentsOnAccount(None)

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo returns an exception and getPoaForNonCrystallisedTaxYear returns a None" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Left(new Exception("Error"))))
        setupMockGetPaymentsOnAccount(None)

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo returns an exception and getPoaForNonCrystallisedTaxYear returns an exception" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Left(new Exception("Error"))))
        setupMockGetPaymentsOnAccountBuildFailure()

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo returns a None and getPoaForNonCrystallisedTaxYear returns a None" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))
        setupMockGetPaymentsOnAccount(None)

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
        val doc: Document = Jsoup.parse(contentAsString(res))
        val docAgent: Document = Jsoup.parse(contentAsString(resAgent))

        status(res) shouldBe INTERNAL_SERVER_ERROR
        doc.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
        status(resAgent) shouldBe INTERNAL_SERVER_ERROR
        docAgent.title() shouldBe "Sorry, there is a problem with the service - GOV.UK"
      }
      "getMongo returns a None and getPoaForNonCrystallisedTaxYear returns an exception" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(None)))
        setupMockGetPaymentsOnAccountBuildFailure()

        val res = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOk)(individualUser, headerCarrier)
        val resAgent = TestWithSessionAndPoa.withSessionDataAndPoa(journeyState = InitialPage)(successfulFutureOkAgent)(agentUser, headerCarrier)
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

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

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.BeforeSubmissionPage
import mocks.services.{MockClaimToAdjustService, MockPaymentOnAccountSessionService}
import models.admin.AdjustPaymentsOnAccount
import models.claimToAdjustPoa.PoAAmendmentData
import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{spy, when}
import play.api.http.Status.SEE_OTHER
import play.api.mvc.Result
import play.api.mvc.Results.Ok
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{ClaimToAdjustService, PaymentOnAccountSessionService}
import testUtils.TestSupport
import views.html.claimToAdjustPoa.WhatYouNeedToKnow

import scala.concurrent.{ExecutionContext, Future}

class WithSessionAndPoaSpec extends TestSupport with MockPaymentOnAccountSessionService with MockClaimToAdjustService {

  val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val TestWithSessionAndPoa: WithSessionAndPoa = new WithSessionAndPoa {
    override val appConfig: FrontendAppConfig = mockAppConfig
    override val poaSessionService: PaymentOnAccountSessionService = mockPaymentOnAccountSessionService
    override val itvcErrorHandler: ItvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
    override val itvcErrorHandlerAgent: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]
    override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    override val claimToAdjustService: ClaimToAdjustService = mockClaimToAdjustService
  }

  val TestWithSessionAndPoaSpy: WithSessionAndPoa = spy(new WithSessionAndPoa {
    override val appConfig: FrontendAppConfig = mockAppConfig
    override val poaSessionService: PaymentOnAccountSessionService = mockPaymentOnAccountSessionService
    override val itvcErrorHandler: ItvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
    override val itvcErrorHandlerAgent: AgentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]
    override implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
    override val claimToAdjustService: ClaimToAdjustService = mockClaimToAdjustService
  })

  val whatYouNeedToKnowView: WhatYouNeedToKnow = app.injector.instanceOf[WhatYouNeedToKnow]

  def successfulFutureOk: PoAAmendmentData => Future[Result] = _ =>
    Future.successful(Ok(whatYouNeedToKnowView(isAgent = false, TaxYear(2023, 2024), showIncreaseAfterPaymentContent = false, "")))

  def successfulFutureOkAgent: PoAAmendmentData => Future[Result] = _ =>
    Future.successful(Ok(whatYouNeedToKnowView(isAgent = true, TaxYear(2023, 2024), showIncreaseAfterPaymentContent = false, "")))

  override def beforeEach(): Unit = {
    super.beforeEach()
    enable(AdjustPaymentsOnAccount)
  }

  "WithSessionAndPoa.withSessionDataAndPoa when not on the initial page" should {
    "redirect to the You Cannot Go Back error page" when {
      "showCannotGoBackErrorPage returns true and getPoaForNonCrystallisedTaxYear call is successful" in {
        setupMockPaymentOnAccountSessionService(Future.successful(Right(Some(PoAAmendmentData(None, None, journeyCompleted = true)))))
        setupMockGetPaymentsOnAccount()

        when(TestWithSessionAndPoaSpy.showCannotGoBackErrorPage(ArgumentMatchers.eq(true), ArgumentMatchers.eq(BeforeSubmissionPage))).thenReturn(true)

        val res = TestWithSessionAndPoaSpy.withSessionData(journeyState = BeforeSubmissionPage)(successfulFutureOk)(tsTestUser, headerCarrier)
        val resAgent = TestWithSessionAndPoaSpy.withSessionData(journeyState = BeforeSubmissionPage)(successfulFutureOkAgent)(tsTestUserAgent, headerCarrier)

        status(res) shouldBe SEE_OTHER
        redirectLocation(res) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(false).url)
        status(resAgent) shouldBe SEE_OTHER
        redirectLocation(resAgent) shouldBe Some(controllers.claimToAdjustPoa.routes.YouCannotGoBackController.show(true).url)
      }
    }
  }
}

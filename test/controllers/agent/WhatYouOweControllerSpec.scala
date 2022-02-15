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

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.WhatYouOweController
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import testConstants.FinancialDetailsTestConstants._
import controllers.agent.utils.SessionKeys
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockBtaNavBarPredicate, MockIncomeSourceDetailsPredicate}
import models.financialDetails.{BalanceDetails, FinancialDetailsModel, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers._
import services.WhatYouOweService
import testUtils.TestSupport
import views.html.WhatYouOwe

import scala.concurrent.Future

class WhatYouOweControllerSpec extends TestSupport
  with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with MockItvcErrorHandler
  with MockBtaNavBarPredicate
  with MockFrontendAuthorisedFunctions {

  trait Setup {

    val whatYouOweService: WhatYouOweService = mock[WhatYouOweService]

    val controller = new controllers.WhatYouOweController(
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      whatYouOweService,
      app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler],
      MockBtaNavBarPredicate,
      mockAuthService,
      mockAuditingService,
      mockIncomeSourceDetailsService,
      app.injector.instanceOf[FrontendAppConfig],
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[WhatYouOwe]
    )
  }

  def testFinancialDetail(taxYear: Int): FinancialDetailsModel = financialDetailsModel(taxYear)

  def whatYouOweChargesListFull: WhatYouOweChargesList = WhatYouOweChargesList(
    BalanceDetails(1.00, 2.00, 3.00),
    List(documentDetailWithDueDateModel(2019)),
    List(documentDetailWithDueDateModel(2020)),
    List(documentDetailWithDueDateModel(2021)),
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("BCD", Some("2020-12-31"), 10.23, 1234), OutstandingChargeModel("ACI", None, 1.23, 1234))
    ))
  )

  def whatYouOweChargesListEmpty: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.00, 2.00, 3.00), List.empty)


  "The WhatYouOweController.show function" when {

    "obtaining a users charge" should {
      "send the user to the paymentsOwe page with full data of charges" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBISWithCurrentYearAsMigrationYear()

        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListFull))

        val result = controller.viewWhatYouOweAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        result.futureValue.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("whatYouOwe")


      }

      "return success page with empty data in WhatYouOwe model" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBISWithCurrentYearAsMigrationYear()

        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListEmpty))

        val result = controller.viewWhatYouOweAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        result.futureValue.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("whatYouOwe")


      }

      "send the user to the Internal error page with PaymentsDueService returning exception in case of error" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBISWithCurrentYearAsMigrationYear()
        mockShowInternalServerError()

        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.failed(new Exception("failed to retrieve data")))

        val result = controller.viewWhatYouOweAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "User fails to be authorised" in new Setup {
      setupMockAgentAuthorisationException(withClientPredicate = false)

      val result = controller.viewWhatYouOweAgent()(fakeRequestWithActiveSession)

      status(result) shouldBe Status.SEE_OTHER

    }
  }
}

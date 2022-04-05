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

package controllers

import testConstants.BaseTestConstants
import testConstants.FinancialDetailsTestConstants._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockNavBarEnumFsPredicate, MockIncomeSourceDetailsPredicate}
import models.financialDetails.{BalanceDetails, FinancialDetailsModel, WhatYouOweChargesList}
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{status, _}
import services.WhatYouOweService
import testConstants.BaseTestConstants.testAgentAuthRetrievalSuccess
import views.html.WhatYouOwe

import scala.concurrent.Future

class WhatYouOweControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions {

  trait Setup {

    val whatYouOweService: WhatYouOweService = mock[WhatYouOweService]

    val controller = new WhatYouOweController(
      app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      whatYouOweService,
      app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AgentItvcErrorHandler],
      MockNavBarPredicate,
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
    List(documentDetailWithDueDateModel(2019))
      ++ List(documentDetailWithDueDateModel(2020))
      ++ List(documentDetailWithDueDateModel(2021)),
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("BCD", Some("2020-12-31"), 10.23, 1234), OutstandingChargeModel("ACI", None, 1.23, 1234))
    ))
  )

  def whatYouOweChargesListEmpty: WhatYouOweChargesList = WhatYouOweChargesList(BalanceDetails(1.00, 2.00, 3.00), List.empty)

  val noFinancialDetailErrors = List(testFinancialDetail(2018))
  val hasFinancialDetailErrors = List(testFinancialDetail(2018), testFinancialDetailsErrorModel)
  val hasAFinancialDetailError = List(testFinancialDetailsErrorModel)

  "The WhatYouOweController.viewPaymentsDue function" when {
    "obtaining a users charge" should {
      "send the user to the paymentsOwe page with full data of charges" in new Setup {
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthSuccessWithSaUtrResponse())

        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListFull))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result = controller.show()(fakeRequestWithActiveSession)
        val resultAgent = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        result.futureValue.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("whatYouOwe")
        status(resultAgent) shouldBe Status.OK
        resultAgent.futureValue.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("whatYouOwe")

      }

      "return success page with empty data in WhatYouOwe model" in new Setup {
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthSuccessWithSaUtrResponse())

        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListEmpty))

        when(whatYouOweService.getCreditCharges()(any(), any()))
          .thenReturn(Future.successful(List()))

        val result = controller.show()(fakeRequestWithActiveSession)
        val resultAgent = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.OK
        result.futureValue.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("whatYouOwe")
        status(resultAgent) shouldBe Status.OK
        resultAgent.futureValue.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("whatYouOwe")

      }

      "send the user to the Internal error page with PaymentsDueService returning exception in case of error" in new Setup {
        mockSingleBISWithCurrentYearAsMigrationYear()
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)
        setupMockAuthRetrievalSuccess(BaseTestConstants.testAuthSuccessWithSaUtrResponse())

        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.failed(new Exception("failed to retrieve data")))

        val result = controller.show()(fakeRequestWithActiveSession)
        val resultAgent = controller.showAgent()(fakeRequestConfirmedClient())

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        status(resultAgent) shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "User fails to be authorised" in new Setup {
        setupMockAgentAuthorisationException(withClientPredicate = false)

        val result = controller.showAgent()(fakeRequestWithActiveSession)

        status(result) shouldBe Status.SEE_OTHER

      }
    }

  }
}

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

package controllers.agent.nextPaymentDue

import assets.BaseTestConstants.{testAgentAuthRetrievalSuccess, testArn, testMtdItAgentUser, testMtditidAgent, testNinoAgent, testSaUtrId}
import assets.FinancialDetailsTestConstants._
import assets.FinancialTransactionsTestConstants._
import audit.models.{WhatYouOweRequestAuditModel, WhatYouOweResponseAuditModel}
import config.featureswitch.{AgentViewer, FeatureSwitching, NewFinancialDetailsApi}
import controllers.agent.utils.SessionKeys
import implicits.ImplicitDateFormatter
import mocks.MockItvcErrorHandler
import mocks.connectors.MockIncomeTaxViewChangeConnector
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import models.financialDetails.{FinancialDetailsModel, WhatYouOweChargesList}
import models.financialTransactions.FinancialTransactionsModel
import models.outstandingCharges.{OutstandingChargeModel, OutstandingChargesModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import services.{FinancialTransactionsService, PaymentDueService}
import testUtils.TestSupport

import scala.concurrent.Future

class PaymentDueControllerSpec extends TestSupport
  with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with MockIncomeTaxViewChangeConnector
  with ImplicitDateFormatter
  with MockItvcErrorHandler
  with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
    disable(AgentViewer)
  }

  trait Setup {

    val financialTransactionsService: FinancialTransactionsService = mock[FinancialTransactionsService]
    val paymentDueService: PaymentDueService = mock[PaymentDueService]

    val controller = new PaymentDueController(
      app.injector.instanceOf[views.html.agent.nextPaymentDue.paymentDue],
      financialTransactionsService,
      paymentDueService,
      mockIncomeSourceDetailsService,
      mockItvcHeaderCarrierForPartialsConverter,
      mockAuditingService,
      mockAuthService
    )(appConfig,
      app.injector.instanceOf[MessagesControllerComponents],
      languageUtils,
      mockImplicitDateFormatter,
      ec,
      mockItvcErrorHandler)
  }

  def testFinancialTransaction(taxYear: Int): FinancialTransactionsModel = financialTransactionsModel(s"$taxYear-04-05")

  def testFinancialDetail(taxYear: Int): FinancialDetailsModel = financialDetailsModel(taxYear)

  def whatYouOweChargesListFull: WhatYouOweChargesList = WhatYouOweChargesList(
    List(documentDetailWithDueDateModel(2019)),
    List(documentDetailWithDueDateModel(2020)),
    List(documentDetailWithDueDateModel(2021)),
    Some(OutstandingChargesModel(List(
      OutstandingChargeModel("BCD", Some("2020-12-31"), 10.23, 1234), OutstandingChargeModel("ACI", None, 1.23, 1234))
    ))
  )

  def whatYouOweChargesListEmpty: WhatYouOweChargesList = WhatYouOweChargesList(List.empty)

  val noFinancialTransactionErrors = List(testFinancialTransaction(2018))
  val hasFinancialTransactionErrors = List(testFinancialTransaction(2018), financialTransactionsErrorModel)

  "The PaymentDueControllerSpec.hasFinancialTransactionsError function" when {
    "checking the list of transactions" should {
      "produce false if there are no errors are present" in new Setup {
        val result: Boolean = controller.hasFinancialTransactionsError(noFinancialTransactionErrors)
        result shouldBe false
      }
      "produce true if any errors are present" in new Setup {
        val result: Boolean = controller.hasFinancialTransactionsError(hasFinancialTransactionErrors)
        result shouldBe true
      }
    }
  }


  "The PaymentDueControllerSpec.show function" when {

    "obtaining a users charge" should {
      "send the user to the paymentsOwe page with full data of charges" in new Setup {
        enable(NewFinancialDetailsApi)
        enable(AgentViewer)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBISWithCurrentYearAsMigrationYear()

        when(paymentDueService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListFull))

        val result: Result = await(controller.show()(fakeRequestConfirmedClient()))

        status(result) shouldBe Status.OK
        result.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("paymentDue")


      }

      "return success page with empty data in WhatYouOwe model" in new Setup {
        enable(NewFinancialDetailsApi)
        enable(AgentViewer)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBISWithCurrentYearAsMigrationYear()

        when(paymentDueService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListEmpty))

        val result: Result = await(controller.show()(fakeRequestConfirmedClient()))

        status(result) shouldBe Status.OK
        result.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("paymentDue")


      }

      "send the user to the Internal error page with PaymentsDueService returning exception in case of error" in new Setup {
        enable(AgentViewer)
        enable(NewFinancialDetailsApi)
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBISWithCurrentYearAsMigrationYear()
        mockShowInternalServerError()

        when(paymentDueService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.failed(new Exception("failed to retrieve data")))

        val result: Result = await(controller.show()(fakeRequestConfirmedClient()))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "User fails to be authorised" in new Setup {
      enable(AgentViewer)
      setupMockAgentAuthorisationException(withClientPredicate = false)

      val result: Result = await(controller.show()(fakeRequestWithActiveSession))

      status(result) shouldBe Status.SEE_OTHER

    }
    disable(NewFinancialDetailsApi)
  }
}

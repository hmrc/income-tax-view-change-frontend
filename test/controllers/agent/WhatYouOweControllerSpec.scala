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

package controllers.agent

import assets.BaseTestConstants.testAgentAuthRetrievalSuccess
import assets.FinancialDetailsTestConstants._
import assets.FinancialTransactionsTestConstants._
import config.featureswitch.FeatureSwitching
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
import services.WhatYouOweService
import testUtils.TestSupport

import scala.concurrent.Future

class WhatYouOweControllerSpec extends TestSupport
  with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with MockIncomeTaxViewChangeConnector
  with ImplicitDateFormatter
  with MockItvcErrorHandler
  with FeatureSwitching {

  trait Setup {

    val whatYouOweService: WhatYouOweService = mock[WhatYouOweService]

    val controller = new WhatYouOweController(
      app.injector.instanceOf[views.html.agent.WhatYouOwe],
      whatYouOweService,
      mockIncomeSourceDetailsService,
      mockAuditingService,
      appConfig,
      mockAuthService
    )(app.injector.instanceOf[MessagesControllerComponents],
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


  "The WhatYouOweController.show function" when {

    "obtaining a users charge" should {
      "send the user to the paymentsOwe page with full data of charges" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBISWithCurrentYearAsMigrationYear()

        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListFull))

        val result: Result = await(controller.show()(fakeRequestConfirmedClient()))

        status(result) shouldBe Status.OK
        result.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("paymentDue")


      }

      "return success page with empty data in WhatYouOwe model" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBISWithCurrentYearAsMigrationYear()

        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.successful(whatYouOweChargesListEmpty))

        val result: Result = await(controller.show()(fakeRequestConfirmedClient()))

        status(result) shouldBe Status.OK
        result.session.get(SessionKeys.chargeSummaryBackPage) shouldBe Some("paymentDue")


      }

      "send the user to the Internal error page with PaymentsDueService returning exception in case of error" in new Setup {
        setupMockAgentAuthRetrievalSuccess(testAgentAuthRetrievalSuccess)

        mockSingleBISWithCurrentYearAsMigrationYear()
        mockShowInternalServerError()

        when(whatYouOweService.getWhatYouOweChargesList()(any(), any()))
          .thenReturn(Future.failed(new Exception("failed to retrieve data")))

        val result: Result = await(controller.show()(fakeRequestConfirmedClient()))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "User fails to be authorised" in new Setup {
      setupMockAgentAuthorisationException(withClientPredicate = false)

      val result: Result = await(controller.show()(fakeRequestWithActiveSession))

      status(result) shouldBe Status.SEE_OTHER

    }
  }
}

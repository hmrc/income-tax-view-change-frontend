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

package services

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import config.featureswitch.FeatureSwitching
import connectors.RepaymentHistoryConnector
import mocks.connectors.MockFinancialDetailsConnector
import mocks.services.{MockChargeHistoryService, MockFinancialDetailsService}
import models.financialDetails.{Payment, Payments, PaymentsError}
import models.incomeSourceDetails.TaxYear
import org.mockito.Mockito.mock
import play.api.http.Status.{NOT_FOUND, UNPROCESSABLE_ENTITY}
import services.PaymentHistoryService.PaymentHistoryError
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.oldUserDetails
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class PaymentHistoryServiceSpec
  extends TestSupport
    with MockFinancialDetailsConnector
    with MockFinancialDetailsService
    with MockChargeHistoryService
    with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  val getCurrentTaxEndYear: Int = {
    val currentDate = fixedDate
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  val paymentFull: List[Payment] = List(Payment(reference = Some("reference"), amount = Some(100.00),
    outstandingAmount = Some(1.00), method = Some("method"), documentDescription = None, lot = Some("lot"), lotItem = Some("lotItem"),
    dueDate = Some(fixedDate), documentDate = fixedDate, Some("DOCID01")))

  val oldUser: MtdItUser[_] = defaultMTDITUser(Some(Individual), oldUserDetails)

  val mockRepaymentHistoryConnector: RepaymentHistoryConnector = mock(classOf[RepaymentHistoryConnector])

  object TestPaymentHistoryService extends PaymentHistoryService(mockRepaymentHistoryConnector,
    mockFinancialDetailsConnector, mockFinancialDetailsService, mockChargeHistoryService, dateService, appConfig)

  "getPaymentHistory" when {
    "An error is returned from the connector" should {
      "return a payment history error" in {
        setupGetPayments(TaxYear(getCurrentTaxEndYear-1, getCurrentTaxEndYear))(PaymentsError(500, "ERROR"))
        setupGetPayments(TaxYear(getCurrentTaxEndYear-2, getCurrentTaxEndYear-1))(Payments(List.empty))
        TestPaymentHistoryService.getPaymentHistory.futureValue shouldBe Left(PaymentHistoryError)

      }

      "return a payment history error for status 422" in {
        setupGetPayments(TaxYear(getCurrentTaxEndYear-1, getCurrentTaxEndYear))(PaymentsError(UNPROCESSABLE_ENTITY, "ERROR"))
        setupGetPayments(TaxYear(getCurrentTaxEndYear-2, getCurrentTaxEndYear-1))(Payments(List.empty))
        TestPaymentHistoryService.getPaymentHistory.futureValue shouldBe Left(PaymentHistoryError)

      }
    }

    "a successful Payment History response is returned from the connector" should {
      "return a list of payments and ignore any payment data not found (404s)" in {
        setupGetPayments(TaxYear(getCurrentTaxEndYear-1, getCurrentTaxEndYear))(PaymentsError(NOT_FOUND, "NOT FOUND"))
        setupGetPayments(TaxYear(getCurrentTaxEndYear-2, getCurrentTaxEndYear-1))(Payments(paymentFull))
        TestPaymentHistoryService.getPaymentHistory.futureValue shouldBe Right(paymentFull)
      }
    }

    "duplicate payments are returned in the response from the connector" should {
      "return a list of payments with no duplicates" in {
        setupGetPayments(TaxYear(getCurrentTaxEndYear-1, getCurrentTaxEndYear))(Payments(paymentFull))
        setupGetPayments(TaxYear(getCurrentTaxEndYear-2, getCurrentTaxEndYear-1))(Payments(paymentFull))
        TestPaymentHistoryService.getPaymentHistory.futureValue shouldBe Right(paymentFull)
      }
    }

  }

  "getPaymentHistoryV2" when {
    "An error is returned from the connector" should {
      "return a payment history error" in {
        setupGetPayments(TaxYear.forYearEnd(getCurrentTaxEndYear - 1),
          TaxYear.forYearEnd(getCurrentTaxEndYear))(PaymentsError(500, "ERROR"))
        TestPaymentHistoryService.getPaymentHistoryV2.futureValue shouldBe Left(PaymentHistoryError)

      }

      "return a payment history error for status 422" in {
        setupGetPayments(TaxYear.forYearEnd(getCurrentTaxEndYear - 1),
          TaxYear.forYearEnd(getCurrentTaxEndYear))(PaymentsError(UNPROCESSABLE_ENTITY, "ERROR"))
        TestPaymentHistoryService.getPaymentHistoryV2.futureValue shouldBe Left(PaymentHistoryError)

      }
    }

    "a successful Payment History response is returned from the connector" should {
      "return a list of payments" in {
        setupGetPayments(TaxYear.forYearEnd(getCurrentTaxEndYear - 1),
          TaxYear.forYearEnd(getCurrentTaxEndYear))(Payments(paymentFull))
        TestPaymentHistoryService.getPaymentHistoryV2.futureValue shouldBe Right(paymentFull)
      }
    }
  }

}


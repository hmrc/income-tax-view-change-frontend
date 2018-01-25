/*
 * Copyright 2018 HM Revenue & Customs
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

package models

import assets.TestConstants.CalcBreakdown.calculationDataFullString
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec
import assets.TestConstants._
import assets.TestConstants.FinancialTransactions._
import play.api.libs.json.Json
import utils.ImplicitDateFormatter


class FinancialTransactionsModelSpec extends UnitSpec with Matchers {

  "The FinancialTransactionsModel" should {

    "have the correct idType, idNumber, regimeType, processingDate and transactions" in {
      financialTransactionsModel.idType shouldBe testIdType
      financialTransactionsModel.idNumber shouldBe testIdNumber
      financialTransactionsModel.regimeType shouldBe testRegimeType
      financialTransactionsModel.processingDate shouldBe testProcessingDate
    }

    "for its transactions contain the return data for each field" in {

      val transaction = financialTransactionsModel.financialTransactions.head
      transaction.chargeType shouldBe Some("PAYE")
      transaction.mainType shouldBe Some("2100")
      transaction.periodKey shouldBe Some("13RL")
      transaction.periodKeyDescription shouldBe Some("abcde")
      transaction.taxPeriodFrom shouldBe Some("2017-04-06".toLocalDate)
      transaction.taxPeriodTo shouldBe Some("2018-04-05".toLocalDate)
      transaction.businessPartner shouldBe Some("6622334455")
      transaction.contractAccountCategory shouldBe Some("02")
      transaction.contractAccount shouldBe Some("X")
      transaction.contractObjectType shouldBe Some("ABCD")
      transaction.contractObject shouldBe Some("00000003000000002757")
      transaction.sapDocumentNumber shouldBe Some("1040000872")
      transaction.sapDocumentNumberItem shouldBe Some("XM00")
      transaction.chargeReference shouldBe Some("XM002610011594")
      transaction.mainTransaction shouldBe Some("1234")
      transaction.subTransaction shouldBe Some("5678")
      transaction.originalAmount shouldBe Some(3400.0)
      transaction.outstandingAmount shouldBe Some(1400.0)
      transaction.clearedAmount shouldBe Some(2000.0)
      transaction.accruedInterest shouldBe Some(0.23)
    }

    "for its items return the correct data for each field" in {

      val item = financialTransactionsModel.financialTransactions.head.items.head
      item.subItem shouldBe Some("000")
      item.dueDate shouldBe Some("2018-02-14".toLocalDate)
      item.amount shouldBe Some(3400.00)
      item.clearingDate shouldBe Some("2018-02-17".toLocalDate)
      item.clearingReason shouldBe Some("A")
      item.outgoingPaymentMethod shouldBe Some("B")
      item.paymentLock shouldBe Some("C")
      item.clearingLock shouldBe Some("D")
      item.interestLock shouldBe Some("E")
      item.dunningLock shouldBe Some("1")
      item.returnFlag shouldBe Some(false)
      item.paymentReference shouldBe Some("F")
      item.paymentAmount shouldBe Some(2000.00)
      item.paymentMethod shouldBe Some("G")
      item.paymentLot shouldBe Some("H")
      item.paymentLotItem shouldBe Some("112")
      item.clearingSAPDocument shouldBe Some("3350000253")
      item.statisticalDocument shouldBe Some("I")
      item.returnReason shouldBe Some("J")
      item.promiseToPay shouldBe Some("K")

    }

    "be formatted to JSON correctly" in {
      Json.toJson[FinancialTransactionsModel](financialTransactionsModel) shouldBe financialTransactionsJson
    }

    "be able to parse a full Json string into the Model" in {
      financialTransactionsJson.as[FinancialTransactionsModel] shouldBe financialTransactionsModel
    }
  }

  "The FinancialTransactionsErrorModel" should {

    "be formatted to JSON correctly" in {
      Json.toJson[FinancialTransactionsErrorModel](financialTransactionsErrorModel) shouldBe financialTransactionsErrorJson
    }

    "be able to parse a full Json string into the Model" in {
      financialTransactionsErrorJson.as[FinancialTransactionsErrorModel] shouldBe financialTransactionsErrorModel
    }

  }

}

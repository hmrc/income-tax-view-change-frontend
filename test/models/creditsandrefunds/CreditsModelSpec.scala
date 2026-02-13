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

package models.creditsandrefunds

import models.core.ErrorModel
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import play.api.libs.json._
import testUtils.UnitSpec
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDate

class CreditsModelSpec extends UnitSpec {

  val availableCreditForRepayment:BigDecimal = 20

  val allocatedCreditForFutureCharges: BigDecimal = 10

  val unallocatedCredit: BigDecimal = 20

  val totalCredit: BigDecimal = 35

  val firstPendingAmountRequested: BigDecimal = 100
  val secondPendingAmountRequested: BigDecimal = 200

  val allCreditsJson: String =
    s"""
      |{
      |  "availableCreditForRepayment" : ${availableCreditForRepayment},
      |  "allocatedCreditForFutureCharges" : ${allocatedCreditForFutureCharges},
      |  "unallocatedCredit" : ${unallocatedCredit},
      |  "totalCredit" : ${totalCredit},
      |  "firstPendingAmountRequested": ${firstPendingAmountRequested},
      |  "secondPendingAmountRequested": ${secondPendingAmountRequested},
      |  "transactions" : [ {
      |    "transactionType" : "refund",
      |    "amount" : 5,
      |    "transactionId" : "REFUND01"
      |  }, {
      |    "transactionType" : "refund",
      |    "amount" : 5,
      |    "transactionId" : "REFUND02"
      |  }, {
      |    "transactionType" : "cutOver",
      |    "amount" : 1,
      |    "taxYear" : 2023,
      |    "dueDate" : "2023-01-01",
      |    "effectiveDateOfPayment" : "2023-01-01",
      |    "transactionId" : "CUTOVER01"
      |  }, {
      |    "transactionType" : "balancingCharge",
      |    "amount" : 2,
      |    "taxYear" : 2024,
      |    "dueDate" : "2024-01-01",
      |    "effectiveDateOfPayment" : "2024-01-01",
      |    "transactionId" : "BALANCING01"
      |  }, {
      |    "transactionType" : "mfa",
      |    "amount" : 3,
      |    "taxYear" : 2021,
      |    "dueDate" : "2021-01-01",
      |    "effectiveDateOfPayment" : "2021-01-01",
      |    "transactionId" : "MFA01"
      |  }, {
      |    "transactionType" : "repaymentInterest",
      |    "amount" : 4,
      |    "taxYear" : 2022,
      |    "dueDate" : "2022-01-01",
      |    "effectiveDateOfPayment" : "2022-01-01",
      |    "transactionId" : "REPAYMENTINT01"
      |  } ]
      |}
      |""".stripMargin

  val allCreditsObj = CreditsModel(availableCreditForRepayment, allocatedCreditForFutureCharges,
    unallocatedCredit, totalCredit, Some(firstPendingAmountRequested), Some(secondPendingAmountRequested), List(
    Transaction(transactionType = Repayment,
      amount = 5,
      taxYear = None,
      dueDate = None,
      effectiveDateOfPayment = None,
      transactionId = "REFUND01"),
    Transaction(transactionType = Repayment,
      amount = 5,
      taxYear = None,
      dueDate = None,
      effectiveDateOfPayment = None,
      transactionId = "REFUND02"),
    Transaction(transactionType = CutOverCreditType,
      amount = 1,
      taxYear = Some(TaxYear.forYearEnd(2023)),
      dueDate = Some(dateInYear(2023)),
      effectiveDateOfPayment = Some(dateInYear(2023)),
      transactionId = "CUTOVER01"),
    Transaction(transactionType = BalancingChargeCreditType,
      amount = 2,
      taxYear = Some(TaxYear.forYearEnd(2024)),
      dueDate = Some(dateInYear(2024)),
      effectiveDateOfPayment = Some(dateInYear(2024)),
      transactionId = "BALANCING01"),
    Transaction(transactionType = MfaCreditType,
      amount = 3,
      taxYear = Some(TaxYear.forYearEnd(2021)),
      dueDate = Some(dateInYear(2021)),
      effectiveDateOfPayment = Some(dateInYear(2021)),
      transactionId = "MFA01"),
    Transaction(transactionType = RepaymentInterest,
      amount = 4,
      taxYear = Some(TaxYear.forYearEnd(2022)),
      dueDate = Some(dateInYear(2022)),
      effectiveDateOfPayment = Some(dateInYear(2022)),
      transactionId = "REPAYMENTINT01")
  ))

  "httpParser" when {

    "response is successful should parse success model successfully" in {
      val response = CreditsModel.reads.read("", "", HttpResponse.apply(200, allCreditsJson))
      response.isRight shouldBe true
      response match {
        case Right(creditsModel: CreditsModel) =>
          creditsModel shouldBe allCreditsObj
        case _ =>
          fail("Success response should be parsed successfully")
      }
    }

    val error =
      """
        |{
        | "code": 500,
        | "message": "Internal server error"
        |}
        |""".stripMargin

    "response is unsuccessful should parse error model successfully" in {
      val response = CreditsModel.reads.read("", "", HttpResponse.apply(500, error))
      response.isLeft shouldBe true
      response match {
        case Left(errorModel: ErrorModel) =>
          errorModel shouldBe ErrorModel(500, "Internal server error")
        case _ =>
          fail("Error response should be parsed successfully")
      }
    }
  }


  "CreditAndRefundModel" should {

    "parse from JSON correctly" in {

      val result: JsResult[CreditsModel] = Json.parse(allCreditsJson)
        .validate[CreditsModel]

      result match {
        case JsSuccess(obj, _) =>
          obj shouldBe allCreditsObj
        case _ => fail("Model did not validate correctly")
      }
    }


    "write to JSON correctly" in {

      val result = Json.toJson(allCreditsObj)

      (result \ "allocatedCreditForFutureCharges").get shouldBe JsNumber(allocatedCreditForFutureCharges)
      (result \ "availableCreditForRepayment").get shouldBe JsNumber(availableCreditForRepayment)
      (result \ "totalCredit").get shouldBe JsNumber(totalCredit)
      (result \ "unallocatedCredit").get shouldBe JsNumber(unallocatedCredit)
      (result \ "firstPendingAmountRequested").get shouldBe JsNumber(firstPendingAmountRequested)
      (result \ "secondPendingAmountRequested").get shouldBe JsNumber(secondPendingAmountRequested)
      (result \ "transactions").get match {
        case r: JsArray =>
          r.value.size shouldBe 6
        case _ => fail("transactions should be JsArray")
      }

      result shouldBe Json.parse(allCreditsJson)

    }

    "return error when data is invalid" in {

      val invalid =
        s"""
          |{
          |  "availableCreditForRepayment" : ${availableCreditForRepayment},
          |  "allocatedCreditForFutureCharges" : ${allocatedCreditForFutureCharges},
          |  "unallocatedCredit" : ${unallocatedCredit},
          |  "totalCredit" : ${totalCredit},
          |  "firstPendingAmountRequested": ${firstPendingAmountRequested},
          |  "secondPendingAmountRequested": ${secondPendingAmountRequested},
          |  "transactions" : [ {
          |    "transactionType" : "invalid credit type",
          |    "amount" : 5,
          |    "transactionId": "invalid01"
          |  } ]
          |}
          |""".stripMargin

      val result: JsResult[CreditsModel] = Json.parse(invalid)
        .validate[CreditsModel]

      result match {
        case JsError(errors) =>
          errors.size shouldBe 1
          s"${errors.head._1}" shouldBe s"""${(JsPath \ "transactions(0)" \ "transactionType")}"""
          errors.head._2.head.message shouldBe "Could not parse transactionType"
        case _ => fail("Model should have returned errors")
      }
    }
  }

  def dateInYear(year: Int) = LocalDate.of(year, 1, 1)


}

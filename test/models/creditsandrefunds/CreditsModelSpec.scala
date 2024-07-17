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

import models.financialDetails._
import play.api.libs.json._
import testUtils.UnitSpec
import uk.gov.hmrc.http.HttpResponse

import java.time.LocalDate

class CreditsModelSpec extends UnitSpec {

  val availableCredit:BigDecimal = 20

  val allocatedCredit:BigDecimal = 10

  val allCreditsJson =
    s"""
      |{
      |  "availableCredit" : ${availableCredit},
      |  "allocatedCredit" : ${allocatedCredit},
      |  "transactions" : [ {
      |    "creditType" : "refund",
      |    "amount" : 5
      |  }, {
      |    "creditType" : "refund",
      |    "amount" : 5
      |  }, {
      |    "creditType" : "cutOver",
      |    "amount" : 1,
      |    "taxYear" : "2023",
      |    "dueDate" : "2023-01-01"
      |  }, {
      |    "creditType" : "balancingCharge",
      |    "amount" : 2,
      |    "taxYear" : "2024",
      |    "dueDate" : "2024-01-01"
      |  }, {
      |    "creditType" : "mfa",
      |    "amount" : 3,
      |    "taxYear" : "2021",
      |    "dueDate" : "2021-01-01"
      |  }, {
      |    "creditType" : "repaymentInterest",
      |    "amount" : 4,
      |    "taxYear" : "2022",
      |    "dueDate" : "2022-01-01"
      |  } ]
      |}
      |""".stripMargin

  val allCreditsObj = CreditsModel(availableCredit, allocatedCredit, List(
    Transaction(creditType = Repayment,
      amount = 5,
      taxYear = None,
      dueDate = None),
    Transaction(creditType = Repayment,
      amount = 5,
      taxYear = None,
      dueDate = None),
    Transaction(creditType = CutOverCreditType,
      amount = 1,
      taxYear = Some("2023"),
      dueDate = Some(dateInYear(2023))),
    Transaction(creditType = BalancingChargeCreditType,
      amount = 2,
      taxYear = Some("2024"),
      dueDate = Some(dateInYear(2024))),
    Transaction(creditType = MfaCreditType,
      amount = 3,
      taxYear = Some("2021"),
      dueDate = Some(dateInYear(2021))),
    Transaction(creditType = RepaymentInterest,
      amount = 4,
      taxYear = Some("2022"),
      dueDate = Some(dateInYear(2022)))
  ))

  "CreditAndRefundModel" should {

    "httpParser" in {
      val response = CreditsModel.reads.read("", "", HttpResponse.apply(200, allCreditsJson))
      response.isRight shouldBe true
//      response.forall(model => {
//
//      })
    }

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

      (result \ "allocatedCredit").get shouldBe JsNumber(allocatedCredit)
      (result \ "availableCredit").get shouldBe JsNumber(availableCredit)
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
          |  "availableCredit" : ${availableCredit},
          |  "allocatedCredit" : ${allocatedCredit},
          |  "transactions" : [ {
          |    "creditType" : "invalid credit type",
          |    "amount" : 5
          |  } ]
          |}
          |""".stripMargin

      val result: JsResult[CreditsModel] = Json.parse(invalid)
        .validate[CreditsModel]

      result match {
        case JsError(errors) =>
          errors.size shouldBe 1
          s"${errors.head._1}" shouldBe s"""${(JsPath \ "transactions(0)" \ "creditType")}"""
          errors.head._2.head.message shouldBe "Could not parse transactionType"
        case _ => fail("Model should have returned errors")
      }
    }
  }

  def dateInYear(year: Int) = LocalDate.of(year, 1, 1)


}

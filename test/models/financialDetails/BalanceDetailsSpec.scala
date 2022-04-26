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

package models.financialDetails

import play.api.libs.json.{JsValue, Json}
import testUtils.UnitSpec

class BalanceDetailsSpec extends UnitSpec {

  def balanceDetailsJson(availableCredit: Option[BigDecimal] = None,
                         firstPendingAmountRequested: Option[BigDecimal] = None,
                         secondPendingAmountRequested: Option[BigDecimal] = None): JsValue = Json.obj(
    "balanceDueWithin30Days" -> 1.00,
    "overDueAmount" -> 2.00,
    "totalBalance" -> 3.00
  ) ++ availableCredit.fold(Json.obj())(amount => Json.obj("availableCredit" -> amount)) ++
    firstPendingAmountRequested.fold(Json.obj())(amount => Json.obj("firstPendingAmountRequested" -> amount)) ++
    secondPendingAmountRequested.fold(Json.obj())(amount => Json.obj("secondPendingAmountRequested" -> amount))


  def balanceDetailsModel(availableCredit: Option[BigDecimal] = None,
                          firstPendingAmountRequested: Option[BigDecimal] = None,
                          secondPendingAmountRequested: Option[BigDecimal] = None): BalanceDetails = BalanceDetails(
    balanceDueWithin30Days = 1.00,
    overDueAmount = 2.00,
    totalBalance = 3.00,
    availableCredit,
    firstPendingAmountRequested,
    secondPendingAmountRequested
  )


  "BalanceDetails model" should {
    "return a correct model" when {
      "minimal json is supplied" in {
        Json.parse(balanceDetailsJson().toString()).as[BalanceDetails] shouldBe balanceDetailsModel()

      }

      "full json is supplied" in {
        val result = Json.parse(
          balanceDetailsJson(Some(BigDecimal(5.00)), Some(BigDecimal(2.00) ), Some(BigDecimal(3.00))).toString()
        ).as[BalanceDetails]

        result shouldBe balanceDetailsModel(Some(BigDecimal(5.00)), Some(BigDecimal(2.00) ), Some(BigDecimal(3.00)))
      }
    }

    "write correct Json" when {
      "a minimal model is supplied" in {
        Json.toJson[BalanceDetails](balanceDetailsModel()) shouldBe balanceDetailsJson()
      }

      "a full model is supplied " in {
        val result = Json.toJson[BalanceDetails](
          balanceDetailsModel(Some(BigDecimal(5.00)), Some(BigDecimal(2.00) ), Some(BigDecimal(3.00)))
        )

        result shouldBe balanceDetailsJson(Some(BigDecimal(5.00)), Some(BigDecimal(2.00) ), Some(BigDecimal(3.00)))
      }
    }

    "display the correct refund status" when {
      "firstPendingAmountRequested is present but secondPendingAmountRequested is not" in {
        balanceDetailsModel(firstPendingAmountRequested = Some(BigDecimal(2.00))).refundInProgress shouldBe true
      }

      "secondPendingAmountRequested is present but firstPendingAmountRequested is not" in {
        balanceDetailsModel(secondPendingAmountRequested = Some(BigDecimal(2.00))).refundInProgress shouldBe true
      }

      "secondPendingAmountRequested and firstPendingAmountRequested are present" in {
        balanceDetailsModel(firstPendingAmountRequested = Some(BigDecimal(2.00)),
          secondPendingAmountRequested = Some(BigDecimal(2.00))).refundInProgress shouldBe true
      }

      "secondPendingAmountRequested and firstPendingAmountRequested are not present" in {
        balanceDetailsModel().refundInProgress shouldBe false
      }
    }
  }
}

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

package views.helpers.injected

import assets.{EstimatesTestConstants, MessagesLookUp}
import models.financialDetails.DocumentDetailWithDueDate
import models.financialTransactions.TransactionModel
import testUtils.ViewSpec
import views.html.helpers.injected.StatusHelper

class StatusHelperSpec extends ViewSpec {

  def testTransaction(paid: Boolean = false, overdue: Boolean = false): Option[TransactionModel] =
    Some(EstimatesTestConstants.transactionModelStatus(paid = paid, overdue = overdue))

  def testCharge(paid: Boolean = false, overdue: Boolean = false): Option[DocumentDetailWithDueDate] =
    Some(EstimatesTestConstants.chargeModelStatus(paid = paid, overdue = overdue))

  val statusHelper: StatusHelper = app.injector.instanceOf[StatusHelper]

  class Test(transaction: Option[TransactionModel] = None, charge: Option[DocumentDetailWithDueDate] = None)
    extends Setup(statusHelper(transaction, charge))

  "The status helper" should {

    "return the correct status with Transaction model" when {

      "provided with no matching financial transaction for an ongoing status" which {

        "has the correct class" in new Test() {
          document.select("span").attr("class") shouldBe "govUk-tag"
        }

        "has the correct message" in new Test() {
          document.select("span").text() shouldBe MessagesLookUp.TaxYears.ongoing
        }
      }

      "provided with an unpaid financial transaction for an ongoing status" which {

        "has the correct class" in new Test(transaction = testTransaction()) {
          document.select("span").attr("class") shouldBe "govUk-tag"
        }

        "has the correct message" in new Test(transaction = testTransaction()) {
          document.select("span").text() shouldBe MessagesLookUp.TaxYears.ongoing
        }
      }

      "provided with a paid financial transaction for a complete status" which {

        "has the correct class" in new Test(transaction = testTransaction(paid = true)) {
          document.select("span").attr("class") shouldBe "govUk-tag govUk-tag--complete"
        }

        "has the correct message" in new Test(transaction = testTransaction(paid = true)) {
          document.select("span").text() shouldBe MessagesLookUp.TaxYears.complete
        }
      }

      "provided with an unpaid financial transaction for an overdue status" which {

        "has the correct class" in new Test(transaction = testTransaction(overdue = true)) {
          document.select("span").attr("class") shouldBe "govUk-tag govUk-tag--overdue"
        }

        "has the correct message" in new Test(transaction = testTransaction(overdue = true)) {
          document.select("span").text() shouldBe MessagesLookUp.TaxYears.overdue
        }
      }
    }

    "return the correct status with Charge model" when {

      "provided with no matching financial details for an ongoing status" which {

        "has the correct class" in new Test {
          document.select("span").attr("class") shouldBe "govUk-tag"
        }

        "has the correct message" in new Test {
          document.select("span").text() shouldBe MessagesLookUp.TaxYears.ongoing
        }
      }

      "provided with an unpaid financial details for an ongoing status" which {

        "has the correct class" in new Test(charge = testCharge()) {
          document.select("span").attr("class") shouldBe "govUk-tag"
        }

        "has the correct message" in new Test(charge = testCharge()) {
          document.select("span").text() shouldBe MessagesLookUp.TaxYears.ongoing
        }
      }

      "provided with a paid financial detail for a complete status" which {

        "has the correct class" in new Test(charge = testCharge(paid = true)) {
          document.select("span").attr("class") shouldBe "govUk-tag govUk-tag--complete"
        }

        "has the correct message" in new Test(charge = testCharge(paid = true)) {
          document.select("span").text() shouldBe MessagesLookUp.TaxYears.complete
        }
      }

      "provided with an unpaid financial detail for an overdue status" which {

        "has the correct class" in new Test(charge = testCharge(overdue = true)) {
          document.select("span").attr("class") shouldBe "govUk-tag govUk-tag--overdue"
        }

        "has the correct message" in new Test(charge = testCharge(overdue = true)) {
          document.select("span").text() shouldBe MessagesLookUp.TaxYears.overdue
        }
      }
    }
  }

}

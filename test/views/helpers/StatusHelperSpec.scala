/*
 * Copyright 2020 HM Revenue & Customs
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

package views.helpers

import java.time.LocalDate

import assets.{EstimatesTestConstants, Messages}
import models.financialTransactions.{SubItemModel, TransactionModel, TransactionModelWithYear}
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import testUtils.TestSupport
import views.html.helpers.statusHelper

class StatusHelperSpec extends TestSupport {

  "The status helper" should {

    "return the correct status" when {

      "provided with no matching financial transaction for an ongoing status" which {
        val result = Jsoup.parse(statusHelper(None).toString())

        "has the correct class" in {
          result.select("div").attr("class") shouldBe "govUk-tag"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe Messages.TaxYears.ongoing
        }
      }

      "provided with an unpaid financial transaction for an ongoing status" which {
        val result = Jsoup.parse(statusHelper(Some(TransactionModelWithYear(EstimatesTestConstants.transactionModelStatus(false, false), 2020))).toString())

        "has the correct class" in {
          result.select("div").attr("class") shouldBe "govUk-tag"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe Messages.TaxYears.ongoing
        }
      }

      "provided with a paid financial transaction for a complete status" which {
        val result = Jsoup.parse(statusHelper(Some(TransactionModelWithYear(EstimatesTestConstants.transactionModelStatus(true, false), 2020))).toString())

        "has the correct class" in {
          result.select("div").attr("class") shouldBe "govUk-tag govUk-tag--complete"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe Messages.TaxYears.complete
        }
      }

      "provided with an unpaid financial transaction for an overdue status" which {
        val result = Jsoup.parse(statusHelper(Some(TransactionModelWithYear(EstimatesTestConstants.transactionModelStatus(false, true), 2020))).toString())

        "has the correct class" in {
          result.select("div").attr("class") shouldBe "govUk-tag govUk-tag--overdue"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe Messages.TaxYears.overdue
        }
      }
    }
  }
}

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

package views.helpers

import assets.{EstimatesTestConstants, MessagesLookUp}
import org.jsoup.Jsoup
import testUtils.TestSupport
import views.html.helpers.injected.StatusHelper

class StatusHelperSpec extends TestSupport {

  lazy val statusHelper = app.injector.instanceOf[StatusHelper]

  "The status helper" should {

    "return the correct status with Transaction model" when {

      "provided with no matching financial transaction for an ongoing status" which {
        val result = Jsoup.parse(statusHelper(None).toString())

        "has the correct class" in {
          result.select("span").attr("class") shouldBe "govUk-tag"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe MessagesLookUp.TaxYears.ongoing
        }
      }

      "provided with an unpaid financial transaction for an ongoing status" which {
        val result = Jsoup.parse(statusHelper(Some(EstimatesTestConstants.transactionModelStatus(paid = false, overdue = false))).toString())

        "has the correct class" in {
          result.select("span").attr("class") shouldBe "govUk-tag"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe MessagesLookUp.TaxYears.ongoing
        }
      }

      "provided with a paid financial transaction for a complete status" which {
        val result = Jsoup.parse(statusHelper(Some(EstimatesTestConstants.transactionModelStatus(paid = true, overdue = false))).toString())

        "has the correct class" in {
          result.select("span").attr("class") shouldBe "govUk-tag govUk-tag--complete"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe MessagesLookUp.TaxYears.complete
        }
      }

      "provided with an unpaid financial transaction for an overdue status" which {
        val result = Jsoup.parse(statusHelper(Some(EstimatesTestConstants.transactionModelStatus(paid = false, overdue = true))).toString())

        "has the correct class" in {
          result.select("span").attr("class") shouldBe "govUk-tag govUk-tag--overdue"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe MessagesLookUp.TaxYears.overdue
        }
      }
    }

    "return the correct status with Charge model" when {

      "provided with no matching financial details for an ongoing status" which {
        val result = Jsoup.parse(statusHelper(None).toString())

        "has the correct class" in {
          result.select("span").attr("class") shouldBe "govUk-tag"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe MessagesLookUp.TaxYears.ongoing
        }
      }

      "provided with an unpaid financial details for an ongoing status" which {
        val result = Jsoup.parse(statusHelper(None, Some(EstimatesTestConstants.chargeModelStatus(paid = false, overdue = false))).toString())

        "has the correct class" in {
          result.select("span").attr("class") shouldBe "govUk-tag"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe MessagesLookUp.TaxYears.ongoing
        }
      }

      "provided with a paid financial detail for a complete status" which {
        val result = Jsoup.parse(statusHelper(None, Some(EstimatesTestConstants.chargeModelStatus(paid = true, overdue = false))).toString())

        "has the correct class" in {
          result.select("span").attr("class") shouldBe "govUk-tag govUk-tag--complete"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe MessagesLookUp.TaxYears.complete
        }
      }

      "provided with an unpaid financial detail for an overdue status" which {
        val result = Jsoup.parse(statusHelper(None, Some(EstimatesTestConstants.chargeModelStatus(paid = false, overdue = true))).toString())

        "has the correct class" in {
          result.select("span").attr("class") shouldBe "govUk-tag govUk-tag--overdue"
        }

        "has the correct message" in {
          result.select("span").text() shouldBe MessagesLookUp.TaxYears.overdue
        }
      }
    }
  }
}

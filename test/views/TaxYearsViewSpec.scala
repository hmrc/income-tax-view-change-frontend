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

package views

import assets.EstimatesTestConstants._
import assets.Messages
import assets.Messages.{Breadcrumbs => breadcrumbMessages}
import config.FrontendAppConfig
import models.calculation.CalculationResponseModelWithYear
import models.financialTransactions.TransactionModelWithYear
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport


class TaxYearsViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  private def pageSetup(calcs: List[CalculationResponseModelWithYear], transactions: List[TransactionModelWithYear] = List()) = new {
    lazy val page: HtmlFormat.Appendable =
      views.html.taxYears(calcs, transactions)(FakeRequest(), applicationMessages, mockAppConfig)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The TaxYears view" when {
    "the user has two tax years with no financial transactions" should {
      val setup = pageSetup(lastTaxCalcWithYearList)
      import setup._
      val messages = Messages.TaxYears

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have a breadcrumb trail" in {
        document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
        document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
        document.getElementById("breadcrumb-tax-years").text shouldBe breadcrumbMessages.taxYears
      }

      "have a header" in {
        document.getElementById("heading").text shouldBe messages.heading
      }

      s"have the paragraph '${messages.p1}'" in {
        document.getElementById("view-taxYears").text shouldBe messages.p1
      }

      "display the links for both of the tax years with taxYears" in {
        document.getElementById(s"taxYears-link-$testYear").text shouldBe messages.taxYearLink((testYear - 1).toString, testYear.toString)
        document.getElementById(s"taxYears-link-$testYearPlusOne").text shouldBe messages.taxYearLink(testYear.toString, testYearPlusOne.toString)
      }
    }

    "the user has three tax years with different financial information" should {
      val setup = pageSetup(lastThreeTaxCalcWithYear, lastThreeTaxYearFinancialTransactions)
      import setup._
      val messages = Messages.TaxYears

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have a header" in {
        document.getElementById("heading").text shouldBe messages.heading
      }

      "display the links for all three of the tax years with taxYears" in {
        document.getElementById(s"taxYears-link-$testYear").text shouldBe messages.taxYearLink((testYear - 1).toString, testYear.toString)
        document.getElementById(s"taxYears-link-$testYearPlusOne").text shouldBe messages.taxYearLink(testYear.toString, testYearPlusOne.toString)
        document.getElementById(s"taxYears-link-$testYearPlusTwo").text shouldBe messages.taxYearLink(testYearPlusOne.toString, testYearPlusTwo.toString)
      }

      "display the correct status for all three of the tax years" in {
        val statuses = document.select(".govUk-tag")
        statuses.get(0).text() shouldBe messages.overdue
        statuses.get(1).text() shouldBe messages.complete
        statuses.get(2).text() shouldBe messages.ongoing
      }
    }

    "the user has no taxYears" should {
      val setup = pageSetup(List())
      import setup._
      val messages = Messages.TaxYears

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have a header" in {
        document.getElementById("heading").text shouldBe messages.heading
      }

      s"have the paragraph '${messages.noEstimates}'" in {
        document.getElementById("no-taxYears").text shouldBe messages.noEstimates
      }
    }
  }
}

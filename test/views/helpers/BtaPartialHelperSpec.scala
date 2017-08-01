/*
 * Copyright 2017 HM Revenue & Customs
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

import assets.TestConstants.Estimates._
import models._
import play.twirl.api.Html
import utils.TestSupport
import assets.TestConstants.Obligations._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

class BtaPartialHelperSpec extends TestSupport {

  "The BtaPartialHelper's whichStatus method" should {
    "return Html corresponding to the ObligationStatus" when {
      val openHtml = Html(
        """
           |<p id="report-due">Your next report is due by 31 October 2017</p>
           |<a id="obligations-link" href=/report-quarterly/income-and-expenses/view/obligations>View deadlines</a>
         """.stripMargin.trim
      )

      val overdueHtml = Html(
        """
           |<p id="report-due">You have an overdue report</p>
           |<a id="obligations-link" href=/report-quarterly/income-and-expenses/view/obligations>View deadlines</a>
         """.stripMargin.trim
      )

      val receivedHtml = Html(
        """
          |<p id="report-due">Your latest report has been received</p>
          |<a id="obligations-link" href=/report-quarterly/income-and-expenses/view/obligations>View deadlines</a>
        """.stripMargin.trim
      )

      "passed 'Open' the Open Html is returned" in {
        BtaPartialHelper.whichStatus(openObligation) shouldBe openHtml
      }
      "passed 'Overdue' the Overdue Html is returned" in {
        BtaPartialHelper.whichStatus(overdueObligation) shouldBe overdueHtml
      }
      "passed 'Received' the Received Html is returned" in {
        BtaPartialHelper.whichStatus(receivedObligation) shouldBe receivedHtml
      }
    }
  }

  "The BtaPartialHelper's showLastEstimate method" should {
    "return Html corresponding to the estimated tax for a single tax year" when {

      val successHtml: Html = Html(
        """
          |<p id="current-estimate-2018">Your estimated tax amount is &pound;543.21</p>
          |<a id="estimates-link-2018" href=/report-quarterly/income-and-expenses/view/estimated-tax-liability/2018>View details</a>
        """.stripMargin.trim
      )

      "passed a number the Html is returned" in {
        BtaPartialHelper.showLastEstimate(List(lastTaxCalcSuccessWithYear)) shouldBe List(successHtml)
      }
    }

    "return Html corresponding to the estimated tax for both tax years" when {

      val successHtml1: Html = Html(
        """
          |<p id="current-estimate-2018">Your estimated tax amount is &pound;543.21</p>
          |<a id="estimates-link-2018" href=/report-quarterly/income-and-expenses/view/estimated-tax-liability/2018>View details</a>
        """.stripMargin.trim
      )
      val successHtml2: Html = Html(
        """
          |<p id="current-estimate-2019">Your estimated tax amount is &pound;6,543.21</p>
          |<a id="estimates-link-2019" href=/report-quarterly/income-and-expenses/view/estimated-tax-liability/2019>View details</a>
        """.stripMargin.trim
      )

      "return Html corresponding to the estimated tax for misaligned tax years" in {
        BtaPartialHelper.showLastEstimate(List(lastTaxCalcSuccessWithYear, LastTaxCalculationWithYear(LastTaxCalculation("CALCID","2018-07-06T12:34:56.789Z", 6543.21), 2019))) shouldBe List(successHtml1,successHtml2)
      }
    }

    "return Html with an estimate for 2018 tax year and no estimate for 2019 tax year" when {

      val successHtml: Html = Html(
        """
          |<p id="current-estimate-2018">Your estimated tax amount is &pound;543.21</p>
          |<a id="estimates-link-2018" href=/report-quarterly/income-and-expenses/view/estimated-tax-liability/2018>View details</a>
        """.stripMargin.trim
      )

      val noCalcHtml: Html = Html(
        """
          |<p>Once you've submitted a report using your accounting software, you can view your estimate for 2018 to 2019 tax year here.</p>
        """.stripMargin.trim
      )

      "" in {
        BtaPartialHelper.showLastEstimate(List(lastTaxCalcSuccessWithYear, LastTaxCalculationWithYear(NoLastTaxCalculation, 2019))) shouldBe List(successHtml,noCalcHtml)
      }
    }
  }

}

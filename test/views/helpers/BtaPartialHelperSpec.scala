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
import config.FrontendAppConfig
import assets.TestConstants.Obligations._
import models._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import utils.TestSupport

class BtaPartialHelperSpec extends TestSupport {

  lazy val mockAppConfig = fakeApplication.injector.instanceOf[FrontendAppConfig]

  "The BtaPartialHelper's whichStatus method" should {
    "return Html corresponding to the ObligationStatus" when {

      lazy val baseUrl = mockAppConfig.itvcFrontendEnvironment
      val open =
        s"""<div class="form-group">
           |<p id="report-due">Your next report is due by 31 October 2017</p>

           |<a id="obligations-link" href="$baseUrl/report-quarterly/income-and-expenses/view/obligations">View deadlines</a>
           |</div>""".stripMargin.replaceAll("\n","")

      val overdue =
        s"""<div class="form-group">
           |<p id="report-due">You have an overdue report</p>

           |<a id="obligations-link" href="$baseUrl/report-quarterly/income-and-expenses/view/obligations">View deadlines</a>
           |</div>""".stripMargin.replaceAll("\n","")

      val received =
        s"""<div class="form-group">
          |<p id="report-due">Your latest report has been received</p>
          |<a id="obligations-link" href="$baseUrl/report-quarterly/income-and-expenses/view/obligations">View deadlines</a>
          |</div>""".stripMargin.replaceAll("\n","")

      "passed 'Open' the Open Html is returned" in {
        BtaPartialHelper.whichStatus(openObligation)(applicationMessages, mockAppConfig) shouldBe open
      }
      "passed 'Overdue' the Overdue Html is returned" in {
        BtaPartialHelper.whichStatus(overdueObligation)(applicationMessages, mockAppConfig) shouldBe overdue
      }
      "passed 'Received' the Received Html is returned" in {
        BtaPartialHelper.whichStatus(receivedObligation)(applicationMessages, mockAppConfig) shouldBe received
      }
    }
  }

  "The BtaPartialHelper's showLastEstimate method" should {
    "return Html corresponding to the estimated tax for a single tax year" when {
      lazy val baseUrl = mockAppConfig.itvcFrontendEnvironment

      val success: String =
        s"""<div class="form-group">
          |<p id="current-estimate-2018">Your estimated tax amount is &pound;543.21</p>

          |<a id="estimates-link-2018" href="$baseUrl/report-quarterly/income-and-expenses/view/estimated-tax-liability/2018">View details</a>
          |</div>""".stripMargin.replaceAll("\n","")

      "passed a number the Html is returned" in {
        BtaPartialHelper.showLastEstimate(List(lastTaxCalcSuccessWithYear))(applicationMessages, mockAppConfig) shouldBe List(success)
      }
    }

    "return Html corresponding to the estimated tax for both tax years" when {
      lazy val baseUrl = mockAppConfig.itvcFrontendEnvironment

      val success1: String =
        s"""<div class="form-group">
          |<p id="current-estimate-2018">Your estimated tax amount for 2017 to 2018 is &pound;543.21</p>
          |<a id="estimates-link-2018" href="$baseUrl/report-quarterly/income-and-expenses/view/estimated-tax-liability/2018">View details</a>
          |</div>""".stripMargin.replaceAll("\n","")

      val success2: String =
        s"""<div class="form-group">
          |<p id="current-estimate-2019">Your estimated tax amount for 2018 to 2019 is &pound;6,543.21</p>
          |<a id="estimates-link-2019" href="$baseUrl/report-quarterly/income-and-expenses/view/estimated-tax-liability/2019">View details</a>
          |</div>""".stripMargin.replaceAll("\n","")

      "return Html corresponding to the estimated tax for misaligned tax years" in {
        BtaPartialHelper.showLastEstimate(List(lastTaxCalcSuccessWithYear, LastTaxCalculationWithYear(LastTaxCalculation("CALCID","2018-07-06T12:34:56.789Z", 6543.21), 2019)))(applicationMessages, mockAppConfig) shouldBe List(success1,success2)
      }
    }

    "return Html with an estimate for 2018 tax year and no estimate for 2019 tax year" when {

      lazy val baseUrl = mockAppConfig.itvcFrontendEnvironment

      val success: String =
        s"""<div class="form-group">
          |<p id="current-estimate-2018">Your estimated tax amount for 2017 to 2018 is &pound;543.21</p>
          |<a id="estimates-link-2018" href="$baseUrl/report-quarterly/income-and-expenses/view/estimated-tax-liability/2018">View details</a>
          |</div>""".stripMargin.replaceAll("\n","")

      val noCalc: String =
        """<div class="form-group">
          |<p id="current-estimate-2019">Once you've submitted a report using your accounting software, you can view your estimate for 2018 to 2019 tax year here.</p>
          |</div>""".stripMargin.replaceAll("\n","")

      "return Html corresponding to the estimate for 2018 and no estimate for 2019" in {
        BtaPartialHelper.showLastEstimate(List(lastTaxCalcSuccessWithYear, LastTaxCalculationWithYear(NoLastTaxCalculation, 2019)))(applicationMessages, mockAppConfig) shouldBe List(success,noCalc)
      }
    }
  }

}

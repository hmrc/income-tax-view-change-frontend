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

import models.{Overdue, Open}
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
           |<a id="obligations-link" href=/check-your-income-tax-and-expenses/obligations>View deadlines</a>
         """.stripMargin.trim
      )

      val overdueHtml = Html(
        """
           |<p id="report-due">Your next report is due by 30 October 2017</p>
           |<a id="obligations-link" href=/check-your-income-tax-and-expenses/obligations>View deadlines</a>
         """.stripMargin.trim
      )

      "passed 'Open' the Open Html is returned" in {
        BtaPartialHelper.whichStatus(openObligation) shouldBe openHtml
      }
      "passed 'Overdue' the Overdue Html is returned" in {
        BtaPartialHelper.whichStatus(overdueObligation) shouldBe overdueHtml
      }
    }
  }

  "The BtaPartialHelper's showLastEstimate method" should {
    "return Html corresponding to the ObligationStatus" when {

      val successHtml: Html = Html(
        """
          |<p id="current-estimate">Your estimated tax amount is &pound;1,000</p>
          |<a id="estimates-link" href=/check-your-income-tax-and-expenses/estimated-tax-liability>View details</a>
        """.stripMargin.trim
      )

      "passed a number the Html is returned" in {
        BtaPartialHelper.showLastEstimate(Some(1000)) shouldBe successHtml
      }
    }
  }

}

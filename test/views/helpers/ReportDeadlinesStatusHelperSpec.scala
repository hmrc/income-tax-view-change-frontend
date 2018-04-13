/*
 * Copyright 2018 HM Revenue & Customs
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

import models.reportDeadlines.{Open, Overdue, Received}
import play.api.i18n.Messages.Implicits._
import play.twirl.api.Html
import utils.ImplicitDateFormatter._
import utils.TestSupport

class ReportDeadlinesStatusHelperSpec extends TestSupport {

  "The ObligationStatusHelper.statusHtml" should {
    "return Html corresponding to the ObligationStatus" when {

      val openHtml = Html(
        s"""
           |<p class="flush--bottom  alert  soft-half--ends soft--right">
           |  <span class='bold-xsmall'>
           |    Due by 25 December 2017
           |  </span>
           |</p>
         """.stripMargin)

      val overdueHtml = Html(
        s"""
           |<p class="flush--bottom  alert  soft-half--ends soft--right">
           |  <span>
           |    25 December 2017 <strong class="task-overdue">Overdue</strong>
           |  </span>
           |</p>
           """.stripMargin)

      val receivedHtml = Html(
        s"""
           |<p class="flush--bottom  alert  soft-half--ends soft--right" style="color: #005ea5;">
           |  <span class='bold-xsmall'>
           |    Received
           |  </span>
           |</p>
           """.stripMargin)


      "passed 'Open' the Open Html is returned" in {
        ReportDeadlineStatusHelper.statusHtml(Open("2017-12-25")) shouldBe openHtml
      }
      "passed 'Overdue' the Overdue Html is returned" in {
        ReportDeadlineStatusHelper.statusHtml(Overdue("2017-12-25")) shouldBe overdueHtml
      }
      "passed 'Received' the Received Html is returned" in {
        ReportDeadlineStatusHelper.statusHtml(Received) shouldBe receivedHtml
      }
    }
  }
}

/*
 * Copyright 2019 HM Revenue & Customs
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

import models.reportDeadlines.{Open, Overdue}
import play.api.i18n.Messages.Implicits._
import play.twirl.api.Html
import implicits.ImplicitDateFormatter._
import testUtils.TestSupport

class ReportDeadlinesStatusHelperSpec extends TestSupport {

  "The ObligationStatusHelper.statusHtml" should {
    "return Html corresponding to the ObligationStatus" when {

      val openHtml = Html("<span>25 Dec 2017</span>")

      val overdueHtml = Html("""<span>25 Dec 2017 <strong class="task-overdue">Overdue</strong></span>""")

      "passed 'Open' the Open Html is returned" in {
        ReportDeadlineStatusHelper.statusHtml(Open("2017-12-25")) shouldBe openHtml
      }
      "passed 'Overdue' the Overdue Html is returned" in {
        ReportDeadlineStatusHelper.statusHtml(Overdue("2017-12-25")) shouldBe overdueHtml
      }
    }
  }
}

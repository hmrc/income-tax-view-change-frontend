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

package views

import assets.Messages.{BtaPartial => messages}
import assets.TestConstants._
import auth.MtdItUser
import config.FrontendAppConfig
import org.jsoup.Jsoup
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.TestSupport
import assets.TestConstants.Obligations._
import assets.TestConstants.Estimates._
import utils.ImplicitCurrencyFormatter._

class BtaPartialViewSpec extends TestSupport {

  lazy val mockAppConfig = fakeApplication.injector.instanceOf[FrontendAppConfig]

  val model = openObligation
  val calcAmount = BigDecimal(1000)
  val testMtdItUser: MtdItUser = MtdItUser(testMtditid, testNino, Some(testUserDetails))

  lazy val page = views.html.btaPartial(model, List(lastTaxCalcSuccessWithYear))(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
  lazy val noEstimatePage = views.html.btaPartial(model, List())(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)

  "The BtaPartial view" when {

    "An estimate has been received" should {

      lazy val document = Jsoup.parse(contentAsString(page))

      s"have an initial sentence stating that the user has signed up for quarterly reporting" in {
        document.getElementsByClass("panel-indent alert--info").text() shouldBe messages.initial
      }

      s"have the title '${messages.heading}'" in {
        document.getElementById("it-quarterly-reporting-heading").text() shouldBe messages.heading
      }

      s"have a report due sentence" in {
        document.getElementById("report-due").text() shouldBe messages.reportDue(model.due.toLongDate)
      }

      s"have an estimated tax sentence" in {
        document.getElementById("current-estimate-2018").text() shouldBe messages.currentEstimate(lastTaxCalcSuccess.calcAmount.toCurrencyString)
      }
    }

    "No estimate has been received" should {

      lazy val document = Jsoup.parse(contentAsString(noEstimatePage))

      s"have an initial sentence stating that the user has signed up for quarterly reporting" in {
        document.getElementsByClass("panel-indent alert--info").text() shouldBe messages.initial
      }

      s"have the title '${messages.heading}'" in {
        document.getElementById("it-quarterly-reporting-heading").text() shouldBe messages.heading
      }

      s"have a report due sentence" in {
        document.getElementById("report-due").text() shouldBe messages.reportDue(model.due.toLongDate)
      }

      s"not have an estimated tax sentence" in {
        document.body.toString.contains(messages.currentEstimate("")) shouldBe false
      }
    }
  }

}

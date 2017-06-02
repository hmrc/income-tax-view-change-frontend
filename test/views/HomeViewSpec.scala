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

import assets.Messages.{Home => messages}
import config.FrontendAppConfig
import org.jsoup.Jsoup
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.TestSupport
import utils.ImplicitCurrencyFormatter._

class HomeViewSpec extends TestSupport {

  lazy val mockAppConfig = fakeApplication.injector.instanceOf[FrontendAppConfig]

  val testAmount: BigDecimal = 12345.99
  val testAmountOutput: String = "£12,345.99"

  lazy val page = views.html.home(testAmount)(FakeRequest(), applicationMessages, mockAppConfig)
  lazy val document = Jsoup.parse(contentAsString(page))

  "The Home view" should {

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }

    s"have the H1 '${messages.heading}'" in {
      document.getElementsByTag("H1").text() shouldBe messages.preheading + " " + messages.heading
    }

    s"have the pre-heading '${messages.preheading}'" in {
      document.getElementsByClass("pre-heading").text() shouldBe messages.preheading
    }

    s"have a Quarterly Reporting section" which {

      lazy val quarterlySection = document.getElementById("quarterly-reporting")

      s"has a parapgraph with '${messages.QuarterlyReporting.p1}'" in {
        quarterlySection.getElementsByTag("p").text() shouldBe messages.QuarterlyReporting.p1
      }
    }

    s"have an Estimated Tax Liability section" which {

      lazy val estimateSection = document.getElementById("estimated-tax")

      s"has a parapgraph with '${messages.QuarterlyReporting.p1}'" in {
        estimateSection.getElementById("p1").text() shouldBe messages.EstimateTax.p1
      }

      s"has the correct Estimated Tax Amount of '$testAmount'" in {
        estimateSection.getElementById("estimate-amount").text() shouldBe testAmountOutput + " " + messages.EstimateTax.toDate
      }
    }

  }

}

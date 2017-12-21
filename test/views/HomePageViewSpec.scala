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

import assets.Messages.{HomePage => messages}
import assets.TestConstants.BusinessDetails._
import assets.TestConstants.PropertyIncome._
import assets.TestConstants._
import auth.{MtdItUser, MtdItUserWithNino}
import config.FrontendAppConfig
import models.IncomeSourcesModel
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.TestSupport


class HomePageViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testMtdItUser: MtdItUserWithNino[_] = MtdItUserWithNino(testMtditid, testNino, Some(testUserDetails))(FakeRequest())

  "The HomePage view" should {

    lazy val page = views.html.home()(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser, serviceInfo)
    lazy val document = Jsoup.parse(contentAsString(page))
    import messages._

    s"have the title '$title'" in {
      document.title() shouldBe title
    }

    s"have the page heading '$pageHeading'" in {
      document.getElementById("page-heading").text() shouldBe pageHeading
    }

    s"have the subheading with mtditid '${pageSubHeading(testMtditid)}'" in {
      document.getElementById("sub-heading").text() shouldBe pageSubHeading(testMtditid)
    }

    s"have a Bills section" which {

      lazy val billsSection = document.getElementById("bills-section")

      s"has the heading '${BillsSection.heading}'" in {
        billsSection.getElementById("bills-heading").text shouldBe BillsSection.heading
      }

      s"has the paragraph '${BillsSection.paragraph}'" in {
        billsSection.getElementById("bills-text").text shouldBe BillsSection.paragraph
      }

      "has a link to bills" which {

        s"has the text '${BillsSection.link}'" in {
          billsSection.getElementById("bills-link").text shouldBe BillsSection.link
        }

        "links to the bills page" in {
          billsSection.getElementById("bills-link").attr("href") shouldBe controllers.routes.BillsController.viewCrystallisedCalculations().url
        }
      }
    }

    s"have a Estimates section" which {

      lazy val estimatesSection = document.getElementById("estimates-section")

      s"has the heading '${EstimatesSection.heading}'" in {
        estimatesSection.getElementById("estimates-heading").text shouldBe EstimatesSection.heading
      }

      s"has the paragraph '${EstimatesSection.paragraph}'" in {
        estimatesSection.getElementById("estimates-text").text shouldBe EstimatesSection.paragraph
      }

      "has a link to estimates" which {

        s"has the text '${EstimatesSection.link}'" in {
          estimatesSection.getElementById("estimates-link").text shouldBe EstimatesSection.link
        }

        "links to the estimates page" in {
          estimatesSection.getElementById("estimates-link").attr("href") shouldBe controllers.routes.EstimatesController.viewEstimateCalculations().url
        }
      }
    }

    s"have a Report Deadlines section" which {

      lazy val reportDeadlinesSection = document.getElementById("deadlines-section")

      s"has the heading '${ReportDeadlinesSection.heading}'" in {
        reportDeadlinesSection.getElementById("deadlines-heading").text shouldBe ReportDeadlinesSection.heading
      }

      s"has the paragraph '${ReportDeadlinesSection.paragraph}'" in {
        reportDeadlinesSection.getElementById("deadlines-text").text shouldBe ReportDeadlinesSection.paragraph
      }

      "has a link to deadlines" which {

        s"has the text '${ReportDeadlinesSection.link}'" in {
          reportDeadlinesSection.getElementById("deadlines-link").text shouldBe ReportDeadlinesSection.link
        }

        "links to the deadlines page" in {
          reportDeadlinesSection.getElementById("deadlines-link").attr("href") shouldBe controllers.routes.ReportDeadlinesController.getReportDeadlines().url
        }
      }
    }

    "have no sidebar section " in {
      document.getElementById("sidebar") shouldBe null
    }
  }
}

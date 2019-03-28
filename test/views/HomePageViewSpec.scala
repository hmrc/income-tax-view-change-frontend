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

package views

import assets.BaseTestConstants._
import assets.Messages.{Breadcrumbs => breadcrumbMessages, HomePage => messages}
import auth.MtdItUserWithNino
import config.FrontendAppConfig
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testUtils.TestSupport


class HomePageViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testMtdItUser: MtdItUserWithNino[_] = MtdItUserWithNino(testMtditid, testNino, Some(testUserDetails))(FakeRequest())

  "The HomePage view" when {

    "the bills Feature is Disabled" should {

      lazy val page = views.html.home()(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))

      "Disable the Bills feature" in {
        mockAppConfig.features.billsEnabled(false)
        mockAppConfig.features.billsEnabled() shouldBe false
      }

      "Not show the Bills section" in {
        Option(document.getElementById("bills-section")) shouldBe None
      }

    }

    "the Report Deadlines Feature is Disabled" should {

      lazy val page = views.html.home()(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))

      "Disable the Report Deadlines feature" in {
        mockAppConfig.features.reportDeadlinesEnabled(false)
        mockAppConfig.features.reportDeadlinesEnabled() shouldBe false
      }

      "Not show the Report Deadlines section" in {
        Option(document.getElementById("deadlines-section")) shouldBe None
      }

    }

    "the Estimates feature is disabled" should {

      lazy val page = views.html.home()(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))

      "Disable the Estimates feature" in {
        mockAppConfig.features.estimatesEnabled(false)
        mockAppConfig.features.estimatesEnabled() shouldBe false
      }

      "not show the Estimates section" in {
        Option(document.getElementById("estimates-section")) shouldBe None
      }
    }

    "the statements feature is disabled" should {

      lazy val page = views.html.home()(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))

      "Disable the statements feature" in {
        mockAppConfig.features.statementsEnabled(false)
        mockAppConfig.features.statementsEnabled() shouldBe false
      }

      "not show the statements section" in {
        Option(document.getElementById("statements-section")) shouldBe None
      }

    }

    "the account details feature is disabled" should {

      lazy val page = views.html.home()(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))

      "Disable the account details feature" in {
        mockAppConfig.features.accountDetailsEnabled(false)
        mockAppConfig.features.accountDetailsEnabled() shouldBe false
      }

      "not show the account details section" in {
        Option(document.getElementById("accounts-section")) shouldBe None
      }

    }

    "all features are enabled" should {

      lazy val page = views.html.home()(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))
      import messages._

      "Enable all features" in {
        mockAppConfig.features.reportDeadlinesEnabled(true)
        mockAppConfig.features.reportDeadlinesEnabled() shouldBe true
        mockAppConfig.features.billsEnabled(true)
        mockAppConfig.features.billsEnabled() shouldBe true
        mockAppConfig.features.estimatesEnabled(true)
        mockAppConfig.features.estimatesEnabled() shouldBe true
        mockAppConfig.features.statementsEnabled(true)
        mockAppConfig.features.statementsEnabled() shouldBe true
        mockAppConfig.features.accountDetailsEnabled(true)
        mockAppConfig.features.accountDetailsEnabled() shouldBe true
        mockAppConfig.features.calcBreakdownEnabled(true)
        mockAppConfig.features.calcBreakdownEnabled() shouldBe true
        mockAppConfig.features.calcDataApiEnabled(true)
        mockAppConfig.features.calcDataApiEnabled() shouldBe true
      }

      s"have the title '$title'" in {
        document.title() shouldBe title
      }

      "display the language selection switch" in {
        document.getElementById("cymraeg-switch").text shouldBe "Cymraeg"
      }

      "have a breadcrumb trail" in {
        document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
        document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
      }

      s"have the page heading '$heading'" in {
        document.getElementById("page-heading").text() shouldBe heading
      }

      s"have the subheading with the users name '$testUserName'" in {
        document.getElementById("sub-heading").text() shouldBe testUserName
      }

      "have a subheading with the users mtd-it-id" in {
        document.select("header p").eq(1).text() shouldBe s"Unique Tax Reference-${testMtdItUser.mtditid}"
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

          s"has the text '${BillsSection.heading}'" in {
            billsSection.getElementById("bills-link").text shouldBe BillsSection.heading
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

          s"has the text '${EstimatesSection.heading}'" in {
            estimatesSection.getElementById("estimates-link").text shouldBe EstimatesSection.heading
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

          s"has the text '${ReportDeadlinesSection.heading}'" in {
            reportDeadlinesSection.getElementById("deadlines-link").text shouldBe ReportDeadlinesSection.heading
          }

          "links to the deadlines page" in {
            reportDeadlinesSection.getElementById("deadlines-link").attr("href") shouldBe controllers.routes.ReportDeadlinesController.getReportDeadlines().url
          }
        }
      }

      s"have a Statements section" which {

        lazy val statementsSection = document.getElementById("statements-section")

        s"has the heading '${ReportDeadlinesSection.heading}'" in {
          statementsSection.getElementById("statements-heading").text shouldBe StatementSection.heading
        }

        s"has the paragraph '${ReportDeadlinesSection.paragraph}'" in {
          statementsSection.getElementById("statements-text").text shouldBe StatementSection.paragraph
        }

        "has a link to statements" which {

          s"has the text '${ReportDeadlinesSection.heading}'" in {
            statementsSection.getElementById("statements-link").text shouldBe StatementSection.heading
          }

          "links to the statements page" in {
            statementsSection.getElementById("statements-link").attr("href") shouldBe controllers.routes.StatementsController.getStatements().url
          }
        }
      }

      s"have an Account Details section" which {

        mockAppConfig.features.accountDetailsEnabled(true)
        lazy val accountDetailsSection = document.getElementById("accounts-section")

        s"has the heading '${AccountDetailsSection.heading}'" in {
          accountDetailsSection.getElementById("accounts-heading").text shouldBe AccountDetailsSection.heading
        }

        s"has the paragraph '${AccountDetailsSection.paragraph}'" in {
          accountDetailsSection.getElementById("accounts-text").text shouldBe AccountDetailsSection.paragraph
        }

        "has a link to statements" which {

          s"has the text '${AccountDetailsSection.heading}'" in {
            accountDetailsSection.getElementById("accounts-link").text shouldBe AccountDetailsSection.heading
          }

          "links to the statements page" in {
            accountDetailsSection.getElementById("accounts-link").attr("href") shouldBe controllers.routes.AccountDetailsController.getAccountDetails().url
          }
        }

      }

      "have no sidebar section " in {
        document.getElementById("sidebar") shouldBe null
      }
    }
  }
}

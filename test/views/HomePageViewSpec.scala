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

import java.time.LocalDate

import assets.BaseTestConstants._
import assets.Messages.{Breadcrumbs => breadcrumbMessages, HomePage => messages}
import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.{AccountDetails, CalcBreakdown, CalcDataApi, FeatureSwitching, Statements}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport


class HomePageViewSpec extends TestSupport with FeatureSwitching {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testMtdItUser: MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testUserDetails),
    IncomeSourceDetailsModel(Nil, None)
  )(FakeRequest())

  val updateDate: LocalDate = LocalDate.of(2018, 1, 1)
  val nextPaymentDueDate: LocalDate = LocalDate.of(2019, 1, 31)

  class Setup(paymentDueDate:Option[LocalDate] = Some(nextPaymentDueDate), billsEnabled: Boolean = false,
              paymentEnabled: Boolean = false, reportDeadlinesEnabled: Boolean = false,
              obligationsPageEnabled: Boolean = false, estimatesEnabled: Boolean = false) {
    lazy val page: HtmlFormat.Appendable = views.html.home(paymentDueDate, updateDate, billsEnabled,
      paymentEnabled, reportDeadlinesEnabled, obligationsPageEnabled, estimatesEnabled)(FakeRequest(),
      applicationMessages, mockAppConfig, testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))
    def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)
  }

  "home" should {

    "have an incomeTaxPayment card" which {
      "has a heading" in new Setup (billsEnabled = true) {
        getTextOfElementById("income-tax-payment-card-heading") shouldBe Some("Income tax payments")
      }
      "has a body" that {
        "contains the date of the next payment due" in new Setup (billsEnabled = true) {
          getTextOfElementById("income-tax-payment-card-body-date") shouldBe Some("31 January 2019")
        }
        "contain a link to the view bill page if the payment feature switch is turned off" in new Setup (billsEnabled = true) {
          getTextOfElementById("bills-link") shouldBe Some("View bill")
          getElementById("bills-link").map(_.attr("href")) shouldBe Some(controllers.routes.PaymentDueController.viewPaymentsDue().url)
        }
        "contains a link to the view bill and make payment page if the payment feature switch is turned on" in new Setup(billsEnabled = true, paymentEnabled= true){
          getTextOfElementById("bills-link") shouldBe Some("View bill and make payment")
          getElementById("bills-link").map(_.attr("href")) shouldBe Some(controllers.routes.PaymentDueController.viewPaymentsDue().url)
        }
        "contains a link to the previous bills page" in new Setup (billsEnabled = true) {
          getTextOfElementById("previous-bill-link") shouldBe Some("Previous bills")
          getElementById("previous-bill-link").map(_.attr("href")) shouldBe Some(controllers.routes.BillsController.viewCrystallisedCalculations().url)
        }
        "not contain a link to the bill page if the feature flag is off" in new Setup {
          getElementById("bills-link") shouldBe None
          getElementById("previous-bill-link") shouldBe None
        }
        "not contain an income tax payments column when the feature flag is off" in new Setup {
          getElementById("income-tax-payment-card") shouldBe None
        }
        "contains the content when there is no next payment due to display and the feature switch is on" in new Setup(None, billsEnabled = true) {
          getTextOfElementById("income-tax-payment-card-body-date") shouldBe Some("No payments due.")
        }
      }
    }

    "have an updates card" which {
      "has a heading" in new Setup {
        getTextOfElementById("updates-card-heading") shouldBe Some("Updates")
      }
      "has a body" that {
        "contains the date of the next update due" in new Setup {
          getTextOfElementById("updates-card-body-date") shouldBe Some("1 January 2018")
        }
        "contains a link to the updates page" in new Setup (reportDeadlinesEnabled = true) {
          getTextOfElementById("deadlines-link") shouldBe Some("View details for updates")
          getElementById("deadlines-link").map(_.attr("href")) shouldBe Some(controllers.routes.ReportDeadlinesController.getReportDeadlines().url)
        }
        "not contain a link to the updates page if the feature flag is off" in new Setup {
          getElementById("deadlines-link") shouldBe None
        }
        "contains a link to the previous updates page" in new Setup (obligationsPageEnabled = true) {
          getTextOfElementById("previous-deadlines-link") shouldBe Some("Previously submitted updates")
          getElementById("previous-deadlines-link").map(_.attr("href")) shouldBe Some(controllers.routes.PreviousObligationsController.getPreviousObligations().url)
        }
        "not contain a link to the previous updates page if the feature flag is off" in new Setup {
          getElementById("previous-deadlines-link") shouldBe None
        }
        "contains a link to the estimates page" in new Setup (estimatesEnabled = true) {
          getTextOfElementById("estimates-link") shouldBe Some("Estimates")
          getElementById("estimates-link").map(_.attr("href")) shouldBe Some(controllers.routes.EstimatesController.viewEstimateCalculations().url)
        }
        "not contain a link to the estimates page if the feature flag is off" in new Setup {
          getElementById("estimates-link") shouldBe None
        }
      }
    }
  }

  "The HomePage view" when {

    "the bills Feature is Disabled" should {

      lazy val page = views.html.home(Some(nextPaymentDueDate),updateDate, billsEnabled = false,
        paymentEnabled = false, reportDeadlinesEnabled = false, obligationsPageEnabled = false, estimatesEnabled = false)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))

      "Not show the Bills section" in {
        Option(document.getElementById("bills-section")) shouldBe None
      }

    }

    "the Report Deadlines Feature is Disabled" should {

      lazy val page = views.html.home(Some(nextPaymentDueDate),updateDate, billsEnabled = false,
        paymentEnabled = false, reportDeadlinesEnabled = false, obligationsPageEnabled = false, estimatesEnabled = false)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))

      "Not show the Report Deadlines section" in {
        Option(document.getElementById("deadlines-section")) shouldBe None
      }

    }

    "the Estimates feature is disabled" should {

      lazy val page = views.html.home(Some(nextPaymentDueDate),updateDate, billsEnabled = false,
        paymentEnabled = false, reportDeadlinesEnabled = false, obligationsPageEnabled = false, estimatesEnabled = false)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))

      "not show the Estimates section" in {
        Option(document.getElementById("estimates-section")) shouldBe None
      }
    }

    "the statements feature is disabled" should {

      lazy val page = views.html.home(Some(nextPaymentDueDate),updateDate, billsEnabled = false,
        paymentEnabled = false, reportDeadlinesEnabled = false, obligationsPageEnabled = false, estimatesEnabled = false)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))

      "Disable the statements feature" in {
        disable(Statements)
      }

      "not show the statements section" in {
        Option(document.getElementById("statements-section")) shouldBe None
      }

    }

    "the account details feature is disabled" should {

      lazy val page = views.html.home(Some(nextPaymentDueDate),updateDate,billsEnabled = false,
        paymentEnabled = false, reportDeadlinesEnabled = false, obligationsPageEnabled = false, estimatesEnabled = false)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))

      "Disable the account details feature" in {
        disable(AccountDetails)
      }

      "not show the account details section" in {
        Option(document.getElementById("accounts-section")) shouldBe None
      }

    }

    "all features are enabled" should {

      lazy val page = views.html.home(Some(nextPaymentDueDate),updateDate, billsEnabled = true,
        paymentEnabled = true, reportDeadlinesEnabled = true, obligationsPageEnabled = true, estimatesEnabled = true)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser)
      lazy val document = Jsoup.parse(contentAsString(page))
      import messages._

      "Enable all features" in {

        enable(Statements)
        enable(AccountDetails)
        enable(CalcBreakdown)
        enable(CalcDataApi)
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
        document.getElementById("income-tax-heading").text() shouldBe heading
      }

      s"have the subheading with the users name '$testUserName'" in {
        document.getElementById("sub-heading").text() shouldBe testUserName
      }

      "have a subheading with the users mtd-it-id" in {
        document.getElementById("utr-reference-heading").text() shouldBe s"Unique Tax Reference-${testMtdItUser.mtditid}"
      }





//      s"have an Account Details section" which {
//
//        mockAppConfig.features.accountDetailsEnabled(true)
//        lazy val accountDetailsSection = document.getElementById("accounts-section")
//
//        s"has the heading '${AccountDetailsSection.heading}'" in {
//          accountDetailsSection.getElementById("accounts-heading").text shouldBe AccountDetailsSection.heading
//        }
//
//        s"has the paragraph '${AccountDetailsSection.paragraph}'" in {
//          accountDetailsSection.getElementById("accounts-text").text shouldBe AccountDetailsSection.paragraph
//        }
//
//        "has a link to statements" which {
//
//          s"has the text '${AccountDetailsSection.heading}'" in {
//            accountDetailsSection.getElementById("accounts-link").text shouldBe AccountDetailsSection.heading
//          }
//
//          "links to the statements page" in {
//            accountDetailsSection.getElementById("accounts-link").attr("href") shouldBe controllers.routes.AccountDetailsController.getAccountDetails().url
//          }
//        }
//
//      }

      "have no sidebar section " in {
        document.getElementById("sidebar") shouldBe null
      }
    }
  }
}

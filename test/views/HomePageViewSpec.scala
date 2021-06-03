/*
 * Copyright 2021 HM Revenue & Customs
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
import assets.MessagesLookUp.{Core => coreMessages, HomePage => homeMessages}
import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch._
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

import java.time.LocalDate


class HomePageViewSpec extends TestSupport with FeatureSwitching {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testMtdItUser: MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(testMtditid, None, Nil, None),
    Some("testUtr"),
    Some("testCredId"),
    Some("Individual"),
    None
  )(FakeRequest())

  val updateDate: LocalDate = LocalDate.of(2018, 1, 1)
  val updateDateLongDate = "1 January 2018"
  val multipleOverdueUpdates = "3 OVERDUE UPDATES"
  val nextPaymentDueDate: LocalDate = LocalDate.of(2019, 1, 31)
  val paymentDateLongDate = "31 January 2019"
  val multipleOverduePayments = "3 OVERDUE PAYMENTS"
  val overdueMessage = "Warning You have overdue payments. You will be charged interest on these until they are paid in full."

  class Setup(paymentDueDate: Option[LocalDate] = Some(nextPaymentDueDate), overDuePayments: Option[Int] = Some(0),
              overDueUpdates: Option[Int] = Some(0), paymentEnabled: Boolean = true, ITSASubmissionIntegrationEnabled: Boolean = true) {

    lazy val page: HtmlFormat.Appendable = views.html.home(
      nextPaymentDueDate = paymentDueDate,
      nextUpdate = updateDate,
      overDuePayments = overDuePayments,
      overDueUpdates = overDueUpdates,
      paymentEnabled = paymentEnabled,
      ITSASubmissionIntegrationEnabled = ITSASubmissionIntegrationEnabled,
      implicitDateFormatter = mockImplicitDateFormatter,
      paymentHistoryEnabled = paymentEnabled
    )(FakeRequest(), implicitly, mockAppConfig, testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))

    def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)

  }

  "home" should {

    s"have the title '${homeMessages.title}'" in new Setup {
      document.title() shouldBe homeMessages.title
    }
    "display the language selection switch" in new Setup {
      getTextOfElementById("switch-welsh") shouldBe Some(coreMessages.welsh)
    }

    s"have the page heading '${homeMessages.heading}'" in new Setup {
      getTextOfElementById("income-tax-heading") shouldBe Some(homeMessages.heading)
    }

    s"have the subheading with the users name '$testUserName'" in new Setup {
      getTextOfElementById("sub-heading") shouldBe Some(testUserName)
    }

    "have the users mtd-it-id" in new Setup {
      getTextOfElementById("utr-reference-heading") shouldBe Some(homeMessages.taxpayerReference(testMtdItUser.mtditid))
    }

    "have an updates tile" which {
      "has a heading" in new Setup {
        getElementById("updates-tile").map(_.select("h2").text) shouldBe Some(homeMessages.updatesHeading)
      }
      "has the date of the next update due" in new Setup {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(updateDateLongDate)
      }
      "display an overdue tag when a single update is overdue" in new Setup(overDueUpdates = Some(1)) {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some("OVERDUE " + updateDateLongDate)
      }
      "has the correct number of overdue updates when three updates are overdue" in new Setup(overDueUpdates = Some(3)) {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(multipleOverdueUpdates)
      }
      "has a link to view updates" in new Setup {
        val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some(controllers.routes.ReportDeadlinesController.getReportDeadlines().url)
        link.map(_.text) shouldBe Some(homeMessages.updatesLink)
      }
    }

    "have a payments due tile" which {
      "has a heading" in new Setup {
        getElementById("payments-tile").map(_.select("h2").text) shouldBe Some(homeMessages.paymentsHeading)
      }
      "has the date of the next update due" in new Setup {
        getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(paymentDateLongDate)
      }

      "dont display an overdue warning message when no payment is overdue" in new Setup(overDuePayments = Some(0)) {
        getTextOfElementById("overdue-warning") shouldBe None
      }

      "display an overdue warning message when a payment is overdue" in new Setup(overDuePayments = Some(1)) {
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessage)
      }

      "display an overdue tag when a single update is overdue" in new Setup(overDuePayments = Some(1)) {
        getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some("OVERDUE " + paymentDateLongDate)
      }

      "has the correct number of overdue updates when three updates are overdue" in new Setup(overDuePayments = Some(3)) {
        getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(multipleOverduePayments)
      }
      "has a link to view payments" in new Setup {
        val link: Option[Elements] = getElementById("payments-tile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentDueController.viewPaymentsDue().url)
        link.map(_.text) shouldBe Some(homeMessages.paymentLink)
      }
    }

    "have a tax years tile" which {
      "has a heading" in new Setup {
        getElementById("tax-years-tile").map(_.select("h2").text) shouldBe Some(homeMessages.taxYearsHeading)
      }
      "has a link to the tax years page" in new Setup {
        val link: Option[Element] = getElementById("tax-years-tile").map(_.select("a").first)
        link.map(_.attr("href")) shouldBe Some(controllers.routes.TaxYearsController.viewTaxYears().url)
        link.map(_.text) shouldBe Some(homeMessages.taxYearsLink)
      }
      "has a link to the view payments page" in new Setup {
        val link: Option[Element] = getElementById("tax-years-tile").map(_.select("a").get(1))
        link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentHistoryController.viewPaymentHistory().url)
        link.map(_.text) shouldBe Some(homeMessages.viewPaymentslink)
      }

    }
  }

  "have an your income tax returns tile" when {
    "has a heading" in new Setup {
      getElementById("manage-income-tax-tile").map(_.select("h2").text) shouldBe Some(homeMessages.ManageYourIncomeTaxReturnHeading)
    }
    "has a link to the send updates page" in new Setup {
      val link: Option[Elements] = getElementById("submit-your-returns-tile").map(_.select("a"))
      link.map(_.attr("href")) shouldBe Some("http://localhost:9302/income-through-software/return/2022/start")
      link.map(_.text) shouldBe Some(homeMessages.submitYourReturnsLink)
    }
  }
}

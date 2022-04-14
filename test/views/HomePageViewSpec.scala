/*
 * Copyright 2022 HM Revenue & Customs
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

import auth.MtdItUser
import config.FrontendAppConfig
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants._
import testConstants.MessagesLookUp.{Core => coreMessages, HomePage => homeMessages}
import testUtils.TestSupport
import views.html.Home


class HomePageViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val currentTaxYear: Int = {
    val currentDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  def testMtdItUser(saUtr: Option[String] = Some("testUtr")): MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(testMtditid, None, Nil, None),
    testNavHtml,
    saUtr,
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
  val overdueMessage = "! Warning You have overdue payments. You may be charged interest on these until they are paid in full."
  val overdueMessageForDunningLocks = "! Warning You have overdue payments and one or more of your tax decisions are being reviewed. You may be charged interest on these until they are paid in full."

  class Setup(paymentDueDate: Option[LocalDate] = Some(nextPaymentDueDate), overDuePaymentsCount: Option[Int] = Some(0),
              overDueUpdatesCount: Option[Int] = Some(0), utr: Option[String] = Some("1234567890"), paymentHistoryEnabled: Boolean = true, ITSASubmissionIntegrationEnabled: Boolean = true,
              user: MtdItUser[_] = testMtdItUser(), dunningLockExists: Boolean = false, isAgent: Boolean = false) {

    val home: Home = app.injector.instanceOf[Home]
    lazy val page: HtmlFormat.Appendable = home(
      nextPaymentDueDate = paymentDueDate,
      nextUpdate = updateDate,
      overDuePaymentsCount = overDuePaymentsCount,
      overDueUpdatesCount = overDueUpdatesCount,
      Some("1234567890"),
      ITSASubmissionIntegrationEnabled = ITSASubmissionIntegrationEnabled,
      dunningLockExists = dunningLockExists,
      currentTaxYear = currentTaxYear,
      isAgent = isAgent
    )(FakeRequest(), implicitly, user, implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))

    def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)

  }

  "home" should {

    "provided with a btaNavPartial" should {

      "render the btaNavPartial" in new Setup {
        document.getElementById(s"nav-bar-link-testEnHome").text shouldBe "testEnHome"
      }
    }

    s"have the correct link to the government homepage" in new Setup {
      document.getElementsByClass("govuk-header__link").attr("href") shouldBe "https://www.gov.uk"
    }

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

    "have the users UTR" in new Setup {
      getTextOfElementById("utr-reference-heading") shouldBe Some(homeMessages.taxpayerReference("testUtr"))
    }

    "not have the users UTR when it is absent in user profile" in new Setup(user = testMtdItUser(saUtr = None)) {
      getElementById("utr-reference-heading") shouldBe None
    }

    "have an updates tile" which {
      "has a heading" in new Setup {
        getElementById("updates-tile").map(_.select("h2").text) shouldBe Some(homeMessages.updatesHeading)
      }
      "has the date of the next update due" in new Setup {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(updateDateLongDate)
      }
      "display an overdue tag when a single update is overdue" in new Setup(overDueUpdatesCount = Some(1)) {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some("OVERDUE " + updateDateLongDate)
      }
      "has the correct number of overdue updates when three updates are overdue" in new Setup(overDueUpdatesCount = Some(3)) {
        getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(multipleOverdueUpdates)
      }
      "has a link to view updates" in new Setup {
        val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some(controllers.routes.NextUpdatesController.getNextUpdates().url)
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

      "don't display an overdue warning message when no payment is overdue" in new Setup(overDuePaymentsCount = Some(0)) {
        getTextOfElementById("overdue-warning") shouldBe None
      }

      "display an overdue warning message when a payment is overdue" in new Setup(overDuePaymentsCount = Some(1)) {
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessage)
      }

      "as an agent don't display an overdue warning message when no payment is overdue" in new Setup(overDuePaymentsCount = Some(0), isAgent = true) {
        getTextOfElementById("overdue-warning") shouldBe None
      }

      "display an dunning lock overdue warning message when a payment is overdue" in new Setup(overDuePaymentsCount = Some(1), dunningLockExists = true) {
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessageForDunningLocks)
      }

      "display an overdue tag when a single update is overdue" in new Setup(overDuePaymentsCount = Some(1)) {
        getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some("OVERDUE " + paymentDateLongDate)
      }

      "has the correct number of overdue updates when three updates are overdue" in new Setup(overDuePaymentsCount = Some(3)) {
        getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(multipleOverduePayments)
      }
      "has a link to view payments" in new Setup {
        val link: Option[Elements] = getElementById("payments-tile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some(controllers.routes.WhatYouOweController.show().url)
        link.map(_.text) shouldBe Some(homeMessages.paymentLink)
      }
    }

    "have a returns tile" which {
      "has a heading" in new Setup {
        getElementById("returns-tile").map(_.select("h2").text) shouldBe Some(homeMessages.taxYearsHeading)
      }
      "has a link to the view payments page" in new Setup {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").first)
        link.map(_.attr("href")) shouldBe Some(controllers.routes.TaxYearSummaryController.renderTaxYearSummaryPage(currentTaxYear).url)
        link.map(_.text) shouldBe Some(homeMessages.viewPaymentsLinkWithDateRange(currentTaxYear))
      }
      "has a link to the update and submit page" in new Setup {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").get(1))
        link.map(_.attr("href")) shouldBe Some(appConfig.submissionFrontendTaxYearsPage(currentTaxYear))
        link.map(_.text) shouldBe Some(homeMessages.viewUpdateAndSubmitLinkWithDateRange(currentTaxYear))
      }
      "dont have a link to the update and submit page when ITSASubmissionIntegrationEnabled is disabled" in new Setup(ITSASubmissionIntegrationEnabled = false) {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").get(1))
        link.map(_.attr("href")) should not be Some(appConfig.submissionFrontendTaxYearsPage(currentTaxYear))
        link.map(_.text) should not be Some(homeMessages.viewUpdateAndSubmitLinkWithDateRange(currentTaxYear))
      }
      "has a link to the tax years page" in new Setup {
        val link: Option[Element] = getElementById("returns-tile").map(_.select("a").last)
        link.map(_.attr("href")) shouldBe Some(controllers.routes.TaxYearsController.showTaxYears().url)
        link.map(_.text) shouldBe Some(homeMessages.taxYearsLink)
      }
    }

    "have a payment history tile" which {
      "has a heading" in new Setup {
        getElementById("payment-history-tile").map(_.select("h2").text) shouldBe Some(homeMessages.paymentHistoryHeading)
      }
      "has a link to the payment and refund history page" in new Setup {
        val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
        link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentHistoryController.viewPaymentHistory().url)
        link.map(_.text) shouldBe Some(homeMessages.paymentHistoryAndCreditView)
      }
    }
  }
}

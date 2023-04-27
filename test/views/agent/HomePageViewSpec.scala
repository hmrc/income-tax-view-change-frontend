/*
 * Copyright 2023 HM Revenue & Customs
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

package views.agent

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch._
import controllers.routes
import exceptions.MissingFieldException
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants._
import testConstants.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.{TestSupport, ViewSpec}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import views.html.Home

import java.time.{LocalDate, Month}
import scala.util.Try

class HomePageViewSpec extends TestSupport with FeatureSwitching with ViewSpec {

  lazy val backUrl: String = controllers.agent.routes.ConfirmClientUTRController.show.url

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val currentTaxYear: Int = {
    val currentDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, Month.APRIL, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  val testMtdItUserNotMigrated: MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(testMtditid, None, Nil, None),
    btaNavPartial = None,
    Some(testSaUtr),
    Some(testCredId),
    Some(Agent),
    Some(testArn)
  )(FakeRequest())

  val testMtdItUserMigrated: MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(testMtditid, Some("2018"), Nil, None),
    btaNavPartial = None,
    Some(testSaUtr),
    Some(testCredId),
    Some(Agent),
    Some(testArn)
  )(FakeRequest())

  val year2018: Int = 2018
  val year2019: Int = 2019

  val nextUpdateDue: LocalDate = LocalDate.of(year2018, Month.JANUARY, 1)

  val nextPaymentDue: LocalDate = LocalDate.of(year2019, Month.JANUARY, 31)

  class Setup(nextPaymentDueDate: Option[LocalDate] = Some(nextPaymentDue),
              nextUpdate: LocalDate = nextUpdateDue,
              overduePaymentExists: Boolean = true,
              overDuePaymentsCount: Option[Int] = None,
              overDueUpdatesCount: Option[Int] = None,
              utr: Option[String] = None,
              paymentHistoryEnabled: Boolean = true,
              ITSASubmissionIntegrationEnabled: Boolean = true,
              dunningLockExists: Boolean = false,
              currentTaxYear: Int = currentTaxYear,
              isAgent: Boolean = true,
              displayCeaseAnIncome: Boolean = false,
              incomeSourcesEnabled: Boolean = false,
              user: MtdItUser[_] = testMtdItUserNotMigrated
             ) {

    val agentHome: Home = app.injector.instanceOf[Home]

    val view: HtmlFormat.Appendable = agentHome(
      nextPaymentDueDate,
      nextUpdate,
      overDuePaymentsCount,
      overDueUpdatesCount,
      utr,
      ITSASubmissionIntegrationEnabled,
      dunningLockExists,
      currentTaxYear,
      displayCeaseAnIncome = displayCeaseAnIncome,
      isAgent,
      creditAndRefundEnabled = false,
      paymentHistoryEnabled = paymentHistoryEnabled,
      isUserMigrated = user.incomeSources.yearOfMigration.isDefined,
      incomeSourcesEnabled = incomeSourcesEnabled
    )(FakeRequest(), implicitly, user, mockAppConfig)

    lazy val document: Document = Jsoup.parse(contentAsString(view))

    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))

    def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)

    def getHintNth(index: Int = 0): Option[String] = {
      Try(document.getElementsByClass("govuk-hint").get(index).text).toOption
    }
  }


  "home" when {

    "all features are enabled" should {

      s"have the correct link to the government homepage" in new Setup {
        document.getElementsByClass("govuk-header__link").attr("href") shouldBe "https://www.gov.uk"
      }

      s"have the title ${messages("htmlTitle.agent", messages("home.agent.heading"))}" in new Setup() {
        document.title() shouldBe messages("htmlTitle.agent", messages("home.agent.heading"))
      }

      s"have the page heading ${messages("home.agent.heading")}" in new Setup {
        document.select("h1").text() shouldBe messages("home.agent.heading")
      }

      s"have the hint with the users name '$testUserName' and utr '$testSaUtr' " in new Setup {
        getHintNth() shouldBe Some(s"Unique Taxpayer Reference (UTR): $testSaUtr Client’s name $testUserName")
      }

      "have an next payment due tile" which {
        "has a heading" in new Setup {
          getElementById("payments-tile").map(_.select("h2").text) shouldBe Some(messages("home.payments.heading"))
        }
        "has content of the next payment due" which {
          "is overdue" in new Setup(nextPaymentDueDate = Some(nextPaymentDue), overDuePaymentsCount = Some(1)) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"OVERDUE 31 January $year2019")
          }
          "is not overdue" in new Setup(nextPaymentDueDate = Some(nextPaymentDue)) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"31 January $year2019")
          }
          "is a count of overdue payments" in new Setup(nextPaymentDueDate = Some(nextPaymentDue), overDuePaymentsCount = Some(2)) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"2 OVERDUE PAYMENTS")
          }
          "has no next payment" in new Setup(nextPaymentDueDate = None) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"No payments due")
          }
        }
        "has the date of the next payment due" in new Setup {
          val paymentDueDateLongDate: String = s"31 January $year2019"
          getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(paymentDueDateLongDate)
        }
        "has a link to check what your client owes" in new Setup {
          val link: Option[Elements] = getElementById("payments-tile").map(_.select("a"))
          link.map(_.attr("href")) shouldBe Some(controllers.routes.WhatYouOweController.showAgent.url)
          link.map(_.text) shouldBe Some(messages("home.agent.payments.view"))
        }
      }

      "dont display an overdue warning message when no payment is overdue" in new Setup(overduePaymentExists = false) {
        getTextOfElementById("overdue-warning") shouldBe None
      }

      "display an overdue warning message when a payment is overdue and dunning lock does not exist" in new Setup(overDuePaymentsCount = Some(1)) {
        val overdueMessageWithoutDunningLock = "! Your client has overdue payments. They may be charged interest on these until they are paid in full."
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessageWithoutDunningLock)
      }

      "display an overdue warning message when a payment is overdue and dunning lock exists" in new Setup(overDuePaymentsCount = Some(1), dunningLockExists = true) {
        val overdueMessageWithDunningLock = "! Your client has overdue payments and one or more of their tax decisions are being reviewed. They may be charged interest on these until they are paid in full."
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessageWithDunningLock)
      }

      "have an next updates due tile" which {
        "has a heading" in new Setup {
          getElementById("updates-tile").map(_.select("h2").text) shouldBe Some(messages("home.updates.heading"))
        }
        "has content of the next update due" which {
          "is overdue" in new Setup(nextPaymentDueDate = Some(nextUpdateDue), overDueUpdatesCount = Some(1)) {
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"OVERDUE 1 January $year2018")
          }
          "is not overdue" in new Setup(nextPaymentDueDate = Some(nextUpdateDue)) {
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"1 January $year2018")
          }
          "is a count of overdue updates" in new Setup(nextPaymentDueDate = Some(nextUpdateDue), overDueUpdatesCount = Some(2)) {
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"2 OVERDUE UPDATES")
          }
        }
        "has a link to view updates" in new Setup {
          val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/next-updates")
          link.map(_.text) shouldBe Some(messages("home.updates.view"))
        }
      }

      "have a language selection switch" which {
        "displays the current language" in new Setup {
          val langSwitch: Option[Element] = getElementById("lang-switch-en")
          langSwitch.map(_.select("li:nth-child(1)").text) shouldBe Some(messages("language-switcher.english"))
        }

        "changes with JS ENABLED" in new Setup {
          val langSwitchScript: Option[Element] = getElementById("lang-switch-en-js")
          langSwitchScript.toString.contains("/report-quarterly/income-and-expenses/view/switch-to-welsh") shouldBe true
          langSwitchScript.toString.contains(messages("language-switcher.welsh")) shouldBe true
        }

        "changes with JS DISABLED" in new Setup {
          val langSwitchNoScript: Option[Element] = getElementById("lang-switch-en-no-js")
          langSwitchNoScript.map(_.select("a").attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/switch-to-welsh")
          langSwitchNoScript.map(_.select("a span:nth-child(2)").text) shouldBe Some(messages("language-switcher.welsh"))
        }
      }

      "have a returns tile" which {
        "has a heading" in new Setup {
          getElementById("returns-tile").map(_.select("h2").text) shouldBe Some(messages("home.tax-years.heading"))
        }
        "has a link to the view payments page" in new Setup {
          val link: Option[Element] = getElementById("returns-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(currentTaxYear).url)
          link.map(_.text) shouldBe Some(s"${messages("home.agent.returns.viewLink", s"${currentTaxYear - 1}", s"$currentTaxYear")}")
        }
        "has a link to the update and submit page" in new Setup {
          val link: Option[Element] = getElementById("returns-tile").map(_.select("a").get(1))
          link.map(_.attr("href")) shouldBe Some(appConfig.submissionFrontendTaxYearsPage(currentTaxYear))
          link.map(_.text) shouldBe Some(s"${messages("home.agent.your-returns.updatesLink", s"${currentTaxYear - 1}", s"$currentTaxYear")}")
        }
        "dont have a link to the update and submit page when ITSASubmissionIntegrationEnabled is disabled" in new Setup(ITSASubmissionIntegrationEnabled = false) {
          val link: Option[Element] = getElementById("returns-tile").map(_.select("a").get(1))
          link.map(_.attr("href")) should not be Some(appConfig.submissionFrontendTaxYearsPage(currentTaxYear))
          link.map(_.text) should not be Some(s"${messages("home.your-returns.updatesLink", s"${currentTaxYear - 1}", s"$currentTaxYear")}")
        }
        "has a link to the tax years page" in new Setup {
          val link: Option[Element] = getElementById("returns-tile").map(_.select("a").last)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.TaxYearsController.showAgentTaxYears.url)
          link.map(_.text) shouldBe Some(messages("home.tax-years.view"))
        }
      }

      "have a payment history tile" which {
        "has payment and refund history heading when payment history feature switch is enabled" in new Setup() {
          getElementById("payment-history-tile").map(_.select("h2").text) shouldBe Some(messages("home.paymentHistoryRefund.heading"))
        }

        "has payment history heading when payment history feature switch is disabled" in new Setup(paymentHistoryEnabled = false) {
          getElementById("payment-history-tile").map(_.select("h2").text) shouldBe Some(messages("home.paymentHistory.heading"))
        }

        "has a link to the Payment and refund history page when payment history feature switch is enabled" in new Setup {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentHistoryController.showAgent.url)
          link.map(_.text) shouldBe Some(messages("home.paymentHistoryRefund.view"))
        }

        "has a link to the payment history page when payment history feature switch is disabled" in new Setup(paymentHistoryEnabled = false) {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.routes.PaymentHistoryController.showAgent.url)
          link.map(_.text) shouldBe Some(messages("home.paymentHistory.view"))
        }

      }

      s"have a change client link" in new Setup {

        val link: Option[Elements] = getElementById("changeClientLink").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/remove-client-sessions")
        link.map(_.text) shouldBe Some(messages("home.agent.changeClientLink"))
      }
    }

    "the feature switches are disabled" should {
      "not have a your income tax returns tile" in new Setup(ITSASubmissionIntegrationEnabled = false) {
        getElementById("manage-income-tax-tile") shouldBe None
      }
      "not have a link to previous payments in the tax years tile" in new Setup(ITSASubmissionIntegrationEnabled = false) {
        document.getOptionalSelector("tax-years-tile").flatMap(_.getOptionalSelector("a:nth-of-type(2)")) shouldBe None
      }
      "not have an Income Sources tile" in new Setup(incomeSourcesEnabled = false) {
        getElementById("income-sources-tile") shouldBe None
      }
    }

    "the feature switches are enabled" should {
      "not have a link to the saViewLandPTile when isAgent" in new Setup(ITSASubmissionIntegrationEnabled = true) {
        val link: Option[Elements] = getElementById("saViewLandPTile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe None
      }

      "dont display the saViewLandPTile when isAgent is true" in new Setup(ITSASubmissionIntegrationEnabled = true) {
        getElementById("saViewLandPTile") shouldBe None
      }

      "not have a link to the saViewLandPTile when isAgent and UTR is present" in new Setup(ITSASubmissionIntegrationEnabled = true) {
        val link: Option[Elements] = getElementById("saViewLandPTile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe None
      }

      "dont display the saViewLandPTile when isAgent is true and UTR is present" in new Setup(ITSASubmissionIntegrationEnabled = true) {
        getElementById("saViewLandPTile") shouldBe None
      }
      "have an Income Sources tile" which {
        "has a heading" in new Setup(user = testMtdItUserMigrated, incomeSourcesEnabled = true) {
          getElementById("income-sources-tile").map(_.select("h2").first().text()) shouldBe Some(messages("home.incomeSources.heading"))
        }
        "has a link to AddIncomeSourceController.showAgent()" in new Setup(user = testMtdItUserMigrated, incomeSourcesEnabled = true) {
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").text()) shouldBe Some(messages("home.incomeSources.addIncomeSource.view"))
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(2) > a").attr("href")) shouldBe Some(routes.AddIncomeSourceController.showAgent().url)
        }
        "has a link to ManageIncomeSourceController.showAgent()" in new Setup(user = testMtdItUserMigrated, incomeSourcesEnabled = true) {
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(3) > a").text()) shouldBe Some(messages("home.incomeSources.manageIncomeSource.view"))
          getElementById("income-sources-tile").map(_.select("div > p:nth-child(3) > a").attr("href")) shouldBe Some(routes.ManageIncomeSourceController.showAgent().url)
        }
      }
    }

    "the home view with an empty next payment due date and one overDuePaymentsCount" should {
      "throw a MissingFieldException" in {
        val expectedException: MissingFieldException = intercept[MissingFieldException] {
          new Setup(ITSASubmissionIntegrationEnabled = true, nextPaymentDueDate = None, overDuePaymentsCount = Some(1))
        }

        expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Next Payment Due Date"
      }
    }
  }

}

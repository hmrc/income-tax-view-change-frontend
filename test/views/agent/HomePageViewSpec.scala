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

package views.agent

import testConstants.BaseTestConstants._
import testConstants.MessagesLookUp.{Core => coreMessages, HomePage => homeMessages}
import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch._
import exceptions.MissingFieldException
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.{TestSupport, ViewSpec}
import views.html.Home

import java.time.{LocalDate, Month}
import scala.util.Try

class HomePageViewSpec extends TestSupport with FeatureSwitching with ViewSpec {

  lazy val backUrl: String = controllers.agent.routes.ConfirmClientUTRController.show().url

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val currentTaxYear: Int = {
    val currentDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, Month.APRIL, 6))) currentDate.getYear
    else currentDate.getYear + 1
  }

  val testMtdItUser: MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(testMtditid, None, Nil, None),
    btaNavPartial = None,
    Some(testSaUtr),
    Some(testCredId),
    Some("agent"),
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
              ITSASubmissionIntegrationEnabled: Boolean = true,
              dunningLockExists: Boolean = false,
              currentTaxYear: Int = currentTaxYear,
              isAgent: Boolean = true
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
      isAgent
    )(FakeRequest(), implicitly, testMtdItUser, mockAppConfig)

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

      s"have the title '${homeMessages.agentTitle}'" in new Setup() {
        document.title() shouldBe homeMessages.agentTitle
      }

      "display the language selection switch" in new Setup {
        getTextOfElementById("switch-welsh") shouldBe Some(coreMessages.welsh)
      }

      s"have the page heading '${homeMessages.agentHeading}'" in new Setup {
        document.select("h1").text() shouldBe homeMessages.agentHeading
      }

      s"have the hint with the users name '$testUserName' and utr '$testSaUtr' " in new Setup {
        getHintNth() shouldBe Some(s"UTR: $testSaUtr Client’s name $testUserName")
      }

      "have an next payment due tile" which {
        "has a heading" in new Setup {
          getElementById("payments-tile").map(_.select("h2").text) shouldBe Some(homeMessages.paymentsHeading)
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
        "has a link to check what you owe" in new Setup {
          val link: Option[Elements] = getElementById("payments-tile").map(_.select("a"))
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/payments-owed")
          link.map(_.text) shouldBe Some(homeMessages.paymentLink)
        }
      }

      "dont display an overdue warning message when no payment is overdue" in new Setup(overduePaymentExists = false) {
        getTextOfElementById("overdue-warning") shouldBe None
      }

      "display an overdue warning message when a payment is overdue and dunning lock does not exist" in new Setup(overDuePaymentsCount = Some(1)) {
        val overdueMessageWithoutDunningLock = "! You have overdue payments. You may be charged interest on these until they are paid in full."
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessageWithoutDunningLock)
      }

      "display an overdue warning message when a payment is overdue and dunning lock exists" in new Setup(overDuePaymentsCount = Some(1), dunningLockExists = true) {
        val overdueMessageWithDunningLock = "! You have overdue payments and one or more of your tax decisions are being reviewed. You may be charged interest on these until they are paid in full."
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessageWithDunningLock)
      }

      "have an next updates due tile" which {
        "has a heading" in new Setup {
          getElementById("updates-tile").map(_.select("h2").text) shouldBe Some(homeMessages.updatesHeading)
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
          link.map(_.text) shouldBe Some(homeMessages.updatesLink)
        }
      }

      "have a returns tile" which {
        "has a heading" in new Setup {
          getElementById("returns-tile").map(_.select("h2").text) shouldBe Some(homeMessages.taxYearsHeading)
        }
        "has a link to the view payments page" in new Setup {
          val link: Option[Element] = getElementById("returns-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.agent.routes.TaxYearSummaryController.show(currentTaxYear).url)
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
          link.map(_.attr("href")) shouldBe Some(controllers.routes.TaxYearsController.showAgentTaxYears().url)
          link.map(_.text) shouldBe Some(homeMessages.taxYearsLink)
        }
      }

      "have a payment history tile" which {
        "has a heading" in new Setup {
          getElementById("payment-history-tile").map(_.select("h2").text) shouldBe Some(homeMessages.paymentHistoryHeading)
        }
        "has a link to the payment history page" in new Setup {
          val link: Option[Element] = getElementById("payment-history-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.agent.routes.PaymentHistoryController.viewPaymentHistory().url)
          link.map(_.text) shouldBe Some(homeMessages.paymentHistoryAndCreditView)
        }
      }

      s"have a change client link" in new Setup {

        val link: Option[Elements] = getElementById("changeClientLink").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/remove-client-sessions")
        link.map(_.text) shouldBe Some(homeMessages.changeClientLink)
      }
    }

    "the feature switches are disabled" should {
      "not have a your income tax returns tile" in new Setup(ITSASubmissionIntegrationEnabled = false) {
        getElementById("manage-income-tax-tile") shouldBe None
      }
      "not have a link to previous payments in the tax years tile" in new Setup(ITSASubmissionIntegrationEnabled = false) {
        document.getOptionalSelector("tax-years-tile").flatMap(_.getOptionalSelector("a:nth-of-type(2)")) shouldBe None
      }
    }

    "the feature switch enabled" should {
      "not have a link to the saViewLandPTile when isAgent" in new Setup(ITSASubmissionIntegrationEnabled = true) {
        val link: Option[Elements] = getElementById("saViewLandPTile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe None
      }

      "dont display the saViewLandPTile when isAgent is true" in new Setup(ITSASubmissionIntegrationEnabled = true) {
        getElementById("saViewLandPTile") shouldBe None
      }

      "not have a link to the saViewLandPTile when isAgent and UTR is present" in new Setup(ITSASubmissionIntegrationEnabled = true, utr = Some(testSaUtr)) {
        val link: Option[Elements] = getElementById("saViewLandPTile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe None
      }

      "dont display the saViewLandPTile when isAgent is true and UTR is present" in new Setup(ITSASubmissionIntegrationEnabled = true, utr = Some(testSaUtr)) {
        getElementById("saViewLandPTile") shouldBe None
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

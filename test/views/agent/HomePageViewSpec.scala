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

package views.agent

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
import testUtils.{TestSupport, ViewSpec}
import views.html.agent.Home

import java.time.LocalDate


class HomePageViewSpec extends TestSupport with FeatureSwitching with ViewSpec {

  lazy val backUrl: String = controllers.agent.routes.ConfirmClientUTRController.show().url

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testMtdItUser: MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(testMtditid, None, Nil, None),
    Some(testSaUtr),
    Some(testCredId),
    Some("agent"),
    Some(testArn)
  )(FakeRequest())

  val nextUpdateDue: LocalDate = LocalDate.of(2018, 1, 1)

  val nextPaymentDue: LocalDate = LocalDate.of(2019, 1, 31)

  class Setup(nextPaymentOrOverdue: Option[Either[(LocalDate, Boolean), Int]] = Some(Left(nextPaymentDue, false)),
              nextUpdateOrOverdue: Either[(LocalDate, Boolean), Int] = Left(nextUpdateDue, false), paymentEnabled: Boolean = true,
              paymentHistoryEnabled: Boolean = true, ITSASubmissionIntegrationEnabled: Boolean = true,
              overduePaymentExists: Boolean = false) {

    val agentHome: Home = app.injector.instanceOf[Home]

    val view: HtmlFormat.Appendable = agentHome(
      nextPaymentOrOverdue = nextPaymentOrOverdue,
      nextUpdateOrOverdue = nextUpdateOrOverdue,
      overduePaymentExists = overduePaymentExists,
      paymentEnabled = paymentEnabled,
      paymentHistoryEnabled = paymentHistoryEnabled,
      ITSASubmissionIntegrationEnabled = ITSASubmissionIntegrationEnabled,
      implicitDateFormatter = mockImplicitDateFormatter
    )(FakeRequest(), implicitly, mockAppConfig, testMtdItUser)

    lazy val document: Document = Jsoup.parse(contentAsString(view))

    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))

    def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)

    def getHintNth(index: Int = 0): Option[String] =
      Option(document.getElementsByClass("form-hint").get(index).map(_.text))
  }


  "home" when {

    "all features are enabled" should {

      s"have the title '${homeMessages.agentTitle}'" in new Setup() {
        document.title() shouldBe homeMessages.agentTitle
      }

      "display the language selection switch" in new Setup {
        getTextOfElementById("switch-welsh") shouldBe Some(coreMessages.welsh)
      }

      s"have the page heading '${homeMessages.agentHeading}'" in new Setup {
        getTextOfElementById("income-tax-heading") shouldBe Some(homeMessages.agentHeading)
      }

      s"have the hint with the users name '$testUserName' and utr '$testSaUtr' " in new Setup {
        getHintNth() shouldBe Some(s"UTR: $testSaUtr Client’s name $testUserName")
      }

      "have an next payment due tile" which {
        "has a heading" in new Setup {
          getElementById("payments-tile").map(_.select("h2").text) shouldBe Some(homeMessages.paymentsHeading)
        }
        "has content of the next payment due" which {
          "is overdue" in new Setup(nextPaymentOrOverdue = Some(Left(nextPaymentDue -> true))) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"OVERDUE 31 January 2019")
          }
          "is not overdue" in new Setup(nextPaymentOrOverdue = Some(Left(nextPaymentDue -> false))) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"31 January 2019")
          }
          "is a count of overdue payments" in new Setup(nextPaymentOrOverdue = Some(Right(2))) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"2 OVERDUE PAYMENTS")
          }
          "has no next payment" in new Setup(nextPaymentOrOverdue = None) {
            getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"No payments due")
          }
        }
        "has the date of the next payment due" in new Setup {
          val paymentDueDateLongDate: String = "31 January 2019"
          getElementById("payments-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(paymentDueDateLongDate)
        }
        "has a link to view what you owe" in new Setup {
          val link: Option[Elements] = getElementById("payments-tile").map(_.select("a"))
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/payments-owed")
          link.map(_.text) shouldBe Some(homeMessages.paymentLink)
        }
      }

      "dont display an overdue warning message when no payment is overdue" in new Setup(overduePaymentExists = false) {
        getTextOfElementById("overdue-warning") shouldBe None
      }

      "display an overdue warning message when a payment is overdue" in new Setup(overduePaymentExists = true) {
        val overdueMessage = "Warning You have overdue payments. You will be charged interest on these until they are paid in full."
        getTextOfElementById("overdue-warning") shouldBe Some(overdueMessage)
      }

      "have an next updates due tile" which {
        "has a heading" in new Setup {
          getElementById("updates-tile").map(_.select("h2").text) shouldBe Some(homeMessages.updatesHeading)
        }
        "has content of the next update due" which {
          "is overdue" in new Setup(nextUpdateOrOverdue = Left(nextUpdateDue -> true)) {
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"OVERDUE 1 January 2018")
          }
          "is not overdue" in new Setup(nextUpdateOrOverdue = Left(nextUpdateDue -> false)) {
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"1 January 2018")
          }
          "is a count of overdue updates" in new Setup(nextUpdateOrOverdue = Right(2)) {
            getElementById("updates-tile").map(_.select("p:nth-child(2)").text) shouldBe Some(s"2 OVERDUE UPDATES")
          }
        }
        "has a link to view updates" in new Setup {
          val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/next-updates")
          link.map(_.text) shouldBe Some(homeMessages.updatesLink)
        }
      }

      "have a tax years tile" which {
        "has a heading" in new Setup {
          getElementById("tax-years-tile").map(_.select("h2").text) shouldBe Some(homeMessages.taxYearsHeading)
        }
        "has a link to the tax years page" in new Setup {
          val link: Option[Element] = getElementById("tax-years-tile").map(_.select("a").first)
          link.map(_.attr("href")) shouldBe Some(controllers.agent.routes.TaxYearsController.show().url)
          link.map(_.text) shouldBe Some(homeMessages.taxYearsLink)
        }
        "has a link to the view payments page" in new Setup {
          val link: Option[Element] = getElementById("tax-years-tile").map(_.select("a").get(1))
          link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/payments/history")
          link.map(_.text) shouldBe Some(homeMessages.viewPaymentslink)
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

      s"have a change client link" in new Setup {

        val link: Option[Elements] = getElementById("changeClientLink").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some("/report-quarterly/income-and-expenses/view/agents/remove-client-sessions")
        link.map(_.text) shouldBe Some(homeMessages.changeClientLink)
      }
    }

    "the feature switches are disabled" should {
      "not have a your income tax returns tile" in new Setup(paymentEnabled = false, ITSASubmissionIntegrationEnabled = false) {
        getElementById("manage-income-tax-tile") shouldBe None
      }
      "not have a payments tile" in new Setup(paymentEnabled = false, ITSASubmissionIntegrationEnabled = false) {
        getElementById("payments-tile") shouldBe None
      }
      "not have a link to previous payments in the tax years tile" in new Setup(paymentEnabled = false, ITSASubmissionIntegrationEnabled = false) {
        document.getOptionalSelector("tax-years-tile").flatMap(_.getOptionalSelector("a:nth-of-type(2)")) shouldBe None
      }
    }
  }

}

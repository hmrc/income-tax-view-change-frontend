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

import java.time.LocalDate

import assets.BaseTestConstants._
import assets.MessagesLookUp.{Breadcrumbs => breadcrumbMessages, Core => coreMessages, HomePage => homeMessages}
import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch._
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.jsoup.select.Elements
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
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(testMtditid,None,Nil, None),
    Some("testUtr"),
    Some("testCredId"),
    Some("individual")
  )(FakeRequest())

  val updateDate: LocalDate = LocalDate.of(2018, 1, 1)
  val updateDateLongDate = "1 January 2018"
  val nextPaymentDueDate: LocalDate = LocalDate.of(2019, 1, 31)

  class Setup(paymentDueDate: Option[LocalDate] = Some(nextPaymentDueDate), paymentEnabled: Boolean = true, ITSASubmissionIntegrationEnabled: Boolean = true) {

    lazy val page: HtmlFormat.Appendable = views.html.home(
      nextPaymentDueDate = paymentDueDate,
      nextUpdate = updateDate,
      paymentEnabled = paymentEnabled,
      ITSASubmissionIntegrationEnabled = ITSASubmissionIntegrationEnabled,
      implicitDateFormatter = mockImplicitDateFormatter,
      PaymentHistoryEnabled = paymentEnabled
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
      getTextOfElementById("cymraeg-switch") shouldBe Some(coreMessages.welsh)
    }

    "have a breadcrumb trail" in new Setup {
      getTextOfElementById("breadcrumb-bta") shouldBe Some(breadcrumbMessages.bta)
      getTextOfElementById("breadcrumb-it") shouldBe Some(breadcrumbMessages.it)
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
      "has a link to view updates" in new Setup {
        val link: Option[Elements] = getElementById("updates-tile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some(controllers.routes.ReportDeadlinesController.getReportDeadlines().url)
        link.map(_.text) shouldBe Some(homeMessages.updatesLink)
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
        getElementById("your-returns-tile").map(_.select("h3").text) shouldBe Some(homeMessages.yourIncomeTaxReturnHeading)
      }
      "has a link to the send updates page" in new Setup {
        val link: Option[Elements] = getElementById("your-returns-tile").map(_.select("a"))
        link.map(_.attr("href")) shouldBe Some(controllers.routes.HomeController.home().url)
        link.map(_.text) shouldBe Some(homeMessages.sendUpdatesLink)
      }
    }
}

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

import config.FrontendAppConfig
import config.featureswitch.{FeatureSwitching, MFACreditsAndDebits}
import implicits.ImplicitDateFormatter
import models.financialDetails.DocumentDetail
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.testMtditid
import testConstants.FinancialDetailsTestConstants._
import testUtils.{TestSupport, ViewSpec}
import views.html.CreditsSummary


class CreditsSummaryViewSpec extends TestSupport with FeatureSwitching with ImplicitDateFormatter with ViewSpec {

  val creditsSummaryView: CreditsSummary = app.injector.instanceOf[CreditsSummary]
  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testCalendarYear: Int = 2022
  val expectedDate: String = "29 Mar 2018"
  val utr: Option[String] = Some(testMtditid)

  val creditsSummaryHeading: String = messages("credits.heading", s"$testCalendarYear")
  val creditsSummaryTitle: String = messages("titlePattern.serviceName.govUk", creditsSummaryHeading)
  val creditsSummaryTitleAgent: String = messages("agent.titlePattern.serviceName.govUk", creditsSummaryHeading)
  val creditsTableHeadDateText: String = messages("credits.tableHead.date")
  val creditsTableHeadTypeText: String = messages("credits.tableHead.type")
  val creditsTableHeadStatusText: String = messages("credits.tableHead.status")
  val creditsTableHeadAmountText: String = messages("credits.tableHead.amount")
  val creditsTableHeadTypeValue: String = messages("credits.tableHead.type.value")
  val creditsTableStatusFullyAllocatedValue: String = messages("credits.table.status-fully-allocated")
  val creditsTableStatusNotYetAllocatedValue: String = messages("credits.table.status-not-yet-allocated")
  val creditsTableStatusPartiallyAllocatedValue: String = messages("credits.table.status-partially-allocated")
  val creditAndRefundClaimRefundBtn: String = messages("credit-and-refund.claim-refund-btn")
  val getPageHelpLinkTextBtn: String = s"${messages("getpagehelp.linkText")}${messages("pagehelp.opensInNewTabText")}"

  class Setup(creditCharges: List[DocumentDetail] = List.empty,
              isAgent: Boolean = false,
              backUrl: String = "testString") {
    lazy val page: HtmlFormat.Appendable =
      creditsSummaryView(
        calendarYear = testCalendarYear,
        backUrl = backUrl,
        utr = utr,
        btaNavPartial = None,
        enableMfaCreditsAndDebits = true,
        charges = creditCharges,
        isAgent = isAgent
      )(FakeRequest(), implicitly, implicitly)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
    lazy val layoutContent: Element = document.selectHead("#main-content")
  }

  "display the Credits Summary page" when {
    "a user have multiple credits" in new Setup(creditCharges = creditAndRefundDocumentDetailListMultipleChargesMFA) {
      enable(MFACreditsAndDebits)

      document.title() shouldBe creditsSummaryTitle
      layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
      document.select("th:nth-child(1)").first().text() shouldBe creditsTableHeadDateText
      document.select("th:nth-child(2)").text() shouldBe creditsTableHeadTypeText
      document.select("th:nth-child(3)").text() shouldBe creditsTableHeadStatusText
      document.select("th:nth-child(4)").text() shouldBe creditsTableHeadAmountText
      document.selectById("balancing-charge-type-0").select("td:nth-child(1)").first().text() shouldBe expectedDate
      document.selectById("balancing-charge-type-0").select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
      document.selectById("balancing-charge-type-0").select("td:nth-child(3)").first().text() shouldBe creditsTableStatusNotYetAllocatedValue
      document.selectById("balancing-charge-type-0").select("td:nth-child(4)").first().text() shouldBe "£1,400.00"

      document.selectById("balancing-charge-type-1").select("td:nth-child(1)").first().text() shouldBe expectedDate
      document.selectById("balancing-charge-type-1").select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
      document.selectById("balancing-charge-type-1").select("td:nth-child(3)").first().text() shouldBe creditsTableStatusPartiallyAllocatedValue
      document.selectById("balancing-charge-type-1").select("td:nth-child(4)").first().text() shouldBe "£800.00"
      document.getElementsByClass("govuk-link govuk-body").first().text() shouldBe creditAndRefundClaimRefundBtn
      document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
    }

    "a user has a credit and the Status is Fully allocated" in new Setup(creditCharges = creditAndRefundDocumentDetailListFullyAllocatedMFA) {
      enable(MFACreditsAndDebits)

      document.title() shouldBe creditsSummaryTitle
      layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
      document.select("th:nth-child(1)").first().text() shouldBe creditsTableHeadDateText
      document.select("td:nth-child(1)").first().text() shouldBe expectedDate
      document.select("th:nth-child(2)").text() shouldBe creditsTableHeadTypeText
      document.select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
      document.select("th:nth-child(3)").text() shouldBe creditsTableHeadStatusText
      document.select("td:nth-child(3)").first().text() shouldBe creditsTableStatusFullyAllocatedValue
      document.select("th:nth-child(4)").text() shouldBe creditsTableHeadAmountText
      document.select("td:nth-child(4)").first().text() shouldBe "£20.00"
      document.getElementsByClass("govuk-link govuk-body").first().text() shouldBe creditAndRefundClaimRefundBtn
      document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
    }

    "a user has a credit and the Status is Not yet allocated" in new Setup(creditCharges = creditAndRefundDocumentDetailListNotYetAllocatedMFA) {
      enable(MFACreditsAndDebits)

      document.title() shouldBe creditsSummaryTitle
      layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
      document.select("th:nth-child(1)").text() shouldBe creditsTableHeadDateText
      document.select("td:nth-child(1)").first().text() shouldBe expectedDate
      document.select("th:nth-child(2)").text() shouldBe creditsTableHeadTypeText
      document.select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
      document.select("th:nth-child(3)").text() shouldBe creditsTableHeadStatusText
      document.select("td:nth-child(3)").first().text() shouldBe creditsTableStatusNotYetAllocatedValue
      document.select("th:nth-child(4)").text() shouldBe creditsTableHeadAmountText
      document.select("td:nth-child(4)").first().text() shouldBe "£3,000.00"
      document.getElementsByClass("govuk-link govuk-body").first().text() shouldBe creditAndRefundClaimRefundBtn
      document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
    }

    "a user has a credit and the Status is Partially allocated" in new Setup(creditCharges = creditAndRefundDocumentDetailListPartiallyAllocatedMFA) {
      enable(MFACreditsAndDebits)

      document.title() shouldBe creditsSummaryTitle
      layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
      document.select("th:nth-child(1)").first().text() shouldBe creditsTableHeadDateText
      document.select("td:nth-child(1)").first().text() shouldBe expectedDate
      document.select("th:nth-child(2)").text() shouldBe creditsTableHeadTypeText
      document.select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
      document.select("th:nth-child(3)").text() shouldBe creditsTableHeadStatusText
      document.select("td:nth-child(3)").first().text() shouldBe creditsTableStatusPartiallyAllocatedValue
      document.select("th:nth-child(4)").text() shouldBe creditsTableHeadAmountText
      document.select("td:nth-child(4)").first().text() shouldBe "£1,000.00"
      document.getElementsByClass("govuk-link govuk-body").first().text() shouldBe creditAndRefundClaimRefundBtn
      document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
    }

    "a user has no credit" in new Setup(creditCharges = List.empty) {
      enable(MFACreditsAndDebits)

      document.title() shouldBe creditsSummaryTitle
      layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
      document.select("th").isEmpty shouldBe true
      document.select("td").isEmpty shouldBe true
      document.getElementsByClass("govuk-link govuk-body").first().text() shouldBe creditAndRefundClaimRefundBtn
      document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
    }

  }


  "displaying agent credit and refund page" should {
    "display the page" when {
      "correct data is provided" in new Setup(creditCharges = creditAndRefundDocumentDetailListMultipleChargesMFA, isAgent = true) {
        enable(MFACreditsAndDebits)

        document.title() shouldBe creditsSummaryTitleAgent
        layoutContent.selectHead("h1").text shouldBe creditsSummaryHeading
        document.select("th:nth-child(1)").first().text() shouldBe creditsTableHeadDateText
        document.select("th:nth-child(2)").text() shouldBe creditsTableHeadTypeText
        document.select("th:nth-child(3)").text() shouldBe creditsTableHeadStatusText
        document.select("th:nth-child(4)").text() shouldBe creditsTableHeadAmountText
        document.selectById("balancing-charge-type-0").select("td:nth-child(1)").first().text() shouldBe expectedDate
        document.selectById("balancing-charge-type-0").select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
        document.selectById("balancing-charge-type-0").select("td:nth-child(3)").first().text() shouldBe creditsTableStatusNotYetAllocatedValue
        document.selectById("balancing-charge-type-0").select("td:nth-child(4)").first().text() shouldBe "£1,400.00"

        document.selectById("balancing-charge-type-1").select("td:nth-child(1)").first().text() shouldBe expectedDate
        document.selectById("balancing-charge-type-1").select("td:nth-child(2)").first().text() shouldBe creditsTableHeadTypeValue
        document.selectById("balancing-charge-type-1").select("td:nth-child(3)").first().text() shouldBe creditsTableStatusPartiallyAllocatedValue
        document.selectById("balancing-charge-type-1").select("td:nth-child(4)").first().text() shouldBe "£800.00"
        document.getElementsByClass("govuk-link govuk-body").first().text() shouldBe creditAndRefundClaimRefundBtn
        document.getElementsByClass("govuk-link").last().text() shouldBe getPageHelpLinkTextBtn
        enable(MFACreditsAndDebits)
      }
    }
  }
}

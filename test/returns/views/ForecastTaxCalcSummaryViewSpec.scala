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

package views

import common.models.liabilitycalculation.EndOfYearEstimate
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.scalatest.prop.TableDrivenPropertyChecks.*
import play.twirl.api.HtmlFormat
import common.testConstants.BaseTestConstants.{testServiceNavigation, testTaxYear}
import common.testUtils.ViewSpec
import returns.views.html.ForecastTaxCalcSummaryView

class ForecastTaxCalcSummaryViewSpec extends ViewSpec {

  val backUrl = "/report-quarterly/income-and-expenses/view/calculation/2021"
  val forecastTaxCalc: String = messages("forecast_taxCalc.heading")

  val endOfYearEstimateModel: EndOfYearEstimate = EndOfYearEstimate(
    incomeSource = None,
    totalEstimatedIncome = Some(10),
    totalTaxableIncome = Some(20),
    totalAllowancesAndDeductions = Some(BigDecimal(40.00)),
    incomeTaxAmount = Some(30),
    nic2 = Some(40),
    nic4 = Some(50),
    totalNicAmount = Some(60),
    totalTaxDeductedBeforeCodingOut = Some(70),
    saUnderpaymentsCodedOut = Some(80),
    totalStudentLoansRepaymentAmount = Some(90),
    totalAnnuityPaymentsTaxCharged = Some(100),
    totalRoyaltyPaymentsTaxCharged = Some(110),
    totalTaxDeducted = Some(120),
    incomeTaxNicAmount = Some(130),
    cgtAmount = Some(140),
    incomeTaxNicAndCgtAmount = None,
    taxRefundedOrSetOffAmount = None,
    totalTaxAndNicsAmount = None
  )

  val endOfYearEstimateModelWithIncomeTaxNicAndCgtAmount: EndOfYearEstimate = endOfYearEstimateModel.copy(incomeTaxNicAndCgtAmount = Some(150))
  val endOfYearEstimateModelWithRefundOrSetOffs: EndOfYearEstimate = endOfYearEstimateModelWithIncomeTaxNicAndCgtAmount.copy(taxRefundedOrSetOffAmount = Some(50), totalTaxAndNicsAmount = Some(100))

  "The Forecast Tax Calc Summary View" when {

    "provided with a endOfYearEstimateModel without incomeTaxNicAndCgtAmount or taxRefundedOrSetOffAmount " should {
      val forecastTaxCalcView: ForecastTaxCalcSummaryView = app.injector.instanceOf[ForecastTaxCalcSummaryView]
      val view: HtmlFormat.Appendable = forecastTaxCalcView(endOfYearEstimateModel, testTaxYear, backUrl, serviceNavigationPartial = Some(testServiceNavigation))

      "render the BA service navigation" in new Setup(view) {
        document.getElementById(s"bta-service-navigation").text shouldBe "testHome"
      }

      "have the correct title" in new Setup(view) {
        document.title() shouldBe messages("htmlTitle", messages("forecast_taxCalc.heading"))
      }

      "have the correct caption" in new Setup(view) {
        val caption: String = messages("forecast_taxCalc.dates", s"${testTaxYear - 1}", s"$testTaxYear")
        document.getElementsByClass("govuk-caption-xl").first().text() shouldBe caption
      }

      "have the correct heading and caption" in new Setup(view) {
        val pageHeading: String = s"${messages("forecast_taxCalc.heading")}"
        layoutContent.hasPageHeading(pageHeading)
      }

      "have the correct data item names and values" which {
        val expectedDataItems = Table(
          ("p:nth-child", "dataItem", "Amount"),
          (0, messages("forecast_taxCalc.totalEstimatedIncome"), "£10.00"),
          (1, messages("forecast_taxCalc.totalAllowancesAndDeductions"), "£40.00"),
          (2, messages("forecast_taxCalc.totalTaxableIncome"), "£20.00"),
          (3, messages("forecast_taxCalc.totalIncomeTax"), "£30.00"),
          (4, messages("forecast_taxCalc.class4Nic"), "£50.00"),
          (5, messages("forecast_taxCalc.class2Nic"), "£40.00"),
          (6, messages("forecast_taxCalc.totalNics"), "£60.00"),
          (7, messages("forecast_taxCalc.totalDeductedBeforeCodingOut"), "£70.00"),
          (8, messages("forecast_taxCalc.collectedThroughPAYE"), "£80.00"),
          (9, messages("forecast_taxCalc.studentLoanRepayments"), "£90.00"),
          (10, messages("forecast_taxCalc.annuityPayments"), "£100.00"),
          (11, messages("forecast_taxCalc.royaltyPayments"), "£110.00"),
          (12, messages("forecast_taxCalc.totalTaxDeducted"), "£120.00"),
          (13, messages("forecast_taxCalc.incomeTaxAndNicsDue"), "£130.00"),
          (14, messages("forecast_taxCalc.capitalGainsTax"), "£140.00"),
          (15, messages("forecast_taxCalc.incomeTaxNicsCgtDue"), "£130.00")
        )
        forAll(expectedDataItems) { (paraNo: Int, dataItem: String, formattedAmount: String) =>
          s"has the dataItem: '$dataItem' with the correct amount value: $formattedAmount" in new Setup(view) {
            val paragraphs: Elements = layoutContent.getElementsByClass("govuk-body-l")
            val para: Element = paragraphs.get(paraNo)
            println(layoutContent)
            para.text shouldBe s"$dataItem $formattedAmount"
          }
        }
      }
    }

    "provided with a endOfYearEstimateModel with incomeTaxNicAndCgtAmount" should {
      val forecastTaxCalcView: ForecastTaxCalcSummaryView = app.injector.instanceOf[ForecastTaxCalcSummaryView]
      val view: HtmlFormat.Appendable = forecastTaxCalcView(endOfYearEstimateModelWithIncomeTaxNicAndCgtAmount, testTaxYear, backUrl, serviceNavigationPartial = Some(testServiceNavigation))

      "have the correct Forecast Self Assessment tax amount when incomeTaxNicAndCgtAmount != None" which {
        val expectedDataItems = Table(
          ("paraNum", "dataItem", "Amount"),
          (0, messages("forecast_taxCalc.totalEstimatedIncome"), "£10.00"),
          (1, messages("forecast_taxCalc.totalAllowancesAndDeductions"), "£40.00"),
          (2, messages("forecast_taxCalc.totalTaxableIncome"), "£20.00"),
          (3, messages("forecast_taxCalc.totalIncomeTax"), "£30.00"),
          (4, messages("forecast_taxCalc.class4Nic"), "£50.00"),
          (5, messages("forecast_taxCalc.class2Nic"), "£40.00"),
          (6, messages("forecast_taxCalc.totalNics"), "£60.00"),
          (7, messages("forecast_taxCalc.totalDeductedBeforeCodingOut"), "£70.00"),
          (8, messages("forecast_taxCalc.collectedThroughPAYE"), "£80.00"),
          (9, messages("forecast_taxCalc.studentLoanRepayments"), "£90.00"),
          (10, messages("forecast_taxCalc.annuityPayments"), "£100.00"),
          (11, messages("forecast_taxCalc.royaltyPayments"), "£110.00"),
          (12, messages("forecast_taxCalc.totalTaxDeducted"), "£120.00"),
          (13, messages("forecast_taxCalc.incomeTaxAndNicsDue"), "£130.00"),
          (14, messages("forecast_taxCalc.capitalGainsTax"), "£140.00"),
          (15, messages("forecast_taxCalc.incomeTaxNicsCgtDue"), "£150.00")
        )

        forAll(expectedDataItems) { (paraNo: Int, dataItem: String, formattedAmount: String) =>
          s"has the dataItem: '$dataItem' with the correct amount value: $formattedAmount" in new Setup(view) {
            val paragraphs: Elements = layoutContent.getElementsByClass("govuk-body-l")
            val para: Element = paragraphs.get(paraNo)
            para.text shouldBe s"$dataItem $formattedAmount"
          }
        }
      }
    }

    "provided with a endOfYearEstimateModel with taxRefundedOrSetOffAmount" should {
      val forecastTaxCalcView: ForecastTaxCalcSummaryView = app.injector.instanceOf[ForecastTaxCalcSummaryView]
      val view: HtmlFormat.Appendable = forecastTaxCalcView(endOfYearEstimateModelWithRefundOrSetOffs, testTaxYear, backUrl, serviceNavigationPartial = Some(testServiceNavigation))

      "include the Tax already refunded in year line item and have the correct Forecast Self Assessment tax amount when taxRefundedOrSetOffAmount is provided" which {
        val expectedDataItems = Table(
          ("p:nth-child", "dataItem", "Amount"),
          (0, messages("forecast_taxCalc.totalEstimatedIncome"), "£10.00"),
          (1, messages("forecast_taxCalc.totalAllowancesAndDeductions"), "£40.00"),
          (2, messages("forecast_taxCalc.totalTaxableIncome"), "£20.00"),
          (3, messages("forecast_taxCalc.totalIncomeTax"), "£30.00"),
          (4, messages("forecast_taxCalc.class4Nic"), "£50.00"),
          (5, messages("forecast_taxCalc.class2Nic"), "£40.00"),
          (6, messages("forecast_taxCalc.totalNics"), "£60.00"),
          (7, messages("forecast_taxCalc.totalDeductedBeforeCodingOut"), "£70.00"),
          (8, messages("forecast_taxCalc.collectedThroughPAYE"), "£80.00"),
          (9, messages("forecast_taxCalc.studentLoanRepayments"), "£90.00"),
          (10, messages("forecast_taxCalc.annuityPayments"), "£100.00"),
          (11, messages("forecast_taxCalc.royaltyPayments"), "£110.00"),
          (12, messages("forecast_taxCalc.totalTaxDeducted"), "£120.00"),
          (13, messages("forecast_taxCalc.incomeTaxAndNicsDue"), "£130.00"),
          (14, messages("forecast_taxCalc.capitalGainsTax"), "£140.00"),
          (15, messages("forecast_taxCalc.taxRefunded"), "£50.00"),
          (16, messages("forecast_taxCalc.incomeTaxNicsCgtDue"), "£100.00")
        )
        forAll(expectedDataItems) { (paraNo: Int, dataItem: String, formattedAmount: String) =>
          s"has the dataItem: '$dataItem' with the correct amount value: $formattedAmount" in new Setup(view) {
            val paragraphs: Elements = layoutContent.getElementsByClass("govuk-body-l")
            val para: Element = paragraphs.get(paraNo)
            para.text shouldBe s"$dataItem $formattedAmount"
          }
        }
      }
    }
  }
}
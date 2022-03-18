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

import models.liabilitycalculation.EndOfYearEstimate
import org.jsoup.select.Elements
import org.scalatest.prop.TableDrivenPropertyChecks._
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testNavHtml, testTaxYear}
import testUtils.ViewSpec
import views.html.ForecastTaxCalcSummary

class ForecastTaxCalcSummaryViewSpec extends ViewSpec {

  val backUrl = "/report-quarterly/income-and-expenses/view/calculation/2021"
  val forecastTaxCalc = "Forecast tax calculation"

  val endOfYearEstimateModel: EndOfYearEstimate = EndOfYearEstimate(
    incomeSource = None,
    totalEstimatedIncome = Some(10),
    totalTaxableIncome = Some(20),
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
    incomeTaxNicAndCgtAmount = Some(150)
  )

  lazy val forecastTaxCalcView: ForecastTaxCalcSummary = app.injector.instanceOf[ForecastTaxCalcSummary]

  val view: HtmlFormat.Appendable = forecastTaxCalcView(endOfYearEstimateModel, testTaxYear, backUrl, btaNavPartial = testNavHtml)

  "The Forecast Tax Calc Summary View" when {
    "provided with a btaNavPartial" should {
      "render the BA Nav bar" in new Setup(view) {
        document.getElementById(s"nav-bar-link-testEnHome").text shouldBe "testEnHome"
      }
    }

    "provided with a full endOfYearEstimateModel" should {
      "have the correct title" in new Setup(view) {
        document.title() shouldBe messages("titlePattern.serviceName.govUk", messages("forecast_taxCalc.heading"))
        document.title() shouldBe messages("titlePattern.serviceName.govUk", messages("forecast_taxCalc.heading"))
      }

      "have the correct heading" in new Setup(view) {
        val pageSubHeading: String = messages("forecast_taxCalc.dates", s"${testTaxYear - 1}", s"$testTaxYear")
        val pageHeading: String = s"$pageSubHeading ${messages("forecast_taxCalc.heading")}"
        layoutContent.hasPageHeading(pageHeading)
      }

      "have the correct data item names and values" which {
        val expectedDataItems = Table(
          ("p:nth-child", "dataItem", "Amount"),
          (2, messages("forecast_taxCalc.totalEstimatedIncome"), "£10.00"),
          (3, messages("forecast_taxCalc.totalTaxableIncome"), "£20.00"),
          (4, messages("forecast_taxCalc.totalIncomeTax"), "£30.00"),
          (6, messages("forecast_taxCalc.class4Nic"), "£50.00"),
          (7, messages("forecast_taxCalc.class2Nic"), "£40.00"),
          (8, messages("forecast_taxCalc.totalNics"), "£60.00"),
          (10, messages("forecast_taxCalc.totalDeductedBeforeCodingOut"), "£70.00"),
          (11, messages("forecast_taxCalc.collectedThroughPAYE"), "£80.00"),
          (12, messages("forecast_taxCalc.studentLoanRepayments"), "£90.00"),
          (13, messages("forecast_taxCalc.annuityPayments"), "£100.00"),
          (14, messages("forecast_taxCalc.royaltyPayments"), "£110.00"),
          (16, messages("forecast_taxCalc.totalTaxDeducted"), "£120.00"),
          (17, messages("forecast_taxCalc.incomeTaxAndNicsDue"), "£130.00"),
          (18, messages("forecast_taxCalc.capitalGainsTax"), "£140.00"),
          (19, messages("forecast_taxCalc.incomeTaxNicsCgtDue"), "£150.00")
        )

        forAll(expectedDataItems) { (paraNo: Int, dataItem: String, formattedAmount: String) =>

          s"has the dataItem: '$dataItem' with the correct amount value: $formattedAmount" in new Setup(view) {
            val para: Elements = layoutContent.select(s"#main-content p:nth-child($paraNo)")
            para.text shouldBe s"$dataItem $formattedAmount"
          }
        }
      }
    }
  }
}

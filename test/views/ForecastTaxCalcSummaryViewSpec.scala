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

import implicits.ImplicitCurrencyFormatter.CurrencyFormatter
import models.liabilitycalculation.EndOfYearEstimate
import play.api.i18n.{Lang, MessagesApi}
import play.twirl.api.HtmlFormat
import testConstants.BaseTestConstants.{testNavHtml, testTaxYear}
import testUtils.ViewSpec
import views.html.ForecastTaxCalcSummary

class ForecastTaxCalcSummaryViewSpec extends ViewSpec {

  val backUrl = "/report-quarterly/income-and-expenses/view/calculation/2021"
  val forecastTaxCalc = "Forecast tax calculation"
  lazy val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit val lang: Lang = Lang("GB")

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

  def getStringFromMessage(message: String): String =
    messagesLookUp(s"forecast_taxCalc.$message")

  def getMessageAndValue(message: String, value: Option[BigDecimal]): String =
    s"${getStringFromMessage(message)} ${value.get.toCurrencyString}"

  def getMessageAndValueInt(message: String, value: Option[Int]): String =
    s"${getStringFromMessage(message)} ${BigDecimal(value.get).toCurrencyString}"

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
        document.title() shouldBe msgs("titlePattern.serviceName.govUk", msgs("forecast_taxCalc.heading"))
        document.title() shouldBe messagesLookUp("titlePattern.serviceName.govUk", messagesLookUp("forecast_taxCalc.heading"))
      }

      "have the correct heading" in new Setup(view) {
        val pageSubHeading: String = messagesLookUp("forecast_taxCalc.dates", s"${testTaxYear - 1}", s"$testTaxYear")
        val pageHeading: String = s"$pageSubHeading ${messagesLookUp("forecast_taxCalc.heading")}"
        layoutContent.hasPageHeading(pageHeading)
      }

      s"display '${getStringFromMessage("totalEstimatedIncome")}' with its correct value" in new Setup(view) {
        layoutContent.select("#main-content p:nth-child(2)").text shouldBe
        getMessageAndValueInt("totalEstimatedIncome", endOfYearEstimateModel.totalEstimatedIncome)
      }

      s"display '${getStringFromMessage("totalTaxableIncome")}' with its correct value" in new Setup(view) {
        layoutContent.select("#main-content p:nth-child(3)").text shouldBe
          getMessageAndValueInt("totalTaxableIncome", endOfYearEstimateModel.totalTaxableIncome)
      }

      s"display '${getStringFromMessage("totalIncomeTax")}' with its correct value" in new Setup(view) {
        layoutContent.select("#main-content p:nth-child(4)").text shouldBe
          getMessageAndValue("totalIncomeTax", endOfYearEstimateModel.incomeTaxAmount)
      }

      s"display '${getStringFromMessage("class4Nic")}' with its correct value" in new Setup(view) {
        layoutContent.select("#main-content p:nth-child(5)").text shouldBe
          getMessageAndValue("class4Nic", endOfYearEstimateModel.nic4)
      }

      s"display '${getStringFromMessage("class2Nic")}' with its correct value" in new Setup(view) {
        layoutContent.select("#main-content p:nth-child(6)").text shouldBe
          getMessageAndValue("class2Nic", endOfYearEstimateModel.nic2)
      }
    }
  }
}

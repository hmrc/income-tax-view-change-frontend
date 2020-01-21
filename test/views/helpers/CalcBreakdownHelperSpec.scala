/*
 * Copyright 2020 HM Revenue & Customs
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

package views.helpers

import assets.Messages
import enums.{Crystallised, Estimate}
import models.calculation._
import models.financialTransactions.TransactionModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport


class CalcBreakdownHelperSpec extends TestSupport {

  implicit val request: Request[AnyContent] = FakeRequest()
  val year: Int = 2018

  class Setup(calcModel: CalcDisplayModel, transactionModel: Option[TransactionModel] = None, taxYear: Int = year) {
    lazy val page: HtmlFormat.Appendable = views.html.helpers.calcBreakdownHelper(calcModel, transactionModel, taxYear)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))

    def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)
  }
  
  val fullCalculationDataModel = Calculation(
    totalIncomeTaxAndNicsDue = Some(0),
    totalIncomeTaxNicsCharged = Some(50000),
    totalTaxableIncome = Some(100000),
    incomeTaxNicAmount = None,
    timestamp = None,
    crystallised = true,
    nationalRegime = Some("UK"),

    payPensionsProfit = PayPensionsProfit(
      totalSelfEmploymentProfit = Some(50000),
      totalPropertyProfit = Some(40000),
      incomeTaxAmount = Some(90000),
      taxableIncome = Some(75000),
      List(TaxBand(
        name = "first",
        rate = 10,
        income = 1000,
        taxAmount = 100.00

      ),
        TaxBand(
          name = "second",
          rate = 20,
          income = 2000.00,
          taxAmount = 400.00

        ),
        TaxBand(
          name = "third",
          rate = 30,
          income = 3000.00,
          taxAmount = 900.00

        ))
    ),

    savingsAndGains = SavingsAndGains(
      Some(30000),
      Some(25000),
      List(TaxBand(
        name = "first",
        rate = 0,
        income = 1000,
        taxAmount = 0.0

      ),
        TaxBand(
          name = "second",
          rate = 10,
          income = 2000,
          taxAmount = 200

        ),
        TaxBand(
          name = "third",
          rate = 20,
          income = 3000,
          taxAmount = 600
        )
    )),

    dividends = Dividends(
      incomeTaxAmount = Some(20000),
      taxableIncome = Some(20000),
      List(TaxBand(
        name = "first",
        rate = 0,
        income = 1000,
        taxAmount = 0
      ),
        TaxBand(
          name = "second",
          rate = 10,
          income = 2000,
          taxAmount = 200
        ),
        TaxBand(
          name = "third",
          rate = 20,
          income = 3000,
          taxAmount = 600
        ))
      ),

    allowancesAndDeductions = AllowancesAndDeductions(
      personalAllowance = Some(5000),
    giftOfInvestmentsAndPropertyToCharity = Some(1234),
  totalAllowancesAndDeductions = None,
  totalReliefs = Some(4000)
    ),
    nic = Nic(
      class2 = Some(5000),
      class4 = Some(10000),
      totalNic = Some(15000)
    ),
    giftAid = GiftAid(
      payments = Some(5000),
      rate = Some(10),
      giftAidTax = Some(500)
    ))

  val fullDataEstimate: CalcDisplayModel = CalcDisplayModel(
    "",
    10000.00,
    fullCalculationDataModel,
    Estimate
  )

  val fullDataBill: CalcDisplayModel = fullDataEstimate.copy(calcStatus = Crystallised)

  val emptyCalculation: Calculation = Calculation(crystallised = false)

  val emptyDataEstimate: CalcDisplayModel = CalcDisplayModel(
    "",
    10000.00,
    emptyCalculation,
    Estimate
  )

  val emptyDataBill: CalcDisplayModel = emptyDataEstimate.copy(calcStatus = Crystallised)

  val emptyTransactionModel: TransactionModel = TransactionModel()

  val tableTests = List(
    ("business-profit", Messages.CalculationBreakdown.incomeBusinessProfit, "£50,000"),
    ("property-income", Messages.CalculationBreakdown.incomeProperty, "£40,000"),
    ("dividend-income", Messages.CalculationBreakdown.incomeDividends, "£20,000"),
    ("savings-income", Messages.CalculationBreakdown.incomeSavings, "£25,000"),
    ("personal-allowance", Messages.CalculationBreakdown.incomePersonalAllowance, "£5,000"),
    ("dividends-allowance", Messages.CalculationBreakdown.incomeDividendsAllowance, "£1,000"),
    ("savings-allowance", Messages.CalculationBreakdown.incomeSavingsAllowance, "£1,000"),
    ("gift-investment-property", Messages.CalculationBreakdown.incomeGiftInvestmentPropertyToCharity, "£1,234"),
    ("income-tax-band-first", Messages.CalculationBreakdown.calculationIncomeTax("£1,000", "10"), "£100"),
    ("income-tax-band-second", Messages.CalculationBreakdown.calculationIncomeTax("£2,000", "20"), "£400"),
    ("income-tax-band-third", Messages.CalculationBreakdown.calculationIncomeTax("£3,000", "30"), "£900"),
    ("dividend-tax-band-first", Messages.CalculationBreakdown.calculationDividend("£1,000", "0"), "£0"),
    ("dividend-tax-band-second", Messages.CalculationBreakdown.calculationDividend("£2,000", "10"), "£200"),
    ("dividend-tax-band-third", Messages.CalculationBreakdown.calculationDividend("£3,000", "20"), "£600"),
    ("savings-tax-band-first", Messages.CalculationBreakdown.calculationSavings("£1,000", "0"), "£0"),
    ("savings-tax-band-second", Messages.CalculationBreakdown.calculationSavings("£2,000", "10"), "£200"),
    ("savings-tax-band-third", Messages.CalculationBreakdown.calculationSavings("£3,000", "20"), "£600"),
    ("nic-class2", Messages.CalculationBreakdown.calculationClassTwoNI, "£5,000"),
    ("nic-class4", Messages.CalculationBreakdown.calculationClassFourNI, "£10,000"),
    ("tax-reliefs", Messages.CalculationBreakdown.calculationTaxRelief, "£4,000")
  )

  "calcBreakdownHelper" should {

    for ((test, label, data) <- tableTests) {
      s"display the $test section with data when the $test is present in the data" in new Setup(fullDataEstimate) {
        getElementById(s"$test-section").isDefined shouldBe true
        getTextOfElementById(s"$test-label") shouldBe Some(label)
        getTextOfElementById(s"$test-data") shouldBe Some(data)
      }
      s"not display the $test section when the $test value is zero" in new Setup(emptyDataEstimate) {
        getElementById(s"$test-section") shouldBe None
        getElementById(s"$test-label") shouldBe None
        getElementById(s"$test-data") shouldBe None
      }
    }

    "display the how estimate calculated sub heading when the calculation breakdown is for an estimate" in new Setup(fullDataEstimate) {
      getTextOfElementById("how-estimate-calculated") shouldBe Some(Messages.CalculationBreakdown.estimateSubHeading("£10,000"))
      getElementById("how-bill-calculated") shouldBe None
    }

    "display the how bill calculated sub heading when the calculation breakdown is for a bill" in new Setup(fullDataBill) {
      getTextOfElementById("how-bill-calculated") shouldBe Some(Messages.CalculationBreakdown.billSubHeading)
      getElementById("how-estimate-calculated") shouldBe None
    }

    "display the national regime section when it is present in the data" in new Setup(fullDataEstimate) {
      getTextOfElementById("national-regime") shouldBe Some(Messages.CalculationBreakdown.nationalRegime("UK"))
    }

    "not display the national regime section when it is not present in the data" in new Setup(emptyDataEstimate) {
      getElementById("national-regime") shouldBe None
    }

    "display the heading for the income table" in new Setup(fullDataEstimate) {
      getTextOfElementById("income-heading") shouldBe Some(Messages.CalculationBreakdown.incomeHeading)
    }

    "display the sub heading for the income table" in new Setup(fullDataEstimate) {
      getTextOfElementById("income-subheading") shouldBe Some(Messages.CalculationBreakdown.incomeSubheading)
    }

    "display the heading for the calculation table" in new Setup(fullDataEstimate) {
      getTextOfElementById("calculation-heading") shouldBe Some(Messages.CalculationBreakdown.calculationHeading)
    }

    "display the sub heading for the calculation table" in new Setup(fullDataEstimate) {
      getTextOfElementById("calculation-subheading") shouldBe Some(Messages.CalculationBreakdown.calculationSubheading("£100,000"))
    }

    "display the estimated total taxable income when the calculation breakdown is for a estimate" in new Setup(fullDataEstimate) {
      getTextOfElementById("estimate-total-taxable-income-label") shouldBe Some(Messages.CalculationBreakdown.incomeEstimatedTotalTaxableIncome)
      getTextOfElementById("estimate-total-taxable-income-data") shouldBe Some("£100,000")
      getElementById("total-taxable-income-label") shouldBe None
      getElementById("total-taxable-income-data") shouldBe None
    }

    "display the total taxable income when the calculation breakdown is for a bill" in new Setup(fullDataBill) {
      getTextOfElementById("total-taxable-income-label") shouldBe Some(Messages.CalculationBreakdown.incomeTotalTaxableIncome)
      getTextOfElementById("total-taxable-income-data") shouldBe Some("£100,000")
      getElementById("estimate-total-taxable-income-label") shouldBe None
      getElementById("estimate-total-taxable-income-data") shouldBe None
    }

    "display the payments to date section" when {
      "the breakdown is for a bill and payments have been made" in new Setup(fullDataBill, Some(emptyTransactionModel.copy(clearedAmount = Some(10000.00)))) {
        getElementById("payments-to-date-section").isDefined shouldBe true
        getTextOfElementById("payments-to-date-label") shouldBe Some(Messages.CalculationBreakdown.calculationPaymentsToDate)
        getTextOfElementById("payments-to-date-data") shouldBe Some("£10,000")
      }

      "the breakdown is for a bill but no payments have been made" in new Setup(fullDataBill, Some(emptyTransactionModel)) {
        getElementById("payments-to-date-section").isDefined shouldBe true
        getTextOfElementById("payments-to-date-label") shouldBe Some(Messages.CalculationBreakdown.calculationPaymentsToDate)
        getTextOfElementById("payments-to-date-data") shouldBe Some("£0")
      }
    }

    "not display the payments to date section" when {
      "the breakdown is for an estimate" in new Setup(fullDataEstimate) {
        getElementById("payments-to-date-section") shouldBe None
        getTextOfElementById("payments-to-date-label") shouldBe None
        getTextOfElementById("payments-to-date-data") shouldBe None
      }
    }

    "display the your total section" when {
      "the breakdown is an estimate" in new Setup(fullDataEstimate) {
        getElementById("your-total-section").isDefined shouldBe true
        getTextOfElementById("your-total-estimate-label") shouldBe Some(Messages.CalculationBreakdown.calculationYourTotalEstimate)
        getTextOfElementById("your-total-estimate-data") shouldBe Some("£10,000")
        getElementById("total-outstanding-label") shouldBe None
        getElementById("total-outstanding-data") shouldBe None
        getElementById("total-outstanding-date") shouldBe None
      }

      "the breakdown is a bill" in new Setup(fullDataBill, Some(emptyTransactionModel.copy(outstandingAmount = Some(100000.00)))) {
        getElementById("your-total-section").isDefined shouldBe true
        getTextOfElementById("total-outstanding-label") shouldBe Some(Messages.CalculationBreakdown.calculationTotalOutstanding)
        getTextOfElementById("total-outstanding-data") shouldBe Some("£100,000")
        getTextOfElementById("total-outstanding-date") shouldBe Some("due 31 January 2019")
        getElementById("your-total-estimate-label") shouldBe None
        getElementById("your-total-estimate-data") shouldBe None
      }

      "display the gift aid extender message when it is present in the data" in new Setup(fullDataEstimate) {

        getTextOfElementById("gift-aid-extender") shouldBe
          Some(Messages.CalculationBreakdown.giftAidExtender("500"))
      }

      "not display the gift aid extender message when it is not present in the data" in new Setup(emptyDataEstimate) {
        getElementById("gift-aid-extender") shouldBe None
      }

    }

  }

}

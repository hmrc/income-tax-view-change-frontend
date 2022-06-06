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

import exceptions.MissingFieldException
import models.liabilitycalculation.taxcalculation.TaxBands
import models.liabilitycalculation.viewmodels.TaxDueSummaryViewModel
import models.liabilitycalculation.{Message, Messages}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor3
import play.twirl.api.Html
import testConstants.NewCalcBreakdownUnitTestConstants._
import testUtils.ViewSpec
import views.html.TaxCalcBreakdown

class TaxCalcBreakdownViewSpec extends TaxCalcBreakdownViewBehaviour {

  override val backUrl = "testUrl"

  override def taxCalcBreakdown(taxDueSummaryViewModel: TaxDueSummaryViewModel, taxYear: Int, backUrl: String, class4UpliftEnabled: Boolean = false): Html =
    app.injector.instanceOf[TaxCalcBreakdown].apply(taxDueSummaryViewModel, taxYear, backUrl, class4UpliftEnabled = class4UpliftEnabled)

  override val expectedPageTitle: String = messages("titlePattern.serviceName.govUk", messages("taxCal_breakdown.heading"))

  override val pageContentSelector = "#main-content"

  override val messageContentSelector = ".govuk-inset-text"

  override val headingSelector = ".govuk-caption-xl"

}

abstract class TaxCalcBreakdownViewBehaviour extends ViewSpec {

  val taxYear2017: Int = 2017
  val sectionHeadingPPP: String = messages("taxCal_breakdown.pay_pensions_profit")
  val sectionHeadingLumpSums: String = messages("taxCal_breakdown.lumpSums")
  val sectionHeadingGainsOnLifePolicies: String = messages("taxCal_breakdown.gains_life_policies")
  val sectionHeadingAdditionalChar: String = messages("taxCal_breakdown.additional_charges")
  val sectionHeadingNationalInsuranceContributionsChar: String = messages("taxCal_breakdown.national_insurance_contributions")
  val sectionHeadingTaxReductions: String = messages("taxCal_breakdown.table.tax_reductions")
  val reductionTableHeader: String = messages("taxCal_breakdown.table.head.reduction")
  val amountTableHeader: String = messages("taxCal_breakdown.table.amount")
  val rateBandTableHeader: String = messages("taxCal_breakdown.table.head.rate_band")
  val nationalInsuranceTypeTableHeader: String = messages("taxCal_breakdown.table.head.national_insurance_type")
  val voluntaryNic2: String = messages("taxCal_breakdown.table.nic2.true")
  val Nic4New_ZRT: String = messages("taxCal_breakdown.table.nic4", "£2,000.00", "1")
  val Nic4New_BRT: String = messages("taxCal_breakdown.table.nic4", "£3,000.00", "2")
  val Nic4New_HRT: String = messages("taxCal_breakdown.table.nic4", "£5,000.00", "3")

  def backUrl: String

  def taxCalcBreakdown(taxDueSummaryViewModel: TaxDueSummaryViewModel, taxYear: Int, backUrl: String, class4UpliftEnabled: Boolean = false): Html

  def expectedPageTitle: String

  def pageContentSelector: String

  def messageContentSelector: String

  def headingSelector: String

  type ROW_INDEX = Int
  type COLUMN_TEXT = String
  type BREAKDOWN_TABLE = TableFor3[ROW_INDEX, COLUMN_TEXT, COLUMN_TEXT]

  def shouldHaveACorrectTableContent(view: Html)(tableNumber: Int, expectedCaption: String, expectedTableRows: BREAKDOWN_TABLE): Unit = {
    lazy val tableIndex = tableNumber - 1
    lazy val expectedRowsCount = expectedTableRows.length

    s"has $expectedRowsCount rows in total" in new Setup(view) {
      pageContent(pageContentSelector) hasTableWithCorrectSize(tableNumber, expectedRowsCount)
    }

    s"has the heading $expectedCaption" in new Setup(view) {
      pageContent(pageContentSelector).select("caption").get(tableIndex).text() shouldBe expectedCaption
    }

    forAll(expectedTableRows) { (rowIndex: Int, firstColumnText: String, secondColumnText: String) =>
      if (rowIndex == 0) {

        s"has a table headers: [$firstColumnText, $secondColumnText]" in new Setup(view) {
          val row: Element = pageContent(pageContentSelector).table(tableNumber).select("tr").get(0)
          row.select("th").first().text() shouldBe firstColumnText
          row.select("th").last().text() shouldBe secondColumnText
        }

      } else {

        s"has the row $rowIndex for $firstColumnText line with the correct amount value" in new Setup(view) {
          val row: Element = pageContent(pageContentSelector).table(tableNumber).select("tr").get(rowIndex)
          row.select("td").first().text() shouldBe firstColumnText
          row.select("td").last().text() shouldBe secondColumnText
        }

      }
    }
  }

  "The taxCalc breakdown view with Scotland tax regime" when {

    val taxBands = Seq(
      TaxBands(
        name = "SRT",
        rate = 10.0,
        bandLimit = 12500,
        apportionedBandLimit = 12500,
        income = 20000,
        taxAmount = 2000.00
      ),
      TaxBands(
        name = "BRT",
        rate = 20.0,
        bandLimit = 12500,
        apportionedBandLimit = 12500,
        income = 20000,
        taxAmount = 4000.00
      ),
      TaxBands(
        name = "IRT",
        rate = 25.0,
        bandLimit = 12500,
        apportionedBandLimit = 12500,
        income = 20000,
        taxAmount = 45000.00
      ),
      TaxBands(
        name = "HRT",
        rate = 40.0,
        bandLimit = 12500,
        apportionedBandLimit = 12500,
        income = 100000,
        taxAmount = 40000.00
      ),
      TaxBands(
        name = "ART_scottish",
        rate = 45.0,
        bandLimit = 12500,
        apportionedBandLimit = 12500,
        income = 500000,
        taxAmount = 22500.00
      )
    )

    "provided with a calculation that is with Pay, pensions and profit table and with all three tax bands including " +
      "top rate displayed " +
      "for the 2017 tax year" should {

      val taxDueSummaryViewModel = TaxDueSummaryViewModel(
        taxRegime = "Scotland",
        payPensionsProfitBands = Some(taxBands)
      )
      lazy val view = taxCalcBreakdown(taxDueSummaryViewModel, taxYear2017, backUrl)

      "have a Pay, pensions and profit table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 1,
          expectedCaption = sectionHeadingPPP,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, messages("taxCal_breakdown.table.head.rates.scotland"), amountTableHeader),
            (1, messages("taxCal_breakdown.table.SRT", "£20,000.00", "10.0"), "£2,000.00"),
            (2, messages("taxCal_breakdown.table.BRT", "£20,000.00", "20.0"), "£4,000.00"),
            (3, messages("taxCal_breakdown.table.IRT", "£20,000.00", "25.0"), "£45,000.00"),
            (4, messages("taxCal_breakdown.table.HRT", "£100,000.00", "40.0"), "£40,000.00"),
            (5, messages("taxCal_breakdown.table.ART_scottish", "£500,000.00", "45.0"), "£22,500.00")
          )
        )

      }
    }

    "provided with a calculation that is with Lump sums table and with all three tax bands including " +
      "top rate displayed " +
      "for the 2017 tax year" should {

      val taxDueSummaryViewModel = TaxDueSummaryViewModel(
        taxRegime = "Scotland",
        lumpSumsBands = Some(taxBands)
      )
      lazy val view = taxCalcBreakdown(taxDueSummaryViewModel, taxYear2017, backUrl)

      "have a Lump sums table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 1,
          expectedCaption = sectionHeadingLumpSums,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, messages("taxCal_breakdown.table.head.rates.scotland"), amountTableHeader),
            (1, messages("taxCal_breakdown.table.SRT", "£20,000.00", "10.0"), "£2,000.00"),
            (2, messages("taxCal_breakdown.table.BRT", "£20,000.00", "20.0"), "£4,000.00"),
            (3, messages("taxCal_breakdown.table.IRT", "£20,000.00", "25.0"), "£45,000.00"),
            (4, messages("taxCal_breakdown.table.HRT", "£100,000.00", "40.0"), "£40,000.00"),
            (5, messages("taxCal_breakdown.table.ART_scottish", "£500,000.00", "45.0"), "£22,500.00")
          )
        )

      }
    }


    "provided with a calculation that is with Gains on life policies table and with all three tax bands including " +
      "top rate displayed " +
      "for the 2017 tax year" should {

      val taxDueSummaryViewModel = TaxDueSummaryViewModel(
        taxRegime = "Scotland",
        gainsOnLifePoliciesBands = Some(taxBands)
      )
      lazy val view = taxCalcBreakdown(taxDueSummaryViewModel, taxYear2017, backUrl)

      "have a Gains on life policies table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 1,
          expectedCaption = sectionHeadingGainsOnLifePolicies,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, rateBandTableHeader, amountTableHeader),
            (1, messages("taxCal_breakdown.table.SRT", "£20,000.00", "10.0"), "£2,000.00"),
            (2, messages("taxCal_breakdown.table.BRT", "£20,000.00", "20.0"), "£4,000.00"),
            (3, messages("taxCal_breakdown.table.IRT", "£20,000.00", "25.0"), "£45,000.00"),
            (4, messages("taxCal_breakdown.table.HRT", "£100,000.00", "40.0"), "£40,000.00"),
            (5, messages("taxCal_breakdown.table.ART_scottish", "£500,000.00", "45.0"), "£22,500.00")
          )
        )

      }
    }
  }

  "The taxCalc breakdown view with UK tax regime" when {

    "provided with a calculation that is without Pay, pensions and profit, without Savings, without Dividends, " +
      "without Additional charges and without Additional deductions sections but has total tax due amount display " +
      "for the 2017 tax year" should {
      val taxDueSummaryViewModel = TaxDueSummaryViewModel(
        taxRegime = "Uk",
        payPensionsProfitBands = None
      )
      lazy val view = taxCalcBreakdown(taxDueSummaryViewModel, taxYear2017, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe expectedPageTitle
      }

      "have the correct heading" in new Setup(view) {
        pageContent(pageContentSelector) hasPageHeading s"${messages("taxCal_breakdown.dates", s"${taxYear2017 - 1}", s"$taxYear2017")} ${messages("taxCal_breakdown.heading")}"
        pageContent(pageContentSelector).h1.select(headingSelector).text() shouldBe messages("taxCal_breakdown.dates", s"${taxYear2017 - 1}", s"$taxYear2017")
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = pageContent(pageContentSelector).getElementById("explanation")
        guidance.text() shouldBe s"${messages("taxCal_breakdown.explanation")} £0.00"
      }
    }


    "provided with a calculation with all the sections displayed which includes Pay, pensions and profit, Savings, Dividends," +
      " lumpsums and gainsOnLifePolicies, Additional charges and Additional deductions as well as the total tax due amount for the 2018 tax year" should {
      val taxYear = 2018

      lazy val view = taxCalcBreakdown(taxDueSummaryViewModelStandard, taxYear, backUrl)
      lazy val viewWithClass4UpliftEnabled = taxCalcBreakdown(taxDueSummaryViewModelStandard, taxYear, backUrl, class4UpliftEnabled = true)
      lazy val viewNone = taxCalcBreakdown(TaxDueSummaryViewModel(), taxYear, backUrl)
      lazy val viewNic2 = taxCalcBreakdown(taxDueSummaryViewModelNic2, taxYear, backUrl)
      lazy val viewVoluntaryNic2 = taxCalcBreakdown(taxDueSummaryViewModelVoluntaryNic2, taxYear, backUrl)
      lazy val viewMarriageAllowanceTransfer = taxCalcBreakdown(taxDueSummaryViewModelMarriageAllowance, taxYear, backUrl)
      lazy val viewTopSlicingRelief = taxCalcBreakdown(taxDueSummaryViewModelTopSlicingRelief, taxYear, backUrl)
      lazy val viewAdChGiftAid = taxCalcBreakdown(taxDueSummaryViewModelGiftAid, taxYear, backUrl)
      lazy val viewAdChPensionLumpSum = taxCalcBreakdown(taxDueSummaryViewModelPensionLumpSum, taxYear, backUrl)
      lazy val viewAdChPensionSavings = taxCalcBreakdown(taxDueSummaryViewModelPensionSavings, taxYear, backUrl)
      lazy val zeroIncome = taxCalcBreakdown(taxDueSummaryViewModelZeroIncome, taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe expectedPageTitle
      }

      "have a fallback backlink" in new Setup(view) {
        document hasFallbackBacklink()
      }

      "have the correct heading" in new Setup(view) {
        pageContent(pageContentSelector) hasPageHeading s"${messages("taxCal_breakdown.dates", s"${taxYear - 1}", s"$taxYear")} ${messages("taxCal_breakdown.heading")}"
        pageContent(pageContentSelector).h1.select(headingSelector).text() shouldBe messages("taxCal_breakdown.dates", s"${taxYear - 1}", s"$taxYear")
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = pageContent(pageContentSelector).getElementById("explanation")
        guidance.text() shouldBe s"${messages("taxCal_breakdown.explanation")} £0.00"
      }

      "have a Pay, pensions and profit table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 1,
          expectedCaption = sectionHeadingPPP,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, messages("taxCal_breakdown.table.head.rates.uk"), amountTableHeader),
            (1, messages("taxCal_breakdown.table.BRT", "£20,000.00", "20.0"), "£4,000.00"),
            (2, messages("taxCal_breakdown.table.HRT", "£100,000.00", "40.0"), "£40,000.00"),
            (3, messages("taxCal_breakdown.table.ART", "£50,000.00", "45.0"), "£22,500.00")
          )
        )

      }

      "have no Pay, pensions and profit Income" which {
        "has no Pay, pensions and profit heading" in new Setup(zeroIncome) {
          pageContent(pageContentSelector).select("caption").text should not include sectionHeadingPPP
        }
      }

      "have an Savings table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 2,
          expectedCaption = messages("taxCal_breakdown.savings"),
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, rateBandTableHeader, amountTableHeader),
            (1, messages("taxCal_breakdown.table.SSR", "£1.00", "0.0"), "£0.00"),
            (2, messages("taxCal_breakdown.table.ZRTBR", "£20.00", "0.0"), "£0.00"),
            (3, messages("taxCal_breakdown.table.BRT", "£20.00", "10.0"), "£2.00"),
            (4, messages("taxCal_breakdown.table.ZRTHR", "£10,000.00", "0.0"), "£0.00"),
            (5, messages("taxCal_breakdown.table.HRT", "£2,000.00", "40.0"), "£800.00"),
            (6, messages("taxCal_breakdown.table.ART", "£100,000.00", "50.0"), "£5,000.00")
          )
        )

      }

      "have no Savings and Gains Income" which {
        "has no Savings and Gains heading" in new Setup(zeroIncome) {
          pageContent(pageContentSelector).select("caption").text should not include messages("taxCal_breakdown.savings")
        }
      }

      "have a Dividends table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 3,
          expectedCaption = messages("taxCal_breakdown.dividends"),
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, rateBandTableHeader, amountTableHeader),
            (1, messages("taxCal_breakdown.table.ZRTBR", "£1,000.00", "0"), "£0.00"),
            (2, messages("taxCal_breakdown.table.BRT", "£1,000.00", "7.5"), "£75.00"),
            (3, messages("taxCal_breakdown.table.ZRTHR", "£2,000.00", "0"), "£0.00"),
            (4, messages("taxCal_breakdown.table.HRT", "£2,000.00", "37.5"), "£750.00"),
            (5, messages("taxCal_breakdown.table.ZRTAR", "£3,000.00", "0"), "£0.00"),
            (6, messages("taxCal_breakdown.table.ART", "£3,000.00", "38.1"), "£1,143.00")
          )
        )

      }

      "have no Dividend Income" which {
        "has no dividend heading" in new Setup(zeroIncome) {
          pageContent(pageContentSelector).select("caption").text should not include messages("taxCal_breakdown.dividends")
        }
      }

      "have a Employment lump sums table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 4,
          expectedCaption = sectionHeadingLumpSums,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, messages("taxCal_breakdown.table.head.rates.uk"), amountTableHeader),
            (1, messages("taxCal_breakdown.table.BRT", "£20,000.00", "20.0"), "£4,000.00"),
            (2, messages("taxCal_breakdown.table.HRT", "£100,000.00", "40.0"), "£40,000.00"),
            (3, messages("taxCal_breakdown.table.ART", "£50,000.00", "45.0"), "£22,500.00")
          )
        )

      }

      "have no Lump Sum Income" which {
        "has no Lump Sum heading" in new Setup(zeroIncome) {
          pageContent(pageContentSelector).select("caption").text should not include sectionHeadingLumpSums
        }
      }

      "have a Gains on life policies table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 5,
          expectedCaption = sectionHeadingGainsOnLifePolicies,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, rateBandTableHeader, amountTableHeader),
            (1, messages("taxCal_breakdown.table.BRT", "£20,000.00", "20.0"), "£4,000.00"),
            (2, messages("taxCal_breakdown.table.HRT", "£100,000.00", "40.0"), "£40,000.00"),
            (3, messages("taxCal_breakdown.table.ART", "£50,000.00", "45.0"), "£22,500.00")
          )
        )

      }

      "have no Gains on Life Policy Income" which {
        "has no Gains on Life Policy heading" in new Setup(zeroIncome) {
          pageContent(pageContentSelector).select("caption").text should not include sectionHeadingGainsOnLifePolicies
        }
      }

      "have no Tax reductions table and heading when there is no any reductions value" in new Setup(viewNone) {
        pageContent(pageContentSelector).select("caption").text should not include sectionHeadingTaxReductions
      }

      "have a Tax reductions table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 6,
          expectedCaption = sectionHeadingTaxReductions,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, reductionTableHeader, amountTableHeader),
            (1, messages("deduction_breakdown.table.marriage_allowance_transfer"), "−£252.00"),
            (2, messages("taxCal_breakdown.table.deficiencyRelief"), "−£1,000.00"),
            (3, messages("taxCal_breakdown.table.top_slicing_relief"), "−£1,200.00"),
            (4, messages("taxCal_breakdown.table.vctSubscriptions"), "−£2,000.00"),
            (5, messages("taxCal_breakdown.table.eisSubscriptions"), "−£3,000.00"),
            (6, messages("taxCal_breakdown.table.seedEnterpriseInvestment"), "−£4,000.00"),
            (7, messages("taxCal_breakdown.table.communityInvestment"), "−£5,000.00"),
            (8, messages("taxCal_breakdown.table.socialEnterpriseInvestment"), "−£6,000.00"),
            (9, messages("taxCal_breakdown.table.maintenancePayments"), "−£7,000.00"),
            (10, messages("taxCal_breakdown.table.property_finance_relief"), "−£5,000.00"),
            (11, messages("taxCal_breakdown.table.total_notional_tax"), "−£7,000.00"),
            (12, messages("taxCal_breakdown.table.total_foreign_tax_credit_relief"), "−£6,000.00"),
            (13, messages("taxCal_breakdown.table.qualifyingDistributionRedemptionOfSharesAndSecurities"), "−£8,000.00"),
            (14, messages("taxCal_breakdown.table.nonDeductibleLoanInterest"), "−£9,000.00"),
            (15, messages("taxCal_breakdown.table.income_tax_due_after_tax_reductions"), "£2,000.00")
          )
        )

      }

      "have a Tax reductions table when only MarriageAllowanceTransfer is set" which {
        shouldHaveACorrectTableContent(viewMarriageAllowanceTransfer)(
          tableNumber = 1,
          expectedCaption = sectionHeadingTaxReductions,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, reductionTableHeader, amountTableHeader),
            (1, messages("deduction_breakdown.table.marriage_allowance_transfer"), "−£1,234.00"),
          )
        )
      }

      "have a Tax reductions table when only TopSlicingRelief is set" which {
        shouldHaveACorrectTableContent(viewTopSlicingRelief)(
          tableNumber = 1,
          expectedCaption = sectionHeadingTaxReductions,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, reductionTableHeader, amountTableHeader),
            (1, messages("taxCal_breakdown.table.top_slicing_relief"), "−£2,345.00"),
          )
        )
      }

      "have no additional charges table and heading when there is no any other charges value" in new Setup(viewNone) {
        pageContent(pageContentSelector).select("caption").text should not include sectionHeadingAdditionalChar
      }

      "have an additional charges table" which {
        val tableNumber = 7

        "has all four table rows" in new Setup(view) {
          pageContent(pageContentSelector) hasTableWithCorrectSize(tableNumber, 4)
        }

        "has the correct heading" in new Setup(view) {
          pageContent(pageContentSelector).selectById("additional_charges").text shouldBe sectionHeadingAdditionalChar
        }

        "has a table header section" in new Setup(view) {
          val row: Element = pageContent(pageContentSelector).table(tableNumber).select("tr").get(0)
          row.select("th").first().text() shouldBe messages("taxCal_breakdown.table.head.charge_type")
          row.select("th").last().text() shouldBe amountTableHeader
        }

        "has only a Gift Aid line with the correct heading and table" in new Setup(viewAdChGiftAid) {
          pageContent(pageContentSelector).selectById("additional_charges").text shouldBe sectionHeadingAdditionalChar
          val row: Element = pageContent(pageContentSelector).table().select("tr").get(1)
          row.select("td").first().text() shouldBe messages("taxCal_breakdown.table.giftAidTax")
          row.select("td").last().text() shouldBe "£5,000.00"
        }

        "has only a Pensions Saving line with the correct heading and table" in new Setup(viewAdChPensionSavings) {
          pageContent(pageContentSelector).selectById("additional_charges").text shouldBe sectionHeadingAdditionalChar
          val row: Element = pageContent(pageContentSelector).table().select("tr").get(1)
          row.select("td").first().text() shouldBe messages("taxCal_breakdown.table.totalPensionSavingsTaxCharges")
          row.select("td").last().text() shouldBe "£5,000.00"
        }

        "has only a Pensions Lump Sum line with the correct heading and table" in new Setup(viewAdChPensionLumpSum) {
          pageContent(pageContentSelector).selectById("additional_charges").text shouldBe sectionHeadingAdditionalChar
          val row: Element = pageContent(pageContentSelector).table().select("tr").get(1)
          row.select("td").first().text() shouldBe messages("taxCal_breakdown.table.statePensionLumpSumCharges")
          row.select("td").last().text() shouldBe "£5,000.00"
        }

      }

      "have a National Insurance contributions table" which {
        shouldHaveACorrectTableContent(view)(
          tableNumber = 8,
          expectedCaption = sectionHeadingNationalInsuranceContributionsChar,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, nationalInsuranceTypeTableHeader, amountTableHeader),
            (1, Nic4New_ZRT, "£100.00"),
            (2, Nic4New_BRT, "£200.00"),
            (3, Nic4New_HRT, "£300.00"),
            (4, voluntaryNic2, "£10,000.00")
          )
        )
      }

      "have a National Insurance contributions table with Uplift message when Class4Uplift is enabled" which {
        shouldHaveACorrectTableContent(viewWithClass4UpliftEnabled)(
          tableNumber = 8,
          expectedCaption = sectionHeadingNationalInsuranceContributionsChar,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, nationalInsuranceTypeTableHeader, amountTableHeader),
            (1, Nic4New_ZRT, "£100.00"),
            (2, s"$Nic4New_BRT ${messages("taxCal_breakdown.table.uplift")}", "£200.00"),
            (3, s"$Nic4New_HRT ${messages("taxCal_breakdown.table.uplift")}", "£300.00"),
            (4, voluntaryNic2, "£10,000.00")
          )
        )
      }

      "have a Nics table showing Class 2 Nics when the voluntaryClass2Contributions flag is false" which {
        shouldHaveACorrectTableContent(viewNic2)(
          tableNumber = 1,
          expectedCaption = sectionHeadingNationalInsuranceContributionsChar,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, nationalInsuranceTypeTableHeader, amountTableHeader),
            (1, messages("tax-year-summary.payments.class2Nic.text"), "£10,000.00")
          )
        )
      }

      "have a Nics table showing Voluntary Class 2 Nics when the voluntaryClass2Contributions flag is true" which {
        shouldHaveACorrectTableContent(viewVoluntaryNic2)(
          tableNumber = 1,
          expectedCaption = sectionHeadingNationalInsuranceContributionsChar,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, nationalInsuranceTypeTableHeader, amountTableHeader),
            (1, voluntaryNic2, "£10,000.00")
          )
        )
      }

      "have no National Insurance contributions table and heading when there is no Nic4 or Nic2 data" in new Setup(viewNone) {
        pageContent(pageContentSelector).select("caption").text should not include sectionHeadingNationalInsuranceContributionsChar
      }

      "have no Capital Gains Tax table and heading when there is no any CGT value" in new Setup(zeroIncome) {
        pageContent(pageContentSelector).select("caption").text should not include messages("taxCal_breakdown.table.capital_gains_tax")
      }

      "have a Capital Gains Tax table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 9,
          expectedCaption = messages("taxCal_breakdown.table.capital_gains_tax"),
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, messages("taxCal_breakdown.table.head.cgt_type"), amountTableHeader),
            (1, messages("taxCal_breakdown.table.cgt.taxable_capital_gains"), "£1,234.56"),
            (2, messages("taxCal_breakdown.table.cgt.assets_or_investors_relief.band.single", "£10,000.00", "10.0"), "£1,000.00"),
            (3, messages("taxCal_breakdown.table.cgt.property_and_interest.band.lowerRate", "£20,000.00", "18.0"), "£3,600.00"),
            (4, messages("taxCal_breakdown.table.cgt.property_and_interest.band.higherRate", "£30,000.00", "28.0"), "£8,400.00"),
            (5, messages("taxCal_breakdown.table.cgt.other_gains.band.lowerRate", "£11,000.00", "20.0"), "£2,200.00"),
            (6, messages("taxCal_breakdown.table.cgt.other_gains.band.higherRate", "£12,000.00", "28.0"), "£3,360.00"),
            (7, messages("taxCal_breakdown.table.cgt.adjustment"), "£123.45"),
            (8, messages("taxCal_breakdown.table.cgt.foreign_tax_credit_relief"), "−£2,345.67"),
            (9, messages("taxCal_breakdown.table.cgt.already_paid"), "−£3,456.78"),
            (10, messages("taxCal_breakdown.table.cgt.due"), "£4,567.89"),
            (11, messages("taxCal_breakdown.table.cgt.overpaid"), "£234.56")
          )
        )

      }

      "have an other charges table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 10,
          expectedCaption = messages("taxCal_breakdown.table.other_charges"),
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, messages("taxCal_breakdown.table.head.charge_type"), amountTableHeader),
            (1, messages("taxCal_breakdown.table.totalStudentLoansRepaymentAmount"), "£5,000.00"),
            (2, messages("taxCal_breakdown.table.payeUnderpaymentsCodedOut", "2017", "2018"), "£254.00"),
            (3, messages("taxCal_breakdown.table.saUnderpaymentsCodedOut", "2017", "2018"), "£400.00")
          )
        )
      }

      "have an additional deductions table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 11,
          expectedCaption = messages("taxCal_breakdown.taxDeductedAtSource"),
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, messages("taxCal_breakdown.table.head.deduction"), amountTableHeader),
            (1, messages("taxCal_breakdown.table.taxDeductedAtSource.inYearAdjustment"), "−£900.00"),
            (2, messages("taxCal_breakdown.table.taxDeductedAtSource.payeEmployments"), "−£100.00"),
            (3, messages("taxCal_breakdown.table.taxDeductedAtSource.ukPensions"), "−£200.00"),
            (4, messages("taxCal_breakdown.table.taxDeductedAtSource.stateBenefits"), "−£300.00"),
            (5, messages("taxCal_breakdown.table.taxDeductedAtSource.cis"), "−£400.00"),
            (6, messages("taxCal_breakdown.table.taxDeductedAtSource.ukLandAndProperty"), "−£500.00"),
            (7, messages("taxCal_breakdown.table.taxDeductedAtSource.specialWithholdingTax"), "−£600.00"),
            (8, messages("taxCal_breakdown.table.taxDeductedAtSource.voidISAs"), "−£700.00"),
            (9, messages("taxCal_breakdown.table.banks_and_building_societies"), "−£800.00"),
            (10, messages("taxCal_breakdown.table.taxDeductedAtSource.total"), "£1,000.00")
          )
        )

      }
    }
  }

  "The tax calc view should display messages" when {

    "provided with all matching generic messages" in {
      lazy val view = taxCalcBreakdown(TaxDueSummaryViewModel(
        messages = Some(Messages(info = Some(Seq(
          Message("C22202", "message2"),
          Message("C22203", "message3"),
          Message("C22206", "message6"),
          Message("C22207", "message7"),
          Message("C22210", "message10"),
          Message("C22211", "message11"),
          Message("C22212", "message12"),
          Message("C22213", "message13"),
          Message("C22214", "message14"),
          Message("C22215", "message15"),
          Message("C22216", "message16"),
          Message("C22217", "message17"),
          Message("C22218", "message18"),
          Message("C22219", "message19")
        ))))
      ), taxYear2017, backUrl)

      val document: Document = Jsoup.parse(view.body)

      document.select(messageContentSelector).size shouldBe 14
      document.select(messageContentSelector).get(0).text shouldBe messages("taxCal_breakdown.message.C22202")
      document.select(messageContentSelector).get(1).text shouldBe messages("taxCal_breakdown.message.C22203")
      document.select(messageContentSelector).get(2).text shouldBe messages("taxCal_breakdown.message.C22206")
      document.select(messageContentSelector).get(3).text shouldBe messages("taxCal_breakdown.message.C22207")
      document.select(messageContentSelector).get(4).text shouldBe messages("taxCal_breakdown.message.C22210")
      document.select(messageContentSelector).get(5).text shouldBe messages("taxCal_breakdown.message.C22211")
      document.select(messageContentSelector).get(6).text shouldBe messages("taxCal_breakdown.message.C22212")
      document.select(messageContentSelector).get(7).text shouldBe messages("taxCal_breakdown.message.C22213")
      document.select(messageContentSelector).get(8).text shouldBe messages("taxCal_breakdown.message.C22214")
      document.select(messageContentSelector).get(9).text shouldBe messages("taxCal_breakdown.message.C22215")
      document.select(messageContentSelector).get(10).text shouldBe messages("taxCal_breakdown.message.C22216")
      document.select(messageContentSelector).get(11).text shouldBe messages("taxCal_breakdown.message.C22217")
      document.select(messageContentSelector).get(12).text shouldBe messages("taxCal_breakdown.message.C22218")
      document.select(messageContentSelector).get(13).text shouldBe messages("taxCal_breakdown.message.C22219")
    }

    "provided with message C22201" in {

      lazy val view = taxCalcBreakdown(TaxDueSummaryViewModel(
        grossGiftAidPayments = Some(5000.98),
        lumpSumsBands = Some(Seq(TaxBands(
          name = "BRT",
          rate = 20.0,
          income = 0,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000)
        )),
        messages = Some(Messages(info = Some(Seq(
          Message("C22201", "message")
        ))))
      ), taxYear2017, backUrl)

      val document: Document = Jsoup.parse(view.body)

      document.select(messageContentSelector).size shouldBe 1
      document.select(messageContentSelector).text shouldBe messages("taxCal_breakdown.message.C22201", "£5,000.98", "£15,000.00")
    }

    "A C22201 message" when {
      "grossGiftAidPayments is not provided" should {
        "produce MissingFieldException" in {

          val expectedException = intercept[MissingFieldException] {
            taxCalcBreakdown(TaxDueSummaryViewModel(
              grossGiftAidPayments = None,
              lumpSumsBands = Some(Seq(TaxBands(
                name = "BRT",
                rate = 20.0,
                income = 0,
                taxAmount = 4000.00,
                bandLimit = 15000,
                apportionedBandLimit = 15000)
              )),
              messages = Some(Messages(info = Some(Seq(
                Message("C22201", "message")
              ))))
            ), taxYear2017, backUrl)
          }

          expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Gross Gift Aid Payments"
        }
      }

      "getModifiedBaseTaxBand returns None" should {
        "produce MissingFieldException" in {

          val expectedException = intercept[MissingFieldException] {
            taxCalcBreakdown(TaxDueSummaryViewModel(
              grossGiftAidPayments = Some(5000.98),
              lumpSumsBands = Some(Seq()),
              messages = Some(Messages(info = Some(Seq(
                Message("C22201", "message")
              ))))
            ), taxYear2017, backUrl)
          }

          expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Modified Base Tax Band"
        }
      }
    }

    "provided with message C22205" in {

      lazy val view = taxCalcBreakdown(TaxDueSummaryViewModel(
        lossesAppliedToGeneralIncome = Some(1000),
        messages = Some(Messages(info = Some(Seq(
          Message("C22205", "message")
        ))))
      ), taxYear2017, backUrl)

      val document: Document = Jsoup.parse(view.body)

      document.select(messageContentSelector).size shouldBe 1
      document.select(messageContentSelector).text shouldBe messages("taxCal_breakdown.message.C22205", "£1,000.00")
    }

    "A C22205 message" when {
      "lossesAppliedToGeneralIncome is missing" should {
        "produce MissingFieldException" in {

          val expectedException = intercept[MissingFieldException] {
            taxCalcBreakdown(TaxDueSummaryViewModel(
              lossesAppliedToGeneralIncome = None,
              messages = Some(Messages(info = Some(Seq(
                Message("C22205", "message")
              ))))
            ), taxYear2017, backUrl)
          }

          expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Losses Applied To General Income"
        }
      }
    }

    "provided with message C22208" in {

      lazy val view = taxCalcBreakdown(TaxDueSummaryViewModel(
        giftAidTax = Some(5000.99),
        lumpSumsBands = Some(Seq(TaxBands(
          name = "BRT",
          rate = 20.0,
          income = 0,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000)
        )),
        messages = Some(Messages(info = Some(Seq(
          Message("C22208", "message")
        ))))
      ), taxYear2017, backUrl)

      val document: Document = Jsoup.parse(view.body)

      document.select(messageContentSelector).size shouldBe 1
      document.select(messageContentSelector).text shouldBe messages("taxCal_breakdown.message.C22208", "£5,000.99", "£15,000.00")
    }

    "A C22208 message" when {
      "giftAidTax is missing" should {
        "produce MissingFieldException" in {

          val expectedException = intercept[MissingFieldException] {
            taxCalcBreakdown(TaxDueSummaryViewModel(
              giftAidTax = None,
              lumpSumsBands = Some(Seq(TaxBands(
                name = "BRT",
                rate = 20.0,
                income = 0,
                taxAmount = 4000.00,
                bandLimit = 15000,
                apportionedBandLimit = 15000)
              )),
              messages = Some(Messages(info = Some(Seq(
                Message("C22208", "message")
              ))))
            ), taxYear2017, backUrl)
          }

          expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Gift Aid Tax"
        }
      }

      "getModifiedBaseTaxBand is missing" should {
        "produce MissingFieldException when " in {

          val expectedException = intercept[MissingFieldException] {
            taxCalcBreakdown(TaxDueSummaryViewModel(
              giftAidTax = Some(5000.99),
              lumpSumsBands = Some(Seq()),
              messages = Some(Messages(info = Some(Seq(
                Message("C22208", "message")
              ))))
            ), taxYear2017, backUrl)
          }

          expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Modified Base Tax Band"
        }
      }
    }

    "provided with message C22209" in {

      lazy val view = taxCalcBreakdown(TaxDueSummaryViewModel(
        giftAidTax = Some(5000.99),
        lumpSumsBands = Some(Seq(TaxBands(
          name = "BRT",
          rate = 20.0,
          income = 0,
          taxAmount = 4000.00,
          bandLimit = 15000,
          apportionedBandLimit = 15000)
        )),
        messages = Some(Messages(info = Some(Seq(
          Message("C22209", "message")
        ))))
      ), taxYear2017, backUrl)

      val document: Document = Jsoup.parse(view.body)

      document.select(messageContentSelector).size shouldBe 1
      document.select(messageContentSelector).text shouldBe messages("taxCal_breakdown.message.C22209", "£5,000.99", "£15,000.00")
    }

    "A C22209 message" when {
      "giftAidTax is missing" should {
        "produce MissingFieldException" in {

          val expectedException = intercept[MissingFieldException] {
            taxCalcBreakdown(TaxDueSummaryViewModel(
              giftAidTax = None,
              lumpSumsBands = Some(Seq(TaxBands(
                name = "BRT",
                rate = 20.0,
                income = 0,
                taxAmount = 4000.00,
                bandLimit = 15000,
                apportionedBandLimit = 15000)
              )),
              messages = Some(Messages(info = Some(Seq(
                Message("C22209", "message")
              ))))
            ), taxYear2017, backUrl)
          }

          expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Gift Aid Tax"
        }
      }

      "getModifiedBaseTaxBand is missing" should {
        "produce MissingFieldException" in {

          val expectedException = intercept[MissingFieldException] {
            taxCalcBreakdown(TaxDueSummaryViewModel(
              giftAidTax = Some(5000.99),
              lumpSumsBands = Some(Seq()),
              messages = Some(Messages(info = Some(Seq(
                Message("C22209", "message")
              ))))
            ), taxYear2017, backUrl)
          }

          expectedException.getMessage shouldBe "Missing Mandatory Expected Field: Modified Base Tax Band"
        }
      }
    }
  }

}

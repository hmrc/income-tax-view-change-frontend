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
import models.liabilitycalculation.{Message, Messages}
import testConstants.NewCalcBreakdownUnitTestConstants._
import models.liabilitycalculation.viewmodels.TaxDueSummaryViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor3
import play.twirl.api.Html
import testConstants.MessagesLookUp.TaxCalcBreakdown
import testUtils.ViewSpec
import views.html.TaxCalcBreakdownNew

class TaxCalcBreakdownViewNewSpec extends TaxCalcBreakdownViewNewBehaviour {

  override val backUrl = "testUrl"

  override def taxCalcBreakdown(taxDueSummaryViewModel: TaxDueSummaryViewModel, taxYear: Int, backUrl: String): Html =
    app.injector.instanceOf[TaxCalcBreakdownNew].apply(taxDueSummaryViewModel, taxYear, backUrl)

  override val expectedPageTitle: String = TaxCalcBreakdown.title

  override val pageContentSelector = "#main-content"

  override val messageContentSelector = ".govuk-inset-text"

  override val headingSelector = ".govuk-caption-xl"

}

abstract class TaxCalcBreakdownViewNewBehaviour extends ViewSpec {

  val taxYear2017: Int = 2017

  def backUrl: String

  def taxCalcBreakdown(taxDueSummaryViewModel: TaxDueSummaryViewModel, taxYear: Int, backUrl: String): Html

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
          expectedCaption = TaxCalcBreakdown.sectionHeadingPPP,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.scotlandRateTableHead, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.pPP_Scot_SRT, "£2,000.00"),
            (2, TaxCalcBreakdown.pPP_Scot_BRT, "£4,000.00"),
            (3, TaxCalcBreakdown.pPP_Scot_IRT, "£45,000.00"),
            (4, TaxCalcBreakdown.pPP_Scot_HRT, "£40,000.00"),
            (5, TaxCalcBreakdown.pPP_Scot_ART, "£22,500.00")
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
          expectedCaption = TaxCalcBreakdown.sectionHeadingLumpSums,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.scotlandRateTableHead, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.ls_Scot_SRT, "£2,000.00"),
            (2, TaxCalcBreakdown.ls_Scot_BRT, "£4,000.00"),
            (3, TaxCalcBreakdown.ls_Scot_IRT, "£45,000.00"),
            (4, TaxCalcBreakdown.ls_Scot_HRT, "£40,000.00"),
            (5, TaxCalcBreakdown.ls_Scot_ART, "£22,500.00")
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
          expectedCaption = TaxCalcBreakdown.sectionHeadingGainsOnLifePolicies,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.rateBandTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.gols_Scot_SRT, "£2,000.00"),
            (2, TaxCalcBreakdown.gols_Scot_BRT, "£4,000.00"),
            (3, TaxCalcBreakdown.gols_Scot_IRT, "£45,000.00"),
            (4, TaxCalcBreakdown.gols_Scot_HRT, "£40,000.00"),
            (5, TaxCalcBreakdown.gols_Scot_ART, "£22,500.00")
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
        pageContent(pageContentSelector) hasPageHeading TaxCalcBreakdown.heading(taxYear2017)
        pageContent(pageContentSelector).h1.select(headingSelector).text() shouldBe TaxCalcBreakdown.subHeading(taxYear2017)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = pageContent(pageContentSelector).getElementById("explanation")
        guidance.text() shouldBe TaxCalcBreakdown.guidance
      }
    }


    "provided with a calculation with all the sections displayed which includes Pay, pensions and profit, Savings, Dividends," +
      " lumpsums and gainsOnLifePolicies, Additional charges and Additional deductions as well as the total tax due amount for the 2018 tax year" should {
      val taxYear = 2018

      lazy val view = taxCalcBreakdown(taxDueSummaryViewModelStandard, taxYear, backUrl)
      lazy val viewNone = taxCalcBreakdown(TaxDueSummaryViewModel(), taxYear, backUrl)
      lazy val viewNic2 = taxCalcBreakdown(taxDueSummaryViewModelNic2, taxYear, backUrl)
      lazy val viewMarriageAllowanceTransfer = taxCalcBreakdown(taxDueSummaryViewModelMarriageAllowance, taxYear, backUrl)
      lazy val viewTopSlicingRelief = taxCalcBreakdown(taxDueSummaryViewModelTopSlicingRelief, taxYear, backUrl)
      lazy val viewNoVoluntaryNic2Flag = taxCalcBreakdown(taxDueSummaryViewModelNic2NoVoluntary, taxYear, backUrl)
      lazy val viewAdChGiftAid = taxCalcBreakdown(taxDueSummaryViewModelGiftAid, taxYear, backUrl)
      lazy val viewAdChPensionLumpSum = taxCalcBreakdown(taxDueSummaryViewModelPensionLumpSum, taxYear, backUrl)
      lazy val viewAdChPensionSavings = taxCalcBreakdown(taxDueSummaryViewModelPensionSavings, taxYear, backUrl)
      lazy val zeroIncome = taxCalcBreakdown(taxDueSummaryViewModelZeroIncome, taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe expectedPageTitle
      }

      "have the correct heading" in new Setup(view) {
        pageContent(pageContentSelector) hasPageHeading TaxCalcBreakdown.heading(taxYear)
        pageContent(pageContentSelector).h1.select(headingSelector).text() shouldBe TaxCalcBreakdown.subHeading(taxYear)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = pageContent(pageContentSelector).getElementById("explanation")
        guidance.text() shouldBe TaxCalcBreakdown.guidance
      }

      "have a Pay, pensions and profit table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 1,
          expectedCaption = TaxCalcBreakdown.sectionHeadingPPP,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.ukRateTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.pPP_BRT, "£4,000.00"),
            (2, TaxCalcBreakdown.pPP_HRT, "£40,000.00"),
            (3, TaxCalcBreakdown.pPP_ART, "£22,500.00")
          )
        )

      }

      "have no Pay, pensions and profit Income" which {
        "has no Pay, pensions and profit heading" in new Setup(zeroIncome) {
          pageContent(pageContentSelector).select("caption").text should not include TaxCalcBreakdown.sectionHeadingPPP
        }
      }

      "have an Savings table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 2,
          expectedCaption = TaxCalcBreakdown.sectionHeadingSavings,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.rateBandTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.saving_SSR, "£0.00"),
            (2, TaxCalcBreakdown.saving_ZRTBR, "£0.00"),
            (3, TaxCalcBreakdown.saving_BRT, "£2.00"),
            (4, TaxCalcBreakdown.saving_ZRTHR, "£0.00"),
            (5, TaxCalcBreakdown.saving_HRT, "£800.00"),
            (6, TaxCalcBreakdown.saving_ART, "£5,000.00")
          )
        )

      }

      "have no Savings and Gains Income" which {
        "has no Savings and Gains heading" in new Setup(zeroIncome) {
          pageContent(pageContentSelector).select("caption").text should not include TaxCalcBreakdown.sectionHeadingSavings
        }
      }

      "have a Dividends table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 3,
          expectedCaption = TaxCalcBreakdown.sectionHeadingDividends,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.rateBandTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.dividend_ZRTBR, "£0.00"),
            (2, TaxCalcBreakdown.dividend_BRT, "£75.00"),
            (3, TaxCalcBreakdown.dividend_ZRTHR, "£0.00"),
            (4, TaxCalcBreakdown.dividend_HRT, "£750.00"),
            (5, TaxCalcBreakdown.dividend_ZRTAR, "£0.00"),
            (6, TaxCalcBreakdown.dividend_ART, "£1,143.00")
          )
        )

      }

      "have no Dividend Income" which {
        "has no dividend heading" in new Setup(zeroIncome) {
          pageContent(pageContentSelector).select("caption").text should not include TaxCalcBreakdown.sectionHeadingDividends
        }
      }

      "have a Employment lump sums table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 4,
          expectedCaption = TaxCalcBreakdown.sectionHeadingLumpSums,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.ukRateTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.ls_BRT, "£4,000.00"),
            (2, TaxCalcBreakdown.ls_HRT, "£40,000.00"),
            (3, TaxCalcBreakdown.ls_ART, "£22,500.00")
          )
        )

      }

      "have no Lump Sum Income" which {
        "has no Lump Sum heading" in new Setup(zeroIncome) {
          pageContent(pageContentSelector).select("caption").text should not include TaxCalcBreakdown.sectionHeadingLumpSums
        }
      }

      "have a Gains on life policies table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 5,
          expectedCaption = TaxCalcBreakdown.sectionHeadingGainsOnLifePolicies,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.rateBandTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.gols_BRT, "£4,000.00"),
            (2, TaxCalcBreakdown.gols_HRT, "£40,000.00"),
            (3, TaxCalcBreakdown.gols_ART, "£22,500.00")
          )
        )

      }

      "have no Gains on Life Policy Income" which {
        "has no Gains on Life Policy heading" in new Setup(zeroIncome) {
          pageContent(pageContentSelector).select("caption").text should not include TaxCalcBreakdown.sectionHeadingGainsOnLifePolicies
        }
      }

      "have a Nic 4 table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 6,
          expectedCaption = TaxCalcBreakdown.sectionHeadingNIC4,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.rateBandTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.Nic4_ZRT, "£100.00"),
            (2, TaxCalcBreakdown.Nic4_BRT, "£200.00"),
            (3, TaxCalcBreakdown.Nic4_HRT, "£300.00"),
            (4, TaxCalcBreakdown.giftAidTaxCharge, "£400.00"),
            (5, TaxCalcBreakdown.totalPensionSavingCharges, "£500.00"),
            (6, TaxCalcBreakdown.statePensionLumpSum, "£600.00"),
            (7, TaxCalcBreakdown.totalStudentLoansRepaymentAmount, "£700.00")
          )
        )

      }

      "have no Tax reductions table and heading when there is no any reductions value" in new Setup(viewNone) {
        pageContent(pageContentSelector).select("caption").text should not include TaxCalcBreakdown.sectionHeadingTaxReductions
      }

      "have a Tax reductions table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 7,
          expectedCaption = TaxCalcBreakdown.sectionHeadingTaxReductions,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.reductionTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.marriageAllowanceTransfer, "−£252.00"),
            (2, TaxCalcBreakdown.deficiencyRelief, "−£1,000.00"),
            (3, TaxCalcBreakdown.topSlicingRelief, "−£1,200.00"),
            (4, TaxCalcBreakdown.vctRelief, "−£2,000.00"),
            (5, TaxCalcBreakdown.eisRelief, "−£3,000.00"),
            (6, TaxCalcBreakdown.seedRelief, "−£4,000.00"),
            (7, TaxCalcBreakdown.citRelief, "−£5,000.00"),
            (8, TaxCalcBreakdown.sitRelief, "−£6,000.00"),
            (9, TaxCalcBreakdown.maintenanceRelief, "−£7,000.00"),
            (10, TaxCalcBreakdown.reliefForFinanceCosts, "−£5,000.00"),
            (11, TaxCalcBreakdown.notionalTax, "−£7,000.00"),
            (12, TaxCalcBreakdown.foreignTaxCreditRelief, "−£6,000.00"),
            (13, TaxCalcBreakdown.reliefClaimedOnQualifyingDis, "−£8,000.00"),
            (14, TaxCalcBreakdown.nonDeductibleLoanInterest, "−£9,000.00"),
            (15, TaxCalcBreakdown.incomeTaxDueAfterTaxReductions, "£2,000.00")
          )
        )

      }

      "have a Tax reductions table when only MarriageAllowanceTransfer is set" which {
        shouldHaveACorrectTableContent(viewMarriageAllowanceTransfer)(
          tableNumber = 1,
          expectedCaption = TaxCalcBreakdown.sectionHeadingTaxReductions,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.reductionTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.marriageAllowanceTransfer, "−£1,234.00"),
          )
        )
      }

      "have a Tax reductions table when only TopSlicingRelief is set" which {
        shouldHaveACorrectTableContent(viewTopSlicingRelief)(
          tableNumber = 1,
          expectedCaption = TaxCalcBreakdown.sectionHeadingTaxReductions,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.reductionTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.topSlicingRelief, "−£2,345.00"),
          )
        )
      }

      "have no additional charges table and heading when there is no any other charges value" in new Setup(viewNone) {
        pageContent(pageContentSelector).select("caption").text should not include TaxCalcBreakdown.sectionHeadingAdditionalChar
      }

      "have an additional charges table" which {
        val tableNumber = 8

        "has all four table rows" in new Setup(view) {
          pageContent(pageContentSelector) hasTableWithCorrectSize(tableNumber, 5)
        }

        "has the correct heading" in new Setup(view) {
          pageContent(pageContentSelector).selectById("additional_charges").text shouldBe TaxCalcBreakdown.sectionHeadingAdditionalChar
        }

        "has a table header section" in new Setup(view) {
          val row: Element = pageContent(pageContentSelector).table(tableNumber).select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.chargeTypeTableHeader
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "has a Voluntary Nic 2 line with the correct value" in new Setup(view) {
          val row: Element = pageContent(pageContentSelector).table(tableNumber).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.VoluntaryNic2
          row.select("td").last().text() shouldBe "£10,000.00"
        }

        "has a Nic2 line with the correct value" in new Setup(viewNic2) {
          val row: Element = pageContent(pageContentSelector).table(1).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic2
          row.select("td").last().text() shouldBe "£10,000.00"
        }

        "has no Nic2 line when Voluntary contribution flag is missing " in new Setup(viewNoVoluntaryNic2Flag) {
          pageContent(pageContentSelector).select("caption").text should not include TaxCalcBreakdown.sectionHeadingAdditionalChar
        }

        "has only a Gift Aid line with the correct heading and table" in new Setup(viewAdChGiftAid) {
          pageContent(pageContentSelector).selectById("additional_charges").text shouldBe TaxCalcBreakdown.sectionHeadingAdditionalChar
          val row: Element = pageContent(pageContentSelector).table().select("tr").get(1)
          println("giftaid row:" + row)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.GiftAid
          row.select("td").last().text() shouldBe "£5,000.00"
        }

        "has only a Pensions Saving line with the correct heading and table" in new Setup(viewAdChPensionSavings) {
          pageContent(pageContentSelector).selectById("additional_charges").text shouldBe TaxCalcBreakdown.sectionHeadingAdditionalChar
          val row: Element = pageContent(pageContentSelector).table().select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.PensionSavings
          row.select("td").last().text() shouldBe "£5,000.00"
        }

        "has only a Pensions Lump Sum line with the correct heading and table" in new Setup(viewAdChPensionLumpSum) {
          pageContent(pageContentSelector).selectById("additional_charges").text shouldBe TaxCalcBreakdown.sectionHeadingAdditionalChar
          val row: Element = pageContent(pageContentSelector).table().select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.PensionLumpSum
          row.select("td").last().text() shouldBe "£5,000.00"
        }

      }

      "have no Capital Gains Tax table and heading when there is no any CGT value" in new Setup(zeroIncome) {
        pageContent(pageContentSelector).select("caption").text should not include TaxCalcBreakdown.sectionHeadingCapitalGainsTax
      }

      "have a Capital Gains Tax table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 9,
          expectedCaption = TaxCalcBreakdown.sectionHeadingCapitalGainsTax,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.cgtTypeTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.taxableCapitalGains, "£1,234.56"),
            (2, TaxCalcBreakdown.assetsDisposalsAndInvestorsRelief, "£1,000.00"),
            (3, TaxCalcBreakdown.propertyAndInterest_LRT, "£3,600.00"),
            (4, TaxCalcBreakdown.propertyAndInterest_HRT, "£8,400.00"),
            (5, TaxCalcBreakdown.otherGains_LRT, "£2,200.00"),
            (6, TaxCalcBreakdown.otherGains_HRT, "£3,360.00"),
            (7, TaxCalcBreakdown.capitalGainsTaxAdj, "£123.45"),
            (8, TaxCalcBreakdown.foreignTaxCreditReliefOnCG, "−£2,345.67"),
            (9, TaxCalcBreakdown.taxOnGainsAlreadyPaid, "−£3,456.78"),
            (10, TaxCalcBreakdown.capitalGainsTaxDue, "£4,567.89"),
            (11, TaxCalcBreakdown.capitalGainsTaxOverpaid, "£234.56")
          )
        )

      }

      "have an other charges table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 10,
          expectedCaption = TaxCalcBreakdown.otherCharges,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.chargeTypeTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.totalStudentLoansRepaymentAmount, "£5,000.00"),
            (2, TaxCalcBreakdown.payeUnderpaymentsCodedOut, "£254.00"),
            (3, TaxCalcBreakdown.saUnderpaymentsCodedOut, "£400.00")
          )
        )
      }

      "have an additional deductions table" which {

        shouldHaveACorrectTableContent(view)(
          tableNumber = 11,
          expectedCaption = TaxCalcBreakdown.sectionHeadingAdditionalDeduc,
          expectedTableRows = Table(
            ("row index", "column 1", "column 2"),
            (0, TaxCalcBreakdown.deductionsTableHeader, TaxCalcBreakdown.amountTableHeader),
            (1, TaxCalcBreakdown.inYearAdjustmentCodedInLaterTaxYear, "−£900.00"),
            (2, TaxCalcBreakdown.employments, "−£100.00"),
            (3, TaxCalcBreakdown.ukPensions, "−£200.00"),
            (4, TaxCalcBreakdown.stateBenefits, "−£300.00"),
            (5, TaxCalcBreakdown.cis, "−£400.00"),
            (6, TaxCalcBreakdown.ukLandAndProperty, "−£500.00"),
            (7, TaxCalcBreakdown.specialWithholdingTax, "−£600.00"),
            (8, TaxCalcBreakdown.voidISAs, "−£700.00"),
            (9, TaxCalcBreakdown.BBSI, "−£800.00"),
            (10, TaxCalcBreakdown.totalDeductions, "£1,000.00")
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
          Message("C22218", "message18")
        ))))
      ), taxYear2017, backUrl)

      val document: Document = Jsoup.parse(view.body)

      document.select(messageContentSelector).size shouldBe 13
      document.select(messageContentSelector).get(0).text shouldBe "Tax due on gift aid payments exceeds your income tax charged so you are liable for gift aid tax"
      document.select(messageContentSelector).get(1).text shouldBe "Class 2 National Insurance has not been charged because your self-employed profits are under the small profit threshold"
      document.select(messageContentSelector).get(2).text shouldBe "One or more of your annual adjustments have not been applied because you have submitted additional income or expenses"
      document.select(messageContentSelector).get(3).text shouldBe "Your payroll giving amount has been included in your adjusted taxable income"
      document.select(messageContentSelector).get(4).text shouldBe "Employment related expenses are capped at the total amount of employment income"
      document.select(messageContentSelector).get(5).text shouldBe "This is a forecast of your annual income tax liability based on the information you have provided to date. Any overpayments of income tax will not be refundable until after you have submitted your final declaration"
      document.select(messageContentSelector).get(6).text shouldBe "Employment and Deduction related expenses have been limited to employment income."
      document.select(messageContentSelector).get(7).text shouldBe "Due to your employed earnings, paying Class 2 Voluntary may not be beneficial."
      document.select(messageContentSelector).get(8).text shouldBe "Your Class 4 has been adjusted for Class 2 due and primary Class 1 contributions."
      document.select(messageContentSelector).get(9).text shouldBe "Due to the level of your current income, you may not be eligible for Marriage Allowance and therefore it has not been included in this calculation."
      document.select(messageContentSelector).get(10).text shouldBe "Due to the level of your income, you are no longer eligible for Marriage Allowance and your claim will be cancelled."
      document.select(messageContentSelector).get(11).text shouldBe "There are one or more underpayments, debts or adjustments that have not been included in the calculation as they do not relate to data that HMRC holds."
      document.select(messageContentSelector).get(12).text shouldBe "The Capital Gains Tax has been included in the estimated annual liability calculation only, the actual amount of Capital Gains Tax will be in the final declaration calculation."
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
        document.select(messageContentSelector).text shouldBe "Your Basic Rate limit has been increased by £5,000.98 to £15,000.00 for Gift Aid payments"
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
        document.select(messageContentSelector).text shouldBe "Total loss from all income sources was capped at £1,000.00"
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
        document.select(messageContentSelector).text shouldBe "Your Basic Rate limit has been increased by £5,000.99 to £15,000.00 for Pension Contribution"
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

    "provided with message C22209" in  {

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
        document.select(messageContentSelector).text shouldBe "Your Basic Rate limit has been increased by £5,000.99 to £15,000.00 for Pension Contribution and Gift Aid payments"
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


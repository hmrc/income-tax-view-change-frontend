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

import assets.CalcBreakdownTestConstants
import assets.MessagesLookUp.TaxCalcBreakdown
import enums.Crystallised
import models.calculation.CalcDisplayModel
import org.jsoup.nodes.Element
import org.scalatest.exceptions.TestFailedException
import play.api.libs.iteratee.Input.Empty
import testUtils.ViewSpec
import views.html.taxCalcBreakdown

class TaxCalcBreakdownViewSpec extends ViewSpec {

  val backUrl = "/report-quarterly/income-and-expenses/view/calculation/2021"

  "The taxCalc breakdown view displayed with Welsh tax regime for the 2017 tax year" should {

    val taxYear = 2017

    lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
      CalcBreakdownTestConstants.calculationBillWelshModel,
      Crystallised), taxYear, backUrl)

    "have the correct tax regime: Wales" in new Setup(view) {
      val regime: Element = content.selectNth("h3", 1).selectFirst("span")
      regime.text() shouldBe TaxCalcBreakdown.regimeWales
    }

  }

  "The taxCalc breakdown view with Scotland tax regime" when {

    "provided with a calculation that is without Pay, pensions and profit, without Savings, without Dividends, " +
      "without Additional charges and without Additional deductions sections but has total tax due amount display " +
      "for the 2017 tax year" should {

      val taxYear = 2017

      lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.calculationBillScotlandModel,
        Crystallised), taxYear, backUrl)

      "have the correct tax regime: Scotland" in new Setup(view) {
        val regime: Element = content.selectNth("h3", 1).selectFirst("span")
        regime.text() shouldBe TaxCalcBreakdown.regimeScotland
      }
    }

    "provided with a calculation that is with Pay, pensions and profit table and with all three tax bands including " +
      "top rate displayed " +
      "for the 2017 tax year" should {

      val taxYear = 2017

      lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.scottishTaxBandModelJustPPP,
        Crystallised), taxYear, backUrl)

      "have the correct tax regime: Scotland" in new Setup(view) {
        val regime: Element = content.selectNth("h3", 1).selectFirst("span")
        regime.text() shouldBe TaxCalcBreakdown.regimeScotland
      }

      "have a Pay, pensions and profit table" which {

        "has all three table rows" in new Setup(view) {
          content hasTableWithCorrectSize(1, 5)
        }

        "has the correct heading" in new Setup(view) {
          content.h3.selectFirst("h3").text().contains(TaxCalcBreakdown.sectionHeadingPPP)
        }

        "has a basic rate threshold(SRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(0)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_SRT
          row.select("td").last().text() shouldBe "£2,000.00"
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_BRT
          row.select("td").last().text() shouldBe "£4,000.00"
        }

        "has a basic rate threshold(IRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_IRT
          row.select("td").last().text() shouldBe "£45,000.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_HRT
          row.select("td").last().text() shouldBe "£40,000.00"
        }

        "has a top rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_ART
          row.select("td").last().text() shouldBe "£22,500.00"
        }
      }
    }
  }

  "The taxCalc breakdown view with UK tax regime" when {

    "provided with a calculation that is without Pay, pensions ( without tax regime displayed) and profit, without Savings, without Dividends, " +
      "without Additional charges and without Additional deductions sections but has total tax due amount display " +
      "for the 2017 tax year" should {
      val taxYear = 2017

      lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.calculationBillTaxableIncomeZeroModel,
        Crystallised), taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe TaxCalcBreakdown.title
      }

      "have the correct heading" in new Setup(view) {
        content hasPageHeading TaxCalcBreakdown.heading(taxYear)
        content.h1.select(".heading-secondary").text() shouldBe TaxCalcBreakdown.subHeading(taxYear)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = content.getElementById("explanation")
        guidance.text() shouldBe TaxCalcBreakdown.guidance
      }
    }

    "provided with a calculation with all the sections displayed which includes Pay, pensions and profit ( with tax regime:UK displayed), Savings, Dividends," +
      "Additional charges and Additional deductions as well as the total tax due amount for the 2018 tax year" should {
      val taxYear = 2018

      lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.testCalcModelCrystallised,
        Crystallised), taxYear, backUrl)

      lazy val viewAllIncome = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.calculationAllIncomeSources,
        Crystallised), taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe TaxCalcBreakdown.title
      }

      "have the correct heading" in new Setup(view) {
        content hasPageHeading TaxCalcBreakdown.heading(taxYear)
        content.h1.select(".heading-secondary").text() shouldBe TaxCalcBreakdown.subHeading(taxYear)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = content.getElementById("explanation")
        guidance.text() shouldBe TaxCalcBreakdown.guidance
      }

      "have the correct tax regime: UK" in new Setup(view) {
        val regime: Element = content.selectNth("h3", 1).selectFirst("span")
        regime.text() shouldBe TaxCalcBreakdown.regime
      }

      "have a Pay, pensions and profit table" which {

        "has all three table rows" in new Setup(view) {
          content hasTableWithCorrectSize(1, 3)
        }

        "has the correct heading" in new Setup(view) {
          content.h3.selectFirst("h3").text().contains(TaxCalcBreakdown.sectionHeadingPPP)
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(0)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_BRT
          row.select("td").last().text() shouldBe "£4,000.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_HRT
          row.select("td").last().text() shouldBe "£40,000.00"
        }

        "has a additional rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_ART
          row.select("td").last().text() shouldBe "£22,500.00"
        }
      }

      "have an Savings table" which {

        "has all six table rows" in new Setup(view) {
          content hasTableWithCorrectSize(2, 6)
        }

        "has the correct heading" in new Setup(view) {
          content.table(2).h3.text() shouldBe TaxCalcBreakdown.sectionHeadingSavings
        }

        "has a Starting rate threshold(SSR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(0)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_SSR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a Basic rate band at nil rate threshold(ZRTBR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_ZRTBR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_BRT
          row.select("td").last().text() shouldBe "£2.00"
        }

        "has a Higher rate band at nil rate threshold(ZRTHR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_ZRTHR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_HRT
          row.select("td").last().text() shouldBe "£800.00"
        }

        "has a Additional rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_ART
          row.select("td").last().text() shouldBe "£5,000.00"
        }
      }

      "have a Dividends table" which {

        "has all six table rows" in new Setup(view) {
          content hasTableWithCorrectSize(3, 6)
        }

        "has the correct heading" in new Setup(view) {
          content.table(3).h3.text() shouldBe TaxCalcBreakdown.sectionHeadingDividends
        }

        "has a basic rate band at nil rate threshold(ZRTBR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(0)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_ZRTBR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_BRT
          row.select("td").last().text() shouldBe "£75.00"
        }

        "has a higher rate band at nil rate threshold(ZRTHR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_ZRTHR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_HRT
          row.select("td").last().text() shouldBe "£750.00"
        }

        "has a additional rate band at nil rate threshold(ZRTAR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_ZRTAR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a additional rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_ART
          row.select("td").last().text() shouldBe "£1,143.00"
        }


      }

      "have a Nic 4 table" which {

        "has all three table rows" in new Setup(view) {
          content hasTableWithCorrectSize(4, 7)
        }

        "has a Nic4 zero rate threshold(ZRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(0)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic4_ZRT
          row.select("td").last().text() shouldBe "£100.00"
        }

        "has a Nic4 basic rate band threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic4_BRT
          row.select("td").last().text() shouldBe "£200.00"
        }

        "has a Nic4 higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic4_HRT
          row.select("td").last().text() shouldBe "£300.00"
        }

        "has a giftAidTaxCharge line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.giftAidTaxCharge
          row.select("td").last().text() shouldBe "£400.00"
        }

        "has a totalPensionSavingCharges line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.totalPensionSavingCharges
          row.select("td").last().text() shouldBe "£500.00"
        }

        "has a statePensionLumpSum line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.statePensionLumpSum
          row.select("td").last().text() shouldBe "£600.00"
        }

        "has a totalStudentLoansRepaymentAmount line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(6)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.totalStudentLoansRepaymentAmount
          row.select("td").last().text() shouldBe "£700.00"
        }
      }

      "have no Tax reductions table and heading when there is no any reductions value" in new Setup(viewAllIncome) {
        val ex = intercept[TestFailedException](content.table(5))
        ex.getMessage shouldBe "table element not found"

      }

      "have a Tax reductions table" which {

        "has a Tax reductions heading" in new Setup(view) {
          content.table(5).h3.text() shouldBe TaxCalcBreakdown.sectionHeadingTaxReductions
        }

        "has all 13 table rows" in new Setup(view) {
          content hasTableWithCorrectSize(5, 13)
        }

        "has a Deficiency Relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(0)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.deficiencyRelief
          row.select("td").last().text() shouldBe "-£1,000.00"
        }

        "has a Venture Capital Trust relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.vctRelief
          row.select("td").last().text() shouldBe "-£2,000.00"
        }

        "has a Enterprise Investment Scheme relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.eisRelief
          row.select("td").last().text() shouldBe "-£3,000.00"
        }

        "has a Seed Enterprise Scheme relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.seedRelief
          row.select("td").last().text() shouldBe "-£4,000.00"
        }

        "has a Community Investment Tax Relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.citRelief
          row.select("td").last().text() shouldBe "-£5,000.00"
        }

        "has a Social Investment Tax Relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.sitRelief
          row.select("td").last().text() shouldBe "-£6,000.00"
        }

        "has a Maintenance and alimony paid line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(6)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.maintenanceRelief
          row.select("td").last().text() shouldBe "-£7,000.00"
        }

        "has a Relief for finance costs line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(7)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.reliefForFinanceCosts
          row.select("td").last().text() shouldBe "-£5,000.00"
        }

        "has a Notional tax from gains on life policies etc line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(8)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.notionalTax
          row.select("td").last().text() shouldBe "-£7,000.00"
        }

        "has a Foreign Tax Credit Relief with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(9)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.foreignTaxCreditRelief
          row.select("td").last().text() shouldBe "-£6,000.00"
        }

        "has a Relief claimed on a qualifying distribution line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(10)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.reliefClaimedOnQualifyingDis
          row.select("td").last().text() shouldBe "-£8,000.00"
        }

        "has a non deductible loan interest line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(11)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.nonDeductibleLoanInterest
          row.select("td").last().text() shouldBe "-£9,000.00"
        }

        "has a Income Tax due after tax reductions with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(12)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.incomeTaxDueAfterTaxReductions
          row.select("td").last().text() shouldBe "£2,000.00"
        }
      }

      "have no additional charges table and heading when there is no any other charges value" in new Setup(viewAllIncome) {
        val ex = intercept[TestFailedException](content.table(6))
        ex.getMessage shouldBe "table element not found"

      }

      "have an additional charges table" which {

        "has all four table rows" in new Setup(view) {
          content hasTableWithCorrectSize(7, 4)
        }

        "has the correct heading" in new Setup(view) {
          content.table(7).h3.text() shouldBe TaxCalcBreakdown.sectionHeadingAdditionalChar
        }

        "has a Tax reductions line with the correct value" in new Setup(view) {
          content.table(8).h3.text() shouldBe TaxCalcBreakdown.otherCharges
        }

        "has a Nic2 line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(0)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic2
          row.select("td").last().text() shouldBe "£10,000.00"
        }
      }

      "have an additional deductions table" which {

        "has one table row" in new Setup(view) {
          content hasTableWithCorrectSize(7, 4)
        }

        "has the correct heading" in new Setup(view) {
          content.table(9).h3.text() shouldBe TaxCalcBreakdown.sectionHeadingAdditionalDeduc
        }

        "has an employments line with the correct value" in new Setup(view) {
          val row: Element = content.table(9).select("tr").get(0)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.employments
          row.select("td").last().text() shouldBe "-£100.00"
        }

        "has a UK pensions line with the correct value" in new Setup(view) {
          val row: Element = content.table(9).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ukPensions
          row.select("td").last().text() shouldBe "-£200.00"
        }
        "has a state benefits line with the correct value" in new Setup(view) {
          val row: Element = content.table(9).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.stateBenefits
          row.select("td").last().text() shouldBe "-£300.00"
        }

        "has a CIS line with the correct value" in new Setup(view) {
          val row: Element = content.table(9).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.cis
          row.select("td").last().text() shouldBe "-£400.00"
        }

        "has a UK land and property line with the correct value" in new Setup(view) {
          val row: Element = content.table(9).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ukLandAndProperty
          row.select("td").last().text() shouldBe "-£500.00"
        }
        "has a Special Withholding Tax with the correct value" in new Setup(view) {
          val row: Element = content.table(9).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.specialWithholdingTax
          row.select("td").last().text() shouldBe "-£600.00"
        }
        "has a VoidISAs with the correct value" in new Setup(view) {
          val row: Element = content.table(9).select("tr").get(6)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.voidISAs
          row.select("td").last().text() shouldBe "-£700.00"
        }
        "has a UK banks and building societies line with the correct value" in new Setup(view) {
          val row: Element = content.table(9).select("tr").get(7)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.BBSI
          row.select("td").last().text() shouldBe "-£800.00"
        }

        "has a total deductions line with the correct value" in new Setup(view) {
          val row: Element = content.table(9).select("tr").get(8)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.totalDeductions
          row.select("td").last().text() shouldBe "£900.00"
        }
      }
    }
  }
}


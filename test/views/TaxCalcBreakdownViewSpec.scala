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

package views

import assets.CalcBreakdownTestConstants
import assets.Messages.TaxCalcBreakdown
import enums.Crystallised
import models.calculation.CalcDisplayModel
import org.jsoup.nodes.Element
import testUtils.ViewSpec
import views.html.taxCalcBreakdown

class TaxCalcBreakdownViewSpec extends ViewSpec {

  "The taxCalc breakdown view displayed with Welsh tax regime for the 2017 tax year" should {

    val taxYear = 2017

    lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
      CalcBreakdownTestConstants.calculationBillWelshModel,
      Crystallised), taxYear)

    "have the correct tax regime: Wales" in new Setup(view) {
      val regime: Element = content.select("span").get(1)
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
        Crystallised), taxYear)

      "have the correct tax regime: Scotland" in new Setup(view) {
        val regime: Element = content.select("span").get(1)
        regime.text() shouldBe TaxCalcBreakdown.regimeScotland
      }
    }

    "provided with a calculation that is with Pay, pensions and profit table and with all three tax bands including " +
      "top rate displayed " +
      "for the 2017 tax year" should {

      val taxYear = 2017

      lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.scottishTaxBandModelJustPPP,
        Crystallised), taxYear)

      "have the correct tax regime: Scotland" in new Setup(view) {
        val regime: Element = content.select("span").get(1)
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
          row.select("td").last().text() shouldBe "£2,000"
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_BRT
          row.select("td").last().text() shouldBe "£4,000"
        }

        "has a basic rate threshold(IRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_IRT
          row.select("td").last().text() shouldBe "£45,000"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_HRT
          row.select("td").last().text() shouldBe "£40,000"
        }

        "has a top rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_ART
          row.select("td").last().text() shouldBe "£22,500"
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
        Crystallised), taxYear)

      "have the correct title" in new Setup(view) {
        document title() shouldBe TaxCalcBreakdown.title
      }

      "have the correct back link" in new Setup(view) {
        content hasBackLinkTo controllers.routes.CalculationController.renderCalculationPage(taxYear).url
      }

      "have the correct heading" in new Setup(view) {
        content hasPageHeading TaxCalcBreakdown.heading(taxYear)
        content.h1.select(".heading-secondary").text() shouldBe TaxCalcBreakdown.subHeading(taxYear)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = content.select("h2").first()
        guidance.text() shouldBe TaxCalcBreakdown.guidance
      }

      "show the total tax due amount" in new Setup(view) {
        content.select(".total-heading").text() shouldBe s"${TaxCalcBreakdown.sectionHeadingTotal} £0"
      }
    }

    "provided with a calculation with all the sections displayed which includes Pay, pensions and profit ( with tax regime:UK displayed), Savings, Dividends," +
      "Additional charges and Additional deductions as well as the total tax due amount for the 2018 tax year" should {
      val taxYear = 2018

      lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.testCalcModelCrystallised,
        Crystallised), taxYear)

      "have the correct title" in new Setup(view) {
        document title() shouldBe TaxCalcBreakdown.title
      }

      "have the correct back link" in new Setup(view) {
        content hasBackLinkTo controllers.routes.CalculationController.renderCalculationPage(taxYear).url
      }

      "have the correct heading" in new Setup(view) {
        content hasPageHeading TaxCalcBreakdown.heading(taxYear)
        content.h1.select(".heading-secondary").text() shouldBe TaxCalcBreakdown.subHeading(taxYear)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = content.select("h2").first()
        guidance.text() shouldBe TaxCalcBreakdown.guidance
      }

      "have the correct tax regime: UK" in new Setup(view) {
        val regime: Element = content.select("span").get(1)
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
          row.select("td").last().text() shouldBe "£4,000"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_HRT
          row.select("td").last().text() shouldBe "£40,000"
        }

        "has a additional rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_ART
          row.select("td").last().text() shouldBe "£22,500"
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
          row.select("td").last().text() shouldBe "£0"
        }

        "has a Basic rate band at nil rate threshold(ZRTBR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_ZRTBR
          row.select("td").last().text() shouldBe "£0"
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_BRT
          row.select("td").last().text() shouldBe "£2"
        }

        "has a Higher rate band at nil rate threshold(ZRTHR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_ZRTHR
          row.select("td").last().text() shouldBe "£0"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_HRT
          row.select("td").last().text() shouldBe "£800"
        }

        "has a Additional rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_ART
          row.select("td").last().text() shouldBe "£5,000"
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
          row.select("td").last().text() shouldBe "£0"
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_BRT
          row.select("td").last().text() shouldBe "£75"
        }

        "has a higher rate band at nil rate threshold(ZRTHR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_ZRTHR
          row.select("td").last().text() shouldBe "£0"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_HRT
          row.select("td").last().text() shouldBe "£750"
        }

        "has a additional rate band at nil rate threshold(ZRTAR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_ZRTAR
          row.select("td").last().text() shouldBe "£0"
        }

        "has a additional rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_ART
          row.select("td").last().text() shouldBe "£1,143"
        }


      }

      "have an additional charges table" which {

        "has all four table rows" in new Setup(view) {
          content hasTableWithCorrectSize(4, 4)
        }

        "has the correct heading" in new Setup(view) {
          content.table(4).h3.text() shouldBe TaxCalcBreakdown.sectionHeadingAdditionalChar
        }

        "has a Nic4 zero rate threshold(ZRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(0)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic4_ZRT
          row.select("td").last().text() shouldBe "£100"
        }

        "has a Nic4 basic rate band threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic4_BRT
          row.select("td").last().text() shouldBe "£200"
        }

        "has a Nic4 higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic4_HRT
          row.select("td").last().text() shouldBe "£300"
        }

        "has a Nic2 line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic2
          row.select("td").last().text() shouldBe "£10,000"
        }
      }

      "have an additional deductions table" which {

        "has one table row" in new Setup(view) {
          content hasTableWithCorrectSize(5, 1)
        }

        "has the correct heading" in new Setup(view) {
          content.table(5).h3.text() shouldBe TaxCalcBreakdown.sectionHeadingAdditionalDeduc
        }

        "has a UK banks and building societies line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(0)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.BBSI
          row.select("td").last().text() shouldBe "£10,000"
        }
      }

      "have a total tax due amount" in new Setup(view) {
          content.select(".total-heading").text() shouldBe s"${TaxCalcBreakdown.sectionHeadingTotal} £543.21"
      }
    }
  }
}

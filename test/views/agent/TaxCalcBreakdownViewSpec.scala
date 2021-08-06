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

package views.agent

import assets.CalcBreakdownTestConstants
import assets.CalcBreakdownTestConstants.calculationDataSuccessModel
import assets.MessagesLookUp.TaxCalcBreakdown
import enums.Crystallised
import models.calculation.TaxDeductedAtSource.{Message, Messages}
import models.calculation.{AllowancesAndDeductions, CalcDisplayModel, CalcOverview, Calculation}
import models.financialTransactions.TransactionModel
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.exceptions.TestFailedException
import play.twirl.api.Html
import testUtils.ViewSpec
import views.html.agent.TaxCalcBreakdown

class TaxCalcBreakdownViewSpec extends ViewSpec {

  val backUrl = "/report-quarterly/income-and-expenses/view/agent/calculation/2021"

  val taxCalcBreakdown: TaxCalcBreakdown = app.injector.instanceOf[TaxCalcBreakdown]
  val testYear: Int = 2020
  val testCalcOverview: CalcOverview = CalcOverview(
    calculation = Calculation(
      crystallised = true,
      timestamp = Some("2020-04-06T12:34:56.789Z"),
      totalIncomeTaxAndNicsDue = Some(100.00),
      totalIncomeReceived = Some(150.00),
      allowancesAndDeductions = AllowancesAndDeductions(totalAllowancesAndDeductions = Some(25.00), totalReliefs = Some(25.00)),
      totalTaxableIncome = Some(30.00)
    ),
    transaction = Some(TransactionModel())
  )

  "The taxCalc breakdown view with Scotland tax regime" when {

    "provided with a calculation that is with Pay, pensions and profit table and with all three tax bands including " +
      "top rate displayed " +
      "for the 2017 tax year" should {

      val taxYear = 2017

      lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.scottishTaxBandModelJustPPP,
        Crystallised), taxYear, backUrl)

      "have a Pay, pensions and profit table" which {

        "has all three table rows" in new Setup(view) {
          content hasTableWithCorrectSize(1, 6)
        }

        "has the correct heading" in new Setup(view) {
          content.h2.selectFirst("h2").text().contains(TaxCalcBreakdown.sectionHeadingPPP)
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.pPPScotlandTableHead
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "has a basic rate threshold(SRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_SRT
          row.select("td").last().text() shouldBe "£2,000.00"
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_BRT
          row.select("td").last().text() shouldBe "£4,000.00"
        }

        "has a basic rate threshold(IRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_IRT
          row.select("td").last().text() shouldBe "£45,000.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_HRT
          row.select("td").last().text() shouldBe "£40,000.00"
        }

        "has a top rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_Scot_ART
          row.select("td").last().text() shouldBe "£22,500.00"
        }
      }
    }

    "provided with a calculation that is with Lump sums table and with all three tax bands including " +
      "top rate displayed " +
      "for the 2017 tax year" should {

      val taxYear = 2017

      lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.scottishTaxBandModelJustLs,
        Crystallised), taxYear, backUrl)

      "have a Lump sums table" which {

        "has all three table rows" in new Setup(view) {
          content hasTableWithCorrectSize(1, 6)
        }

        "has the correct heading" in new Setup(view) {
          content.h2.selectFirst("h2").text().contains(TaxCalcBreakdown.sectionHeadingLumpSums)
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.pPPScotlandTableHead
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "has a basic rate threshold(SRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ls_Scot_SRT
          row.select("td").last().text() shouldBe "£2,000.00"
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ls_Scot_BRT
          row.select("td").last().text() shouldBe "£4,000.00"
        }

        "has a basic rate threshold(IRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ls_Scot_IRT
          row.select("td").last().text() shouldBe "£45,000.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ls_Scot_HRT
          row.select("td").last().text() shouldBe "£40,000.00"
        }

        "has a top rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ls_Scot_ART
          row.select("td").last().text() shouldBe "£22,500.00"
        }
      }
    }


    "provided with a calculation that is with Gains on life policies table and with all three tax bands including " +
      "top rate displayed " +
      "for the 2017 tax year" should {

      val taxYear = 2017

      lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.scottishTaxBandModelJustGols,
        Crystallised), taxYear, backUrl)

      "have a Pay, pensions and profit table" which {

        "has all three table rows" in new Setup(view) {
          content hasTableWithCorrectSize(1, 6)
        }

        "has the correct heading" in new Setup(view) {
          content.h2.selectFirst("h2").text().contains(TaxCalcBreakdown.sectionHeadingGainsOnLifePolicies)
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.lsTableHeader
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "has a basic rate threshold(SRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.gols_Scot_SRT
          row.select("td").last().text() shouldBe "£2,000.00"
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.gols_Scot_BRT
          row.select("td").last().text() shouldBe "£4,000.00"
        }

        "has a basic rate threshold(IRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.gols_Scot_IRT
          row.select("td").last().text() shouldBe "£45,000.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.gols_Scot_HRT
          row.select("td").last().text() shouldBe "£40,000.00"
        }

        "has a top rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.gols_Scot_ART
          row.select("td").last().text() shouldBe "£22,500.00"
        }
      }
    }
  }


  "The taxCalc breakdown view with UK tax regime" when {

    "provided with a calculation that is without Pay, pensions and profit, without Savings, without Dividends, " +
      "without Additional charges and without Additional deductions sections but has total tax due amount display " +
      "for the 2017 tax year" should {
      val taxYear = 2017

      lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.calculationBillTaxableIncomeZeroModel,
        Crystallised), taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe TaxCalcBreakdown.agentTitle
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

    "provided with a calculation with all the sections displayed which includes Pay, pensions and profit, Savings, Dividends," +
      " lumpsums and gainsOnLifePolicies, Additional charges and Additional deductions as well as the total tax due amount for the 2018 tax year" should {
      val taxYear = 2018

      lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.testCalcModelCrystallised,
        Crystallised), taxYear, backUrl)

      lazy val viewNic2 = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.testCalcModelNic2,
        Crystallised), taxYear, backUrl)

      lazy val viewAllIncome = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.calculationAllIncomeSources,
        Crystallised), taxYear, backUrl)

      lazy val zeroIncome = taxCalcBreakdown(CalcDisplayModel("", 1,
        CalcBreakdownTestConstants.testCalcModelZeroIncome,
        Crystallised), taxYear, backUrl)

      "have the correct title" in new Setup(view) {
        document title() shouldBe TaxCalcBreakdown.agentTitle
      }

      "have the correct heading" in new Setup(view) {
        content hasPageHeading TaxCalcBreakdown.heading(taxYear)
        content.h1.select(".heading-secondary").text() shouldBe TaxCalcBreakdown.subHeading(taxYear)
      }

      "have the correct guidance" in new Setup(view) {
        val guidance: Element = content.getElementById("explanation")
        guidance.text() shouldBe TaxCalcBreakdown.guidance
      }

      "have a Pay, pensions and profit table" which {

        "has all three table rows" in new Setup(view) {
          content hasTableWithCorrectSize(1, 4)
        }

        "has the correct heading" in new Setup(view) {
          content.h2.selectFirst("h2").text().contains(TaxCalcBreakdown.sectionHeadingPPP)
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.pPPTableHead
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "have the correct UK regime rate message" in new Setup(view) {
          val regime: Element = content.selectNth("caption", 1).selectFirst("p")
          regime.text() shouldBe TaxCalcBreakdown.regimeUkRateText
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_BRT
          row.select("td").last().text() shouldBe "£4,000.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_HRT
          row.select("td").last().text() shouldBe "£40,000.00"
        }

        "has a additional rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(1).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.pPP_ART
          row.select("td").last().text() shouldBe "£22,500.00"
        }
      }

      "have no Pay, pensions and profit Income" which {
        "has no Pay, pensions and profit heading" in new Setup(zeroIncome) {
          document.select("h2").text().contains(TaxCalcBreakdown.sectionHeadingPPP) shouldBe false
        }
      }

      "have an Savings table" which {

        "has all six table rows" in new Setup(view) {
          content hasTableWithCorrectSize(2, 7)
        }

        "has the correct heading" in new Setup(view) {
          content.table(2).h2.text() shouldBe TaxCalcBreakdown.sectionHeadingSavings
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.pPPTableHead
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "has a Starting rate threshold(SSR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_SSR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a Basic rate band at nil rate threshold(ZRTBR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_ZRTBR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_BRT
          row.select("td").last().text() shouldBe "£2.00"
        }

        "has a Higher rate band at nil rate threshold(ZRTHR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_ZRTHR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_HRT
          row.select("td").last().text() shouldBe "£800.00"
        }

        "has a Additional rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(2).select("tr").get(6)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.saving_ART
          row.select("td").last().text() shouldBe "£5,000.00"
        }
      }

      "have no Savings and Gains Income" which {
        "has no Savings and Gains heading" in new Setup(zeroIncome) {
          document.select("h2").text().contains(TaxCalcBreakdown.sectionHeadingSavings) shouldBe false
        }
      }

      "have a Dividends table" which {

        "has all six table rows" in new Setup(view) {
          content hasTableWithCorrectSize(3, 7)
        }

        "has the correct heading" in new Setup(view) {
          content.table(3).h2.text() shouldBe TaxCalcBreakdown.sectionHeadingDividends
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.pPPTableHead
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "has a basic rate band at nil rate threshold(ZRTBR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_ZRTBR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_BRT
          row.select("td").last().text() shouldBe "£75.00"
        }

        "has a higher rate band at nil rate threshold(ZRTHR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_ZRTHR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_HRT
          row.select("td").last().text() shouldBe "£750.00"
        }

        "has a additional rate band at nil rate threshold(ZRTAR) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_ZRTAR
          row.select("td").last().text() shouldBe "£0.00"
        }

        "has a additional rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(3).select("tr").get(6)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.dividend_ART
          row.select("td").last().text() shouldBe "£1,143.00"
        }
      }

      "have no Dividend Income" which {
        "has no dividend heading" in new Setup(zeroIncome) {
          document.select("h2").text().contains(TaxCalcBreakdown.sectionHeadingDividends) shouldBe false
        }
      }

      "have a Employment lump sums table" which {
        "has all three table rows" in new Setup(view) {
          content hasTableWithCorrectSize(4, 4)
        }

        "has the correct heading" in new Setup(view) {
          content.h2.selectFirst("h2").text().contains(TaxCalcBreakdown.sectionHeadingLumpSums
          )
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.pPPTableHead
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "have the correct UK regime rate message" in new Setup(view) {
          val regime: Element = content.selectNth("table", 4).selectFirst("p")
          regime.text() shouldBe TaxCalcBreakdown.regimeUkRateText
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ls_BRT
          row.select("td").last().text() shouldBe "£4,000.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ls_HRT
          row.select("td").last().text() shouldBe "£40,000.00"
        }

        "has a additional rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(4).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ls_ART
          row.select("td").last().text() shouldBe "£22,500.00"
        }
      }

      "have no Lump Sum Income" which {
        "has no Lump Sum heading" in new Setup(zeroIncome) {
          document.select("h2").text().contains(TaxCalcBreakdown.sectionHeadingLumpSums) shouldBe false
        }
      }

      "have a Gains on life policies table" which {

        "has all three table rows" in new Setup(view) {
          content hasTableWithCorrectSize(5, 4)
        }

        "has the correct heading" in new Setup(view) {
          content.h2.selectFirst("h2").text().contains(TaxCalcBreakdown.sectionHeadingGainsOnLifePolicies)
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.pPPTableHead
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "has a basic rate threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.gols_BRT
          row.select("td").last().text() shouldBe "£4,000.00"
        }

        "has a higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.gols_HRT
          row.select("td").last().text() shouldBe "£40,000.00"
        }

        "has a additional rate threshold(ART) line with the correct value" in new Setup(view) {
          val row: Element = content.table(5).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.gols_ART
          row.select("td").last().text() shouldBe "£22,500.00"
        }
      }

      "have no Gains on Life Policy Income" which {
        "has no Gains on Life Policy heading" in new Setup(zeroIncome) {
          document.select("h2").text().contains(TaxCalcBreakdown.sectionHeadingGainsOnLifePolicies) shouldBe false
        }
      }

      "have a Nic 4 table" which {

        "has all three table rows" in new Setup(view) {
          content hasTableWithCorrectSize(6, 8)
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.pPPTableHead
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "has a Nic4 zero rate threshold(ZRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(6).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic4_ZRT
          row.select("td").last().text() shouldBe "£100.00"
        }

        "has a Nic4 basic rate band threshold(BRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(6).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic4_BRT
          row.select("td").last().text() shouldBe "£200.00"
        }

        "has a Nic4 higher rate threshold(HRT) line with the correct value" in new Setup(view) {
          val row: Element = content.table(6).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic4_HRT
          row.select("td").last().text() shouldBe "£300.00"
        }

        "has a giftAidTaxCharge line with the correct value" in new Setup(view) {
          val row: Element = content.table(6).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.giftAidTaxCharge
          row.select("td").last().text() shouldBe "£400.00"
        }

        "has a totalPensionSavingCharges line with the correct value" in new Setup(view) {
          val row: Element = content.table(6).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.totalPensionSavingCharges
          row.select("td").last().text() shouldBe "£500.00"
        }

        "has a statePensionLumpSum line with the correct value" in new Setup(view) {
          val row: Element = content.table(6).select("tr").get(6)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.statePensionLumpSum
          row.select("td").last().text() shouldBe "£600.00"
        }

        "has a totalStudentLoansRepaymentAmount line with the correct value" in new Setup(view) {
          val row: Element = content.table(6).select("tr").get(7)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.totalStudentLoansRepaymentAmount
          row.select("td").last().text() shouldBe "£700.00"
        }
      }

      "have no Tax reductions table and heading when there is no any reductions value" in new Setup(viewAllIncome) {
        val ex = intercept[TestFailedException](content.table(7))
        ex.getMessage shouldBe "table element not found"

      }

      "have a Tax reductions table" which {

        "has a Tax reductions heading" in new Setup(view) {
          content.table(7).h2.text() shouldBe TaxCalcBreakdown.sectionHeadingTaxReductions
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.pPPTableHead
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "has all 13 table rows" in new Setup(view) {
          content hasTableWithCorrectSize(7, 14)
        }

        "has a Deficiency Relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.deficiencyRelief
          row.select("td").last().text() shouldBe "−£1,000.00"
        }

        "has a Venture Capital Trust relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.vctRelief
          row.select("td").last().text() shouldBe "−£2,000.00"
        }

        "has a Enterprise Investment Scheme relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.eisRelief
          row.select("td").last().text() shouldBe "−£3,000.00"
        }

        "has a Seed Enterprise Scheme relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.seedRelief
          row.select("td").last().text() shouldBe "−£4,000.00"
        }

        "has a Community Investment Tax Relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.citRelief
          row.select("td").last().text() shouldBe "−£5,000.00"
        }

        "has a Social Investment Tax Relief line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(6)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.sitRelief
          row.select("td").last().text() shouldBe "−£6,000.00"
        }

        "has a Maintenance and alimony paid line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(7)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.maintenanceRelief
          row.select("td").last().text() shouldBe "−£7,000.00"
        }

        "has a Relief for finance costs line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(8)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.reliefForFinanceCosts
          row.select("td").last().text() shouldBe "−£5,000.00"
        }

        "has a Notional tax from gains on life policies etc line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(9)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.notionalTax
          row.select("td").last().text() shouldBe "−£7,000.00"
        }

        "has a Foreign Tax Credit Relief with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(10)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.foreignTaxCreditRelief
          row.select("td").last().text() shouldBe "−£6,000.00"
        }

        "has a Relief claimed on a qualifying distribution line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(11)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.reliefClaimedOnQualifyingDis
          row.select("td").last().text() shouldBe "−£8,000.00"
        }

        "has a non deductible loan interest line with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(12)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.nonDeductibleLoanInterest
          row.select("td").last().text() shouldBe "−£9,000.00"
        }

        "has a Income Tax due after tax reductions with the correct value" in new Setup(view) {
          val row: Element = content.table(7).select("tr").get(13)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.incomeTaxDueAfterTaxReductions
          row.select("td").last().text() shouldBe "£2,000.00"
        }
      }

      "have no additional charges table and heading when there is no any other charges value" in new Setup(viewAllIncome) {
        val ex = intercept[TestFailedException](content.table(8))
        ex.getMessage shouldBe "table element not found"

      }

      "have an additional charges table" which {

        "has all four table rows" in new Setup(view) {
          content hasTableWithCorrectSize(8, 5)
        }

        "has the correct heading" in new Setup(view) {
          content.table(8).h2.text() shouldBe TaxCalcBreakdown.sectionHeadingAdditionalChar
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.pPPTableHead
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "has a Tax reductions line with the correct value" in new Setup(view) {
          content.table(9).h2.text() shouldBe TaxCalcBreakdown.otherCharges
        }

        "has a Voluntary Nic line with the correct value" in new Setup(view) {
          val row: Element = content.table(8).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.VoluntaryNic2
          row.select("td").last().text() shouldBe "£10,000.00"
        }

        "has a Nic2 line with the correct value" in new Setup(viewNic2) {
          val row: Element = content.table(8).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.Nic2
          row.select("td").last().text() shouldBe "£10,000.00"
        }
      }

      "have an additional deductions table" which {

        "has one table row" in new Setup(view) {
          content hasTableWithCorrectSize(8, 5)
        }

        "has the correct heading" in new Setup(view) {
          content.table(10).h2.text() shouldBe TaxCalcBreakdown.sectionHeadingAdditionalDeduc
        }

        "has a table header section" in new Setup(view) {
          val row: Element = content.table().select("tr").get(0)
          row.select("th").first().text() shouldBe TaxCalcBreakdown.pPPTableHead
          row.select("th").last().text() shouldBe TaxCalcBreakdown.amountTableHeader
        }

        "has an employments line with the correct value" in new Setup(view) {
          val row: Element = content.table(10).select("tr").get(1)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.employments
          row.select("td").last().text() shouldBe "−£100.00"
        }

        "has a UK pensions line with the correct value" in new Setup(view) {
          val row: Element = content.table(10).select("tr").get(2)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ukPensions
          row.select("td").last().text() shouldBe "−£200.00"
        }
        "has a state benefits line with the correct value" in new Setup(view) {
          val row: Element = content.table(10).select("tr").get(3)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.stateBenefits
          row.select("td").last().text() shouldBe "−£300.00"
        }

        "has a CIS line with the correct value" in new Setup(view) {
          val row: Element = content.table(10).select("tr").get(4)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.cis
          row.select("td").last().text() shouldBe "−£400.00"
        }

        "has a UK land and property line with the correct value" in new Setup(view) {
          val row: Element = content.table(10).select("tr").get(5)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.ukLandAndProperty
          row.select("td").last().text() shouldBe "−£500.00"
        }
        "has a Special Withholding Tax with the correct value" in new Setup(view) {
          val row: Element = content.table(10).select("tr").get(6)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.specialWithholdingTax
          row.select("td").last().text() shouldBe "−£600.00"
        }
        "has a VoidISAs with the correct value" in new Setup(view) {
          val row: Element = content.table(10).select("tr").get(7)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.voidISAs
          row.select("td").last().text() shouldBe "−£700.00"
        }
        "has a UK banks and building societies line with the correct value" in new Setup(view) {
          val row: Element = content.table(10).select("tr").get(8)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.BBSI
          row.select("td").last().text() shouldBe "−£800.00"
        }

        "has a total deductions line with the correct value" in new Setup(view) {
          val row: Element = content.table(10).select("tr").get(9)
          row.select("td").first().text() shouldBe TaxCalcBreakdown.totalDeductions
          row.select("td").last().text() shouldBe "£900.00"
        }
      }
    }
  }

	"The tax calc view should display messages" when {

		"provided with all matching generic messages" in {
			val taxYear = 2017
			val displayModel = calculationDataSuccessModel.copy(messages = Some(Messages(
				Some(Seq(
					Message("C22202", "message2"),
					Message("C22203", "message3"),
					Message("C22206", "message6"),
					Message("C22207", "message7"),
					Message("C22210", "message10"),
					Message("C22211", "message11")
				))
			)))

			lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
				displayModel,
				Crystallised), taxYear, backUrl)

			val document: Document = Jsoup.parse(view.body)

			document.select("div.panel-border-wide").size shouldBe 6
			document.select("div.panel-border-wide").get(0).text shouldBe "Tax due on gift aid payments exceeds your income tax charged so you are liable for gift aid tax"
			document.select("div.panel-border-wide").get(1).text shouldBe "Class 2 National Insurance has not been charged because your self-employed profits are under the small profit threshold"
			document.select("div.panel-border-wide").get(2).text shouldBe "One or more of your annual adjustments have not been applied because you have submitted additional income or expenses"
			document.select("div.panel-border-wide").get(3).text shouldBe "Your payroll giving amount has been included in your adjusted taxable income"
			document.select("div.panel-border-wide").get(4).text shouldBe "Employment related expenses are capped at the total amount of employment income"
			document.select("div.panel-border-wide").get(5).text shouldBe "This is a forecast of your annual income tax liability based on the information you have provided to date. Any overpayments of income tax will not be refundable until after you have submitted your final declaration"
		}

		"provided with message C22201" in {
			val taxYear = 2017
			val displayModel = calculationDataSuccessModel.copy(messages = Some(Messages(
				Some(Seq(
					Message("C22201", "message"),
				))
			)))

			lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
				displayModel,
				Crystallised), taxYear, backUrl)

			val document: Document = Jsoup.parse(view.body)

			document.select("div.panel-border-wide").size shouldBe 1
			document.select("div.panel-border-wide").text shouldBe "Your Basic Rate limit has been increased by £5,000.99 to £15,000.00 for Gift Aid payments"
		}

		"provided with message C22205" in {
			val taxYear = 2017
			val displayModel = calculationDataSuccessModel.copy(messages = Some(Messages(
				Some(Seq(
					Message("C22205", "message"),
				))
			)),
				allowancesAndDeductions = AllowancesAndDeductions(lossesAppliedToGeneralIncome = Some(1000.0))
			)

			lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
				displayModel,
				Crystallised), taxYear, backUrl)

			val document: Document = Jsoup.parse(view.body)

			document.select("div.panel-border-wide").size shouldBe 1
			document.select("div.panel-border-wide").text shouldBe "Total loss from all income sources was capped at £1,000.00"
		}

		"provided with message C22208" in {
			val taxYear = 2017
			val displayModel = calculationDataSuccessModel.copy(messages = Some(Messages(
				Some(Seq(
					Message("C22208", "message"),
				))
			)))

			lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
				displayModel,
				Crystallised), taxYear, backUrl)

			val document: Document = Jsoup.parse(view.body)

			document.select("div.panel-border-wide").size shouldBe 1
			document.select("div.panel-border-wide").text shouldBe "Your Basic Rate limit has been increased by £5,000.99 to £15,000.00 for Pension Contribution"
		}

		"provided with message C22209" in {
			val taxYear = 2017
			val displayModel = calculationDataSuccessModel.copy(messages = Some(Messages(
				Some(Seq(
					Message("C22209", "message"),
				))
			)))

			lazy val view = taxCalcBreakdown(CalcDisplayModel("", 1,
				displayModel,
				Crystallised), taxYear, backUrl)

			val document: Document = Jsoup.parse(view.body)

			document.select("div.panel-border-wide").size shouldBe 1
			document.select("div.panel-border-wide").text shouldBe "Your Basic Rate limit has been increased by £5,000.99 to £15,000.00 for Pension Contribution and Gift Aid payments"
		}
	}
}


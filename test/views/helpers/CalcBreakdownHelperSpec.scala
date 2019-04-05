/*
 * Copyright 2019 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.CalcBreakdownTestConstants.{busPropBRTCalcDataModel, _}
import assets.EstimatesTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.Messages
import auth.MtdItUser
import ch.qos.logback.core.pattern.color.MagentaCompositeConverter
import config.FrontendAppConfig
import implicits.ImplicitCurrencyFormatter._
import models.calculation._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport


class CalcBreakdownHelperSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val bizAndPropertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), businessAndPropertyAligned)(FakeRequest())
  val bizUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), singleBusinessIncome)(FakeRequest())
  val propertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), propertyIncomeOnly)(FakeRequest())


  private def pageSetup(calcDataModel: CalculationDataModel, user: MtdItUser[_]) = new {
    lazy val page: HtmlFormat.Appendable =
      views.html.helpers.calcBreakdownHelper(calculationDisplaySuccessModel(calcDataModel), None, testYear)(FakeRequest(), applicationMessages, mockAppConfig, user)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    implicit val model: CalculationDataModel = calcDataModel

    def personalAllowance: String = "-" + (
      model.personalAllowance
      ).toCurrencyString

    def savingsAllowance: String = "-" + (
      model.savingsAllowanceSummaryData
      ).toCurrencyString
  }

  val basicModel = CalculationDataModel(
    None,0, 0, 0, 0, 0,
    IncomeReceivedModel(0, 0, 0, 0),
    SavingsAndGainsModel(0, Seq(BandModel(0, 0, 0, "SSR"),BandModel(0, 0, 0, "ZRT"),BandModel(0, 0, 0, "BRT"),BandModel(0, 0, 0, "HRT"),BandModel(0, 0, 0, "ART"))),
    DividendsModel(0, Seq(DividendsBandModel("basic band",0, None, None, 0,0))),
    GiftAidModel(0,0,0),
    NicModel(0, 0),
    None
  )

  val allSavingInterestModel: CalculationDataModel = basicModel.copy(
    savingsAndGains = SavingsAndGainsModel(
      0,
      Seq(
        BandModel(5000, 0.0, 5000, "SSR"),
        BandModel(4000, 0.0, 4000, "ZRT"),
        BandModel(3000, 20, 600, "BRT"),
        BandModel(2000, 40, 800, "HRT"),
        BandModel(1000, 45, 450, "ART")
      )
    )
  )

  "saving interests" should {


    def getBandText(document: Document, bandName : String) : Option[String] = {
      Option(document.getElementById(s"$bandName-it-calc-heading")).map(_.text)
    }

    def getBandAmountText(document: Document, bandName : String) : Option[String] = {
      Option(document.getElementById(s"$bandName-amount")).map(_.text)
    }

    "not display all savings interest bands when no band have income" in {
      val setup = pageSetup(basicModel, bizAndPropertyUser)
      import setup._

      getBandText(document, "srtSi") shouldBe None
      getBandText(document, "zrtSi") shouldBe None
      getBandText(document, "brtSi") shouldBe None
      getBandText(document, "hrtSi") shouldBe None
      getBandText(document, "artSi") shouldBe None
    }

    "display all savings interests bands when all bands have income" in {
      val setup = pageSetup(allSavingInterestModel, bizAndPropertyUser)
      import setup._

      getBandText(document, "srtSi") shouldBe Some("Savings Interest (£5,000 at 0.0%)")
      getBandText(document, "zrtSi") shouldBe Some("Savings Interest (£4,000 at 0.0%)")
      getBandText(document, "brtSi") shouldBe Some("Savings Interest (£3,000 at 20%)")
      getBandText(document, "hrtSi") shouldBe Some("Savings Interest (£2,000 at 40%)")
      getBandText(document, "artSi") shouldBe Some("Savings Interest (£1,000 at 45%)")

      getBandAmountText(document, "srtSi") shouldBe Some("£5,000")
      getBandAmountText(document, "zrtSi") shouldBe Some("£4,000")
      getBandAmountText(document, "brtSi") shouldBe Some("£600")
      getBandAmountText(document, "hrtSi") shouldBe Some("£800")
      getBandAmountText(document, "artSi") shouldBe Some("£450")
    }

  }
  "The Calculation Breakdown view on the Estimate Page" should {

    val messages = new Messages.Calculation(testYear)

    "have a Calculation Breakdown section" that {

      "for users with both a property and a business" which {

        "have just the starter rate of tax" should {
          val setup = pageSetup(scottishBandModelJustSRT, bizAndPropertyUser)
          import setup._
          val total = (
            model.incomeReceived.selfEmployment +
              model.incomeReceived.ukProperty +
              model.incomeReceived.bankBuildingSocietyInterest
            ).toCurrencyString

          s"have a business profit section amount of $total" in {
            document.getElementById("business-profit-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.businessProfitAndSaving
            document.getElementById("business-profit").text shouldBe total
          }

          s"have a personal allowance amount of ${model.personalAllowance}" in {
            document.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowance
            document.getElementById("personal-allowance").text shouldBe personalAllowance
          }

          s"have a savings allowance amount of ${model.savingsAllowanceSummaryData}" in {
            document.getElementById("savings-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.savingsAllownce
            document.getElementById("savings-allowance").text shouldBe savingsAllowance
          }

          "not show the additional allowances section" in {
            document.getElementById("additionalAllowances") shouldBe null
          }

          s"have a taxable income amount of ${model.taxableIncomeTaxIncome.toCurrencyString}" in {
            document.getElementById("taxable-income-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.yourTaxableIncome
            document.getElementById("taxable-income").text shouldBe model.taxableIncomeTaxIncome.toCurrencyString
          }

          s"have a savings income amount of ${model.taxableSavingsIncome.toCurrencyString}" in {
            document.getElementById("taxable-savings-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.yourTaxableSavings
            document.getElementById("taxable-savings").text shouldBe model.taxableSavingsIncome.toCurrencyString
          }

          s"have an Income Tax section" which {
            val srtBand = model.payAndPensionsProfitBands.find(_.name == "SRT").get

            "has the correct amount of income taxed from payPensionsProfit at SRT" in {
              document.getElementById("SRTPpp-it-calc-heading").text shouldBe s"Pay, Pensions, Profit Income Tax (${srtBand.income.toCurrencyString} at ${srtBand.rate}%)"
            }
            "has the correct tax charged at SRT" in {
              document.getElementById("SRTPpp-amount").text shouldBe srtBand.taxAmount.toCurrencyString
            }
          }
          s"have a National Insurance Class 2 amount of ${model.nic.class2}" in {
            document.getElementById("nic2-amount-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.nic2
            document.getElementById("nic2-amount").text shouldBe model.nic.class2.toCurrencyString
          }
          s"have a National Insurance Class 4 amount of ${model.nic.class4}" in {
            document.getElementById("nic4-amount-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.nic4
            document.getElementById("nic4-amount").text shouldBe model.nic.class4.toCurrencyString
          }
          s"have a Tax reliefs amount of ${model.taxReliefs}" in {
            document.getElementById("tax-relief-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.reliefs
            document.getElementById("tax-relief").text shouldBe "-" + model.taxReliefs.toCurrencyString
          }
          s"have a total tax estimate of ${model.totalIncomeTaxNicYtd}" in {

            document.getElementById("total-estimate-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.total
            document.getElementById("total-estimate").text shouldBe model.totalIncomeTaxNicYtd.toCurrencyString
          }
        }

        "have the basic rate of tax" should {

          val setup = pageSetup(busPropBRTCalcDataModel, bizAndPropertyUser)
          import setup._
          val total = (model.incomeReceived.selfEmployment + model.incomeReceived.ukProperty + model.incomeReceived.bankBuildingSocietyInterest).toCurrencyString

          s"have a national regime section" in {
            document.getElementById("national-regime-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.nationalRegime
            document.getElementById("national-regime").text shouldBe "Scotland"
          }

          s"have a business profit section amount of $total" in {
            document.getElementById("business-profit-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.businessProfit
            document.getElementById("business-profit").text shouldBe total
          }

          s"have a personal allowance amount of ${model.personalAllowance}" in {
            document.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowance
            document.getElementById("personal-allowance").text shouldBe personalAllowance
          }

          s"have a savings allowance amount of ${model.savingsAllowanceSummaryData}" in {
            Option(document.getElementById("savings-allowance-heading")) shouldBe None
          }

          "not show the additional allowances section" in {
            document.getElementById("additionalAllowances") shouldBe null
          }

          s"have a taxable income amount of ${model.taxableIncomeTaxIncome.toCurrencyString}" in {
            document.getElementById("taxable-income-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.yourTaxableIncome
            document.getElementById("taxable-income").text shouldBe model.taxableIncomeTaxIncome.toCurrencyString
          }

          s"have an Income Tax from payPensionsProfit section" which {
            val brtBand = model.payAndPensionsProfitBands.find(_.name == "BRT").get

            "has the correct amount of income taxed at BRT" in {
              document.getElementById("BRTPpp-it-calc-heading").text shouldBe s"Pay, Pensions, Profit Income Tax (${brtBand.income.toCurrencyString} at ${brtBand.rate}%)"
            }
            "has the correct tax charged at BRT" in {
              document.getElementById("BRTPpp-amount").text shouldBe brtBand.taxAmount.toCurrencyString
            }
          }
          s"have a National Insurance Class 2 amount of ${model.nic.class2}" in {
            document.getElementById("nic2-amount-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.nic2
            document.getElementById("nic2-amount").text shouldBe model.nic.class2.toCurrencyString
          }
          s"have a National Insurance Class 4 amount of ${model.nic.class4}" in {
            document.getElementById("nic4-amount-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.nic4
            document.getElementById("nic4-amount").text shouldBe model.nic.class4.toCurrencyString
          }
          s"have a Tax reliefs amount of ${model.taxReliefs}" in {
            document.getElementById("tax-relief-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.reliefs
            document.getElementById("tax-relief").text shouldBe "-" + model.taxReliefs.toCurrencyString
          }
          s"have a total tax estimate of ${model.totalIncomeTaxNicYtd}" in {

            document.getElementById("total-estimate-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.total
            document.getElementById("total-estimate").text shouldBe model.totalIncomeTaxNicYtd.toCurrencyString
          }
        }

        "have just the IRT rate of tax" should {
          val setup = pageSetup(scottishBandModelIRT, bizAndPropertyUser)
          import setup._

          val total = (
            model.incomeReceived.selfEmployment +
              model.incomeReceived.ukProperty +
              model.incomeReceived.bankBuildingSocietyInterest
            ).toCurrencyString

          s"have a business profit section amount of $total" in {
            document.getElementById("business-profit-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.businessProfit
            document.getElementById("business-profit").text shouldBe total
          }

          s"have a personal allowance amount of ${model.personalAllowance}" in {
            document.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowance
            document.getElementById("personal-allowance").text shouldBe personalAllowance
          }


          "not show the additional allowances section" in {
            document.getElementById("additionalAllowances") shouldBe null
          }

          s"have a taxable income amount of ${model.taxableIncomeTaxIncome.toCurrencyString}" in {
            document.getElementById("taxable-income-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.yourTaxableIncome
            document.getElementById("taxable-income").text shouldBe model.taxableIncomeTaxIncome.toCurrencyString
          }

          s"have an Income Tax from payPensionsProfit section" which {
            val irtBand = model.payAndPensionsProfitBands.find(_.name == "IRT").get

            "has a SRT section" in {
              document.getElementById("SRTPpp-section") should not be null
            }

            "has a BRT section" in {
              document.getElementById("BRTPpp-section") should not be null
            }

            "has the correct amount of income taxed at IRT" in {
              document.getElementById("IRTPpp-it-calc-heading").text shouldBe s"Pay, Pensions, Profit Income Tax (${irtBand.income.toCurrencyString} at ${irtBand.rate}%)"
            }
            "has the correct tax charged at IRT" in {
              document.getElementById("IRTPpp-amount").text shouldBe irtBand.taxAmount.toCurrencyString
            }
          }
          s"have a National Insurance Class 2 amount of ${model.nic.class2}" in {
            document.getElementById("nic2-amount-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.nic2
            document.getElementById("nic2-amount").text shouldBe model.nic.class2.toCurrencyString
          }
          s"have a National Insurance Class 4 amount of ${model.nic.class4}" in {
            document.getElementById("nic4-amount-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.nic4
            document.getElementById("nic4-amount").text shouldBe model.nic.class4.toCurrencyString
          }
          s"have a Tax reliefs amount of ${model.taxReliefs}" in {
            document.getElementById("tax-relief-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.reliefs
            document.getElementById("tax-relief").text shouldBe "-" + model.taxReliefs.toCurrencyString
          }
          s"have a total tax estimate of ${model.totalIncomeTaxNicYtd}" in {

            document.getElementById("total-estimate-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.total
            document.getElementById("total-estimate").text shouldBe model.totalIncomeTaxNicYtd.toCurrencyString
          }
        }

        "have the higher rate of tax" should {
          val setup = pageSetup(busBropHRTCalcDataModel, bizAndPropertyUser)
          import setup._

          s"have an Income Tax from payPensionsProfit section" which {

            val hrtBand = model.payAndPensionsProfitBands.find(_.name == "HRT").get

            "has a SRT section" in {
              document.getElementById("BRTPpp-section") should not be null
            }

            "has a BRT section" in {
              document.getElementById("BRTPpp-section") should not be null
            }

            "has the correct amount of income taxed at HRT" in {
              document.getElementById("HRTPpp-it-calc-heading").text shouldBe s"Pay, Pensions, Profit Income Tax (${hrtBand.income.toCurrencyString} at ${hrtBand.rate}%)"
            }
            "has the correct tax charged at HRT" in {
              document.getElementById("HRTPpp-amount").text shouldBe hrtBand.taxAmount.toCurrencyString
            }

            "does not have an ART section" in {
              document.getElementById("artPpp-section") shouldBe null
            }
          }
        }

        "have gift aid payment" should {
          val setup = pageSetup(busPropARTCalcDataModel, bizAndPropertyUser)
          import setup._

          "have text Gift Aid payment" in {
            document.getElementById("gift-aid-heading").text shouldBe messages.Bills.giftAid
          }

          "have gift aid charge £500" in {
            document.getElementById("gift-aid").text shouldBe "£500"
          }
        }

        "when no gift aid payment" should {
          val setup1 = pageSetup(calculationDataSuccessModel, propertyUser)
          import setup1._

          "not have text Gift Aid payment" in {
            document.getElementById("gift-aid-heading") shouldBe null
          }

          "not have gift aid charges" in {
            document.getElementById("gift-aid") shouldBe null
          }
        }

        "have the additional rate of tax" should {
          val setup = pageSetup(busPropARTCalcDataModel, bizAndPropertyUser)
          import setup._

          s"have an Income Tax from payPensionsProfit section" which {

            val artBand = model.payAndPensionsProfitBands.find(_.name == "ART").get

            "has a BRT section" in {
              document.getElementById("BRTPpp-section") should not be null
            }
            "has a HRT section" in {
              document.getElementById("HRTPpp-section") should not be null
            }

            "has the correct amount of income taxed at ART" in {
              document.getElementById("ARTPpp-it-calc-heading").text shouldBe s"Pay, Pensions, Profit Income Tax (${artBand.income.toCurrencyString} at ${artBand.rate}%)"
            }
            "has the correct tax charged at ART" in {
              document.getElementById("ARTPpp-amount").text shouldBe artBand.taxAmount.toCurrencyString
            }
          }
        }

        "have all the tax bands including scotish" should {
          val setup = pageSetup(scottishBandModelAllIncomeBands, bizAndPropertyUser)
          import setup._

          s"have an Income Tax from payPensionsProfit section" which {

            "has a SRT section" in {
              document.getElementById("SRTPpp-section") should not be null
            }
            "has a BRT section" in {
              document.getElementById("BRTPpp-section") should not be null
            }
            "has a IRT section" in {
              document.getElementById("IRTPpp-section") should not be null
            }
            "has a HRT section" in {
              document.getElementById("HRTPpp-section") should not be null
            }
            "has a ART section" in {
              document.getElementById("ARTPpp-section") should not be null
            }
          }
        }

        "has no taxable income and no NI contributions" should {
          val setup = pageSetup(noTaxOrNICalcDataModel, bizAndPropertyUser)
          import implicits.ImplicitCurrencyFormatter._
          import setup._
          s"have a taxable income amount of ${model.taxableIncomeTaxIncome.toCurrencyString}" in {
            document.getElementById("taxable-income").text shouldBe model.taxableIncomeTaxIncome.toCurrencyString
          }
          s"not have a National Insurance amount" in {
            document.getElementById("ni-amount") shouldBe null
          }
          s"have a total tax estimate of ${model.totalIncomeTaxNicYtd}" in {
            document.getElementById("total-estimate").text shouldBe model.totalIncomeTaxNicYtd.toCurrencyString
          }
        }

        "has no taxable income and some NI contribution" should {
          val setup = pageSetup(noTaxJustNICalcDataModel, bizAndPropertyUser)
          import implicits.ImplicitCurrencyFormatter._
          import setup._
          s"have a taxable income amount of ${model.taxableIncomeTaxIncome.toCurrencyString}" in {
            document.getElementById("taxable-income").text shouldBe model.taxableIncomeTaxIncome.toCurrencyString
          }
          s"have a National Insurance Class 2 amount of ${model.nic.class2}" in {
            document.getElementById("nic2-amount").text shouldBe model.nic.class2.toCurrencyString
          }
          s"have a National Insurance Class 4 amount of ${model.nic.class4}" in {
            document.getElementById("nic4-amount").text shouldBe model.nic.class4.toCurrencyString
          }
          s"have a Tax reliefs amount of ${model.taxReliefs}" in {
            document.getElementById("tax-relief").text shouldBe "-" + model.taxReliefs.toCurrencyString
          }
          s"have a total tax estimate of ${model.totalIncomeTaxNicYtd}" in {
            document.getElementById("total-estimate").text shouldBe model.totalIncomeTaxNicYtd.toCurrencyString
          }
        }
      }

      "for users with income from Dividends at the Basic Rate" should {

        val setup = pageSetup(dividendAtBRT, bizAndPropertyUser)
        import setup._

        "display income from dividends" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendIncome}" in {
            document.getElementById("dividend-income-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.dividendIncome
          }

          s"should have the amount ${dividendAtBRT.incomeReceived.ukDividends}" in {
            document.getElementById("dividend-income").text shouldBe dividendAtBRT.incomeReceived.ukDividends.toCurrencyString
          }

        }

        "display the dividend allowance that applies" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendAllowance}" in {
            document.getElementById("dividend-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.dividendAllowance
          }

          s"should have the amount ${dividendAtBRT.dividends.totalAmount}" in {
            document.getElementById("dividend-allowance").text shouldBe "-" + dividendAtBRT.dividends.totalAmount.toCurrencyString
          }

        }

        "display the total taxable dividends" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.taxableDividends}" in {
            document.getElementById("taxable-dividend-income-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.taxableDividends
          }

          s"should have the amount ${dividendAtBRT.taxableDividendIncome}" in {
            document.getElementById("taxable-dividend-income").text shouldBe dividendAtBRT.taxableDividendIncome.toCurrencyString
          }

        }

        "display the total taxable savings and gains" which {

          s"should have the heading Income from savings" in {
            document.getElementById("business-profit-bbs-interest-heading").text shouldBe "Income from savings"
          }

          s"should have the amount ${dividendAtBRT.taxableDividendIncome}" in {
            document.getElementById("business-profit-bbs-interest").text shouldBe dividendAtBRT.incomeReceived.bankBuildingSocietyInterest.toCurrencyString
          }

        }

        "have a section for Dividends charged at the Basic Rate" which {

          s"should have the heading ${
            messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
              dividendAtBRT.dividends.band(0).income.toCurrencyString, dividendAtBRT.dividends.band(0).rate.toString.replace(".0", "")
            )
          }" in {
            document.getElementById(s"dividend-${dividendAtBRT.dividends.band(0).name}-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtBRT.dividends.band(0).income.toCurrencyString, dividendAtBRT.dividends.band(0).rate.toString.replace(".0", "")
              )
          }

          s"should have the amount ${dividendAtBRT.dividends.band(0).amount}" in {
           document.getElementById(s"dividend-${dividendAtBRT.dividends.band(0).name}-amount").text shouldBe dividendAtBRT.dividends.band(0).amount.toCurrencyString
          }

        }

      }

      "for users with income from Dividends at the Higher Rate" should {

        val setup = pageSetup(dividendAtHRT, bizAndPropertyUser)
        import setup._

        "display income from dividends" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendIncome}" in {
            document.getElementById("dividend-income-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.dividendIncome
          }

          s"should have the amount ${dividendAtHRT.incomeReceived.ukDividends}" in {
            document.getElementById("dividend-income").text shouldBe dividendAtHRT.incomeReceived.ukDividends.toCurrencyString
          }

        }

        "display the dividend allowance that applies" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendAllowance}" in {
            document.getElementById("dividend-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.dividendAllowance
          }

          s"should have the amount ${dividendAtHRT.dividends.totalAmount}" in {
            document.getElementById("dividend-allowance").text shouldBe "-" + dividendAtHRT.dividends.totalAmount.toCurrencyString
          }

        }

        "display the total taxable dividends" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.taxableDividends}" in {
            document.getElementById("taxable-dividend-income-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.taxableDividends
          }

          s"should have the amount ${dividendAtHRT.taxableDividendIncome}" in {
            document.getElementById("taxable-dividend-income").text shouldBe dividendAtHRT.taxableDividendIncome.toCurrencyString
          }

        }

        "have a section for Dividends charged at the Basic Rate" which {

          s"should have the heading ${
            messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
              dividendAtHRT.dividends.band(0).income.toCurrencyString, dividendAtHRT.dividends.band(0).rate.toString.replace(".0", "")
            )
          }" in {
            document.getElementById(s"dividend-${dividendAtBRT.dividends.band(0).name}-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtHRT.dividends.band(0).income.toCurrencyString, dividendAtHRT.dividends.band(0).rate.toString.replace(".0", "")
              )
          }

          s"should have the amount ${dividendAtHRT.dividends.band(0).amount}" in {
            document.getElementById(s"dividend-${dividendAtBRT.dividends.band(0).name}-amount").text shouldBe dividendAtHRT.dividends.band(0).amount.toCurrencyString
          }

        }

        "have a section for Dividends charged at the Higher Rate" which {

          s"should have the heading ${
            messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
              dividendAtHRT.dividends.band(1).income.toCurrencyString, dividendAtHRT.dividends.band(1).rate.toString.replace(".0", "")
            )
          }" in {
            document.getElementById(s"dividend-${dividendAtHRT.dividends.band(1).name}-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtHRT.dividends.band(1).income.toCurrencyString, dividendAtHRT.dividends.band(1).rate.toString.replace(".0", "")
              )
          }

          s"should have the amount ${dividendAtHRT.dividends.band(1).amount}" in {
            document.getElementById(s"dividend-${dividendAtHRT.dividends.band(1).name}-amount").text shouldBe dividendAtHRT.dividends.band(1).amount.toCurrencyString
          }

        }

      }

      "for users with income from Dividends at the Additional Rate" should {

        val setup = pageSetup(dividendAtART, bizAndPropertyUser)
        import setup._

        "display income from dividends" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendIncome}" in {
            document.getElementById("dividend-income-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.dividendIncome
          }

          s"should have the amount ${dividendAtART.incomeReceived.ukDividends}" in {
            document.getElementById("dividend-income").text shouldBe dividendAtART.incomeReceived.ukDividends.toCurrencyString
          }

        }

        "display the dividend allowance that applies" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendAllowance}" in {
            document.getElementById("dividend-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.dividendAllowance
          }

          s"should have the amount ${dividendAtART.dividends.totalAmount}" in {
            document.getElementById("dividend-allowance").text shouldBe "-" + dividendAtART.dividends.totalAmount.toCurrencyString
          }

        }

        "display the total taxable dividends" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.taxableDividends}" in {
            document.getElementById("taxable-dividend-income-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.taxableDividends
          }

          s"should have the amount ${dividendAtART.taxableDividendIncome}" in {
            document.getElementById("taxable-dividend-income").text shouldBe dividendAtART.taxableDividendIncome.toCurrencyString
          }

        }

        "have a section for Dividends charged at the Basic Rate" which {

          s"should have the heading ${
            messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
              dividendAtART.dividends.band(0).income.toCurrencyString, dividendAtART.dividends.band(0).rate.toString.replace(".0", "")
            )
          }" in {
            document.getElementById(s"dividend-${dividendAtBRT.dividends.band(0).name}-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtART.dividends.band(0).income.toCurrencyString, dividendAtART.dividends.band(0).rate.toString.replace(".0", "")
              )
          }

          s"should have the amount ${dividendAtART.dividends.band(0).amount}" in {
            document.getElementById(s"dividend-${dividendAtBRT.dividends.band(0).name}-amount").text shouldBe dividendAtART.dividends.band(0).amount.toCurrencyString
          }

        }

        "have a section for Dividends charged at the Higher Rate" which {

          s"should have the heading ${
            messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
              dividendAtART.dividends.band(1).income.toCurrencyString, dividendAtART.dividends.band(1).rate.toString.replace(".0", "")
            )
          }" in {
            document.getElementById(s"dividend-${dividendAtBRT.dividends.band(1).name}-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtART.dividends.band(1).income.toCurrencyString, dividendAtART.dividends.band(1).rate.toString.replace(".0", "")
              )
          }

          s"should have the amount ${dividendAtART.dividends.band(1).amount}" in {
            document.getElementById(s"dividend-${dividendAtBRT.dividends.band(1).name}-amount").text shouldBe dividendAtART.dividends.band(1).amount.toCurrencyString
          }

        }

        "have a section for Dividends charged at the Additional Rate" which {

          s"should have the heading ${
            messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
              dividendAtART.dividends.band(2).income.toCurrencyString, dividendAtART.dividends.band(2).rate.toString.replace(".0", "")
            )
          }" in {
            document.getElementById(s"dividend-${dividendAtBRT.dividends.band(2).name}-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtART.dividends.band(2).income.toCurrencyString, dividendAtART.dividends.band(2).rate.toString.replace(".0", "")
              )
          }

          s"should have the amount ${dividendAtART.dividends.band(2).amount}" in {
            document.getElementById(s"dividend-${dividendAtBRT.dividends.band(2).name}-amount").text shouldBe dividendAtART.dividends.band(2).amount.toCurrencyString
          }
        }
      }

      "for users with both property and a business with income from savings and additional allowances" should {

        val setup = pageSetup(calculationDataSuccessModel, bizAndPropertyUser)
        import setup._
        val totalProfit = (model.incomeReceived.bankBuildingSocietyInterest
          + model.incomeReceived.selfEmployment + model.incomeReceived.ukProperty).toCurrencyString

        "display the business profit heading with income from savings included" in {
          document.getElementById("business-profit-heading").text shouldBe "Business profit and income from savings"
        }

        "display the business profit amount including the income from savings" in {
          document.getElementById("business-profit").text shouldBe totalProfit
        }

        "display the personal allowances heading with income savings" in {
          document.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowanceEstimates
        }

        s"have an additional allowances section with an amount of ${model.additionalAllowances}" in {
          document.getElementById("additional-allowances-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.additionalAllowances
          document.getElementById("additional-allowances").text shouldBe "-" + model.additionalAllowances.toCurrencyString
        }

      }

      "for users with only property and with income from savings" should {

        val setup = pageSetup(justPropertyWithSavingsCalcDataModel, propertyUser)
        import setup._
        val totalProfit = (model.incomeReceived.bankBuildingSocietyInterest
          + model.incomeReceived.selfEmployment + model.incomeReceived.ukProperty).toCurrencyString

        "display the business profit heading with income from savings included" in {
          document.getElementById("business-profit-heading").text shouldBe "Property profit and income from savings"
        }

        "display the business profit amount including the income from savings" in {
          document.getElementById("business-profit").text shouldBe totalProfit
        }

        "display the personal allowances heading with income savings" in {
          document.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowanceEstimates
        }
      }

      "for users with income from savings of zero on the bills page" should {

        val setup = pageSetup(justPropertyCalcDataModel, propertyUser)
        import setup._
        val totalProfit = (model.incomeReceived.selfEmployment + model.incomeReceived.ukProperty).toCurrencyString

        "display the personal allowances heading with income savings" in {
          document.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowance
        }

        "have gift aid payment" should {
          val setup1 = pageSetup(justPropertyCalcDataModel, propertyUser)
          import setup._

          "have text Gift Aid payment" in {
            document.getElementById("gift-aid-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.giftAid
          }

          "have gift aid charge £30" in {
            document.getElementById("gift-aid").text shouldBe "£30"
          }

        }

        "when no gift aid payment" should {
          val setup1 = pageSetup(calculationDataSuccessModel, propertyUser)
          import setup1._

          "not have text Gift Aid payment" in {
            document.getElementById("gift-aid-heading") shouldBe null
          }

          "not have gift aid charge £5" in {
            document.getElementById("gift-aid") shouldBe null
          }
        }
      }

      "when no breakdown data is retrieved" should {
        lazy val noBreakdownPage = views.html.estimatedTaxLiability(
          calculationDisplayNoBreakdownModel,
          testYear)(
          FakeRequest(),
          applicationMessages,
          mockAppConfig,
          testMtdItUser)
        lazy val noBreakdownDocument = Jsoup.parse(contentAsString(noBreakdownPage))

        "not display a breakdown section" in {
          noBreakdownDocument.getElementById("calc-breakdown-inner-link") shouldBe null
        }
      }

      "when the user only has businesses registered" should {

        val setup = pageSetup(justBusinessCalcDataModel, bizUser)
        import setup._

        "display the business profit amount" in {
          document.getElementById("business-profit").text shouldBe "£3,000"
        }

      }

      "when the user only has a business registered but has a property profit value" should {
        val setup = pageSetup(busPropBRTCalcDataModel, bizUser)
        import setup._

        "display the business profit heading" in {
          document.getElementById("business-profit-heading").text shouldBe "Business profit"
        }
        "display the business profit amount" in {
          document.getElementById("business-profit").text shouldBe "£3,000"
        }
      }

      "when the user only has properties registered" should {

        val setup = pageSetup(justPropertyCalcDataModel, propertyUser)
        import setup._

        "display the property profit heading" in {
          document.getElementById("business-profit-heading").text shouldBe "Property profit"
        }
        "display the property profit section" in {
          document.getElementById("business-profit").text shouldBe "£3,000"
        }
      }

      "when the user only has properties registered but has a business profit value" should {
        val setup = pageSetup(busPropBRTCalcDataModel, propertyUser)
        import setup._

        "display the Business profit heading" in {
          document.getElementById("business-profit-heading").text shouldBe "Business profit"
        }
        "display the business profit amount" in {
          document.getElementById("business-profit").text shouldBe "£3,000"
        }
      }

      "when the user has property income, self Employed Income and  Savings and gains"  should {
        val setup = pageSetup(dividendAtART, bizAndPropertyUser)
        import setup._

        "display bbs interest section " in {
          document.getElementById("business-profit-self-employed-heading").text shouldBe "Business profit(for self-employment)"
        }

        "display income recieved from self income" in {
          document.getElementById("business-profit-property-heading").text shouldBe "Business profit(for property)"
        }

        "display income recieved from property" in {
          document.getElementById("business-profit-bbs-interest-heading").text    shouldBe "Income from savings"
        }

      }
    }
  }
}

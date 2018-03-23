/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.Messages
import assets.Messages.{Breadcrumbs => breadcrumbMessages}
import assets.BusinessDetailsTestConstants._
import assets.CalcBreakdownTestConstants._
import assets.EstimatesTestConstants._
import assets.PropertyDetailsTestConstants._
import assets.FinancialTransactionsTestConstants._
import assets.BaseTestConstants._
import auth.MtdItUser
import config.FrontendAppConfig
import models.calculation.CalculationDataModel
import models.incomeSourcesWithDeadlines.IncomeSourcesWithDeadlinesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import utils.ImplicitCurrencyFormatter._
import utils.{ImplicitCurrencyFormatter, TestSupport}
import utils.ImplicitDateFormatter


class EstimatedTaxLiabilityViewSpec extends TestSupport with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testIncomeSources: IncomeSourcesWithDeadlinesModel = IncomeSourcesWithDeadlinesModel(List(businessIncomeModelAlignedTaxYear), Some(propertyIncomeModel))
  val testBusinessIncomeSource: IncomeSourcesWithDeadlinesModel = IncomeSourcesWithDeadlinesModel(List(businessIncomeModelAlignedTaxYear), None)
  val testPropertyIncomeSource: IncomeSourcesWithDeadlinesModel = IncomeSourcesWithDeadlinesModel(List.empty, Some(propertyIncomeModel))

  private def pageSetup(calcDataModel: CalculationDataModel, incomeSources: IncomeSourcesWithDeadlinesModel) = new {
    val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), incomeSources)(FakeRequest())
    lazy val page: HtmlFormat.Appendable = views.html.estimatedTaxLiability(
      calculationDisplaySuccessModel(calcDataModel),
      testYear)(FakeRequest(),applicationMessages, mockAppConfig, testMtdItUser, incomeSources)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    lazy val cPage: HtmlFormat.Appendable = views.html.crystallised(
      calculationDisplaySuccessCrystalisationModel(calcDataModel), transactionModel(),
      testYear)(FakeRequest(),applicationMessages, mockAppConfig, testMtdItUser, incomeSources)
    lazy val cDocument: Document = Jsoup.parse(contentAsString(cPage))

    implicit val model: CalculationDataModel = calcDataModel

    def personalAllowanceTotal: String = "-" + (
      model.personalAllowance +
        model.savingsAndGains.startBand.taxableIncome +
        model.savingsAndGains.zeroBand.taxableIncome
      ).toCurrencyString
  }

  "The EstimatedTaxLiability view" should {

    val setup = pageSetup(busPropBRTCalcDataModel, testIncomeSources)
    import setup._
    val messages = new Messages.Calculation(testYear)

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }

    "have a breadcrumb trail" in {
      document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
      document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
      document.getElementById("breadcrumb-estimates").text shouldBe breadcrumbMessages.estimates
      document.getElementById("breadcrumb-it-estimate").text shouldBe breadcrumbMessages.itEstimate
    }

    s"have the tax year '${messages.taxYearSubHeading}'" in {
      document.getElementById("tax-year").text() shouldBe messages.taxYearSubHeading
    }

    s"have the page heading '${messages.pageHeading}'" in {
      document.getElementById("page-heading").text() shouldBe messages.pageHeading
    }

    s"have an Estimated Tax Liability section" which {

      lazy val estimateSection = document.getElementById("estimated-tax")

      "has a section for EoY Estimate" which {

        lazy val eoySection = estimateSection.getElementById("eoyEstimate")

        s"has the correct Annual Tax Amount Estimate Heading of '${
          messages.EoyEstimate.heading(busPropBRTCalcDataModel.eoyEstimate.get.incomeTaxNicAmount.toCurrencyString)
        }" in {
          eoySection.getElementById("eoyEstimateHeading").text shouldBe
            messages.EoyEstimate.heading(busPropBRTCalcDataModel.eoyEstimate.get.incomeTaxNicAmount.toCurrencyString)
        }

        s"has the correct estimate p1 paragraph '${messages.EoyEstimate.p1}'" in {
          eoySection.getElementById("eoyP1").text shouldBe messages.EoyEstimate.p1
        }

        s"has the correct estimate p2 paragraph '${messages.EoyEstimate.p2}'" in {
          eoySection.getElementById("eoyP2").text shouldBe messages.EoyEstimate.p2
        }
      }

      "has a section for In Year (Current) Estimate" which {

        lazy val inYearSection = estimateSection.getElementById("inYearEstimate")

        s"has the correct Annual Tax Amount Estimate Heading of '${messages.InYearEstimate.heading(busPropBRTCalcDataModel.totalIncomeTaxNicYtd.toCurrencyString)}" in {
          inYearSection.getElementById("inYearEstimateHeading").text shouldBe
            messages.InYearEstimate.heading(busPropBRTCalcDataModel.totalIncomeTaxNicYtd.toCurrencyString)
        }

        s"has the correct estimate p1 paragraph '${messages.InYearEstimate.p1(lastTaxCalcSuccess.calcTimestamp.toLocalDateTime.toLongDateTime)}'" in {
          inYearSection.getElementById("inYearP1").text shouldBe
            messages.InYearEstimate.p1(lastTaxCalcSuccess.calcTimestamp.toLocalDateTime.toLongDateTime)
        }

        s"has the correct estimate p2 paragraph '${messages.InYearEstimate.p2}'" in {
          inYearSection.getElementById("inYearP2").text shouldBe messages.InYearEstimate.p2
        }

        "has progressive disclosure for why there estimate might change" which {

          s"has the heading '${messages.InYearEstimate.WhyThisMayChange.heading}'" in {
            inYearSection.getElementById("whyEstimateMayChange").text shouldBe messages.InYearEstimate.WhyThisMayChange.heading
          }

          s"has the p1 heading '${messages.InYearEstimate.WhyThisMayChange.p1}'" in {
            inYearSection.getElementById("whyMayChangeP1").text shouldBe messages.InYearEstimate.WhyThisMayChange.p1
          }

          s"has the 1st bullet '${messages.InYearEstimate.WhyThisMayChange.bullet1}'" in {
            inYearSection.select("#whyMayChange ul li:nth-child(1)").text shouldBe messages.InYearEstimate.WhyThisMayChange.bullet1
          }

          s"has the 2nd bullet '${messages.InYearEstimate.WhyThisMayChange.bullet2}'" in {
            inYearSection.select("#whyMayChange ul li:nth-child(2)").text shouldBe messages.InYearEstimate.WhyThisMayChange.bullet2
          }

          s"has the 2nd bullet '${messages.InYearEstimate.WhyThisMayChange.bullet3}'" in {
            inYearSection.select("#whyMayChange ul li:nth-child(3)").text shouldBe messages.InYearEstimate.WhyThisMayChange.bullet3
          }
        }
      }
    }

    "have a Calculation Breakdown section" that {

      "for users with both a property and a business" which {

        "have just the basic rate of tax" should {

          val total = (model.incomeReceived.selfEmployment + model.incomeReceived.ukProperty + model.incomeReceived.bankBuildingSocietyInterest).toCurrencyString
          val setup = pageSetup(busPropBRTCalcDataModel, testIncomeSources)
          import setup._

          s"have a business profit section amount of $total" in {
            document.getElementById("business-profit-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.businessProfit
            document.getElementById("business-profit").text shouldBe total
          }

          s"have a personal allowance amount of ${model.personalAllowance}" in {
            document.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowance
            document.getElementById("personal-allowance").text shouldBe personalAllowanceTotal
          }

          "not show the additional allowances section" in {
            document.getElementById("additionalAllowances") shouldBe null
          }

          s"have a taxable income amount of ${model.taxableIncomeTaxIncome.toCurrencyString}" in {
            document.getElementById("taxable-income-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.yourTaxableIncome
            document.getElementById("taxable-income").text shouldBe model.taxableIncomeTaxIncome.toCurrencyString
          }

          s"have an Income Tax section" which {
            "has the correct amount of income taxed at BRT" in {
              document.getElementById("brt-it-calc").text shouldBe
                (model.payPensionsProfit.basicBand.taxableIncome + model.savingsAndGains.basicBand.taxableIncome).toCurrencyString
            }
            "has the correct BRT rate" in {
              document.getElementById("brt-rate").text shouldBe model.payPensionsProfit.basicBand.taxRate.toString
            }
            "has the correct tax charged at BRT" in {
              document.getElementById("brt-amount").text shouldBe
                (model.payPensionsProfit.basicBand.taxAmount + model.savingsAndGains.basicBand.taxAmount).toCurrencyString
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
          val setup = pageSetup(busBropHRTCalcDataModel, testIncomeSources)
          import ImplicitCurrencyFormatter._
          import setup._

          s"have an Income Tax section" which {
            "has a BRT section" in {
              document.getElementById("brt-section") should not be null
            }

            "has the correct amount of income taxed at HRT" in {
              document.getElementById("hrt-it-calc").text shouldBe
                (model.payPensionsProfit.higherBand.taxableIncome + model.savingsAndGains.higherBand.taxableIncome).toCurrencyString
            }
            "has the correct HRT rate" in {
              document.getElementById("hrt-rate").text shouldBe model.payPensionsProfit.higherBand.taxRate.toString
            }
            "has the correct tax charged at HRT" in {
              document.getElementById("hrt-amount").text shouldBe
                (model.payPensionsProfit.higherBand.taxAmount + model.savingsAndGains.higherBand.taxAmount).toCurrencyString

            }

            "does not have an ART section" in {
              document.getElementById("art-section") shouldBe null
            }
          }
        }

        "have the additional rate of tax" should {
          val setup = pageSetup(busPropARTCalcDataModel, testIncomeSources)
          import ImplicitCurrencyFormatter._
          import setup._
          s"have an Income Tax section" which {
            "has a BRT section" in {
              document.getElementById("brt-section") should not be null
            }
            "has a HRT section" in {
              document.getElementById("hrt-section") should not be null
            }

            "has the correct amount of income taxed at ART" in {
              document.getElementById("art-it-calc").text shouldBe
                (model.payPensionsProfit.additionalBand.taxableIncome + model.savingsAndGains.additionalBand.taxableIncome).toCurrencyString
            }
            "has the correct ART rate" in {
              document.getElementById("art-rate").text shouldBe model.payPensionsProfit.additionalBand.taxRate.toString
            }
            "has the correct tax charged at ART" in {
              document.getElementById("art-amount").text shouldBe
                (model.payPensionsProfit.additionalBand.taxAmount + model.savingsAndGains.additionalBand.taxAmount).toCurrencyString
            }
          }
        }

        "has no taxable income and no NI contributions" should {
          val setup = pageSetup(noTaxOrNICalcDataModel, testIncomeSources)
          import ImplicitCurrencyFormatter._
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
          val setup = pageSetup(noTaxJustNICalcDataModel, testIncomeSources)
          import ImplicitCurrencyFormatter._
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

        val setup = pageSetup(dividendAtBRT, testIncomeSources)
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

          s"should have the amount ${dividendAtBRT.dividends.allowance}" in {
            document.getElementById("dividend-allowance").text shouldBe "-" + dividendAtBRT.dividends.allowance.toCurrencyString
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

        "have a section for Dividends charged at the Basic Rate" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
            dividendAtBRT.dividends.basicBand.taxableIncome.toCurrencyString, dividendAtBRT.dividends.basicBand.taxRate.toString.replace(".0","")
          )}" in {
            document.getElementById("dividend-brt-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtBRT.dividends.basicBand.taxableIncome.toCurrencyString, dividendAtBRT.dividends.basicBand.taxRate.toString.replace(".0","")
              )
          }

          s"should have the amount ${dividendAtBRT.dividends.basicBand.taxAmount}" in {
            document.getElementById("dividend-brt-amount").text shouldBe dividendAtBRT.dividends.basicBand.taxAmount.toCurrencyString
          }

        }

      }

      "for users with income from Dividends at the Higher Rate" should {

        val setup = pageSetup(dividendAtHRT, testIncomeSources)
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

          s"should have the amount ${dividendAtHRT.dividends.allowance}" in {
            document.getElementById("dividend-allowance").text shouldBe "-" + dividendAtHRT.dividends.allowance.toCurrencyString
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

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
            dividendAtHRT.dividends.basicBand.taxableIncome.toCurrencyString, dividendAtHRT.dividends.basicBand.taxRate.toString.replace(".0","")
          )}" in {
            document.getElementById("dividend-brt-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtHRT.dividends.basicBand.taxableIncome.toCurrencyString, dividendAtHRT.dividends.basicBand.taxRate.toString.replace(".0","")
              )
          }

          s"should have the amount ${dividendAtHRT.dividends.basicBand.taxAmount}" in {
            document.getElementById("dividend-brt-amount").text shouldBe dividendAtHRT.dividends.basicBand.taxAmount.toCurrencyString
          }

        }

        "have a section for Dividends charged at the Higher Rate" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
            dividendAtHRT.dividends.higherBand.taxableIncome.toCurrencyString, dividendAtHRT.dividends.higherBand.taxRate.toString.replace(".0","")
          )}" in {
            document.getElementById("dividend-hrt-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtHRT.dividends.higherBand.taxableIncome.toCurrencyString, dividendAtHRT.dividends.higherBand.taxRate.toString.replace(".0","")
              )
          }

          s"should have the amount ${dividendAtHRT.dividends.higherBand.taxAmount}" in {
            document.getElementById("dividend-hrt-amount").text shouldBe dividendAtHRT.dividends.higherBand.taxAmount.toCurrencyString
          }

        }

      }

      "for users with income from Dividends at the Additional Rate" should {

        val setup = pageSetup(dividendAtART, testIncomeSources)
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

          s"should have the amount ${dividendAtART.dividends.allowance}" in {
            document.getElementById("dividend-allowance").text shouldBe "-" + dividendAtART.dividends.allowance.toCurrencyString
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

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
            dividendAtART.dividends.basicBand.taxableIncome.toCurrencyString, dividendAtART.dividends.basicBand.taxRate.toString.replace(".0","")
          )}" in {
            document.getElementById("dividend-brt-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtART.dividends.basicBand.taxableIncome.toCurrencyString, dividendAtART.dividends.basicBand.taxRate.toString.replace(".0","")
              )
          }

          s"should have the amount ${dividendAtART.dividends.basicBand.taxAmount}" in {
            document.getElementById("dividend-brt-amount").text shouldBe dividendAtART.dividends.basicBand.taxAmount.toCurrencyString
          }

        }

        "have a section for Dividends charged at the Higher Rate" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
            dividendAtART.dividends.higherBand.taxableIncome.toCurrencyString, dividendAtART.dividends.higherBand.taxRate.toString.replace(".0","")
          )}" in {
            document.getElementById("dividend-hrt-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtART.dividends.higherBand.taxableIncome.toCurrencyString, dividendAtART.dividends.higherBand.taxRate.toString.replace(".0","")
              )
          }

          s"should have the amount ${dividendAtART.dividends.higherBand.taxAmount}" in {
            document.getElementById("dividend-hrt-amount").text shouldBe dividendAtART.dividends.higherBand.taxAmount.toCurrencyString
          }

        }

        "have a section for Dividends charged at the Additional Rate" which {

          s"should have the heading ${messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
            dividendAtART.dividends.additionalBand.taxableIncome.toCurrencyString, dividendAtART.dividends.additionalBand.taxRate.toString.replace(".0","")
          )}" in {
            document.getElementById("dividend-art-calc-heading").text shouldBe
              messages.InYearEstimate.CalculationBreakdown.dividendAtRate(
                dividendAtART.dividends.additionalBand.taxableIncome.toCurrencyString, dividendAtART.dividends.additionalBand.taxRate.toString.replace(".0","")
              )
          }

          s"should have the amount ${dividendAtART.dividends.additionalBand.taxAmount}" in {
            document.getElementById("dividend-art-amount").text shouldBe dividendAtART.dividends.additionalBand.taxAmount.toCurrencyString
          }

        }


      }

      "for users with both property and a business with income from savings and additional allowances" should {

        val setup = pageSetup(calculationDataSuccessModel, testIncomeSources)
        import setup._
        val totalProfit = (model.incomeReceived.bankBuildingSocietyInterest + model.incomeReceived.selfEmployment + model.incomeReceived.ukProperty).toCurrencyString

        "display the business profit heading with income from savings included" in {
          document.getElementById("business-profit-heading").text shouldBe "Business profit and income from savings"
        }

        "display the business profit amount including the income from savings" in {
          document.getElementById("business-profit").text shouldBe totalProfit
        }

        "display the personal allowances heading with income savings" in {
          document.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowanceSavingsEstimates
        }

        "display the correct personal allowance amount" in {
          cDocument.getElementById("personal-allowance").text shouldBe personalAllowanceTotal
        }

        s"have an additional allowances section with an amount of ${model.additionalAllowances}" in {
          document.getElementById("additional-allowances-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.additionalAllowances
          document.getElementById("additional-allowances").text shouldBe "-" + model.additionalAllowances.toCurrencyString
        }

      }

      "for users with only property and with income from savings" should {

        val setup = pageSetup(justPropertyWithSavingsCalcDataModel, testPropertyIncomeSource)
        import setup._
        val totalProfit = (model.incomeReceived.bankBuildingSocietyInterest + model.incomeReceived.selfEmployment + model.incomeReceived.ukProperty).toCurrencyString

        "display the business profit heading with income from savings included" in {
          document.getElementById("business-profit-heading").text shouldBe "Property profit and income from savings"
        }

        "display the business profit amount including the income from savings" in {
          document.getElementById("business-profit").text shouldBe totalProfit
        }

        "display the personal allowances heading with income savings" in {
          document.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowanceSavingsEstimates
        }

        "display the correct personal allowance amount" in {
          cDocument.getElementById("personal-allowance").text shouldBe personalAllowanceTotal
        }

      }

      "for users with both property and a business with income from savings on the bills page" should {

        val setup = pageSetup(calculationDataSuccessModel, testIncomeSources)
        import setup._
        val totalProfit = (model.incomeReceived.selfEmployment + model.incomeReceived.ukProperty).toCurrencyString

        "display the business profit heading with income from savings included" in {
          cDocument.getElementById("business-profit-heading").text shouldBe "Business profit"
        }

        "display the business profit amount including the income from savings" in {
          cDocument.getElementById("business-profit").text shouldBe totalProfit
        }

        "display the income from savings heading" in {
          cDocument.getElementById("savings-income-heading").text shouldBe "Income from savings"
        }

        "display the income from savings amount" in {
          cDocument.getElementById("savings-income").text shouldBe model.incomeReceived.bankBuildingSocietyInterest.toCurrencyString
        }

        "display the personal allowances heading with income savings" in {
          cDocument.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowanceSavingsBills
        }

        "display the correct personal allowance amount" in {
          cDocument.getElementById("personal-allowance").text shouldBe personalAllowanceTotal
        }

      }

      "for users with only property and with income from savings on the bills page" should {

        val setup = pageSetup(justPropertyWithSavingsCalcDataModel, testPropertyIncomeSource)
        import setup._
        val totalProfit = (model.incomeReceived.selfEmployment + model.incomeReceived.ukProperty).toCurrencyString

        "display the business profit heading with income from savings included" in {
          cDocument.getElementById("business-profit-heading").text shouldBe "Property profit"
        }

        "display the business profit amount including the income from savings" in {
          cDocument.getElementById("business-profit").text shouldBe totalProfit
        }

        "display the income from savings heading" in {
          cDocument.getElementById("savings-income-heading").text shouldBe "Income from savings"
        }

        "display the income from savings amount" in {
          cDocument.getElementById("savings-income").text shouldBe model.incomeReceived.bankBuildingSocietyInterest.toCurrencyString
        }

        "display the personal allowances heading with income savings" in {
          cDocument.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowanceSavingsBills
        }

        "display the correct personal allowance amount" in {
          cDocument.getElementById("personal-allowance").text shouldBe personalAllowanceTotal
        }

      }

      "for users with income from savings of zero on the bills page" should {

        val setup = pageSetup(justPropertyCalcDataModel, testPropertyIncomeSource)
        import setup._
        val totalProfit = (model.incomeReceived.selfEmployment + model.incomeReceived.ukProperty).toCurrencyString

        "display the income from savings heading" in {
          cDocument.getElementById("savings-income-heading") shouldBe null
        }

        "display the income from savings amount" in {
          cDocument.getElementById("savings-income") shouldBe null
        }

        "display the personal allowances heading with income savings" in {
          document.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowance
        }

      }

      "when no breakdown data is retrieved" should {
        lazy val noBreakdownPage = views.html.estimatedTaxLiability(
          calculationDisplayNoBreakdownModel,
          testYear)(
          FakeRequest(),
          applicationMessages,
          mockAppConfig,
          testMtdItUser,
          testIncomeSources)
        lazy val noBreakdownDocument = Jsoup.parse(contentAsString(noBreakdownPage))

        "not display a breakdown section" in {
          noBreakdownDocument.getElementById("calc-breakdown-inner-link") shouldBe null
        }
      }

      "when the user only has businesses registered" should {

        val setup = pageSetup(justBusinessCalcDataModel, testBusinessIncomeSource)
        import setup._

        "display the business profit amount" in {
          document.getElementById("business-profit").text shouldBe "£3,000"
        }

      }

      "when the user only has a business registered but has a property profit value" should {
        val setup = pageSetup(busPropBRTCalcDataModel, testBusinessIncomeSource)
        import setup._

        "display the business profit heading" in {
          document.getElementById("business-profit-heading").text shouldBe "Business profit"
        }
        "display the business profit amount" in {
          document.getElementById("business-profit").text shouldBe "£3,000"
        }
      }

      "when the user only has properties registered" should {

        val setup = pageSetup(justPropertyCalcDataModel, testPropertyIncomeSource)
        import setup._

        "display the property profit heading" in {
          document.getElementById("business-profit-heading").text shouldBe "Property profit"
        }
        "display the property profit section" in {
          document.getElementById("business-profit").text shouldBe "£3,000"
        }
      }

      "when the user only has properties registered but has a business profit value" should {
        val setup = pageSetup(busPropBRTCalcDataModel, testPropertyIncomeSource)
        import setup._

        "display the Business profit heading" in {
          document.getElementById("business-profit-heading").text shouldBe "Business profit"
        }
        "display the business profit amount" in {
          document.getElementById("business-profit").text shouldBe "£3,000"
        }
      }
    }

    "show a back link to the Income Tax home page" in {
      val setup = pageSetup(busPropBRTCalcDataModel, testIncomeSources)
      import setup._
      document.getElementById("it-home-back") shouldNot be(null)
    }
  }
}

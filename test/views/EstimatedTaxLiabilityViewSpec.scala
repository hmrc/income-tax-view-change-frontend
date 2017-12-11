/*
 * Copyright 2017 HM Revenue & Customs
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
import assets.Messages.{Sidebar => sidebarMessages}
import assets.TestConstants.BusinessDetails._
import assets.TestConstants.CalcBreakdown._
import assets.TestConstants.Estimates._
import assets.TestConstants.PropertyIncome._
import assets.TestConstants._
import auth.MtdItUser
import config.FrontendAppConfig
import models.{CalculationDataModel, IncomeSourcesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import utils.ImplicitCurrencyFormatter._
import utils.{ImplicitCurrencyFormatter, TestSupport}


class EstimatedTaxLiabilityViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testMtdItUser: MtdItUser = MtdItUser(testMtditid, testNino, Some(testUserDetails))
  val testIncomeSources: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), Some(propertyIncomeModel))
  val testBusinessIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), None)
  val testPropertyIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List.empty, Some(propertyIncomeModel))

  private def pageSetup(calcDataModel: CalculationDataModel, incomeSources: IncomeSourcesModel) = new {
    lazy val page: HtmlFormat.Appendable = views.html.estimatedTaxLiability(
      CalcBreakdown.calculationDisplaySuccessModel(calcDataModel),
      testYear)(FakeRequest(),applicationMessages, mockAppConfig, testMtdItUser, incomeSources, serviceInfo)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    implicit val model: CalculationDataModel = calcDataModel
  }

  "The EstimatedTaxLiability view" should {

    val setup = pageSetup(busPropBRTCalcDataModel, testIncomeSources)
    import setup._
    val messages = new Messages.Calculation(taxYear = 2018)

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
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

        s"has the correct Annual Tax Amount Estimate Heading of '${messages.EoyEstimate.heading(busPropBRTCalcDataModel.eoyEstimate.get.incomeTaxNicAmount.toCurrencyString)}" in {
          eoySection.getElementById("eoyEstimateHeading").text shouldBe messages.EoyEstimate.heading(busPropBRTCalcDataModel.eoyEstimate.get.incomeTaxNicAmount.toCurrencyString)
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

        s"has the correct Annual Tax Amount Estimate Heading of '${messages.InYearEstimate.heading(busPropBRTCalcDataModel.incomeTaxYTD.toCurrencyString)}" in {
          inYearSection.getElementById("inYearEstimateHeading").text shouldBe messages.InYearEstimate.heading(busPropBRTCalcDataModel.incomeTaxYTD.toCurrencyString)
        }

        s"has the correct estimate p1 paragraph '${messages.InYearEstimate.p1(Estimates.lastTaxCalcSuccess.calcTimestamp.toLocalDateTime.toLongDateTime)}'" in {
          inYearSection.getElementById("inYearP1").text shouldBe messages.InYearEstimate.p1(Estimates.lastTaxCalcSuccess.calcTimestamp.toLocalDateTime.toLongDateTime)
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
          val setup = pageSetup(busPropBRTCalcDataModel, testIncomeSources)
          import setup._

          s"have a business profit section amount of ${model.profitFromSelfEmployment}" in {
            document.getElementById("business-profit-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.businessProfit
            document.getElementById("business-profit").text shouldBe model.profitFromSelfEmployment.toCurrencyString
          }

          s"have a property profit amount of ${model.profitFromUkLandAndProperty}" in {
            document.getElementById("property-profit-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.propertyProfit
            document.getElementById("property-profit").text shouldBe model.profitFromSelfEmployment.toCurrencyString
          }

          s"have a personal allowance amount of ${model.proportionAllowance}" in {
            document.getElementById("personal-allowance-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.personalAllowance
            document.getElementById("personal-allowance").text shouldBe "-"+model.proportionAllowance.toCurrencyString
          }

          s"have a taxable income amount of ${model.totalIncomeOnWhichTaxIsDue}" in {
            document.getElementById("taxable-income-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.yourTaxableIncome
            document.getElementById("taxable-income").text shouldBe model.totalIncomeOnWhichTaxIsDue.toCurrencyString
          }

          s"have an income tax section" which {
            "has the correct amount of income taxed at BRT" in {
              document.getElementById("brt-it-calc").text shouldBe model.payPensionsProfitAtBRT.get.toCurrencyString
            }
            "has the correct BRT rate" in {
              document.getElementById("brt-rate").text shouldBe model.rateBRT.toString
            }
            "has the correct tax charged at BRT" in {
              document.getElementById("brt-amount").text shouldBe model.incomeTaxOnPayPensionsProfitAtBRT.toCurrencyString
            }
          }
          s"have a National Insurance Class 2 amount of ${model.nationalInsuranceClass2Amount}" in {
            document.getElementById("nic2-amount-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.nic2
            document.getElementById("nic2-amount").text shouldBe model.nationalInsuranceClass2Amount.toCurrencyString
          }
          s"have a National Insurance Class 4 amount of ${model.totalClass4Charge}" in {
            document.getElementById("nic4-amount-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.nic4
            document.getElementById("nic4-amount").text shouldBe model.totalClass4Charge.toCurrencyString
          }
          s"have a total tax estimate of ${model.incomeTaxYTD}" in {
            document.getElementById("total-estimate-heading").text shouldBe messages.InYearEstimate.CalculationBreakdown.total
            document.getElementById("total-estimate").text shouldBe model.incomeTaxYTD.toCurrencyString
          }
        }

        "have the higher rate of tax" should {
          val setup = pageSetup(busBropHRTCalcDataModel, testIncomeSources)
          import ImplicitCurrencyFormatter._
          import setup._

          s"have an income tax section" which {
            "has a BRT section" in {
              document.getElementById("brt-section") should not be null
            }

            "has the correct amount of income taxed at HRT" in {
              document.getElementById("hrt-it-calc").text shouldBe model.payPensionsProfitAtHRT.get.toCurrencyString
            }
            "has the correct HRT rate" in {
              document.getElementById("hrt-rate").text shouldBe model.rateHRT.toString
            }
            "has the correct tax charged at HRT" in {
              document.getElementById("hrt-amount").text shouldBe model.incomeTaxOnPayPensionsProfitAtHRT.toCurrencyString
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
          s"have an income tax section" which {
            "has a BRT section" in {
              document.getElementById("brt-section") should not be null
            }
            "has a HRT section" in {
              document.getElementById("hrt-section") should not be null
            }

            "has the correct amount of income taxed at ART" in {
              document.getElementById("art-it-calc").text shouldBe model.payPensionsProfitAtART.get.toCurrencyString
            }
            "has the correct ART rate" in {
              document.getElementById("art-rate").text shouldBe model.rateART.toString
            }
            "has the correct tax charged at ART" in {
              document.getElementById("art-amount").text shouldBe model.incomeTaxOnPayPensionsProfitAtART.toCurrencyString
            }
          }
        }

        "has no taxable income and no NI contributions" should {
          val setup = pageSetup(noTaxOrNICalcDataModel, testIncomeSources)
          import ImplicitCurrencyFormatter._
          import setup._
          s"have a taxable income amount of ${model.totalIncomeOnWhichTaxIsDue}" in {
            document.getElementById("taxable-income").text shouldBe model.totalIncomeOnWhichTaxIsDue.toCurrencyString
          }
          s"not have a National Insurance amount" in {
            document.getElementById("ni-amount") shouldBe null
          }
          s"have a total tax estimate of ${model.incomeTaxYTD}" in {
            document.getElementById("total-estimate").text shouldBe model.incomeTaxYTD.toCurrencyString
          }
        }

        "has no taxable income and some NI contribution" should {
          val setup = pageSetup(noTaxJustNICalcDataModel, testIncomeSources)
          import ImplicitCurrencyFormatter._
          import setup._
          s"have a taxable income amount of ${model.totalIncomeOnWhichTaxIsDue}" in {
            document.getElementById("taxable-income").text shouldBe model.totalIncomeOnWhichTaxIsDue.toCurrencyString
          }
          s"have a National Insurance Class 2 amount of ${model.nationalInsuranceClass2Amount}" in {
            document.getElementById("nic2-amount").text shouldBe model.nationalInsuranceClass2Amount.toCurrencyString
          }
          s"have a National Insurance Class 4 amount of ${model.totalClass4Charge}" in {
            document.getElementById("nic4-amount").text shouldBe model.totalClass4Charge.toCurrencyString
          }
          s"have a total tax estimate of ${model.incomeTaxYTD}" in {
            document.getElementById("total-estimate").text shouldBe model.incomeTaxYTD.toCurrencyString
          }
        }
      }

      "when no breakdown data is retrieved" should {
        lazy val noBreakdownPage = views.html.estimatedTaxLiability(
          CalcBreakdown.calculationDisplayNoBreakdownModel, testYear)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser, testIncomeSources, serviceInfo)
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

        "not display the property profit section" in {
          document.getElementById("property-profit-section") shouldBe null
        }
      }

      "when the user only has a business registered but has a property profit value" should {
        val setup = pageSetup(busPropBRTCalcDataModel, testBusinessIncomeSource)
        import setup._

        "display the business profit amount" in {
          document.getElementById("business-profit").text shouldBe "£1,500"
        }
        "display the property profit amount" in {
          document.getElementById("property-profit").text shouldBe "£1,500"
        }
      }

      "when the user only has properties registered" should {

        val setup = pageSetup(justPropertyCalcDataModel, testPropertyIncomeSource)
        import setup._

        "display the property profit section" in {
          document.getElementById("property-profit").text shouldBe "£3,000"
        }
        "not display the business profit section" in {
          document.getElementById("business-profit") shouldBe null
        }
      }

      "when the user only has properties registered but has a business profit value" should {
        val setup = pageSetup(busPropBRTCalcDataModel, testPropertyIncomeSource)
        import setup._

        "display the business profit amount" in {
          document.getElementById("business-profit").text shouldBe "£1,500"
        }
        "display the property profit amount" in {
          document.getElementById("property-profit").text shouldBe "£1,500"
        }
      }
    }

    "have sidebar section " in {
      document.getElementById("sidebar") shouldNot be(null)
    }
  }
}

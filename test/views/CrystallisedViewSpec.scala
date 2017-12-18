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

class CrystallisedViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testIncomeSources: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), Some(propertyIncomeModel))
  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), testIncomeSources)(FakeRequest())
  val testBusinessIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), None)
  val testPropertyIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List.empty, Some(propertyIncomeModel))

  private def pageSetup(calcDataModel: CalculationDataModel, incomeSources: IncomeSourcesModel) = new {
    lazy val page: HtmlFormat.Appendable = views.html.crystallised(
      CalcBreakdown.calculationDisplaySuccessModel(calcDataModel),
      testYear)(FakeRequest(),applicationMessages, mockAppConfig, testMtdItUser, incomeSources, serviceInfo)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    implicit val model: CalculationDataModel = calcDataModel
  }

  "The Crystallised view" should {

    val setup = pageSetup(busPropBRTCalcDataModel, testIncomeSources)
    import setup._
    val messages = new Messages.Calculation(taxYear = 2018)
    val crysMessages = new Messages.Calculation(taxYear = 2018).Crystallised

    s"have the title '${crysMessages.tabTitle}'" in {
      document.title() shouldBe crysMessages.tabTitle
    }

    s"have the tax year '${crysMessages.subHeading}'" in {
      document.getElementById("tax-year").text() shouldBe crysMessages.subHeading
    }

    s"have the page heading '${crysMessages.heading}'" in {
      document.getElementById("page-heading").text() shouldBe crysMessages.heading
    }

    s"have an Owed Tax section" which {

      lazy val owedTaxSection = document.getElementById("owed-tax")

      "has a section for What you owe" which {

        lazy val wyoSection = owedTaxSection.getElementById("whatYouOwe")

        s"has the correct 'What you owe' heading" in {
          wyoSection.getElementById("whatYouOweHeading").text shouldBe crysMessages.wyoHeading(busPropBRTCalcDataModel.incomeTaxYTD.toCurrencyString)
        }

        s"has the correct 'whatYouOwe' p1 paragraph '${crysMessages.p1}'" in {
          wyoSection.getElementById("inYearP1").text shouldBe crysMessages.p1
        }

        s"has the correct 'whatYouOwe' p2 paragraph '${crysMessages.directDebit}'" in {
          wyoSection.getElementById("inYearP2").text shouldBe crysMessages.directDebit
        }

        s"has the correct warning message for late payments '${crysMessages.warning}'" in {
          wyoSection.getElementById("late-warning").text shouldBe crysMessages.warning
        }

      }

    }

    "have a Calculation Breakdown" that {

      "for users with both a property and a business" which {
        "have just the basic rate of tax" should {
          val setup = pageSetup(busPropBRTCalcDataModel, testIncomeSources)
          import setup._

          s"have a heading of ${crysMessages.breakdownHeading}" in {
            document.getElementById("howCalculatedHeading").text shouldBe crysMessages.breakdownHeading
          }

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
              document.getElementById("brt-it-calc").text shouldBe model.payPensionsProfitAtBRT.toCurrencyString
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
              document.getElementById("hrt-it-calc").text shouldBe model.payPensionsProfitAtHRT.toCurrencyString
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
              document.getElementById("art-it-calc").text shouldBe model.payPensionsProfitAtART.toCurrencyString
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

    "have a couple of sentences about adjustments" in {
      document.getElementById("adjustments").text shouldBe crysMessages.errors
      document.getElementById("changes").text shouldBe crysMessages.changes
    }

    "have an Additional Payments section" that {

      lazy val aPSection = document.getElementById("Additional payment")

      s"has a heading of ${crysMessages.aPHeading}" in {
        aPSection.getElementById("AdditionalPaymentHeading").text shouldBe crysMessages.aPHeading
      }

      s"has a paragraph about advanced payments" in {
        aPSection.getElementById("advancedPayment").text shouldBe crysMessages.advancedPayment
      }

      "have a progressive disclosure section" that {

        s"has a label of ${}" in {
          aPSection.getElementById("aboutPoA").text shouldBe crysMessages.aboutPoA
        }

        "has multiple paragraphs with information about payments on account" in {
          aPSection.getElementById("PoA-p1").text shouldBe crysMessages.aPp1
          aPSection.getElementById("PoA-p2").text shouldBe crysMessages.aPp2
          aPSection.getElementById("PoA-p3").text shouldBe crysMessages.aPp3
          aPSection.getElementById("PoA-p4").text shouldBe crysMessages.aPp4
        }

      }

    }

  }

}

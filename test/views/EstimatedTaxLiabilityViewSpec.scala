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

import assets.Messages.{EstimatedTaxLiability => messages, Sidebar => sidebarMessages}
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
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import utils.{ImplicitCurrencyFormatter, TestSupport}

class EstimatedTaxLiabilityViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = fakeApplication.injector.instanceOf[FrontendAppConfig]

  val testMtdItUser: MtdItUser = MtdItUser(testMtditid, testNino, Some(testUserDetails))
  val testIncomeSources: IncomeSourcesModel = IncomeSourcesModel(Some(businessIncomeModel), Some(propertyIncomeModel))
  val testBusinessIncomeSource: IncomeSourcesModel = IncomeSourcesModel(Some(businessIncomeModel), None)
  val testPropertyIncomeSource: IncomeSourcesModel = IncomeSourcesModel(None, Some(propertyIncomeModel))

  private def pageSetup(calcDataModel: CalculationDataModel, incomeSources: IncomeSourcesModel) = new {
    lazy val page: HtmlFormat.Appendable = views.html.estimatedTaxLiability(
      CalcBreakdown.calculationDisplaySuccessModel(calcDataModel),
      testYear)(FakeRequest(),applicationMessages, mockAppConfig, testMtdItUser, incomeSources)
    lazy val document: Document = Jsoup.parse(contentAsString(page))

    implicit val model: CalculationDataModel = calcDataModel
  }

  "The EstimatedTaxLiability view" should {

    val setup = pageSetup(busPropBRTCalcDataModel,testIncomeSources)
    import setup._

    s"have the user name '$testUserName' in the service info bar" in {
      document.getElementById("service-info-user-name").text() shouldBe testUserName
    }

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }

    s"have the tax year '${messages.taxYear}'" in {
      document.getElementById("tax-year").text() shouldBe messages.taxYear
    }

    s"have the page heading '${messages.pageHeading}'" in {
      document.getElementById("page-heading").text() shouldBe messages.pageHeading
    }

    s"have an Estimated Tax Liability section" which {

      lazy val estimateSection = document.getElementById("estimated-tax")

      s"has a paragraph with '${messages.EstimateTax.p1}'" in {
        estimateSection.getElementById("p1").text() shouldBe messages.EstimateTax.p1
      }

      s"has the correct Estimated Tax Amount of '${busPropBRTCalcDataModel.incomeTaxYTD}'" in {
        estimateSection.getElementById("in-year-estimate").text shouldBe "£" + busPropBRTCalcDataModel.incomeTaxYTD
      }

      s"has a calculation date paragraph with '${messages.EstimateTax.calcDate("6 July 2017")}'" in {
        estimateSection.getElementById("in-year-estimate-date").html() shouldBe messages.EstimateTax.calcDate("6 July 2017")
      }

      s"has a paragraph to warn them that their estimate might change" in {
        estimateSection.getElementById("changes").text shouldBe messages.EstimateTax.changes
      }

      s"has a calculation date of the 6 July 2017" in {
        estimateSection.getElementById("calc-date").text shouldBe "6 July 2017"
      }

      s"has a payment paragraph with '${messages.EstimateTax.payment}'" in {
        estimateSection.getElementById("payment").text() shouldBe messages.EstimateTax.payment
      }
    }

    "have a Calculation Breakdown section" that {

      "for users with both a property and a business" which {
        "have just the basic rate of tax" should {
          val setup = pageSetup(busPropBRTCalcDataModel, testIncomeSources)
          import ImplicitCurrencyFormatter._
          import setup._

          s"have a business profit section amount of ${model.profitFromSelfEmployment}" in {
            document.getElementById("business-profit").text shouldBe model.profitFromSelfEmployment.toCurrencyString
          }

          s"have a property profit amount of ${model.profitFromUkLandAndProperty}" in {
            document.getElementById("property-profit").text shouldBe model.profitFromSelfEmployment.toCurrencyString
          }

          s"have a personal allowance amount of ${model.personalAllowance}" in {
            document.getElementById("personal-allowance").text shouldBe model.personalAllowance.toCurrencyString
          }

          s"have a taxable income amount of ${model.totalIncomeOnWhichTaxIsDue}" in {
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
          s"have a National Insurance amount of ${model.nicTotal}" in {
            document.getElementById("ni-amount").text shouldBe model.nicTotal.toCurrencyString
          }
          s"have a total tax estimate of ${model.incomeTaxYTD}" in {
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
          s"have a National Insurance amount of ${model.nicTotal}" in {
            document.getElementById("ni-amount").text shouldBe model.nicTotal.toCurrencyString
          }
          s"have a total tax estimate of ${model.incomeTaxYTD}" in {
            document.getElementById("total-estimate").text shouldBe model.incomeTaxYTD.toCurrencyString
          }

        }
      }

      "when no breakdown data is retrieved" should {
        lazy val noBreakdownPage = views.html.estimatedTaxLiability(
          CalcBreakdown.calculationDisplayNoBreakdownModel, testYear)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser, testIncomeSources)
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
    }

    "have sidebar section " in {
      document.getElementById("sidebar") shouldNot be(null)
    }
  }
}

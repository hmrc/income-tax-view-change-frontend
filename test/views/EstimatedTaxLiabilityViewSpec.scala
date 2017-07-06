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

import assets.Messages.{EstimatedTaxLiability => messages}
import assets.Messages.{Sidebar => sidebarMessages}
import config.FrontendAppConfig
import org.jsoup.Jsoup
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.TestSupport
import assets.TestConstants._
import assets.TestConstants.Estimates._
import assets.TestConstants.PropertyIncome._
import assets.TestConstants.BusinessDetails._
import assets.TestConstants.CalcBreakdown._
import auth.MtdItUser
import models.IncomeSourcesModel

class EstimatedTaxLiabilityViewSpec extends TestSupport {

  lazy val mockAppConfig = fakeApplication.injector.instanceOf[FrontendAppConfig]

  val testMtdItUser: MtdItUser = MtdItUser(testMtditid, testNino)
  val testIncomeSources: IncomeSourcesModel = IncomeSourcesModel(Some(businessIncomeModel), Some(propertyIncomeModel))
  val testBusinessIncomeSource: IncomeSourcesModel = IncomeSourcesModel(Some(businessIncomeModel), None)
  val testPropertyIncomeSource: IncomeSourcesModel = IncomeSourcesModel(None, Some(propertyIncomeModel))

  lazy val page = views.html.estimatedTaxLiability(
    CalcBreakdown.calculationDisplaySuccessModel(busPropJustBRTCalcDataModel),
    testYear)(FakeRequest(),applicationMessages, mockAppConfig, testMtdItUser, testIncomeSources)

  lazy val document = Jsoup.parse(contentAsString(page))
  "The EstimatedTaxLiability view" should {

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

      s"has the correct Estimated Tax Amount of '${busPropJustBRTCalcDataModel.incomeTaxYTD}'" in {
        estimateSection.getElementById("in-year-estimate").text shouldBe "£" + busPropJustBRTCalcDataModel.incomeTaxYTD
      }

      s"has a payment paragraph with '${messages.EstimateTax.payment}'" in {
        estimateSection.getElementById("payment").text() shouldBe messages.EstimateTax.payment
      }
    }

    "have a Calculation Breakdown section" which {
      "when no breakdown data is retrieved" should {
        lazy val noBreakdownPage = views.html.estimatedTaxLiability(
          CalcBreakdown.calculationDisplayNoBreakdownModel, testYear)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser, testIncomeSources)
        lazy val noBreakdownDocument = Jsoup.parse(contentAsString(noBreakdownPage))

        "not display a breakdown section" in {
          noBreakdownDocument.getElementById("calc-breakdown-inner-link") shouldBe null
        }
      }
      "when the user only has businesses registered" should {
        lazy val page = views.html.estimatedTaxLiability(
          CalcBreakdown.calculationDisplaySuccessModel(justBusinessCalcDataModel),
          testYear)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser, testBusinessIncomeSource)
        lazy val document = Jsoup.parse(contentAsString(page))

        "display the business profit amount" in {
          document.getElementById("business-profit").text shouldBe "£3,000"
        }

        "not display the property profit section" in {
          document.getElementById("property-profit-section") shouldBe null
        }
      }
      "when the user only has properties registered" should {
        "display the property profit section" in {

        }
        "not display the business profit section" in {

        }
      }
    }

    "have sidebar section " which {

      lazy val sidebarSection = document.getElementById("sidebar")

      "has a heading for the MTDITID" in {
        sidebarSection.getElementById("it-reference-heading").text() shouldBe sidebarMessages.mtditidHeading
      }

      "has the correct value for the MTDITID/reporting ref" in {
        sidebarSection.getElementById("it-reference").text() shouldBe testMtdItUser.mtditid
      }

      "has a heading for viewing your reports" in {
        sidebarSection.getElementById("obligations-heading").text() shouldBe sidebarMessages.reportsHeading
      }

      "has a link to view your reports" which {

        s"has the correct href to '${controllers.routes.ObligationsController.getObligations().url}'" in {
          sidebarSection.getElementById("obligations-link").attr("href") shouldBe controllers.routes.ObligationsController.getObligations().url
        }

        s"has the correct link wording of '${sidebarMessages.reportsLink}'" in {
          sidebarSection.getElementById("obligations-link").text() shouldBe sidebarMessages.reportsLink
        }

      }

      "has a link to view self assessment details" which {
        "has a heading for viewing self assessment details" in {
          sidebarSection.getElementById("sa-link-heading").text shouldBe sidebarMessages.selfAssessmentHeading
        }

        s"has the correct href to 'business-tax-account/self-assessment'" in {
          sidebarSection.getElementById("sa-link").attr("href") should endWith("/business-account/self-assessment")
        }

        "has the correct link wording" in {
          sidebarSection.getElementById("sa-link").text shouldBe sidebarMessages.selfAssessmentLink
        }
      }
    }
  }

}

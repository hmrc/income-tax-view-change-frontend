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

package views.getLatestCalculation

import assets.BaseTestConstants._
import assets.EstimatesTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.Messages
import assets.Messages.{Breadcrumbs => breadcrumbMessages}
import auth.MtdItUser
import implicits.ImplicitDateFormatter
import models.calculation.EstimatesViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class EstimateViewSpec extends TestSupport with ImplicitDateFormatter {

  val bizAndPropertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), businessAndPropertyAligned)(FakeRequest())
  val bizUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), singleBusinessIncome)(FakeRequest())
  val propertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), propertyIncomeOnly)(FakeRequest())

  private def pageSetup(model: EstimatesViewModel, user: MtdItUser[_]) = new {
    lazy val page: HtmlFormat.Appendable = views.html.getLatestCalculation.estimate(model)(FakeRequest(),applicationMessages, frontendAppConfig, user)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The estimate view" when {

    "given a EstimatesViewModel containing an annual estimate" should {

      val setup = pageSetup(fullEstimateViewModel, bizAndPropertyUser)
      import setup._
      val messages = new Messages.Calculation(testYear)

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have a breadcrumb trail" in {
        document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
        document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
        document.getElementById("breadcrumb-estimates").text shouldBe breadcrumbMessages.estimates
        document.getElementById("breadcrumb-it-estimate").text shouldBe breadcrumbMessages.itEstimate(testYear)
      }

      s"have the tax year '${messages.subheading}'" in {
        document.getElementById("sub-heading").text() shouldBe messages.subheading
      }

      s"have the page heading '${messages.heading}'" in {
        document.getElementById("heading").text() shouldBe messages.heading
      }

      s"has a paragraph to explain the reported figures '${messages.reportedFigures}'" in {
        document.getElementById("reported-figures").text() shouldBe messages.reportedFigures
      }

      s"have an Estimated Tax Liability section" which {

        lazy val estimateSection = document.getElementById("estimated-tax")

        "has a section for EoY Estimate" which {

          lazy val eoySection = estimateSection.getElementById("eoyEstimate")
          lazy val annualEstimateText = s"£${fullEstimateViewModel.annualEstimate.getOrElse("")}"

          s"has the correct Annual Tax Amount Estimate Heading of '${
            messages.EoyEstimate.heading(annualEstimateText)
          }" in {
            eoySection.getElementById("eoyEstimateHeading").text shouldBe
            messages.EoyEstimate.heading(annualEstimateText)
          }

          s"has the correct estimate p1 paragraph '${messages.EoyEstimate.p1}'" in {
            eoySection.getElementById("eoyP1").text shouldBe messages.EoyEstimate.p1
          }
        }

        "has a section for In Year (Current) Estimate" which {

          lazy val inYearSection = estimateSection.getElementById("inYearEstimate")
          lazy val currentEstimateText = s"£${fullEstimateViewModel.currentEstimate}"

          s"has the correct Annual Tax Amount Estimate Heading of '${
            messages.InYearEstimate.heading(currentEstimateText)
          }" in {
            inYearSection.getElementById("inYearEstimateHeading").text shouldBe
            messages.InYearEstimate.heading(currentEstimateText)
          }

          s"has the correct estimate p1 paragraph '${messages.InYearEstimate.p1(lastTaxCalcSuccess.calcTimestamp.get.toLocalDateTime.toLongDateTime)}'" in {
            inYearSection.getElementById("inYearP1").text shouldBe
              messages.InYearEstimate.p1(lastTaxCalcSuccess.calcTimestamp.get.toLocalDateTime.toLongDateTime)
          }

          "has progressive disclosure for why their estimate might change" which {

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
    }
  }

  "given a EstimatesViewModel containing no annual estimate" should {

    val setup = pageSetup(minEstimateViewModel, bizAndPropertyUser)
    import setup._
    val messages = new Messages.Calculation(testYear)

    s"have an Estimated Tax Liability section" which {

      lazy val estimateSection = document.getElementById("estimated-tax")

      "has no section for EoY Estimate" in {

        lazy val eoySection = estimateSection.getElementById("eoyEstimate")

        intercept[NullPointerException](eoySection.getElementById("eoyEstimateHeading").text)
      }
    }
  }
}

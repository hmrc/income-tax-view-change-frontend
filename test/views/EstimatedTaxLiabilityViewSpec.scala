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

import assets.BaseTestConstants._
import assets.CalcBreakdownTestConstants._
import assets.EstimatesTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.Messages.{Breadcrumbs => breadcrumbMessages}
import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import implicits.ImplicitDateFormatter
import models.calculation.Calculation
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport


class EstimatedTaxLiabilityViewSpec extends TestSupport with ImplicitDateFormatter with FeatureSwitching {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val bizAndPropertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), businessAndPropertyAligned)(FakeRequest())
  val bizUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), singleBusinessIncome)(FakeRequest())
  val propertyUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), propertyIncomeOnly)(FakeRequest())

  val request: Request[_] = FakeRequest()

  trait Setup {

    val calcDataModel: Calculation = busPropBRTCalcDataModel

    val page: HtmlFormat.Appendable = views.html.estimatedTaxLiability(
      calculationDisplaySuccessModel(calcDataModel), testYear
    )(request, applicationMessages, mockAppConfig, bizAndPropertyUser)

    val document: Document = Jsoup.parse(contentAsString(page))

    def getElementById(id: String): Option[Element] = Option(document.getElementById(id))

    def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)

    def getLinkOfElementById(id: String): Option[String] = getElementById(id).map(_.attr("href"))
  }

  "estimatedTaxLiability" should {

    "have a title" in new Setup {
      document.title shouldBe s"Tax estimate for ${testYear - 1} - $testYear"
    }

    "have a breadcrumb trail" in new Setup {
      getTextOfElementById("breadcrumb-bta") shouldBe Some(breadcrumbMessages.bta)
      getLinkOfElementById("breadcrumb-bta") shouldBe Some(mockAppConfig.businessTaxAccount)

      getTextOfElementById("breadcrumb-it") shouldBe Some(breadcrumbMessages.it)
      getLinkOfElementById("breadcrumb-it") shouldBe Some(controllers.routes.HomeController.home().url)

      getTextOfElementById("breadcrumb-tax-years") shouldBe Some(breadcrumbMessages.taxYears)
      getLinkOfElementById("breadcrumb-tax-years") shouldBe Some(controllers.routes.TaxYearsController.viewTaxYears().url)

      getTextOfElementById("breadcrumb-it-estimate") shouldBe Some(breadcrumbMessages.basicItEstimate(testYear))
      getLinkOfElementById("breadcrumb-it-estimate") shouldBe Some("")
    }

    "have a heading" which {
      "has a name of the user" in new Setup {
        getTextOfElementById("user-name-heading") shouldBe Some("Albert Einstein")
      }
      "has the main heading for the page" in new Setup {
        getTextOfElementById("heading") shouldBe Some(s"Tax estimate for ${testYear - 1} - $testYear")
      }
      "has the utr reference" in new Setup {
        getTextOfElementById("utr-reference-heading") shouldBe Some(s"Unique Taxpayer Reference - ${user.mtditid}")
      }
    }

    "not display the subheading with information when there is a breakdown" in new Setup {
      getElementById("inYearEstimateHeading") shouldBe None
      getElementById("inYearP1") shouldBe None
    }

    "have a calculation breakdown when it is present and the feature switch is on" in new Setup {
      getElementById("inYearCalcBreakdown").isDefined shouldBe true
    }

  }


}

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
import assets.Messages.{Sidebar => sidebarMessages}
import assets.TestConstants.BusinessDetails._
import assets.TestConstants.Estimates._
import assets.TestConstants.PropertyIncome._
import assets.TestConstants._
import auth.MtdItUser
import config.FrontendAppConfig
import models.{IncomeSourcesModel, LastTaxCalculationWithYear}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import utils.TestSupport


class EstimatesViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testIncomeSources: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), Some(propertyIncomeModel))
  val testBusinessIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), None)
  val testPropertyIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List.empty, Some(propertyIncomeModel))

  private def pageSetup(incomeSources: IncomeSourcesModel, calcs: List[LastTaxCalculationWithYear]) = new {
    val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), incomeSources)
    lazy val page: HtmlFormat.Appendable =
      views.html.estimates(calcs,testYear)(FakeRequest(),applicationMessages, mockAppConfig)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The EstimatedTaxLiability view" when {
    "the user has estimates for two tax years" should {
      val setup = pageSetup(testIncomeSources, lastTaxCalcWithYearList)
      import setup._
      val messages = new Messages.Estimates

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have no sidebar section " in {
        document.getElementById("sidebar") should be(null)
      }

      s"have the paragraph '${messages.p1}'" in {
        document.getElementById("view-estimates").text shouldBe messages.p1
      }

      "display the links for both of the tax years with estimates" in {
        document.getElementById(s"estimates-link-$testYear").text shouldBe messages.taxYearLink((testYear - 1).toString, testYear.toString)
        document.getElementById(s"estimates-link-$testYearPlusOne").text shouldBe messages.taxYearLink(testYear.toString, testYearPlusOne.toString)
      }
    }

    "the user has no estimates" should {
      val setup = pageSetup(testIncomeSources, List())
      import setup._
      val messages = new Messages.Estimates

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have sidebar section " in {
        document.getElementById("sidebar") should be(null)
      }

      s"have the paragraph '${messages.noEstimates}'" in {
        document.getElementById("no-estimates").text shouldBe messages.noEstimates
      }
    }

  }
}

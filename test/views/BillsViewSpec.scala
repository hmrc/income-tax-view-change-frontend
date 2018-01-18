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
import models.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import utils.TestSupport

class BillsViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testIncomeSources: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), Some(propertyIncomeModel))
  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), testIncomeSources)(FakeRequest())
  val testBusinessIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), None)
  val testPropertyIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List.empty, Some(propertyIncomeModel))

  private def pageSetup(incomeSources: IncomeSourcesModel) = new {
    lazy val pageNoBills: HtmlFormat.Appendable = views.html.bills(
        List())(FakeRequest(),applicationMessages, mockAppConfig, incomeSources)
    lazy val documentNoBills: Document = Jsoup.parse(contentAsString(pageNoBills))

    lazy val page2Bills: HtmlFormat.Appendable = views.html.bills(
        lastTaxCalcWithYearCrystallisedList)(FakeRequest(),applicationMessages, mockAppConfig, incomeSources)
    lazy val document2Bills: Document = Jsoup.parse(contentAsString(page2Bills))
  }

  "The EstimatedTaxLiability view" should {

    val setup = pageSetup(testIncomeSources)
    import setup._
    val messages = new Messages.Calculation(testYear).Bills
    val messages2019 = new Messages.Calculation(testYearPlusOne).Bills

    "when the user has bills for 2 taxYears" should {

      s"have the title '${messages.billsTitle}'" in {
        document2Bills.title() shouldBe messages.billsTitle
      }

      s"state ${messages.viewBills}" in {
        document2Bills.getElementById("finalised-bills").text shouldBe messages.viewBills
      }

      "display the links for both of the tax years with bills" in {
        document2Bills.getElementById(s"bills-link-$testYear").text shouldBe messages.billLink
        document2Bills.getElementById(s"bills-link-$testYearPlusOne").text shouldBe messages2019.billLink
      }

      "show a link to earlier bills, with the correct URL" in {
        document2Bills.getElementById("earlier-bills").text shouldBe messages.earlierBills
        document2Bills.getElementById("view-sa-calcs").attr("href") shouldBe mockAppConfig.selfAssessmentUrl
      }

      "have no sidebar section " in {
        document2Bills.getElementById("sidebar") should be(null)
      }

    }

    "without any bills" should {

      s"have the title '${messages.billsTitle}'" in {
        documentNoBills.title() shouldBe messages.billsTitle
      }

      "state that you've had no bills" in {
        documentNoBills.getElementById("no-bills").text shouldBe messages.noBills
      }

      "show a link to earlier bills, with the correct URL" in {
        documentNoBills.getElementById("earlier-bills").text shouldBe messages.earlierBills
        documentNoBills.getElementById("view-sa-calcs").attr("href") shouldBe mockAppConfig.selfAssessmentUrl
      }

      "have no sidebar section " in {
        documentNoBills.getElementById("sidebar") should be(null)
      }

    }
  }
}

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
import assets.BusinessDetailsTestConstants.businessIncomeModelAlignedTaxYear
import assets.PropertyDetailsTestConstants.propertyIncomeModel
import assets.BusinessDetailsTestConstants._
import assets.BaseTestConstants._
import auth.MtdItUser
import config.FrontendAppConfig
import models.{BusinessIncomeModel, IncomeSourcesModel, PropertyIncomeModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import utils.TestSupport

class AccountDetailsViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testIncomeSources: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), Some(propertyIncomeModel))
  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), testIncomeSources)(FakeRequest())
  val testBusinessIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), None)
  val testPropertyIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List.empty, Some(propertyIncomeModel))

  private def pageSetup(businesses: List[(BusinessIncomeModel,Int)], properties: Option[PropertyIncomeModel]) = new {
    lazy val page: HtmlFormat.Appendable = views.html.accountDetailsView(
      businesses, properties)(FakeRequest(),applicationMessages, mockAppConfig, testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The Account Details view" when {

    "passed a business and a property" should {

      val setup = pageSetup(List((businessIncomeModel, 0)), Some(propertyIncomeModel))
      import setup._
      val messages = Messages.AccountDetails

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have a breadcrumb trail" in {
        document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
        document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
        document.getElementById("breadcrumb-account").text shouldBe breadcrumbMessages.details
      }

      s"have the page heading '${messages.heading}'" in {
        document.getElementById("page-heading").text() shouldBe messages.heading
      }

      s"have a 'your-businesses' section" in {
        document.getElementById("your-businesses").text() shouldBe messages.yourBusinesses
        document.getElementById("business-link-1").text() shouldBe "business"
      }

      s"have a 'your-properties' section" in {
        document.getElementById("your-properties").text() shouldBe messages.yourProperties
        document.getElementById("reporting-period").text() shouldBe messages.reportingPeriod("6 April", "5 April")
      }

      "show a back link to the Income Tax home page, when the home page feature is enabled" in {
        mockAppConfig.features.homePageEnabled(true)
        document.getElementById("it-home-back") shouldNot be(null)
      }

    }

    "only passed a business" should {

      val setup = pageSetup(List((businessIncomeModel, 0)), None)
      import setup._
      val messages = Messages.AccountDetails

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have a breadcrumb trail" in {
        document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
        document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
        document.getElementById("breadcrumb-account").text shouldBe breadcrumbMessages.details
      }

      s"have the page heading '${messages.heading}'" in {
        document.getElementById("page-heading").text() shouldBe messages.heading
      }

      s"have a 'your-businesses' section" in {
        document.getElementById("your-businesses").text() shouldBe messages.yourBusinesses
        document.getElementById("business-link-1").text() shouldBe "business"
      }

      s"not have a 'your-properties' section" in {
        document.getElementById("your-properties") shouldBe null
        document.getElementById("reporting-period") shouldBe null
      }

      "show a back link to the Income Tax home page, when the home page feature is enabled" in {
        mockAppConfig.features.homePageEnabled(true)
        document.getElementById("it-home-back") shouldNot be(null)
      }

    }

    "only passed a property" should {

      val setup = pageSetup(List(), Some(propertyIncomeModel))
      import setup._
      val messages = Messages.AccountDetails

      s"have the title '${messages.title}'" in {
        document.title() shouldBe messages.title
      }

      "have a breadcrumb trail" in {
        document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
        document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
        document.getElementById("breadcrumb-account").text shouldBe breadcrumbMessages.details
      }

      s"have the page heading '${messages.heading}'" in {
        document.getElementById("page-heading").text() shouldBe messages.heading
      }

      s"not have a 'your-businesses' section" in {
        document.getElementById("your-businesses") shouldBe null
        document.getElementById("business-link-1") shouldBe null
      }

      s"have a 'your-properties' section" in {
        document.getElementById("your-properties").text() shouldBe messages.yourProperties
        document.getElementById("reporting-period").text() shouldBe messages.reportingPeriod("6 April", "5 April")
      }

      "show a back link to the Income Tax home page, when the home page feature is enabled" in {
        mockAppConfig.features.homePageEnabled(true)
        document.getElementById("it-home-back") shouldNot be(null)
      }

    }

  }

}

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
import assets.TestConstants.BusinessDetails.businessIncomeModelAlignedTaxYear
import assets.TestConstants.PropertyIncome.propertyIncomeModel
import auth.MtdItUser
import config.FrontendAppConfig
import utils.TestSupport
import assets.TestConstants._
import models.{BusinessModel, IncomeSourcesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat

class BusinessDetailsViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testIncomeSources: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), Some(propertyIncomeModel))
  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), testIncomeSources)(FakeRequest())
  val testBusinessIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List(businessIncomeModelAlignedTaxYear), None)
  val testPropertyIncomeSource: IncomeSourcesModel = IncomeSourcesModel(List.empty, Some(propertyIncomeModel))

  private def pageSetup(businessModel: BusinessModel) = new {
    lazy val page: HtmlFormat.Appendable = views.html.businessDetailsView(
      BusinessDetails.business1)(FakeRequest(),applicationMessages, mockAppConfig, testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The Business Details view" should {

    val setup = pageSetup(BusinessDetails.business1)
    import setup._
    val messages = Messages.BusinessDetails
    val business = BusinessDetails.business1

    s"have the title '${messages.title}'" in {
      document.title() shouldBe business.tradingName
    }

    "have a breadcrumb trail" in {
      document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
      document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
      document.getElementById("breadcrumb-details").text shouldBe breadcrumbMessages.details
      document.getElementById(s"${business.tradingName}").text shouldBe BusinessDetails.business1.tradingName
    }

    s"have the page heading '${business.tradingName}'" in {
      document.getElementById("page-heading").text() shouldBe business.tradingName
    }

    s"have a reporting period of ${business.accountingPeriod.start} to ${business.accountingPeriod.end}" in {
      document.getElementById("reporting-period").text() shouldBe
        messages.reportingPeriod(s"${business.accountingPeriod.start.toLongDateNoYear}", s"${business.accountingPeriod.end.toLongDateNoYear}")
    }

    "not have a 'ceased trading' message" in {
      document.getElementById("cessation-date") shouldBe null
    }

    "have an Address and contact section" which {
      s"has the sub-heading ${messages.addressAndContact}" in {
        document.getElementById("address-details").text() shouldBe messages.addressAndContact
      }
      s"has the correct values for the heading and value of 'trading name'" in {
        document.getElementById("trading-name").text() shouldBe messages.tradingName
        document.getElementById("trading-name-business").text() shouldBe business.tradingName
      }
      s"has the correct values for the heading and address lines of 'business address'" in {
        document.getElementById("business-address").text() shouldBe messages.businessAddress
        document.getElementById("address-line-1").text() shouldBe business.businessAddressLineOne.get
        document.getElementById("address-line-2").text() shouldBe business.businessAddressLineTwo.get
        document.getElementById("address-line-3").text() shouldBe business.businessAddressLineThree.get
        document.getElementById("address-line-4").text() shouldBe business.businessAddressLineFour.get
      }
    }

    "have an additional information section stating the accounting method" in {
      document.getElementById("additional-information").text() shouldBe messages.additionalInfo
      document.getElementById("accounting-method").text() shouldBe messages.accountingMethod(business.accountingType.toLowerCase)
    }

    "show a back link to the account details page" in {
      document.getElementById("it-account-back") shouldNot be(null)
    }

  }

}

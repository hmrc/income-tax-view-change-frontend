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

import assets.BaseTestConstants._
import assets.BusinessDetailsTestConstants._
import assets.IncomeSourceDetailsTestConstants.businessAndPropertyAligned
import assets.Messages
import assets.Messages.{Breadcrumbs => breadcrumbMessages}
import auth.MtdItUser
import config.FrontendAppConfig
import models.incomeSourceDetails.BusinessDetailsModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import implicits.ImplicitDateFormatter._
import testUtils.TestSupport

class BusinessDetailsViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), businessAndPropertyAligned)(FakeRequest())

  private def pageSetup(businessModel: BusinessDetailsModel) = new {
    lazy val page: HtmlFormat.Appendable = views.html.businessDetailsView(businessModel)(FakeRequest(),applicationMessages, mockAppConfig, testMtdItUser)
    lazy val document: Document = Jsoup.parse(contentAsString(page))
  }

  "The Business Details view" when {

    "passed a business without a cessation date" should {

      val setup = pageSetup(business1)
      import setup._
      val messages = Messages.BusinessDetails
      val business = business1

      s"have the title '${business.tradingName.get}'" in {
        document.title() shouldBe business.tradingName.get
      }

      "have a breadcrumb trail" in {
        document.getElementById("breadcrumb-bta").text shouldBe breadcrumbMessages.bta
        document.getElementById("breadcrumb-it").text shouldBe breadcrumbMessages.it
        document.getElementById("breadcrumb-account").text shouldBe breadcrumbMessages.details
        document.getElementById(s"${business.tradingName.get}").text shouldBe business1.tradingName.get
      }

      s"have the page heading '${business.tradingName.get}'" in {
        document.getElementById("page-heading").text() shouldBe business.tradingName.get
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
          document.getElementById("trading-name-business").text() shouldBe business.tradingName.get
        }
        s"has the correct values for the heading and address lines of 'business address'" in {
          document.getElementById("business-address").text() shouldBe messages.businessAddress
          document.getElementById("address-line-1").text() shouldBe business.address.get.addressLine1
          document.getElementById("address-line-2").text() shouldBe business.address.get.addressLine2.get
          document.getElementById("address-line-3").text() shouldBe business.address.get.addressLine3.get
          document.getElementById("address-line-4").text() shouldBe business.address.get.addressLine4.get
          document.getElementById("address-line-5").text() shouldBe business.address.get.postCode.get
        }
      }

      "have an additional information section stating the accounting method" in {
        document.getElementById("additional-information").text() shouldBe messages.additionalInfo
        document.getElementById("accounting-method").text() shouldBe messages.accountingMethod(business.cashOrAccruals.get.toLowerCase)
      }

      "show a back link to the account details page" in {
        document.getElementById("it-account-back") shouldNot be(null)
      }
    }

    "passed a business with a cessation date" should {

      val setup = pageSetup(ceasedBusiness)
      import setup._
      val messages = Messages.BusinessDetails
      val cessationDate = ceasedBusiness.cessation.get.date.get

      "have a 'ceased trading' message" in {
        document.getElementById("cessation-date").text() shouldBe messages.ceasedTrading(s"${cessationDate.toLongDate}")
      }

    }
  }

}

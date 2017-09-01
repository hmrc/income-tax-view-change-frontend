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

import assets.Messages.{Obligations => messages, Sidebar => sidebarMessages}
import assets.TestConstants.IncomeSourceDetails._
import assets.TestConstants.{testMtditid, testNino, testUserDetails, testUserName}
import auth.MtdItUser
import config.FrontendAppConfig
import models.{ObligationModel, ObligationsModel}
import org.jsoup.Jsoup
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.ImplicitDateFormatter._
import utils.TestSupport

class ObligationsViewSpec extends TestSupport{

  lazy val mockAppConfig = app.injector.instanceOf[FrontendAppConfig]

  val model = ObligationModel(start = "2017-1-1".toLocalDate, end = "2017-3-31".toLocalDate, due = "2017-4-5".toLocalDate, true)
  val testMtdItUser: MtdItUser = MtdItUser(testMtditid, testNino, Some(testUserDetails))
  val dummymodel = ObligationsModel(List(model))

  lazy val bothPage = views.html.obligations(
    Some(dummymodel), Some(dummymodel))(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser, bothIncomeSourceSuccessMisalignedTaxYear, serviceInfo)
  lazy val bizPage  =
    views.html.obligations(Some(dummymodel), None)(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser, bothIncomeSourceSuccessMisalignedTaxYear, serviceInfo)
  lazy val propPage =
    views.html.obligations(None, Some(dummymodel))(FakeRequest(), applicationMessages, mockAppConfig, testMtdItUser, bothIncomeSourceSuccessMisalignedTaxYear, serviceInfo)

  "The Obligations view" should {

    lazy val document = Jsoup.parse(contentAsString(bothPage))

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }

    s"have the an intro para '${messages.info}'" in {
      document.getElementById("obligation-intro").text() shouldBe messages.info
    }

    "have a table containing the obligations" should {

      "contain the heading for Report period" in {
        document.getElementById("bi-period-heading").text() shouldBe "Report period"
      }

      "contain the heading for Status" in {
        document.getElementById("bi-status-heading").text() shouldBe "Report due date"
      }

      "contain the first row and have the start date as '1 January 2017' and status 'Received'" in {
        document.getElementById("bi-ob-1-start").text() shouldBe "1 January 2017"
        document.getElementById("bi-ob-1-status").text() shouldBe "Received"
      }

      "not contain a second row" in {
        document.getElementById("bi-ob-2-status") shouldBe null
      }
    }

    "have sidebar section " in {
      document.getElementById("sidebar") shouldNot be(null)
    }

    "when only business obligations are returned" should {

      lazy val bizDocument = Jsoup.parse(contentAsString(bizPage))

      "contain a section for Business Obligations" in {
        bizDocument.getElementById("bi-section").text() shouldBe messages.businessHeading
      }

      "not contain Property Obligations section" in {
        bizDocument.getElementById("pi-section") shouldBe null
      }
    }

    "when only property obligations are returned" should {

      lazy val propDocument = Jsoup.parse(contentAsString(propPage))

      "contain a section for Business Obligations" in {
        propDocument.getElementById("pi-section").text() shouldBe messages.propertyHeading
      }

      "not contain Property Obligations section" in {
        propDocument.getElementById("bi-section") shouldBe null
      }
    }
  }

}

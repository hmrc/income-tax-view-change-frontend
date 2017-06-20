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

import auth.MtdItUser
import config.FrontendAppConfig
import models.{ObligationModel, ObligationsModel}
import org.jsoup.Jsoup
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.TestSupport
import assets.Messages.{Obligations => messages}
import assets.Messages.{Sidebar => sidebarMessages}
import assets.TestConstants.{testMtditid, testNino}
import utils.ImplicitDateFormatter._

class ObligationsViewSpec extends TestSupport{

  lazy val mockAppConfig = fakeApplication.injector.instanceOf[FrontendAppConfig]

  val model = ObligationModel(start = "2017-1-1".toLocalDate, end = "2017-3-31".toLocalDate, due = "2017-4-5".toLocalDate, true)
  val testMtdItUser: MtdItUser = MtdItUser(testMtditid, testNino)


  val dummymodel = ObligationsModel(List(model))

  lazy val page = views.html.obligations(dummymodel)(FakeRequest(), applicationMessages, mockAppConfig, user = testMtdItUser)
  lazy val document = Jsoup.parse(contentAsString(page))

  "The Obligations view" should {

    s"have the title '${messages.title}'" in {
      document.title() shouldBe messages.title
    }

    s"have the an intro para '${messages.info}'" in {
      document.getElementById("obligation-intro").text() shouldBe messages.info
    }

    "have a table containing the obligations" should {

      "contain the first row and have the start date as '1 January 2017' and status 'Received'" in {
        document.getElementById("bi-ob-1-start").text() shouldBe "1 January 2017"
        document.getElementById("bi-ob-1-status").text() shouldBe "Received"
      }

      "not contain a second row" in {
        document.getElementById("bi-ob-2-status") shouldBe null
      }
    }
    "have sidebar section " which {

      lazy val sidebarSection = document.getElementById("sidebar")

      "has a heading for the MTDITID" in {
        sidebarSection.getElementById("reporting-ref-heading").text() shouldBe sidebarMessages.mtditidHeading
      }

      "has the correct value for the MTDITID/reporting ref" in {
        sidebarSection.getElementById("reporting-ref").text() shouldBe testMtdItUser.mtditid
      }

      "has a heading for viewing your estimates" in {
        sidebarSection.getElementById("estimates-heading").text() shouldBe sidebarMessages.estimatesHeading
      }

      "has a link to view your estimates" which {

        s"has the correct href to '${controllers.routes.EstimatedTaxLiabilityController.getEstimatedTaxLiability().url}'" in {
          sidebarSection.getElementById("estimates-link").attr("href") shouldBe controllers.routes.EstimatedTaxLiabilityController.getEstimatedTaxLiability().url
        }

        s"has the correct link wording of '${sidebarMessages.estimatesLink}'" in {
          sidebarSection.getElementById("estimates-link").text() shouldBe sidebarMessages.estimatesLink
        }

      }

      "has a link to view self assessment details" which {
        "has a heading for viewing self assessment details" in {
          sidebarSection.getElementById("sa-link-heading").text shouldBe sidebarMessages.selfAssessmentHeading
        }

        s"has the correct href to ''" in {
          sidebarSection.getElementById("sa-link").attr("href") shouldBe ""
        }

        "has the correct link wording" in {
          sidebarSection.getElementById("sa-link").text shouldBe sidebarMessages.selfAssessmentLink
        }
      }
    }


  }

}

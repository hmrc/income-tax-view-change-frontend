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
import utils.ImplicitDateFormatter.localDate

class ObligationsViewSpec extends TestSupport{

  lazy val mockAppConfig = fakeApplication.injector.instanceOf[FrontendAppConfig]

  val model = ObligationModel(start = localDate("2017-1-1"), end = localDate("2017-3-31"), due = localDate("2017-4-5"), true)

  val dummymodel = ObligationsModel(List(model))

  lazy val page = views.html.obligations(dummymodel)(FakeRequest(), applicationMessages, mockAppConfig, user = MtdItUser("BobTheRaccoon", "AA111111A"))
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

  }

}

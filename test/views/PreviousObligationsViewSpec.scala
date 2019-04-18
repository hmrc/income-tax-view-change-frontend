/*
 * Copyright 2019 HM Revenue & Customs
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

import assets.BaseTestConstants.testMtdItUser
import assets.Messages.{PreviousObligations => previousObligations}
import config.FrontendAppConfig
import implicits.ImplicitDateFormatter
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.twirl.api.HtmlFormat
import testUtils.TestSupport

class PreviousObligationsViewSpec extends TestSupport with ImplicitDateFormatter {

  lazy val mockAppConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  class Setup {
    val html: HtmlFormat.Appendable = views.html.previousObligations(Nil)(FakeRequest(), implicitly, mockAppConfig, testMtdItUser)
    val pageDocument: Document = Jsoup.parse(contentAsString(html))

    def getElementById(id: String): Option[Element] = Option(pageDocument.getElementById(id))
    def getTextOfElementById(id: String): Option[String] = getElementById(id).map(_.text)
  }

  "previousObligations" should {

    "have a title" in new Setup {
      pageDocument.title shouldBe previousObligations.title
    }

  }

}


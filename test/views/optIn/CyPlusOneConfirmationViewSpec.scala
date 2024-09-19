/*
 * Copyright 2024 HM Revenue & Customs
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

package views.optIn

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}
import testUtils.TestSupport
import views.html.optIn.CyPlusOneConfirmation

class CyPlusOneConfirmationViewSpec extends TestSupport {

  val cyPlusOneConfirmationView: CyPlusOneConfirmation = app.injector.instanceOf[CyPlusOneConfirmation]

  class Setup(isAgent: Boolean = true) {
    val pageDocument: Document = Jsoup.parse(contentAsString(cyPlusOneConfirmationView(isAgent)))
  }

  object plusOneConfirmationMessages {
    val heading: String = messages("optin.cyPlusOneConfirmation.heading")
    val title: String = messages("htmlTitle", heading)
    val text: String = messages("optin.cyPlusOneConfirmation.text")
    val continueButton: String = messages("optin.cyPlusOneConfirmation.confirm")
    val cancelButton: String = messages("optin.cyPlusOneConfirmation.cancel")
    val cancelButtonHref: String = controllers.routes.NextUpdatesController.show().url
    val agentCancelButtonHref: String = controllers.routes.NextUpdatesController.showAgent.url
  }
}

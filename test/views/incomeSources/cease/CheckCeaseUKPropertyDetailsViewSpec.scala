/*
 * Copyright 2023 HM Revenue & Customs
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

package views.incomeSources.cease

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.contentAsString
import play.twirl.api.HtmlFormat
import testUtils.{TestSupport, ViewSpec}
import views.html.incomeSources.cease.CheckCeaseUKPropertyDetails

class CheckCeaseUKPropertyDetailsViewSpec extends TestSupport with ViewSpec{
  val checkCeaseUKPropertyDetailsView = app.injector.instanceOf[CheckCeaseUKPropertyDetails]
  val businessEndDate = "2022-04-23"
  val businessEndShortLongDate = "23 Apr 2022"
  lazy val viewAgent: HtmlFormat.Appendable = checkCeaseUKPropertyDetailsView(true)(FakeRequest().withSession(forms.utils.SessionKeys.ceaseUKPropertyEndDate -> "2022-04-23"), implicitly)
  lazy val view: HtmlFormat.Appendable = checkCeaseUKPropertyDetailsView(false)(FakeRequest().withSession(forms.utils.SessionKeys.ceaseUKPropertyEndDate -> "2022-04-23"), implicitly)

  val caption = messages("incomeSources.ceaseUKProperty.checkDetails.paragraph")
  val heading = messages("incomeSources.ceaseUKProperty.checkDetails.heading")
  val businessStopDateLabel = messages("incomeSources.ceaseUKProperty.checkDetails.content")
  val buttonLabel = messages("incomeSources.ceaseUKProperty.checkDetails.confirm")
  val changeUrl = controllers.incomeSources.cease.routes.UKPropertyEndDateController.show().url
  val changeUrlAgent = controllers.incomeSources.cease.routes.UKPropertyEndDateController.showAgent().url
  val formAction = controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.submit().url
  val formActionAgent = controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.submitAgent().url

  "CheckCeaseUKPropertyDetailsView - Individual" should {

    "render the page " in new Setup(view) {
        document.hasPageHeading(heading)
        document.getElementById("caption").text() shouldBe  caption
        document.getElementById("businessStopDateLabel").text() shouldBe businessStopDateLabel
        document.getElementById("businessStopDate").text() shouldBe businessEndShortLongDate
        document.getElementById("change").attr("href") shouldBe changeUrl
        document.getElementById("continue-button").text() shouldBe buttonLabel
        document.form.attr("action") shouldBe formAction

    }
  }

  "CheckCeaseUKPropertyDetailsView - Agent" should {

    "render the page " in new Setup(viewAgent) {
        document.hasPageHeading(heading)
        document.getElementById("caption").text() shouldBe  caption
        document.getElementById("businessStopDateLabel").text() shouldBe businessStopDateLabel
        document.getElementById("businessStopDate").text() shouldBe businessEndShortLongDate
        document.getElementById("change").attr("href") shouldBe changeUrlAgent
        document.getElementById("continue-button").text() shouldBe buttonLabel
        document.form.attr("action") shouldBe formActionAgent

    }
  }
}

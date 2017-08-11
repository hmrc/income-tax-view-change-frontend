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

import assets.Messages.{ExitSurvey => messages}
import config.FrontendAppConfig
import forms.ExitSurveyForm
import models.ExitSurveyModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Play.current
import play.api.data.Form
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, _}
import play.twirl.api.HtmlFormat
import utils.TestSupport


class ExitSurveyViewSpec extends TestSupport {

  lazy val mockAppConfig: FrontendAppConfig = fakeApplication.injector.instanceOf[FrontendAppConfig]

  "The exit survey view" should {

    "not errored" should {

      lazy val page: HtmlFormat.Appendable =
        views.html.exit_survey(ExitSurveyForm.exitSurveyForm, controllers.routes.HomeController.redirect())(FakeRequest(), applicationMessages, mockAppConfig)
      lazy val document: Document = Jsoup.parse(contentAsString(page))

      s"have the title ${messages.title}" in {
        document.title shouldBe messages.title
      }

      s"have the heading ${messages.heading}" in {
        document.getElementById("page-heading").text shouldBe messages.heading
      }

      s"have the question ${messages.q1}" in {
        document.getElementById("q1-satisfaction").text shouldBe messages.q1
      }

      s"have options for the satisfaction score" that {

        s"has the option '${messages.Options.option1}'" in {
          document.getElementById("satisfaction-1").attr("value") shouldBe messages.Options.option1
        }

        s"has the option '${messages.Options.option2}'" in {
          document.getElementById("satisfaction-2").attr("value") shouldBe messages.Options.option2
        }

        s"has the option '${messages.Options.option3}'" in {
          document.getElementById("satisfaction-3").attr("value") shouldBe messages.Options.option3
        }

        s"has the option '${messages.Options.option4}'" in {
          document.getElementById("satisfaction-4").attr("value") shouldBe messages.Options.option4
        }

        s"has the option '${messages.Options.option5}'" in {
          document.getElementById("satisfaction-5").attr("value") shouldBe messages.Options.option5
        }

      }

      s"have the question ${messages.q2}" in {
        document.getElementById("q2-improvements").text shouldBe messages.q2
      }

      "has a paragraph to advise users not to supply personal information" in {
        document.getElementById("no-personal-info").text shouldBe messages.p1
      }

      "have a free-format text intput box for improvements" in {
        document.select("textarea #improvements") shouldNot be(null)
      }
    }

    "has errors" should {

      lazy val form = ExitSurveyForm.exitSurveyForm.bind(Map("improvements" -> "a" * (ExitSurveyForm.improvementsMaxLength + 1)))
      lazy val page: HtmlFormat.Appendable =
        views.html.exit_survey(form, controllers.routes.HomeController.redirect())(FakeRequest(), applicationMessages, mockAppConfig)
      lazy val document: Document = Jsoup.parse(contentAsString(page))

      "render the summary error" in {
        document.getElementById("error-summary-display") shouldNot be(null)
      }

      "have the correct summary warning message" in {
        document.select("#error-summary-display li").text shouldBe messages.Errors.maxImprovementsError
      }

      "have the correct field error message" in {
        document.getElementById("error-message-improvements").text shouldBe messages.Errors.maxImprovementsError
      }
    }
  }

}

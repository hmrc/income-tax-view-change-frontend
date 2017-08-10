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

package forms

import assets.{Messages => messageLookup}
import models.ExitSurveyModel
import play.api.i18n.MessagesApi
import utils.TestSupport

class ExitSurveyFormSpec extends TestSupport {

  object TestExitSurveyForm extends ExitSurveyForm()(fakeApplication.injector.instanceOf[MessagesApi])

  val improvementsMaxLength = 1200

  "The ExitSurveyForm" should {

    "have no errors" when {

      "the length of 'improvements' is equal to the maxLength" should {

        lazy val formInput: Map[String, String] = Map("improvements" -> "a" * improvementsMaxLength)
        lazy val result = TestExitSurveyForm.exitSurveyForm.bind(formInput)

        "have no errors" in {
          result.hasErrors shouldBe false
        }
      }

      "the length of 'improvements' is less than the maxLength" should {

        lazy val formInput: Map[String, String] = Map("improvements" -> "a" * (improvementsMaxLength - 1))
        lazy val result = TestExitSurveyForm.exitSurveyForm.bind(formInput)

        "have no errors" in {
          result.hasErrors shouldBe false
        }
      }

      "valid numeric is supplied for 'satisfaction'" should {

        lazy val formInput: Map[String, String] = Map("satisfaction" -> "1")
        lazy val result = TestExitSurveyForm.exitSurveyForm.bind(formInput)

        "have no errors" in {
          result.hasErrors shouldBe false
        }

        "can fold a valid form into a ExitSurveyModel" in {
          result.fold(
            _ => false shouldBe true,
            success => success.satisfaction shouldBe Some(1)
          )
        }

        "should take a ExitSurveyModel and unapply to form" in {
          val model = ExitSurveyModel(Some(3), Some("Awesome Site"))
          TestExitSurveyForm.exitSurveyForm.fill(model).fold(
            _ => false shouldBe true,
            success => success shouldBe model
          )
        }
      }
    }

    "have errors" when {

      "the max length of 'improvements' is exceeded" should {

        lazy val formInput: Map[String, String] = Map("improvements" -> "a" * (improvementsMaxLength + 1))
        lazy val result = TestExitSurveyForm.exitSurveyForm.bind(formInput)

        "have an error" in {
          result.hasErrors shouldBe true
        }

        s"has the error message '${messageLookup.ExitSurvey.maxImprovementsError}'" in {
          result.errors.head.message shouldBe messageLookup.ExitSurvey.maxImprovementsError
        }
      }


      "a non-numeric value is supplied for 'satisfaction'" should {

        lazy val formInput: Map[String, String] = Map("satisfaction" -> "a")
        lazy val result = TestExitSurveyForm.exitSurveyForm.bind(formInput)

        "have an error" in {
          result.hasErrors shouldBe true
        }
      }
    }
  }
}
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

package forms.validation

import forms.FeedbackForm
import forms.validation.models.{FieldError, SummaryError}
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.data.{Field, Form, FormError}

class ErrorMessageHelperSpec extends AnyWordSpec with Matchers {
  def testFeedbackForm(rating: Option[String] = Some("5"),
                       name: String = "name",
                       email: String = "email@email.com",
                       comments: String = "comments",
                       csrfToken: String = "csrfToken"): FeedbackForm = FeedbackForm(
    experienceRating = rating,
    name = name,
    email = email,
    comments = comments,
    csrfToken = csrfToken
  )

  def getForm(optValue: Option[FeedbackForm]): Form[FeedbackForm] = FeedbackForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(
      value => Map(
        FeedbackForm.feedbackRating -> value.experienceRating.getOrElse("N/A"),
        FeedbackForm.feedbackName -> value.name,
        FeedbackForm.feedbackEmail -> value.email,
        FeedbackForm.feedbackComments -> value.comments,
        FeedbackForm.feedbackCsrfToken -> value.csrfToken)
    )
  )

  "ErrorMessageHelper" must {
    "get field error" when {
      "invalid field and none parent form is provided" in {
        val fieldError = FieldError("feedback-rating", Seq("feedback.radiosError"))
        val form = getForm(Some(testFeedbackForm(rating = Some(""))))
        val field = Field(form, "feedback-rating", Nil, None, Seq(FormError("", "", Seq(fieldError))), None)

        ErrorMessageHelper.getFieldError(field, None).get mustBe fieldError
      }

      "invalid field and parent form is provided" in {
        val fieldError = FieldError("feedback-rating", Seq("feedback.radiosError"))
        val form = getForm(Some(testFeedbackForm(rating = Some(""))))
          .copy(errors = Seq(FormError("feedback-rating", "", Seq(fieldError))))
        val field = Field(form, "feedback-rating", Nil, None, Seq(FormError("", "", Seq(fieldError))), None)

        ErrorMessageHelper.getFieldError(field, Some(form)).get mustBe fieldError
      }

      "only invalid field is provided" in {
        val fieldError = FieldError("feedback-rating", Seq("feedback.radiosError"))
        val form = getForm(Some(testFeedbackForm(rating = Some(""))))
        val field = Field(form, "feedback-rating", Nil, None, Seq(FormError("", "", Seq(fieldError))), None)

        ErrorMessageHelper.getFieldError(field).get mustBe fieldError
      }
    }

    "not get field error" when {
      "valid field is provided" in {
        val form = getForm(Some(testFeedbackForm(rating = Some("5"))))
        val field = Field(form, "feedback-rating", Nil, None, Nil, None)

        ErrorMessageHelper.getFieldError(field, None) mustBe None
      }
    }

    "get summary error" when {
      "invalid form is provided" in {
        val fieldError = FieldError("feedback-rating", Seq("feedback.radiosError"))
        val summaryError = forms.validation.models.SummaryError("invalid form", Seq("invalid form"))
        val form = getForm(Some(testFeedbackForm(rating = Some(""))))
          .copy(errors = Seq(FormError("", "", Seq(fieldError, summaryError))))

        ErrorMessageHelper.getSummaryErrors(form).map(_._2) mustBe List(SummaryError("invalid form", List("invalid form")))
      }
    }
  }
}

/*
 * Copyright 2022 HM Revenue & Customs
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

import org.scalatest.{MustMatchers, WordSpec}
import play.api.data.{Form, FormError}

class FeedbackFormSpec extends WordSpec with MustMatchers {

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

  val aToZString = "abcdefghijklmnopqrstuvwxyz"

  def form(optValue: Option[FeedbackForm]): Form[FeedbackForm] = FeedbackForm.form.bind(
    optValue.fold[Map[String, String]](Map.empty)(
      value => Map(
        FeedbackForm.feedbackRating -> value.experienceRating.getOrElse("N/A"),
        FeedbackForm.feedbackName -> value.name,
        FeedbackForm.feedbackEmail -> value.email,
        FeedbackForm.feedbackComments -> value.comments,
        FeedbackForm.feedbackCsrfToken -> value.csrfToken)
    )
  )

  "FeedbackForm" must {
    "feedbackRating" when {
      "bound with a valid feedbackRating" in {
        form(Some(testFeedbackForm(rating = Some("5")))).value.get.name mustBe Some(testFeedbackForm(rating = Some("5"))).value.name
      }
      "bound with a empty feedbackRating" in {
        form(Some(testFeedbackForm(rating = Some("")))).errors mustBe Seq(FormError("feedback-rating", Seq("feedback.radiosError")))
      }
    }
    "feedbackName" when {
      "bound with a valid feedbackName" in {
        form(Some(testFeedbackForm(name = "test name"))).value.get.name mustBe Some(testFeedbackForm(name = "test name")).value.name
      }
      "bound with a empty feedbackName" in {
        form(Some(testFeedbackForm(name = ""))).errors mustBe Seq(
          FormError("feedback-name", Seq("feedback.fullName.error.empty")), FormError("feedback-name", Seq("feedback.fullName.error.invalid")))
      }
      "bound with a invalid feedbackName with special character" in {
        form(Some(testFeedbackForm(name = "test@name"))).errors mustBe Seq(
          FormError("feedback-name", Seq("feedback.fullName.error.invalid")))
      }
      "bound with a invalid feedbackName with multiple special characters" in {
        form(Some(testFeedbackForm(name = "test@$£&^name"))).errors mustBe Seq(
          FormError("feedback-name", Seq("feedback.fullName.error.invalid")))
      }
      "bound with a invalid feedbackName with more than 70 characters" in {
        form(Some(testFeedbackForm(name = aToZString * 3))).errors mustBe Seq(
          FormError("feedback-name", Seq("feedback.fullName.error.length")))
      }
    }
    "feedbackEmail" when {
      "bound with a valid feedbackEmail" in {
        form(Some(testFeedbackForm(email = "test@test.com"))).value.get.name mustBe Some(testFeedbackForm(email = "test@test.com")).value.name
      }
      "bound with a invalid feedbackEmail format" in {
        form(Some(testFeedbackForm(email = aToZString))).errors mustBe Seq(
          FormError("feedback-email", Seq("feedback.email.error")))
      }
      "bound with a invalid feedbackEmail with more than 70 characters" in {
        form(Some(testFeedbackForm(email = s"${aToZString * 3}@${aToZString * 7}.com"))).errors mustBe Seq(
          FormError("feedback-email", Seq("feedback.email.error.length")))
      }
    }
    "feedbackComments" when {
      "bound with a valid feedbackComments" in {
        form(Some(testFeedbackForm(comments = "test comments"))).value.get.name mustBe Some(testFeedbackForm(comments = "test comments")).value.name
      }
      "bound with a invalid feedbackComments length" in {
        form(Some(testFeedbackForm(comments = s"${aToZString * 77}"))).errors mustBe Seq(
          FormError("feedback-comments", Seq("feedback.comments.error.length")))
      }
    }
  }

}

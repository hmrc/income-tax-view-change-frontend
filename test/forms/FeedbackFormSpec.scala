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
import play.api.data.Form

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
    "return a valid feedbackRating" when {
      "bound with a valid feedbackRating" in {
        form(Some(testFeedbackForm(rating = Some("5")))).value.get.name mustBe Some(testFeedbackForm(rating = Some("5"))).value.name
      }
    }
  }

}


package forms

import org.scalatest.{MustMatchers, WordSpec}
import play.api.data.Form

class FeedbackFormSpec extends WordSpec with MustMatchers {

  def testFeedbackForm(
                        experienceRating: Option[String],
                        name: String,
                        email: String,
                        comments: String): FeedbackForm = FeedbackForm(
    experienceRating = experienceRating,
    name = name,
    email = email,
    comments = comments
  )

  def form(optValue: Option[FeedbackForm]): Form[String] = FeedbackForm.form.bind(
    optValue.fold[Map[String, FeedbackForm]](Map.empty)(
      value => Map(FeedbackForm -> value)
//      feedbackRating => Map(FeedbackForm.feedbackRating -> feedbackRating),
//      feedbackName => Map(FeedbackForm.feedbackName -> feedbackName),
//      feedbackEmail => Map(FeedbackForm.feedbackEmail -> feedbackEmail),
//      feedbackComments => Map(FeedbackForm.feedbackComments -> feedbackComments)
    )
  )

}

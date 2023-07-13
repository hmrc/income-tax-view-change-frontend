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

package forms

import play.api.data.Forms.{mapping, optional, text}
import play.api.data.format.Formatter
import play.api.data.{FieldMapping, Form, FormError}

import scala.util.matching.Regex

object FeedbackForm {

  val feedbackRating: String = "feedback-rating"
  val feedbackName: String = "feedback-name"
  val feedbackEmail: String = "feedback-email"
  val feedbackComments: String = "feedback-comments"
  val feedbackCsrfToken: String = "csrfToken"
  val feedbackReferrer: String = "referrer"

  val radiosEmptyError: String = "feedback.radiosError"
  val nameEmptyError: String = "feedback.fullName.error.empty"
  val nameLengthError: String = "feedback.fullName.error.length"
  val nameInvalidError: String = "feedback.fullName.error.invalid"
  val emailInvalidError: String = "feedback.email.error"
  val emailLengthError: String = "feedback.email.error.length"
  val commentsEmptyError: String = "feedback.comments.error.empty"
  val commentsEmptyLength: String = "feedback.comments.error.length"

  def validate(email: String): Boolean =
    email.split("@").toList match {
      case name :: domain :: Nil => validateName(name) && (validateDomain(domain) || validateIp(domain))
      case _ => false
    }

  private def validateName(name: String): Boolean = {
    val validNamePattern: Regex = """(?i)^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:[\\.][a-z0-9!#$%&'*+/=?^_`{|}~-]+)*$""".r
    validNamePattern.findFirstIn(name).isDefined
  }

  def validateDomain(domain: String): Boolean = {
    val validDomainPattern =
      """(?i)^(?:(?:(?:(?:[a-zA-Z0-9][-a-zA-Z0-9]*)?[a-zA-Z0-9])[\\.])*(?:[a-zA-Z][-a-zA-Z0-9]*[a-zA-Z0-9]|[a-zA-Z]))$""".r
    validDomainPattern.findFirstIn(domain).isDefined
  }

  def validateIp(domain: String): Boolean = {
    val validIpPattern =
      """^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)[\\.]){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$""".r
    validIpPattern.findFirstIn(domain).isDefined
  }

  val form: Form[FeedbackForm] = Form[FeedbackForm](
    mapping(
      feedbackRating -> optional(text)
        .verifying(radiosEmptyError, rating => rating.isDefined && rating.get.trim.nonEmpty),
      feedbackName -> text
        .verifying(nameEmptyError, name => name.trim.nonEmpty)
        .verifying(nameInvalidError, name => name.matches("""^[A-Za-z\-.,()'"\s]+$"""))
        .verifying(nameLengthError, name => name.length <= 70),
      feedbackEmail -> text
        .verifying(emailInvalidError, email => validate(email))
        .verifying(emailLengthError, email => email.length <= 255),
      feedbackComments -> FieldMapping[String]()(new Formatter[String] {

        override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
          data.get(key) match {
            case Some(value) if value.trim.nonEmpty => Right(value.trim)
            case _ => Left(Seq(FormError(key, commentsEmptyError, Nil)))
          }
        }

        override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
      }).verifying(commentsEmptyLength, comment => comment.length <= 2000),
      feedbackCsrfToken -> text
    )(FeedbackForm.apply)(FeedbackForm.unapply)
  )
}

case class FeedbackForm(
                         experienceRating: Option[String],
                         name: String,
                         email: String,
                         comments: String,
                         csrfToken: String
                       ) {
  def toFormMap(referrer: String): Map[String, Seq[String]] =
    Map(FeedbackForm.feedbackRating -> Seq(experienceRating.getOrElse("N/A")),
      FeedbackForm.feedbackName -> Seq(name),
      FeedbackForm.feedbackEmail -> Seq(email),
      FeedbackForm.feedbackComments -> Seq(comments),
      FeedbackForm.feedbackCsrfToken -> Seq(csrfToken),
      FeedbackForm.feedbackReferrer -> Seq(referrer)
    )
}

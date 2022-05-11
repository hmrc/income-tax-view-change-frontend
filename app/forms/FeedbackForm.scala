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

import play.api.data.{FieldMapping, Form, FormError}
import play.api.data.Forms.{mapping, optional, text}
import play.api.data.format.Formatter

import scala.util.matching.Regex

object FeedbackForm {

  val radiosEmptyError: String = "feedback.radiosError"
  val nameEmptyError: String = "feedback.fullName.error.empty"
  val nameLengthError: String = "feedback.name.error.length"
  val nameInvalidError: String = "feedback.fullName.error.invalid"
  val emailInvalidError: String = "feedback.email.error"
  val emailLengthError: String = "feedback.email.error.length"
  val commentsError: String = "feedback.comments.error"

  def validate(email: String): Boolean =
    email.split("@").toList match {
      case name :: domain :: Nil => validateName(name) && (validateDomain(domain) || validateIp(domain))
      case _                     => false
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
      "feedback-rating" -> optional(text)
        .verifying(radiosEmptyError, rating => rating.isDefined && rating.get.trim.nonEmpty),
      "feedback-name" -> text
        .verifying(nameEmptyError, name => name.trim.nonEmpty)
        .verifying(nameInvalidError, name => name.matches("""^[A-Za-z\-.,()'"\s]+$"""))
        .verifying(nameLengthError, name => name.length <= 70),
      "feedback-email" -> text
        .verifying(emailInvalidError, email => validate(email))
        .verifying("feedback.email.error.length", email => email.length <= 255),
      "feedback-comments" -> FieldMapping[String]()(new Formatter[String] {

        override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
          data.get(key) match {
            case Some(value) if value.trim.nonEmpty => Right(value.trim)
            case Some(_) => Left(Seq(FormError(key, commentsError, Nil)))
            case None => Left(Seq(FormError(key, commentsError, Nil)))
          }
        }

        override def unbind(key: String, value: String): Map[String, String] = Map(key -> value)
      }).verifying("feedback.comments.error", comment => {
        val result = comment.length <= 2000
        result
      })
    )(FeedbackForm.apply)(FeedbackForm.unapply)
  )
}

case class FeedbackForm(
                         experienceRating: Option[String],
                         name: String,
                         email: String,
                         comments: String
                       )


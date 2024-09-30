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

package testOnly.models

import play.api.data.Forms.{boolean, mapping, optional, text}
import play.api.data.{Form, Mapping}
import play.api.libs.json.{Json, OFormat}

case class User(nino: Nino, isAgent: Boolean)

object User {
  val ninoNonEmptyMapping: Mapping[Nino] = {

    // TODO: remove usage of .head
    text.verifying("You must supply a valid Nino", nino => {
      Nino.isValid(nino.split(" ").head)
    }).transform[Nino](Nino(_), _.value)
  }

  val form: Form[User] =
    Form(
      mapping(
        "nino" -> ninoNonEmptyMapping,
        "isAgent" -> boolean
      )(User.apply)(User.unapply)
    )
}

case class UserRecord(nino: String, mtditid: String, utr: String, description: String)

object UserRecord {
  implicit val formats: OFormat[UserRecord] = Json.format[UserRecord]
}

case class PostedUser(nino: String,
                      isAgent: Boolean,
                      cyMinusOneCrystallisationStatus: Option[String],
                      cyMinusOneItsaStatus: Option[String],
                      cyItsaStatus: Option[String],
                      cyPlusOneItsaStatus: Option[String]
                     ) {

  def isOptOutWhitelisted(optOutUserPrefixes: Seq[String]): Boolean = {
    optOutUserPrefixes.contains(nino.take(2))
  }
}

object PostedUser {
    val form: Form[PostedUser] =
      Form(
        mapping(
          "nino" -> text,
          "Agent" -> boolean,
          "cyMinusOneCrystallisationStatus" -> optional(text),
          "cyMinusOneItsaStatus" -> optional(text),
          "cyItsaStatus" -> optional(text),
          "cyPlusOneItsaStatus" -> optional(text)
        )(PostedUser.apply)(PostedUser.unapply)
      )
}
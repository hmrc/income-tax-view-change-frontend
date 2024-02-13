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

package testOnly.models

import play.api.data.Forms.{boolean, mapping, text}
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
                      cyMinusOneCrystallisationStatus: String,
                      cyMinusOneItsaStatus: String,
                      cyItsaStatus: String,
                      cyPlusOneItsaStatus: String
                     )

object PostedUser {
    val form: Form[PostedUser] =
      Form(
        mapping(
          "nino" -> text,
          "Agent" -> boolean,
          "cyMinusOneCrystallisationStatus" -> text,
          "cyMinusOneItsaStatus" -> text,
          "cyItsaStatus" -> text,
          "cyPlusOneItsaStatus" -> text
        )(PostedUser.apply)(PostedUser.unapply)
      )
}
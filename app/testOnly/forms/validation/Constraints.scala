/*
 * Copyright 2021 HM Revenue & Customs
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

package testOnly.forms.validation

import play.api.data.validation.{Constraint, Valid}
import play.api.libs.json.Json
import testOnly.forms.validation.utils.ConstraintUtil._

import scala.util.{Failure, Success, Try}

object Constraints {

  def nonEmpty(msg: String): Constraint[String] = constraint[String](
    x => if (x.isEmpty) ErrorMessageFactory.error(msg) else Valid
  )

  val validJson: Constraint[String] = constraint[String](
    x => Try {Json.parse(x)} match {
      case Success(_) => Valid
      case Failure(_) => ErrorMessageFactory.error("Invalid Json Format")
    }
  )

  val oValidJson: Constraint[Option[String]] = constraint[Option[String]](
    x => x.isEmpty match {
      case true => Valid
      case false => Try {
        Json.parse(x.get)
      } match {
        case Success(_) => Valid
        case Failure(_) => ErrorMessageFactory.error("Invalid Json Format")
      }
    }
  )

  val isNumeric: Constraint[String] = constraint[String](
    x => Try {x.toInt} match {
      case Success(_) => Valid
      case Failure(_) => ErrorMessageFactory.error("Invalid Numeric")
    }
  )
}

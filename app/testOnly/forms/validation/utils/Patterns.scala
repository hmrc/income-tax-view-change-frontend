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

package testOnly.forms.validation.utils

object Patterns {

  // ISO 8859-1 standard
  // ASCII range {32 to 126} + {160 to 255} all values inclusive
  val iso8859_1Regex = """^([\x20-\x7E\xA0-\xFF])*$"""

  val emailRegex = """(^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$)"""

  def validText(text: String): Boolean = text matches iso8859_1Regex

  def validEmail(text: String): Boolean = text matches emailRegex

}

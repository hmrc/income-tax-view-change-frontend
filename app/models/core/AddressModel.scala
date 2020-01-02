/*
 * Copyright 2020 HM Revenue & Customs
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

package models.core

import play.api.libs.json.{Format, Json}

case class AddressModel(addressLine1: String,
                        addressLine2: Option[String],
                        addressLine3: Option[String],
                        addressLine4: Option[String],
                        postCode: Option[String],
                        countryCode: String)

object AddressModel {
  implicit val format: Format[AddressModel] = Json.format[AddressModel]
}

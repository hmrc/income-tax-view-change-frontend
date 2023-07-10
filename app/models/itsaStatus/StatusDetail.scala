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

package models.itsaStatus

import play.api.libs.json.{Format, Json}

case class StatusDetail(submittedOn: String,
                        status: String,
                        statusReason: String,
                        businessIncomePriorTo2Years: Option[BigDecimal] = None) {

  def isMandatedOrVoluntary: Boolean = status.equals("MTD Mandated") || status.equals("MTD Voluntary")

}

object StatusDetail {
  implicit val format: Format[StatusDetail] = Json.format
}

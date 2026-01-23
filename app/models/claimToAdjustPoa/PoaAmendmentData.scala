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

package models.claimToAdjustPoa

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Json, OFormat, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class PoaSessionData(
                           sessionId: String,
                           poaAmendmentData: Option[PoaAmendmentData] = None,
                           lastUpdated: Instant = Instant.now
                            )

object PoaSessionData {
  implicit val format: OFormat[PoaSessionData] = Json.format[PoaSessionData]
}

case class PoaAmendmentData(
                             poaAdjustmentReason: Option[SelectYourReason] = None,
                             newPoaAmount: Option[BigDecimal] = None,
                             journeyCompleted: Boolean = false
                            )

object PoaAmendmentData {
  implicit val formats: OFormat[PoaAmendmentData] = Json.format[PoaAmendmentData]
}

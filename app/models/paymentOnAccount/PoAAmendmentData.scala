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

package models.paymentOnAccount

import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Json, OFormat, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class PoASessionData(
                           sessionId: String,
                           poaAmendmentData: Option[PoAAmendmentData] = None,
                           lastUpdated: Instant = Instant.now
                            )

object PoASessionData {
  implicit val format: OFormat[PoASessionData] = {
    ((__ \ "sessionId").format[String]
      ~ (__ \ "poaAmmendmentData").formatNullable[PoAAmendmentData]
      ~ (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat)
      )(PoASessionData.apply, unlift(PoASessionData.unapply)
    )
  }
}

case class PoAAmendmentData(
                              poaAdjustmentReason: Option[String] = None,
                              newPoAAmount: Option[BigDecimal] = None
                            )

object PoAAmendmentData {
  implicit val formats: OFormat[PoAAmendmentData] = Json.format[PoAAmendmentData]
}

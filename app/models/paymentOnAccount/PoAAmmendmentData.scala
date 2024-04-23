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
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class PoAAmmendmentData(
                              sessionId: String,
                              poaAdjustmentReason: Option[String] = None,
                              newPoAAmount: Option[BigDecimal] = None,
                              lastUpdated: Instant = Instant.now
                            )

object PoAAmmendmentData {
  implicit val format: OFormat[PoAAmmendmentData] = {
    ((__ \ "sessionId").format[String]
      ~ (__ \ "poaAdjustmentReason").formatNullable[String]
      ~ (__ \ "newPoAAmount").formatNullable[BigDecimal]
      ~ (__ \ "lastUpdated").format(MongoJavatimeFormats.instantFormat)
      )(PoAAmmendmentData.apply, unlift(PoAAmmendmentData.unapply)
    )
  }
}

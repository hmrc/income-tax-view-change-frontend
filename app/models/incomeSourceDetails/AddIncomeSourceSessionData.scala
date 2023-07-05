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

package models.incomeSourceDetails

import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

final case class AddIncomeSourceSessionData(
                              sessionId: String,
                              journeyType: String,
                              businessName: Option[String] = None,
                              dateStarted: Option[LocalDate] = None,
                              lastUpdated: Instant = Instant.now
                            )

object AddIncomeSourceSessionData {

  val reads: Reads[AddIncomeSourceSessionData] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "sessionId").read[String] and
        (__ \ "journeyType").read[String] and
        (__ \ "businessName").readNullable[String] and
        (__ \ "dateStarted").readNullable[LocalDate] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      ) (AddIncomeSourceSessionData.apply _)
  }

    val writes: OWrites[AddIncomeSourceSessionData] = {

      import play.api.libs.functional.syntax._

      (
        (__ \ "sessionId").write[String] and
          (__ \ "journeyType").write[String] and
          (__ \ "businessName").writeNullable[String] and
          (__ \ "dateStarted").writeNullable[LocalDate] and
          (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
        ) (unlift(AddIncomeSourceSessionData.unapply))
    }

  implicit val format: OFormat[AddIncomeSourceSessionData] = OFormat(reads, writes)
}

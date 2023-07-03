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

final case class UserAnswers(
                              sessionId: String,
                              journeyType: String,
                              businessName: Option[String] = None,
                              dateStarted: Option[LocalDate] = None,
                              lastUpdated: Instant = Instant.now
                            ) {
  //  var id = "unset"
  //  def setid()(implicit hc: HeaderCarrier) = {
  //    id = s"${hc.sessionId.get.value}-${journeyType}"
  //  }
  //  def apply(journeyType: String)(implicit hc: HeaderCarrier): UserAnswers = {
  //    val ua = UserAnswers(journeyType)
  //    ua.setid()
  //    ua
  //  }
  //  def id(implicit hc: HeaderCarrier) =
  def getId(): String = {
    sessionId + "!!" + journeyType
  }
}

object UserAnswers {

  val reads: Reads[UserAnswers] = {

    import play.api.libs.functional.syntax._

    (
      (__ \ "sessionId").read[String] and
        (__ \ "journeyType").read[String] and
        (__ \ "businessName").readNullable[String] and
        (__ \ "dateStarted").readNullable[LocalDate] and
        (__ \ "lastUpdated").read(MongoJavatimeFormats.instantFormat)
      ) (UserAnswers.apply _)
  }

  //  val writes: OWrites[UserAnswers] = {
  //
  //    import play.api.libs.functional.syntax._
  //
  //    (
  //      (__ \ "_id").write[String] and
  //        (__ \ "journeyType").write[String] and
  //        (__ \ "businessName").writeNullable[String] and
  //        (__ \ "dateStarted").writeNullable[LocalDate] and
  //        (__ \ "lastUpdated").write(MongoJavatimeFormats.instantFormat)
  //      ) (unlift(UserAnswers.unapply))
  //  }
  val writer = new OWrites[UserAnswers] {
    def writes(foo: UserAnswers): JsObject = {
      Json.obj("journeyType" -> foo.journeyType,
        "sessionId" -> foo.sessionId,
        "businessName" -> foo.businessName,
        "dateStarted" -> foo.dateStarted,
        "lastUpdated" -> foo.lastUpdated,
        "_id" -> foo.getId())
    }
  }
  implicit val format: OFormat[UserAnswers] = OFormat(reads, writer)


}

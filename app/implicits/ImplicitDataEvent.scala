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

package implicits

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, JsValue, Json, Writes}
import uk.gov.hmrc.play.audit.model.{DataEvent, ExtendedDataEvent}
import java.time.Instant

import uk.gov.hmrc.play.audit.model.DataEvent


object ImplicitDataEventFormatter {


  //implicit val dataEventWrites: Writes[DataEvent] = Json.writes[DataEvent]

//  implicit val dataEventWrites: Writes[DataEvent] = (
//    (JsPath \ "auditSource").write[String] and
//      (JsPath \ "auditType").write[String] and
//      (JsPath \ "eventId").write[String] and
//      (JsPath \ "tags").write[Map[String, String]] and
//      (JsPath \ "detail").write[Map[String, String]] and
//      (JsPath \ "generatedAt").write[Instant]
//    ) (unlift(DataEvent.unapply))
}

//object ImplicitExtendedDataEvent {
//  implicit val extendedDataEventWrites: Writes[ExtendedDataEvent] = (
//    (JsPath \ "auditSource").write[String] and
//      (JsPath \ "auditType").write[String] and
//      (JsPath \ "eventId").write[String] and
//      (JsPath \ "tags").write[Map[String, String]] and
//      (JsPath \ "detail").write[JsValue] and
//      (JsPath \ "generatedAt").write[Instant]
//    ) (unlift(ExtendedDataEvent.unapply))
//}

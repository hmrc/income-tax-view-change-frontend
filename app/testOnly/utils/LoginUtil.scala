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

package testOnly.utils

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class KVPair(key: String, value: String)

case class Enrolment(key: String, identifiers: Seq[KVPair], state: String)

case class EnrolmentValues(mtditid: String, utr: String)

case class DelegatedEnrolmentValues(mtditid: String, utr: String)

case class DelegatedEnrolment(key: String, identifiers: Seq[KVPair], delegatedAuthRule: String)

object LoginUtil {

  implicit val kvPairWrites: Writes[KVPair] =
    Json.writes[KVPair]

  implicit val enrolmentWrites: Writes[Enrolment] =
    Json.writes[Enrolment]

  implicit val delegatedEnrolmentWrites: Writes[DelegatedEnrolment] =
    Json.writes[DelegatedEnrolment]

  def getEnrolmentData(isAgent: Boolean, enrolment: EnrolmentValues): JsValue = {
    val es = if (isAgent) {
      val enrolmentKey = "HMRC-AS-AGENT"
      Seq(
        Enrolment(key = enrolmentKey, identifiers =
          Seq(KVPair(key = "AgentReferenceNumber", value = "1")), state = "Activated")
      )
    } else {
      val enrolmentKey = "HMRC-MTD-IT"
      val saEnrolment = Enrolment(key = "IR-SA", identifiers =
        Seq(KVPair(key = "UTR", value = enrolment.utr)), state = "Activated")
      if(enrolment.mtditid.contains("Not Enrolled")) {
        Seq(saEnrolment)
      } else {
        Seq(Enrolment(key = enrolmentKey, identifiers =
          Seq(KVPair(key = "MTDITID", value = enrolment.mtditid)), state = "Activated"),
          saEnrolment
        )
      }
    }
    Json.toJson[Seq[Enrolment]](es)
  }

  def getDelegatedEnrolmentData(isAgent: Boolean, isSupporting: Boolean, enrolment: EnrolmentValues): JsValue = {
    val es = if (isAgent && !enrolment.mtditid.contains("Not Enrolled")) {
      if(isSupporting) Seq(
        DelegatedEnrolment(key = "HMRC-MTD-IT-SUPP", identifiers =
          Seq(KVPair(key = "MTDITID", value = enrolment.mtditid)), delegatedAuthRule = "mtd-it-auth-supp")
      )
      else Seq(
        DelegatedEnrolment(key = "HMRC-MTD-IT", identifiers =
          Seq(KVPair(key = "MTDITID", value = enrolment.mtditid)), delegatedAuthRule = "mtd-it-auth")

      )
    } else {
      Nil
    }
    Json.toJson[Seq[DelegatedEnrolment]](es)
  }


}

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

package models.financialDetails

import enums.CodingOutType.{CODING_OUT_ACCEPTED, CODING_OUT_CANCELLED, CODING_OUT_CLASS2_NICS, CODING_OUT_FULLY_COLLECTED}
import play.api.libs.json._

sealed trait CodedOutStatusType  {
  val key: String
}

case object Nics2 extends CodedOutStatusType {
  override val key = "Nics2"
}

case object Accepted extends CodedOutStatusType {
  override val key = "Accepted"
}

case object Cancelled extends CodedOutStatusType {
  override val key = "Cancelled"
}

case object FullyCollected extends CodedOutStatusType {
  override val key = "FullyCollected"
}

object CodedOutStatusType {

  @deprecated
  def fromDocumentText(documentText: String): Option[CodedOutStatusType] = {
    documentText match {
      case CODING_OUT_CLASS2_NICS.name =>
        Some(Nics2)
      case CODING_OUT_ACCEPTED.name =>
        Some(Accepted)
      case CODING_OUT_CANCELLED.name =>
        Some(Cancelled)
      case _ => None
    }
  }

  def fromCodedOutStatusAndDocumentText(documentText: Option[String], codedOutStatus: Option[String]): Option[CodedOutStatusType] = {
    (documentText, codedOutStatus) match {
      case (Some(CODING_OUT_CLASS2_NICS.name),     _)                                              => Some(Nics2)
      case (Some(CODING_OUT_ACCEPTED.name),        _) | (_, Some(CODING_OUT_ACCEPTED.code))        => Some(Accepted)
      case (Some(CODING_OUT_CANCELLED.name),       _) | (_, Some(CODING_OUT_CANCELLED.code))       => Some(Cancelled)
      case (Some(CODING_OUT_FULLY_COLLECTED.name), _) | (_, Some(CODING_OUT_FULLY_COLLECTED.code)) => Some(FullyCollected)
      case _                                                                                       => None
    }
  }

  implicit val write: Writes[CodedOutStatusType] = (transactionType: CodedOutStatusType) => {
    JsString(transactionType.key)
  }

  val read: Reads[CodedOutStatusType] = JsPath.read[String].collect(JsonValidationError("Could not parse codedOutStatus")) {
    case CODING_OUT_CLASS2_NICS.name     => Nics2
    case CODING_OUT_ACCEPTED.name        => Accepted
    case CODING_OUT_CANCELLED.name       => Cancelled
    case CODING_OUT_FULLY_COLLECTED.name => FullyCollected
  }

  implicit val format: Format[CodedOutStatusType] = Format(read, write)
}

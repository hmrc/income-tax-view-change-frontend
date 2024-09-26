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

import enums.CodingOutType.{CODING_OUT_ACCEPTED, CODING_OUT_CANCELLED, CODING_OUT_CLASS2_NICS}
import play.api.libs.json._

sealed trait SubTransactionType extends TransactionType {
  val key: String
}

case object Nics2 extends SubTransactionType {
  override val key = "Nics2"
}

case object Accepted extends SubTransactionType {
  override val key = "Accepted"
}

case object Cancelled extends SubTransactionType {
  override val key = "Cancelled"
}

//
//case object Nics4 extends SubTransactionType {
//  override val key = "POA1"
//}
//
//case object StudentLoan extends SubTransactionType {
//  override val key = "POA1"
//}
//
//case object CapitalGainsTax extends SubTransactionType {
//  override val key = "POA1"
//}


object SubTransactionType {

  def fromDocumentText(documentText: String): Option[SubTransactionType] = {
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

  implicit val write: Writes[SubTransactionType] = new Writes[SubTransactionType] {
    def writes(transactionType: SubTransactionType): JsValue = {
      JsString(transactionType.key)
    }
  }

  val read: Reads[SubTransactionType] = (JsPath).read[String].collect(JsonValidationError("Could not parse transactionType")) {
    case CODING_OUT_CLASS2_NICS.name => Nics2
    case CODING_OUT_ACCEPTED.name => Accepted
    case CODING_OUT_CANCELLED.name => Cancelled

  }

  implicit val format: Format[SubTransactionType] = Format( read, write)

}

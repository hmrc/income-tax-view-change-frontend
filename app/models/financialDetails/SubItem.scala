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

package models.financialDetails

import play.api.Logger
import play.api.libs.json._

import java.time.LocalDate


case class SubItem(dueDate: Option[LocalDate] = None,
                   subItemId: Option[String] = None,
                   amount: Option[BigDecimal] = None,
                   dunningLock: Option[String] = None,
                   interestLock: Option[String] = None,
                   clearingDate: Option[LocalDate] = None,
                   clearingReason: Option[String] = None,
                   clearingSAPDocument: Option[String] = None,
                   outgoingPaymentMethod: Option[String] = None,
                   paymentReference: Option[String] = None,
                   paymentAmount: Option[BigDecimal] = None,
                   paymentMethod: Option[String] = None,
                   paymentLot: Option[String] = None,
                   paymentLotItem: Option[String] = None,
                   paymentId: Option[String] = None,
                   transactionId: Option[String] = None,
                   codedOutStatus: Option[String] = None)

object SubItem {

  implicit val writes: OWrites[SubItem] = Json.writes[SubItem]

  implicit val reads: Reads[SubItem] = for {
    subItemId <- (JsPath \ "subItemId").readNullable[String](Reads.of[String].filter(subItemJsonError)(isIntString))
    amount <- (JsPath \ "amount").readNullable[BigDecimal]
    dunningLock <- (JsPath \ "dunningLock").readNullable[String]
    interestLock <- (JsPath \ "interestLock").readNullable[String]
    clearingDate <- (JsPath \ "clearingDate").readNullable[String]
    clearingReason <- (JsPath \ "clearingReason").readNullable[String]
    clearingSAPDocument <- (JsPath \ "clearingSAPDocument").readNullable[String]
    outgoingPaymentMethod <- (JsPath \ "outgoingPaymentMethod").readNullable[String]
    paymentReference <- (JsPath \ "paymentReference").readNullable[String]
    paymentAmount <- (JsPath \ "paymentAmount").readNullable[BigDecimal]
    dueDate <- (JsPath \ "dueDate").readNullable[String]
    paymentMethod <- (JsPath \ "paymentMethod").readNullable[String]
    paymentLot <- (JsPath \ "paymentLot").readNullable[String]
    paymentLotItem <- (JsPath \ "paymentLotItem").readNullable[String]
    codedOutStatus <- (JsPath \ "codedOutStatus").readNullable[String]
  } yield {
    val id: Option[String] = for {
      pl <- paymentLot
      pli <- paymentLotItem
    } yield s"$pl-$pli"
    SubItem(
      dueDate.map(date => LocalDate.parse(date)),
      subItemId,
      amount,
      dunningLock,
      interestLock,
      clearingDate.map(date => LocalDate.parse(date)),
      clearingReason,
      clearingSAPDocument,
      outgoingPaymentMethod,
      paymentReference,
      paymentAmount,
      paymentMethod,
      paymentLot,
      paymentLotItem,
      id,
      codedOutStatus = codedOutStatus
    )
  }

  private def isIntString(s: String): Boolean = {
    try {
      s.toInt
      true
    } catch {
      case _: Exception =>
        Logger("application").warn(s"The returned 'subItem' field <$s> could not be parsed as an integer")
        false
    }
  }

  private def subItemJsonError: JsonValidationError = JsonValidationError(
    message = "The field 'subItem' should be parsable as an integer"
  )
}

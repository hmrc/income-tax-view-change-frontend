/*
 * Copyright 2021 HM Revenue & Customs
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

import models.financialDetails.FinancialDetail.Types._
import play.api.libs.json.{Format, Json}

case class FinancialDetail(taxYear: String,
                           mainType: Option[String] = None,
                           transactionId: Option[String] = None,
                           transactionDate: Option[String] = None,
                           `type`: Option[String] = None,
                           totalAmount: Option[BigDecimal] = None,
                           originalAmount: Option[BigDecimal] = None,
                           outstandingAmount: Option[BigDecimal] = None,
                           clearedAmount: Option[BigDecimal] = None,
                           chargeType: Option[String] = None,
                           items: Option[Seq[SubItem]]
                          ) {

  lazy val messageKeyByTypes: Option[String] = FinancialDetail.getMessageKeyByTypes(mainType, chargeType)

  lazy val messageKeyForChargeType: Option[String] = FinancialDetail.getMessageKeyForChargeType(chargeType)

  lazy val dunningLockExists: Boolean = dunningLocks.nonEmpty

  lazy val dunningLocks: Seq[SubItem] = {
    items.fold(Seq.empty[SubItem]) { subItems =>
      subItems.filter(_.dunningLock.contains("Stand over order"))
    }
  }

  lazy val payments: Seq[Payment] = items match {
    case Some(subItems) => subItems.map { subItem =>
      Payment(reference = subItem.paymentReference, amount = subItem.paymentAmount, method = subItem.paymentMethod,
        lot = subItem.paymentLot, lotItem = subItem.paymentLotItem, date = subItem.clearingDate, transactionId = subItem.transactionId)
    }.filter(_.reference.isDefined)
    case None => Seq.empty[Payment]
  }

  lazy val allocation: Option[PaymentsWithChargeType] = items
    .map { subItems =>
      subItems.collect {
        case subItem if subItem.paymentLot.isDefined && subItem.paymentLotItem.isDefined =>
          Payment(reference = subItem.paymentReference, amount = subItem.amount, method = subItem.paymentMethod,
            lot = subItem.paymentLot, lotItem = subItem.paymentLotItem, date = subItem.clearingDate, transactionId = subItem.transactionId)
      }
    }
    .collect {
      case payments if payments.nonEmpty => PaymentsWithChargeType(payments, mainType, chargeType)
    }
    .filter(_.getPaymentAllocationTextInChargeSummary.isDefined)
}


object FinancialDetail {
  implicit val format: Format[FinancialDetail] = Json.format[FinancialDetail]

  def getMessageKeyByTypes(mainType: Option[String], chargeType: Option[String]): Option[String] = {
    for {
      mainTypeValue <- mainType
      chargeTypeValue <- chargeType
      chargeTypeParts <- supportedCTypePartsByMainType.get(mainTypeValue)
      if chargeTypeParts.exists(supportedCTypePart => chargeTypeValue.startsWith(supportedCTypePart))
      mainTypeKey <- getMessageKeyForMainType(mainType)
      chargeTypeKey <- getMessageKeyForChargeType(chargeType)
    } yield s"$mainTypeKey.$chargeTypeKey"
  }

  def getMessageKeyForMainType(mainType: Option[String]): Option[String] = mainType collect {
    case MTypePOA1 => "poa1"
    case MTypePOA2 => "poa2"
    case MTypeBCD => "bcd"
  }

  def getMessageKeyForChargeType(chargeType: Option[String]): Option[String] = chargeType collect {
    case ct if ct.startsWith(CTypePartNIC4) => "nic4"
    case ct if ct.startsWith(CTypePartITSA) => "incomeTax"
    case ct if ct.startsWith(CTypePartVoluntaryNIC2) => "vcnic2"
    case ct if ct.startsWith(CTypePartNIC2) => "nic2"
    case ct if ct.startsWith(CTypeCGT) => "cgt"
    case ct if ct.startsWith(CTypeSL) => "sl"
  }

  object Types {
    val MTypePOA1 = "SA Payment on Account 1"
    val MTypePOA2 = "SA Payment on Account 2"
    val MTypeBCD = "SA Balancing Charge"

    val CTypePartITSA = "ITSA"
    val CTypePartNIC4 = "NIC4"
    val CTypePartVoluntaryNIC2 = "Voluntary NIC2"
    val CTypePartNIC2 = "NIC2"
    val CTypeSL = "SL"
    val CTypeCGT = "CGT"

    val supportedPOA1CTypeParts, supportedPOA2CTypeParts = Set(CTypePartITSA, CTypePartNIC4)
    val supportedBCDCTypeParts = Set(CTypePartITSA, CTypePartNIC4, CTypePartNIC2, CTypePartVoluntaryNIC2, CTypeCGT, CTypeSL)

    val supportedCTypePartsByMainType = Map(
      MTypePOA1 -> supportedPOA1CTypeParts,
      MTypePOA2 -> supportedPOA2CTypeParts,
      MTypeBCD -> supportedBCDCTypeParts
    )
  }
}

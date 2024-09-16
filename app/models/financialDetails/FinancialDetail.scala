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

import models.financialDetails.FinancialDetail.Types._
import play.api.libs.json.{Format, Json}
import services.DateServiceInterface

import java.time.LocalDate

case class FinancialDetail(taxYear: String,
                           mainType: Option[String] = None,
                           mainTransaction: Option[String] = None,
                           transactionId: Option[String] = None,
                           transactionDate: Option[LocalDate] = None,
                           chargeReference: Option[String] = None,
                           `type`: Option[String] = None,
                           totalAmount: Option[BigDecimal] = None,
                           originalAmount: Option[BigDecimal] = None,
                           outstandingAmount: Option[BigDecimal] = None,
                           clearedAmount: Option[BigDecimal] = None,
                           chargeType: Option[String] = None,
                           accruedInterest: Option[BigDecimal] = None,
                           items: Option[Seq[SubItem]]
                          ) {

  lazy val messageKeyByTypes: Option[String] = FinancialDetail.getMessageKeyByTypes(mainType, chargeType)

  lazy val messageKeyForChargeType: Option[String] = FinancialDetail.getMessageKeyForChargeType(chargeType)

  lazy val dunningLockExists: Boolean = dunningLocks.nonEmpty

  lazy val interestLockExists: Boolean = interestLocks.nonEmpty

  lazy val hasAccruedInterest: Boolean = accruedInterest.isDefined

  lazy val dunningLocks: Seq[SubItem] = {
    items.fold(Seq.empty[SubItem]) { subItems =>
      subItems.filter(_.dunningLock.contains("Stand over order"))
    }
  }

  lazy private val interestLockReasons = Set(
    "Clerical Interest Signal",
    "Manual Interest Calculated",
    "C18 Appeal in Progress",
    "Manual RPI Signal",
    "Breathing Space Moratorium Act")

  lazy val interestLocks: Seq[SubItem] = {
    items.fold(Seq.empty[SubItem]) { subItems =>
      subItems.filter(_.interestLock.exists(interestLockReasons.contains))
    }
  }

  def payments(implicit dateService: DateServiceInterface): Seq[Payment] = items match {
    case Some(subItems) => subItems.map { subItem =>
      Payment(reference = subItem.paymentReference, amount = subItem.paymentAmount, outstandingAmount = None,
        method = subItem.paymentMethod, documentDescription = None, lot = subItem.paymentLot, lotItem = subItem.paymentLotItem,
        dueDate = subItem.clearingDate, documentDate = dateService.getCurrentDate, transactionId = subItem.transactionId)
    }.filter(_.reference.isDefined)
    case None => Seq.empty[Payment]
  }

  def getCreditType: Option[CreditType] = mainTransaction.flatMap(CreditType.fromCode)
}


object FinancialDetail {
  implicit val format: Format[FinancialDetail] = Json.format[FinancialDetail]

  def getMessageKeyByTypes(mainType: Option[String], chargeType: Option[String]): Option[String] = {
    if (MfaDebitUtils.isMFADebitMainType(mainType)) {
      Some("hmrcAdjustment.text")
    }
    else {
      for {
        mainTypeValue <- mainType
        chargeTypeValue <- chargeType
        chargeTypeParts <- supportedCTypePartsByMainType.get(mainTypeValue)
        if chargeTypeParts.exists(supportedCTypePart => chargeTypeValue.startsWith(supportedCTypePart))
        mainTypeKey <- getMessageKeyForMainType(mainType)
        chargeTypeKey <- getMessageKeyForChargeType(chargeType)
      } yield s"$mainTypeKey.$chargeTypeKey"
    }
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
    case ct if ct.startsWith(CTypeAccepted) => "accepted"
    case ct if ct.startsWith(CTypeCancelled) => "cancelled"
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
    val CTypeCancelled = "Cancelled"
    val CTypeAccepted = "Balancing"

    val supportedPOA1CTypeParts, supportedPOA2CTypeParts = Set(CTypePartITSA, CTypePartNIC4)

    val supportedBCDCTypeParts = Set(CTypePartITSA, CTypePartNIC4, CTypePartNIC2, CTypePartVoluntaryNIC2, CTypeCGT,
      CTypeSL, CTypeCancelled, CTypeAccepted)

    val supportedCTypePartsByMainType = Map(
      MTypePOA1 -> supportedPOA1CTypeParts,
      MTypePOA2 -> supportedPOA2CTypeParts,
      MTypeBCD -> supportedBCDCTypeParts
    )
  }
}
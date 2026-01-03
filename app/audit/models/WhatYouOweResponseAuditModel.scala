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

package audit.models

import audit.Utilities._
import auth.MtdItUser
import models.financialDetails.{ChargeItem, CodingOutDetails, WhatYouOweChargesList}
import models.outstandingCharges.OutstandingChargesModel
import play.api.libs.json._
import services.DateServiceInterface

case class WhatYouOweResponseAuditModel(user: MtdItUser[_],
                                        whatYouOweChargesList: WhatYouOweChargesList)
                                       (implicit val dateService: DateServiceInterface) extends ExtendedAuditModel with PaymentSharedFunctions {

  val currentTaxYear: Int = dateService.getCurrentTaxYearEnd

  override val transactionName: String = enums.TransactionName.WhatYouOweResponse
  override val auditType: String = enums.AuditType.WhatYouOweResponse

  private val docDetailsListJson: List[JsObject] =
    whatYouOweChargesList.chargesList.map(documentDetails) ++ whatYouOweChargesList.outstandingChargesModel.map(outstandingChargeDetails)

  override val detail: JsValue = {
    val base = userAuditDetails(user) ++ Json.obj("charges" -> docDetailsListJson)

    val withBalance = balanceDetailsJson match {
      case Some(js) => base ++ js
      case None => base
    }

    whatYouOweChargesList.codedOutDetails.map(chargeItem => Json.obj("codingOut" -> codingOut(chargeItem))) match {
      case Some(js) => withBalance ++ js
      case None => withBalance
    }
  }

  private lazy val balanceDetailsJson: Option[JsObject] = {
    def onlyIfPositive(name: String, amount: BigDecimal): Option[(String, JsValue)] =
      if (amount > 0) Some(name -> JsNumber(amount)) else None

    val fields: Seq[(String, JsValue)] = Seq(
      onlyIfPositive("balanceDueWithin30Days", whatYouOweChargesList.balanceDetails.balanceDueWithin30Days),
      onlyIfPositive("overDueAmount", whatYouOweChargesList.balanceDetails.overDueAmount),
      onlyIfPositive("totalBalance", whatYouOweChargesList.balanceDetails.totalBalance),
      whatYouOweChargesList.balanceDetails.unallocatedCredit.map("creditAmount" -> JsNumber(_))
    ).flatten

    val secondOrMoreYearOfMigration = user.incomeSources.yearOfMigration.exists(currentTaxYear > _.toInt)

    if (secondOrMoreYearOfMigration && fields.nonEmpty)
      Some(Json.obj("balanceDetails" -> JsObject(fields)))
    else None
  }



  private def documentDetails(chargeItem: ChargeItem): JsObject = {
    Json.obj(
      "chargeUnderReview" -> chargeItem.dunningLock,
      "outstandingAmount" -> chargeItem.remainingToPayByChargeOrInterest
    ) ++
      Json.obj("chargeType"-> getChargeType(chargeItem, chargeItem.isAccruingInterest)) ++
      Json.obj("dueDate"-> chargeItem.dueDate) ++
      accruingInterestJson(chargeItem) ++
      Json.obj("endTaxYear" -> chargeItem.taxYear.endYear) ++
      Json.obj("overDue" -> chargeItem.isOverdue())
  }

  private def accruingInterestJson(chargeItem: ChargeItem): JsObject = {
    if (chargeItem.hasAccruingInterest) {
      Json.obj() ++
        Json.obj("accruingInterest"-> chargeItem.interestOutstandingAmount) ++
        Json.obj("interestRate"-> chargeItem.interestRate.map(ratePctString)) ++
        Json.obj("interestFromDate"-> chargeItem.interestFromDate) ++
        Json.obj("interestEndDate"-> chargeItem.interestEndDate)
    } else {
      Json.obj()
    }
  }

  private def outstandingChargeDetails(outstandingCharge: OutstandingChargesModel) = Json.obj(
    "chargeType" -> "Remaining balance"
  ) ++
    Json.obj ("outstandingAmount"-> outstandingCharge.bcdChargeType.map(_.chargeAmount)) ++
    Json.obj("dueDate"-> outstandingCharge.bcdChargeType.map(_.relevantDueDate)) ++
    Json.obj("accruingInterest"-> outstandingCharge.aciChargeType.map(_.chargeAmount))

  private def codingOut(chargeItem: CodingOutDetails): JsObject = {
      Json.obj(
        "amountCodedOut" -> chargeItem.amountCodedOut,
        "endTaxYear" -> chargeItem.codingTaxYear.endYear.toString
      )
  }
}

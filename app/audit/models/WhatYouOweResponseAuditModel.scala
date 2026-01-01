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
    (whatYouOweChargesList.codedOutDetails.map(chargeItem => Json.obj("codingOut" -> codingOut(chargeItem)))) match {
      case Some(codingOutJson) => userAuditDetails(user) ++
        balanceDetailsJson ++
        Json.obj("charges" -> docDetailsListJson) ++
        codingOutJson
      case _ =>
        userAuditDetails(user) ++
          balanceDetailsJson ++
          Json.obj("charges" -> docDetailsListJson)
    }
  }

  private lazy val balanceDetailsJson: JsObject = {
    def onlyIfPositive(amount: BigDecimal): Option[BigDecimal] = Some(amount).filter(_ > 0)

    val fields: JsObject = {
      Json.obj() ++
        Json.obj("balanceDueWithin30Days"-> onlyIfPositive(whatYouOweChargesList.balanceDetails.balanceDueWithin30Days)) ++
        Json.obj("overDueAmount"-> onlyIfPositive(whatYouOweChargesList.balanceDetails.overDueAmount)) ++
        Json.obj("totalBalance"-> onlyIfPositive(whatYouOweChargesList.balanceDetails.totalBalance)) ++
        Json.obj("creditAmount"-> whatYouOweChargesList.balanceDetails.unallocatedCredit)
    }

    val secondOrMoreYearOfMigration = user.incomeSources.yearOfMigration.exists(currentTaxYear > _.toInt)

    if (secondOrMoreYearOfMigration && fields.values.nonEmpty) Json.obj("balanceDetails" -> fields)
    else Json.obj()
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

/*
 * Copyright 2018 HM Revenue & Customs
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

import auth.MtdItUser
import models.calculation.{CalcDisplayModel, CalculationModel}
import models.incomeSourceDetails.BusinessDetailsModel

object BillsAuditing {

  case class BillsAuditModel[A](user: MtdItUser[A], dataModel: CalcDisplayModel) extends AuditModel {

    override val auditType: String = "billsPageView"
    override val transactionName: String = "bills-page-view"

    val business: Option[BusinessDetailsModel] = user.incomeSources.businesses.headOption
    override val detail: Seq[(String, String)] = Seq(
      "mtditid" -> user.mtditid,
      "nino" -> user.nino,
      "hasBusiness" -> user.incomeSources.hasBusinessIncome.toString,
      "hasProperty" -> user.incomeSources.hasPropertyIncome.toString,
      "bizAccPeriodStart" -> business.fold("-")(x => s"${x.accountingPeriod.start}"),
      "bizAccPeriodEnd" -> business.fold("-")(x => s"${x.accountingPeriod.end}"),
      "propAccPeriodStart" -> user.incomeSources.property.fold("-")(x => s"${x.accountingPeriod.start}"),
      "propAccPeriodEnd" -> user.incomeSources.property.fold("-")(x => s"${x.accountingPeriod.end}"),
      "currentBill" -> dataModel.calcDataModel.fold(dataModel.calcAmount)(_.totalIncomeTaxNicYtd).toString
    )
  }

  case class BasicBillsAuditModel[A](user: MtdItUser[A], dataModel: CalculationModel) extends AuditModel {

    override val auditType: String = "billsPageView"
    override val transactionName: String = "bills-page-view-api-19a"

    val billAmount: Seq[(String, String)] = dataModel.displayAmount match {
      case Some(amount) => Seq("currentBill" -> amount.toString)
      case None => Seq.empty
    }

    override val detail: Seq[(String, String)] = Seq(
      "mtditid" -> user.mtditid,
      "nino" -> user.nino
    ) ++ billAmount
  }
}

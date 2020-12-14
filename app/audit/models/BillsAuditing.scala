/*
 * Copyright 2020 HM Revenue & Customs
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
import models.incomeSourceDetails.BusinessDetailsModel
import play.api.libs.json.{JsValue, Json, Writes}

object BillsAuditing {

  case class BillsAuditModel(user: MtdItUser[_], calcAmount: BigDecimal) extends ExtendedAuditModel {

    override val auditType: String = "TaxYearBillView"
    override val transactionName: String = "view-tax-year_bill"

    val business: Option[BusinessDetailsModel] = user.incomeSources.businesses.headOption

    private case class AuditDetail(mtditid: String,
                                   nationalInsuranceNumber: String,
                                   hasBusiness: Boolean,
                                   hasProperty: Boolean,
                                   bizAccPeriodStart: String,
                                   bizAccPeriodEnd: String,
                                   propAccPeriodStart: String,
                                   propAccPeriodEnd: String,
                                   currentBill: String,
                                   saUtr: Option[String],
                                   credId: Option[String],
                                   userType: Option[String])

    private implicit val auditDetailWrites: Writes[AuditDetail] = Json.writes[AuditDetail]

    override val detail: JsValue = Json.toJson(
      AuditDetail(
        user.mtditid,
        user.nino,
        user.incomeSources.hasBusinessIncome,
        user.incomeSources.hasPropertyIncome,
        business.fold("-")(x => s"${x.accountingPeriod.start}"),
        business.fold("-")(x => s"${x.accountingPeriod.end}"),
        user.incomeSources.property.fold("-")(x => s"${x.accountingPeriod.start}"),
        user.incomeSources.property.fold("-")(x => s"${x.accountingPeriod.end}"),
        calcAmount.toString(),
        user.saUtr,
        user.credId,
        user.userType
      )
    )
  }
}

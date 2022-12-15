/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.libs.json.{JsObject, JsValue, Json}

// TODO: nested object examples in TaxYearSummaryResponseAuditModel
object CreditsSummaryAuditing {

  case class CreditDetails(date: String, descirption: String, status: String, amount: String)

  case class CreditsSummaryModel(saUTR: String,
                                 nino: String,
                                 userType: String,
                                 creditId: String,
                                 mtdRef: String,
                                 creditOnAccount: String,
                                 creditDetails: Seq[CreditDetails]) extends ExtendedAuditModel {

    override val transactionName: String = enums.TransactionName.CreditsSummary
    override val auditType: String = enums.AuditType.CreditSummaryResponse

    private def creditDetailToJson(credit: CreditDetails): JsObject = {
      Json.obj(
        "date" -> credit.date,
        "description" -> credit.descirption,
        "status" -> credit.status,
        "amount" -> credit.amount)
    }

    private def getCreditDetails: Seq[JsObject] = creditDetails.map(creditDetailToJson)

    override val detail: JsValue =
      Json.obj("saUtr" -> saUTR,
        "nationalInsuranceNumber" -> nino,
        "userType" -> userType,
        "credId" -> creditId,
        "mtditid" -> mtdRef,
        "creditOnAccount" -> creditOnAccount,
        "creditDetails" -> getCreditDetails
      )

  }

}

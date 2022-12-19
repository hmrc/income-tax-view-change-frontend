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

import models.creditDetailModel.{CreditDetailModel, CreditType}
import play.api.i18n.Messages
import play.api.libs.json.{JsObject, JsValue, Json}

object CreditsSummaryAuditing {

  case class CreditSummaryDetails(date: String, description: String, status: String, amount: String)

  private def toStatus(charge: CreditDetailModel) : String = charge.documentDetail.getChargePaidStatus match {
    case "paid" => "Fully allocated"
    case "part-paid" => "Partially allocated"
    case "unpaid" | _ => "Not allocated"
  }

  private def toDescription(creditType: CreditType)
                           (implicit messages: Messages): String =
    messages(creditType.key)


  implicit def creditDetailModelToCreditSummaryDetails(charge: CreditDetailModel)
                                                      (implicit messages: Messages): CreditSummaryDetails = {
    CreditSummaryDetails(
      date = charge.date.toString,
      description = toDescription(charge.creditType)(messages),
      status = toStatus(charge),
      amount = charge.documentDetail.originalAmount.map(_.abs.toString()).getOrElse("TODO: raise error??"))
  }

  implicit def toCreditSummaryDetailsSeq(charge: Seq[CreditDetailModel])
                                                            (implicit messages: Messages): Seq[CreditSummaryDetails] = charge.map(creditDetailModelToCreditSummaryDetails)


  case class CreditsSummaryModel(saUTR: String,
                                 nino: String,
                                 userType: String,
                                 credId: String,
                                 mtdRef: String,
                                 creditOnAccount: String,
                                 creditDetails: Seq[CreditSummaryDetails]) extends ExtendedAuditModel {

    override val transactionName: String = enums.TransactionName.CreditsSummary
    override val auditType: String = enums.AuditType.CreditSummaryResponse

    private def creditDetailToJson(credit: CreditSummaryDetails): JsObject = {
      Json.obj(
        "date" -> credit.date,
        "description" -> credit.description,
        "status" -> credit.status,
        "amount" -> credit.amount)
    }

    private def getCreditDetails: Seq[JsObject] = creditDetails.map(creditDetailToJson)

    override val detail: JsValue =
      Json.obj("saUtr" -> saUTR,
        "nationalInsuranceNumber" -> nino,
        "userType" -> userType,
        "credId" -> credId,
        "mtditid" -> mtdRef,
        "creditOnAccount" -> creditOnAccount,
        "creditDetails" -> getCreditDetails
      )

  }

}

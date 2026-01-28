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

import models.creditDetailModel.CreditDetailModel
import models.financialDetails.CreditType
import play.api.i18n.{Lang, MessagesApi}
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.language.implicitConversions

object CreditSummaryAuditing {

  case class CreditSummaryDetails(date: String, description: String, status: String, amount: String)

  private def toStatus(credit: CreditDetailModel): String = credit.charge.getChargePaidStatus match {
    case "paid" => "Fully allocated"
    case "part-paid" => "Partially allocated"
    case "unpaid" | _ => "Not allocated"
  }

  private def toDescription(creditType: CreditType)
                           (implicit messages: MessagesApi): String = messages(s"credit.description.${creditType.key}")(Lang("en"))

  implicit def creditDetailModelToCreditSummaryDetails(credit: CreditDetailModel)
                                                      (implicit messages: MessagesApi): CreditSummaryDetails = {
    // we assume that we would never show amount equal to zero
    CreditSummaryDetails(
      date = credit.date.toString,
      description = toDescription(credit.creditType)(messages),
      status = toStatus(credit),
      amount = credit.charge.originalAmount.abs.toString())
  }

  implicit def toCreditSummaryDetailsSeq(charge: Seq[CreditDetailModel])
                                        (implicit messages: MessagesApi): Seq[CreditSummaryDetails] = charge.map(creditDetailModelToCreditSummaryDetails)


  case class CreditsSummaryModel(saUTR: String,
                                 nino: String,
                                 userType: String,
                                 credId: String,
                                 mtdRef: String,
                                 creditOnAccount: String,
                                 creditDetails: Seq[CreditSummaryDetails]) extends ExtendedAuditModel {

    override val transactionName: String = enums.TransactionName.CreditsSummary
    override val auditType: String = enums.AuditType.AuditType.CreditsSummaryResponse

    private def creditDetailToJson(credit: CreditSummaryDetails): JsObject = {
      Json.obj(
        "date" -> credit.date,
        "description" -> credit.description,
        "status" -> credit.status,
        "amount" -> credit.amount)
    }

    def getCreditDetails: Seq[JsObject] = creditDetails.map(creditDetailToJson)

    override val detail: JsValue =
      Json.obj("saUtr" -> saUTR,
        "nino" -> nino,
        "userType" -> userType,
        "credId" -> credId,
        "mtditid" -> mtdRef,
        "creditOnAccount" -> creditOnAccount,
        "creditDetails" -> getCreditDetails
      )

  }

}

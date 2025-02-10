/*
 * Copyright 2025 HM Revenue & Customs
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

package models.penalties.lateSubmission

import play.api.libs.json.{Format, JsResult, JsValue}
import utils.JsonUtils

import java.time.LocalDate

case class LateSubmission(lateSubmissionID: String,
                          taxPeriod: Option[String],
                          taxReturnStatus: Option[TaxReturnStatusEnum],
                          taxPeriodStartDate: Option[LocalDate],
                          taxPeriodEndDate: Option[LocalDate],
                          taxPeriodDueDate: Option[LocalDate],
                          returnReceiptDate: Option[LocalDate]
                         )

object LateSubmission extends JsonUtils {
  implicit val format: Format[LateSubmission] = new Format[LateSubmission] {
    override def reads(json: JsValue): JsResult[LateSubmission] = {
      for {
        lateSubmissionID <- (json \ "lateSubmissionID").validate[String]
        taxPeriod <- (json \ "taxPeriod").validateOpt[String]
        optTaxReturnStatus <- (json \ "taxReturnStatus").validateOpt[TaxReturnStatusEnum](TaxReturnStatusEnum.format)
        taxPeriodStartDate <- (json \ "taxPeriodStartDate").validateOpt[LocalDate]
        taxPeriodEndDate <- (json \ "taxPeriodEndDate").validateOpt[LocalDate]
        taxPeriodDueDate <- (json \ "taxPeriodDueDate").validateOpt[LocalDate]
        returnReceiptDate <- (json \ "returnReceiptDate").validateOpt[LocalDate]
      } yield {
        LateSubmission(lateSubmissionID, taxPeriod, optTaxReturnStatus, taxPeriodStartDate, taxPeriodEndDate, taxPeriodDueDate, returnReceiptDate)
      }
    }

    override def writes(o: LateSubmission): JsValue =
      jsonObjNoNulls(
        "lateSubmissionID" -> o.lateSubmissionID,
        "taxPeriod" -> o.taxPeriod,
        "taxReturnStatus" -> o.taxReturnStatus,
        "taxPeriodStartDate" -> o.taxPeriodStartDate,
        "taxPeriodEndDate" -> o.taxPeriodEndDate,
        "taxPeriodDueDate" -> o.taxPeriodDueDate,
        "returnReceiptDate" -> o.returnReceiptDate
      )
  }
}

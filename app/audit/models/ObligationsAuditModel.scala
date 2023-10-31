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

import audit.Utilities
import utils.Utilities._
import auth.MtdItUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.core.TaxYearId
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import play.api.libs.json.{JsObject, JsValue, Json}
import models.core.TaxYearId._

case class ObligationsAuditModel(incomeSourceType: IncomeSourceType,
                                 obligations: ObligationsViewModel,
                                 businessName: String,
                                 reportingMethod: String,
                                 taxYearId: TaxYearId
                                )(implicit user: MtdItUser[_]) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.Obligations

  override val auditType: String = enums.AuditType.Obligations

  val journey: String = incomeSourceType match {
    case SelfEmployment => "SE"
    case UkProperty => "UKPROPERTY"
    case ForeignProperty => "FOREIGNPROPERTY"
  }

  val name: String = if (businessName == "Not Found") "Sole trader business" else businessName


  val repMethod: String = if (reportingMethod == "quarterly") "Quarterly" else "Annual"

  val quarterly: Seq[(Int, Seq[DatesModel])] = {
    ((if (obligations.quarterlyObligationsDatesYearOne.nonEmpty)
      Seq((obligations.quarterlyObligationsDatesYearOne.head.inboundCorrespondenceFrom.getYear, obligations.quarterlyObligationsDatesYearOne))
    else
      Seq()
      )
      ++
      (if (obligations.quarterlyObligationsDatesYearTwo.nonEmpty)
        Seq((obligations.quarterlyObligationsDatesYearTwo.head.inboundCorrespondenceFrom.getYear, obligations.quarterlyObligationsDatesYearTwo))
      else
        Seq()
        ))
  }

  private val quarterlyUpdatesOption: Option[Seq[JsObject]] = {
    if (quarterly.nonEmpty) {
      Some(quarterly.collect {
        case (taxYearAsInt, dataModel) => {
          val localTaxYearId = mkTaxYearId(taxYearAsInt)
          Json.obj(
            "taxYear" -> s"${localTaxYearId.normalised}",
            "quarter" -> dataModel.map { entry =>
              Json.obj(
                "startDate" -> entry.inboundCorrespondenceFrom,
                "endDate" -> entry.inboundCorrespondenceTo,
                "deadline" -> entry.inboundCorrespondenceDue
              )
            }
          )
        }
      })
    } else None
  }

  private val eopsObligationsOption: Option[Seq[JsObject]] = {
    if (obligations.eopsObligationsDates.nonEmpty){
    Some(obligations.eopsObligationsDates.map { eops =>
      Json.obj(
        "taxYearStartDate" -> eops.inboundCorrespondenceFrom,
        "taxYearEndDate" -> eops.inboundCorrespondenceTo,
        "deadline" -> eops.inboundCorrespondenceDue
      )
    })} else None
  }

  private val finalDeclarationDatesOption: Option[Seq[JsObject]] = {
    if (obligations.finalDeclarationDates.nonEmpty) {
      Some(obligations.finalDeclarationDates.map { finalDec =>
        Json.obj(
          "taxYearStartDate" -> finalDec.inboundCorrespondenceFrom,
          "taxYearEndDate" -> finalDec.inboundCorrespondenceTo,
          "deadline" -> finalDec.inboundCorrespondenceDue
        )
      })
    } else None
  }

  override val detail: JsValue = {
    Utilities.userAuditDetails(user) ++
      Json.obj(
        "journeyType" -> journey,
        "incomeSourceInfo" ->
          Json.obj(
            "businessName" -> name,
            "reportingMethod" -> repMethod,
            "taxYear" -> s"${taxYearId.normalised}"
          )) ++
      ("quarterlyUpdates", quarterlyUpdatesOption) ++
      ("EOPstatement", eopsObligationsOption) ++
      ("finalDeclaration", finalDeclarationDatesOption)
  }
}

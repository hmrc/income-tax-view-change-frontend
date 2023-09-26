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
import auth.MtdItUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.incomeSourceDetails.TaxYear
import models.incomeSourceDetails.viewmodels.{CeasedBusinessDetailsViewModel, DatesModel, ObligationsViewModel, ViewBusinessDetailsViewModel, ViewPropertyDetailsViewModel}
import play.api.libs.json.{JsValue, Json}

case class ObligationsAuditModel(incomeSourceType: IncomeSourceType,
                                 obligations: ObligationsViewModel,
                                 businessName: String,
                                 reportingMethod: String,
                                 taxYear: TaxYear
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

  override val detail: JsValue = {
    Utilities.userAuditDetails(user) ++
      Json.obj(
        "journeyType" -> journey,
        "incomeSourceInfo" ->
          Json.obj(
            "businessName" -> name,
            "reportingMethod" -> repMethod,
            "taxYear" -> s"${taxYear.startYear}-${taxYear.endYear}"
          ),
        "quarterlyUpdates" ->
          quarterly.map { value =>
            Json.obj(
              "taxYear" -> s"${value._1}-${value._1 + 1}",
              "quarter" -> value._2.map { entry =>
                Json.obj(
                  "startDate" -> entry.inboundCorrespondenceFrom,
                  "endDate" -> entry.inboundCorrespondenceTo,
                  "deadline" -> entry.inboundCorrespondenceDue
                )
              }
            )
          },
        "EOPstatement" ->
          obligations.eopsObligationsDates.map { eops =>
            Json.obj(
              "taxYearStartDate" -> eops.inboundCorrespondenceFrom,
              "taxYearEndDate" -> eops.inboundCorrespondenceDue,
              "deadline" -> eops.inboundCorrespondenceDue
            )
          },
        "finalDeclaration" ->
          obligations.finalDeclarationDates.map { finalDec =>
            Json.obj(
              "taxYearStartDate" -> finalDec.inboundCorrespondenceFrom,
              "taxYearEndDate" -> finalDec.inboundCorrespondenceDue,
              "deadline" -> finalDec.inboundCorrespondenceDue
            )
          }
      )
  }
}

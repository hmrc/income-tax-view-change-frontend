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

import audit.Utilities.userAuditDetails
import auth.MtdItUser
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import models.createIncomeSource.{CreateIncomeSourceErrorResponse, CreateIncomeSourceListResponseError, CreateIncomeSourceResponse}
import models.incomeSourceDetails.viewmodels.CheckDetailsViewModel
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.Utilities._


case class CreateIncomeSourceAuditModel(incomeSourceType: IncomeSourceType,
                                        viewModel: CheckDetailsViewModel,
                                        failureCategory: Option[String],
                                        failureReason: Option[String],
                                        createIncomeSourceResponse: Option[CreateIncomeSourceResponse]
                                       )(implicit user: MtdItUser[_]) extends ExtendedAuditModel {


  private val isSuccessful = failureCategory.isEmpty

  override val transactionName: String = enums.TransactionName.CreateIncomeSource
  override val auditType: String = enums.AuditType.CreateIncomeSource

  private val outcome: JsObject = {
    val outcome: JsObject = Json.obj("isSuccessful" -> isSuccessful)

    if (isSuccessful) outcome
    else outcome ++ Json.obj(
      "isSuccessful" -> isSuccessful,
      "failureCategory" -> failureCategory,
      "failureReason" -> failureReason)
  }


  override val detail: JsValue = {

    val ex = {
      val seDetails = Json.obj() ++
        ("businessName", viewModel.businessName) ++
        ("businessDescription", viewModel.businessTrade) ++
        ("addressLine1", viewModel.businessAddressLine1) ++
        ("addressLine2", viewModel.businessAddressLine2) ++
        ("addressLine3", viewModel.businessAddressLine3) ++
        ("addressTownOrCity", viewModel.businessAddressLine4) ++
        ("addressPostcode", viewModel.businessPostalCode) ++
        ("addressCountry", viewModel.businessCountryCode)

      val baseDetails = userAuditDetails(user) ++
        Json.obj(
          "outcome" -> outcome,
          "journeyType" -> incomeSourceType.journeyType
        ) ++
        ("addedIncomeSourceID", createIncomeSourceResponse.map(x => x.incomeSourceId)) ++
        ("dateStarted", viewModel.businessStartDate) ++ (if (incomeSourceType == SelfEmployment) {
        seDetails

      } else Json.obj()) ++
        ("accountingMethod", viewModel.incomeSourcesAccountingMethod.map {
          case "accruals" => "Traditional accounting"
          case "cash" => "Cash basis accounting"
        })


      incomeSourceType match {
        case SelfEmployment => baseDetails ++ seDetails
        case _ => baseDetails
      }
    }
    println(ex)
    println("LOOOOOOK")
    ex
  }
}
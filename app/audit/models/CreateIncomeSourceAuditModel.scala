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
import enums.IncomeSourceJourney.IncomeSourceType
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.viewmodels.{CheckBusinessDetailsViewModel, CheckDetailsViewModel, CheckPropertyViewModel}
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

    val baseDetails = userAuditDetails(user) ++
      Json.obj(
        "outcome" -> outcome,
        "journeyType" -> incomeSourceType.journeyType
      ) ++
     Json.obj ("addedIncomeSourceID" -> createIncomeSourceResponse.map(x => x.incomeSourceId))

    val businessDetails = viewModel match {
      case businessDetailsViewModel: CheckBusinessDetailsViewModel =>
        val seDetails = Json.obj() ++
         Json.obj("businessName"-> Some(businessDetailsViewModel.businessName)) ++
          Json.obj("dateStarted"-> Some(businessDetailsViewModel.businessStartDate)) ++
          Json.obj("businessDescription" -> businessDetailsViewModel.businessTrade) ++
          Json.obj("addressLine1" -> businessDetailsViewModel.businessAddressLine1) ++
          Json.obj("addressLine2"-> businessDetailsViewModel.businessAddressLine2) ++
          Json.obj("addressLine3"-> businessDetailsViewModel.businessAddressLine3) ++
          Json.obj("addressTownOrCity"-> businessDetailsViewModel.businessAddressLine4) ++
          Json.obj("addressPostcode"-> businessDetailsViewModel.businessPostalCode) ++
          Json.obj("addressCountry"-> businessDetailsViewModel.businessCountryCode)

        seDetails

      case propertyViewModel: CheckPropertyViewModel =>
        val propDetails = Json.obj("dateStarted" -> propertyViewModel.tradingStartDate)

        propDetails
    }

    baseDetails ++ businessDetails

  }
}
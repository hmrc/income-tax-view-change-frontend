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
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import models.incomeSourceDetails.viewmodels.ManageIncomeSourceDetailsViewModel
import play.api.libs.json.{JsValue, Json}

case class ManageYourDetailsResponseAuditModel(
                                                viewModel: ManageIncomeSourceDetailsViewModel
                                              )(implicit user: MtdItUser[_]) extends ExtendedAuditModel {
  override val transactionName: String = enums.TransactionName.ManageIncomeSourceDetails
  override val auditType: String = enums.AuditType.ManageIncomeSourceDetails

  override val detail: JsValue =
    Utilities.userAuditDetails(user) ++
      Json.obj(
        "journeyType" ->
          (viewModel.incomeSourceType match {
            case SelfEmployment => "SE"
            case UkProperty => "UKPROPERTY"
            case ForeignProperty => "FOREIGNPROPERTY"
          }),
        "businessName" -> (viewModel.tradingName match {
          case Some(name) => s"$name"
          case None => None
        }),
        "businessAddressLine1" -> viewModel.address.map(address => address.addressLine1),
        "businessAddressLine2" -> viewModel.address.flatMap(address => address.addressLine2),
        "businessAddressLine3" -> viewModel.address.flatMap(address => address.addressLine3),
        "businessAddressTownOrCity" -> viewModel.address.flatMap(address => address.addressLine4),
        "businessAddressPostcode" -> viewModel.address.flatMap(address => address.postCode),
        "businessAddressCountry" -> viewModel.address.map(address => address.countryCode),
        "dateStarted" -> viewModel.tradingStartDate,
        "accountingMethod" ->
          (viewModel.businessAccountingMethod match {
            case Some(value) => if (value) {
              "Traditional accounting"
            } else {
              "Cash basis accounting"
            }
            case None => None
          }),
        "incomeReportingMethodYear1" -> viewModel.latencyDetails.map(reportingMethod =>
          Json.obj(
            "reportingMethod" ->
              (reportingMethod.latencyIndicator1 match {
                case "A" => "Annually"
                case "Q" => "Quarterly"
              }),
            "taxYear" -> s"${reportingMethod.taxYear1}-${(reportingMethod.taxYear1.toInt + 1).toString}"
          )),
        "incomeReportingMethodYear2" -> viewModel.latencyDetails.map(reportingMethod =>
          Json.obj(
            "reportingMethod" ->
              (reportingMethod.latencyIndicator2 match {
                case "A" => "Annually"
                case "Q" => "Quarterly"
              }),
            "taxYear" -> s"${reportingMethod.taxYear2}-${(reportingMethod.taxYear2.toInt + 1).toString}"
          ))
      )

}

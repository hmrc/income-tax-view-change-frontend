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
import models.incomeSourceDetails.viewmodels.{CeasedBusinessDetailsViewModel, ViewBusinessDetailsViewModel, ViewPropertyDetailsViewModel}
import play.api.libs.json.{JsValue, Json}

case class ManageIncomeSourcesAuditModel(soleTraderBusinesses: List[ViewBusinessDetailsViewModel],
                                         ukProperty: Option[ViewPropertyDetailsViewModel],
                                         foreignProperty: Option[ViewPropertyDetailsViewModel],
                                         ceasedBusinesses: List[CeasedBusinessDetailsViewModel]
                                        )(implicit user: MtdItUser[_]) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.ManageIncomeSourcesHome

  override val auditType: String = enums.AuditType.ManageIncomeSourcesHome

  override val detail: JsValue =
    Utilities.userAuditDetails(user) ++
      Json.obj(
        "soleTraderBusinesses" ->
          soleTraderBusinesses.map(business =>
            Json.obj(
              "businessName" -> business.tradingName,
              "dateStarted" -> business.tradingStartDate
            )
          ),
        "ukProperty" ->
          ukProperty.map(property =>
            Json.obj(
              "dateStarted" -> property.tradingStartDate
            )
          ),
        "foreignProperty" ->
          foreignProperty.map(property =>
            Json.obj(
              "dateStarted" -> property.tradingStartDate
            )
          ),
        "ceasedBusinesses" ->
          ceasedBusinesses.map(business =>
            Json.obj(
              "businessName" -> business.tradingName,
              "dateStarted" -> business.tradingStartDate,
              "dateEnded" -> business.cessationDate
            )
          )
      )
}
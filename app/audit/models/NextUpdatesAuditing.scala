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
import models.incomeSourceDetails.{BusinessDetailsModel, PropertyDetailsModel}
import play.api.libs.json.{JsValue, Json}

import scala.language.implicitConversions

object NextUpdatesAuditing {

  private val nextUpdateTransactionName = enums.TransactionName.ObligationsPageView
  private val nextUpdateAuditType: String = enums.AuditType.AuditType.ObligationsPageView

  case class NextUpdatesAuditModel[A](user: MtdItUser[A]) extends ExtendedAuditModel {
    override val transactionName: String = nextUpdateTransactionName
    val business: Option[BusinessDetailsModel] = user.incomeSources.businesses.headOption

    val property: Option[PropertyDetailsModel] = user.incomeSources.properties.headOption

    override val detail: JsValue =
      userAuditDetails(user) ++
        Json.obj(
          "hasBusiness" -> user.incomeSources.hasBusinessIncome.toString,
          "hasProperty" -> user.incomeSources.hasPropertyIncome.toString,
          "bizAccPeriodStart" -> business.fold("-")(x => s"${x.accountingPeriod.map(ac => ac.start)}"),
          "bizAccPeriodEnd" -> business.fold("-")(x => s"${x.accountingPeriod.map(ac => ac.end)}"),
          "propAccPeriodStart" -> property.fold("-")(x => s"${x.accountingPeriod.map(ac => ac.start)}"),
          "propAccPeriodEnd" -> property.fold("-")(x => s"${x.accountingPeriod.map(ac => ac.end)}")
        )
    override val auditType: String = nextUpdateAuditType
  }
}
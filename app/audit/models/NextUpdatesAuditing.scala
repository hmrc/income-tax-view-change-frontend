/*
 * Copyright 2021 HM Revenue & Customs
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

import auth.MtdItUser

object NextUpdatesAuditing {

  val nextUpdateTransactionName = "ITVCObligations"
  val nextUpdateAuditType = "obligationsPageView"

  case class NextUpdatesAuditModel[A](user: MtdItUser[A]) extends AuditModel {
    override val transactionName: String = nextUpdateTransactionName
    val business = user.incomeSources.businesses.headOption
    override val detail: Seq[(String, String)] = Seq(
      "mtdid" -> user.mtditid,
      "nino" -> user.nino,
      "hasBusiness" -> user.incomeSources.hasBusinessIncome.toString,
      "hasProperty" -> user.incomeSources.hasPropertyIncome.toString,
      "bizAccPeriodStart" -> business.fold("-")(x => s"${x.accountingPeriod.start}"),
      "bizAccPeriodEnd" -> business.fold("-")(x => s"${x.accountingPeriod.end}"),
      "propAccPeriodStart" -> user.incomeSources.property.fold("-")(x => s"${x.accountingPeriod.start}"),
      "propAccPeriodEnd" -> user.incomeSources.property.fold("-")(x => s"${x.accountingPeriod.end}")
    )
    override val auditType: String = nextUpdateAuditType
  }

}

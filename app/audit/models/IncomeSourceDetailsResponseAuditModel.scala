/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.libs.json.Json

case class IncomeSourceDetailsResponseAuditModel[A](selfEmploymentIds: List[String],
                                                    propertyIncomeId: Option[String])(implicit user: MtdItUser[A]) extends AuditModel {

  override val transactionName: String = "income-source-details-response"
  override val auditType: String = "incomeSourceDetailsResponse"

  val seIds: Option[(String, String)] =
    if (selfEmploymentIds.isEmpty) None else Some("selfEmploymentIncomeSourceIds" -> Json.toJson(selfEmploymentIds).toString)

  override val detail: Seq[(String, String)] = Seq(
    Some("mtditid" -> user.mtditid),
    Some("nino" -> user.nino),
    seIds,
    propertyIncomeId.map("propertyIncomeSourceId" -> _)
  ).flatten
}
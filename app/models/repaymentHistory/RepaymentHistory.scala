/*
 * Copyright 2022 HM Revenue & Customs
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

package models.repaymentHistory

import play.api.libs.json.{Json, OFormat}

case class RepaymentHistory(amountApprovedforRepayment: Option[BigDecimal],
                            amountRequested: BigDecimal,
                            repaymentMethod: String,
                            totalRepaymentAmount: BigDecimal,
                            repaymentItems : Seq[RepaymentItem],
                            estimatedRepaymentDate: String,
                            creationDate: String,
                            repaymentRequestNumber: String
                           )

object RepaymentHistory {
  implicit val format: OFormat[RepaymentHistory] = Json.format[RepaymentHistory]
}

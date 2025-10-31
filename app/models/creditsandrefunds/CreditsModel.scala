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

package models.creditsandrefunds

import models.core.ResponseModel.{AResponseReads, SuccessModel}
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import play.api.libs.json._

import java.time.LocalDate


case class CreditsModel(availableCreditForRepayment: BigDecimal,
                        allocatedCredit: BigDecimal,
                        allocatedCreditForFutureCharges: BigDecimal,
                        unallocatedCredit: BigDecimal,
                        totalCredit: BigDecimal,
                        transactions: List[Transaction] ) extends SuccessModel

object CreditsModel {

  implicit val format: OFormat[CreditsModel] = Json.format[CreditsModel]

  implicit val reads: CreditsResponseReads = new CreditsResponseReads
  class CreditsResponseReads extends AResponseReads[CreditsModel] {
    implicit val format: Format[CreditsModel] = CreditsModel.format
  }
}

case class Transaction(transactionType: CreditType,
                       amount: BigDecimal,
                       taxYear: Option[TaxYear],
                       dueDate: Option[LocalDate],
                       transactionId: String)

object Transaction {
  implicit val format: OFormat[Transaction] = Json.format[Transaction]
}




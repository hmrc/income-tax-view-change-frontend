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
import models.liabilitycalculation.viewmodels.AllowancesAndDeductionsViewModel
import play.api.libs.json.{JsValue, Json}

import scala.language.implicitConversions

case class AllowanceAndDeductionsResponseAuditModel(mtdItUser: MtdItUser[_],
                                                    viewModel: AllowancesAndDeductionsViewModel) extends ExtendedAuditModel {

  override val transactionName: String = enums.TransactionName.AllowancesDeductionsDetailsResponse
  override val auditType: String = enums.AuditType.AuditType.AllowancesDeductionsDetailsResponse

  override val detail: JsValue = {
    userAuditDetails(mtdItUser) ++
      Json.obj("personalAllowance"-> viewModel.personalAllowance) ++
      Json.obj("pensionContributions"-> viewModel.pensionContributions) ++
      Json.obj("lossRelief" -> viewModel.lossesAppliedToGeneralIncome) ++
      Json.obj("giftsToCharity"-> viewModel.giftOfInvestmentsAndPropertyToCharity) ++
      Json.obj("annualPayments"-> viewModel.grossAnnuityPayments) ++
      Json.obj("qualifyingLoanInterest"-> viewModel.qualifyingLoanInterestFromInvestments) ++
      Json.obj("postCessationTradeReceipts"-> viewModel.postCessationTradeReceipts) ++
      Json.obj("tradeUnionPayments"-> viewModel.paymentsToTradeUnionsForDeathBenefits) ++
      Json.obj("marriageAllowanceTransfer"-> viewModel.transferredOutAmount)
  }
}

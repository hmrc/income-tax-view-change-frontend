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

package audit.models

import audit.Utilities.userAuditDetails
import auth.MtdItUserBase
import models.liabilitycalculation.viewmodels.AllowancesAndDeductionsViewModel
import play.api.libs.json.JsValue
import utils.Utilities._

case class AllowanceAndDeductionsResponseAuditModel(mtdItUser: MtdItUserBase[_],
                                                    viewModel: AllowancesAndDeductionsViewModel) extends ExtendedAuditModel {

  override val transactionName: String = "allowances-deductions-details-response"
  override val auditType: String = "AllowancesDeductionsDetailsResponse"

  override val detail: JsValue = {
    userAuditDetails(mtdItUser) ++
      ("personalAllowance", viewModel.personalAllowance) ++
      ("pensionContributions", viewModel.pensionContributions) ++
      ("lossRelief", viewModel.lossesAppliedToGeneralIncome) ++
      ("giftsToCharity", viewModel.giftOfInvestmentsAndPropertyToCharity) ++
      ("annualPayments", viewModel.grossAnnuityPayments) ++
      ("qualifyingLoanInterest", viewModel.qualifyingLoanInterestFromInvestments) ++
      ("postCessationTradeReceipts", viewModel.postCessationTradeReceipts) ++
      ("tradeUnionPayments", viewModel.paymentsToTradeUnionsForDeathBenefits) ++
      ("marriageAllowanceTransfer", viewModel.transferredOutAmount)
  }
}

/*
 * Copyright 2024 HM Revenue & Customs
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

package models.chargeSummary

import enums.DocumentType
import enums.GatewayPage._
import models.chargeHistory.AdjustmentHistoryModel
import models.financialDetails.{DocumentDetailWithDueDate, FinancialDetail, FinancialDetailsModel}
import play.twirl.api.Html

import java.time.LocalDate

case class ChargeSummaryViewModel(
                                   currentDate: LocalDate,
                                   documentDetailWithDueDate: DocumentDetailWithDueDate,
                                   backUrl: String,
                                   paymentBreakdown: List[FinancialDetail],
                                   paymentAllocations: List[PaymentHistoryAllocations],
                                   payments: FinancialDetailsModel,
                                   chargeHistoryEnabled: Boolean,
                                   paymentAllocationEnabled: Boolean,
                                   latePaymentInterestCharge: Boolean,
                                   codingOutEnabled: Boolean,
                                   isAgent: Boolean = false,
                                   btaNavPartial: Option[Html] = None,
                                   origin: Option[String] = None,
                                   gatewayPage: Option[GatewayPage] = None,
                                   isMFADebit: Boolean,
                                   documentType: DocumentType,
                                   adjustmentHistory: AdjustmentHistoryModel
                                 )


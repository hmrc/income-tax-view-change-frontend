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

package models.homePage

import models.incomeSourceDetails.TaxYear
import models.nextUpdates.NextUpdatesTileViewModel

import java.time.LocalDate

case class HomePageViewModel(utr: Option[String],
                            nextPaymentsTileViewModel: NextPaymentsTileViewModel,
                            returnsTileViewModel: ReturnsTileViewModel,
                            nextUpdatesTileViewModel: NextUpdatesTileViewModel,
                            paymentCreditAndRefundHistoryTileViewModel: PaymentCreditAndRefundHistoryTileViewModel,
                            yourBusinessesTileViewModel: YourBusinessesTileViewModel,
                            dunningLockExists: Boolean = false,
                            origin: Option[String] = None)

case class NextPaymentsTileViewModel(nextPaymentDueDate: Option[LocalDate], overDuePaymentsCount: Int) {
  if (!(overDuePaymentsCount == 0)) {
    require(nextPaymentDueDate.isDefined, "Error, overDuePaymentsCount was non-0 while nextPaymentDueDate was empty")
  }
}

case class ReturnsTileViewModel(currentTaxYear: TaxYear, iTSASubmissionIntegrationEnabled: Boolean)

case class YourBusinessesTileViewModel(displayCeaseAnIncome: Boolean, incomeSourcesEnabled: Boolean,
                                       incomeSourcesNewJourneyEnabled: Boolean)
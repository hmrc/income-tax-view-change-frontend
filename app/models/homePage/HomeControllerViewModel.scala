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

import models.nextUpdates.NextUpdatesTileViewModel

import java.time.LocalDate

case class HomeControllerViewModel (
                                     origin: Option[String] = None,
                                     utr: Option[String],
                                     isAgent: Boolean,
                                     currentTaxYear: Int,
                                     dunningLockExists: Boolean,
                                     nextPaymentDueDate: Option[LocalDate],
                                     overDuePaymentsCount: Option[Int],
                                     displayCeaseAnIncome: Boolean,
                                     incomeSourcesEnabled: Boolean,
                                     nextUpdatesTileViewModel: NextUpdatesTileViewModel,
                                     creditAndRefundEnabled: Boolean,
                                     paymentHistoryEnabled: Boolean,
                                     isUserMigrated: Boolean,
                                     incomeSourcesNewJourneyEnabled: Boolean,
                                     ITSASubmissionIntegrationEnabled: Boolean,
                                     paymentCreditAndRefundHistoryTileViewModel: PaymentCreditAndRefundHistoryTileViewModel,
                                   )


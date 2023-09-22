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

package models.incomeSourceDetails.viewmodels

import java.time.LocalDate

final case class ObligationsViewModel(quarterlyObligationsDatesYearOne: Seq[DatesModel], quarterlyObligationsDatesYearTwo: Seq[DatesModel],
                                      eopsObligationsDates: Seq[DatesModel], finalDeclarationDates: Seq[DatesModel], currentTaxYear: Int, showPrevTaxYears: Boolean, businessName: Option[String] = None)

final case class DatesModel(inboundCorrespondenceFrom: LocalDate, inboundCorrespondenceTo: LocalDate,
                            inboundCorrespondenceDue: LocalDate, periodKey: String, isFinalDec: Boolean)

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

package models.reportingObligations.signUp

import models.incomeSourceDetails.TaxYear

sealed trait CheckYourAnswersViewModel {
  val startYear: String
  val endYear: String
  val isAgent: Boolean
  val cancelURL: String
  val intentIsNextYear: Boolean
}

case class MultiYearCheckYourAnswersViewModel(intentTaxYear: TaxYear,
                                              isAgent: Boolean,
                                              cancelURL: String,
                                              intentIsNextYear: Boolean = false)
  extends CheckYourAnswersViewModel {
  val startYear: String = intentTaxYear.startYear.toString
  val endYear: String = intentTaxYear.endYear.toString
}
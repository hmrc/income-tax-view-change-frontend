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

package models.optout

import models.incomeSourceDetails.TaxYear

sealed trait OptOutCheckpointViewModel

case class OptOutOneYearCheckpointViewModel(taxYear: TaxYear, showFutureChangeInfo: Boolean) extends OptOutCheckpointViewModel

case class OptOutMultiYearViewModel(intent: TaxYear) extends OptOutCheckpointViewModel


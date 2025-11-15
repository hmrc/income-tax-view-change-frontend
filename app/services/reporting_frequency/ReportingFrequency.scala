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

package services.reporting_frequency

import models.incomeSourceDetails.TaxYear
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear

object ReportingFrequency {

  val noQuarterlyUpdates = 0

  case class QuarterlyUpdatesCountForTaxYearModel(counts: Seq[QuarterlyUpdatesCountForTaxYear]) {

    def getCountFor(offeredTaxYear: TaxYear): Int = counts
      .filter(taxYearCounts => taxYearCounts.taxYear == offeredTaxYear)
      .map(_.count).sum

    val isQuarterlyUpdatesMade: Boolean = counts.map(_.count).sum > noQuarterlyUpdates
  }

}

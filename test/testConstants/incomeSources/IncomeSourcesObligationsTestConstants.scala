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

package testConstants.incomeSources

import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}

import java.time.LocalDate

object IncomeSourcesObligationsTestConstants {
  val taxYear: Int = 2022

  val testObligationsViewModel: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDatesYearOne = Seq(datesModel2022, datesModel2023),
    quarterlyObligationsDatesYearTwo = Seq(datesModel2023),
    eopsObligationsDates = Seq(datesModel2023),
    finalDeclarationDates = Seq(datesModel2023),
    currentTaxYear = taxYear,
    showPrevTaxYears = false
  )

  def generateDatesModel(year: Int): DatesModel = {
    DatesModel(LocalDate.of(year, 1, 6),
      LocalDate.of(year, 4, 5),
      LocalDate.of(year, 5, 5), "EOPS", false)
  }

  val datesModel2022: DatesModel = generateDatesModel(taxYear)
  val datesModel2023: DatesModel = generateDatesModel(taxYear + 1)

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    NextUpdatesModel("123", List(NextUpdateModel(
      LocalDate.of(taxYear, 1, 6),
      LocalDate.of(taxYear, 4, 5),
      LocalDate.of(taxYear, 5, 5),
      "Quarterly",
      None,
      "#001"
    ),
      NextUpdateModel(
        LocalDate.of(taxYear, 1, 6),
        LocalDate.of(taxYear, 4, 5),
        LocalDate.of(taxYear, 5, 5),
        "Quarterly",
        None,
        "#002"
      )
    ))
  ))
}

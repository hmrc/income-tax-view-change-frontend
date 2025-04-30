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

package testConstants

import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}

import java.time.LocalDate

object IncomeSourcesObligationsIntegrationTestConstants {
  val taxYear: Int = 2022

  val datesModelSeq2022: Seq[DatesModel] = Seq(generateDatesModel(taxYear))
  val datesModelSeq2023: Seq[DatesModel] = Seq(generateDatesModel(taxYear + 1))
  val datesModel: DatesModel = (generateDatesModel(taxYear + 1))


  val testQuarterlyObligationDates = Seq(datesModelSeq2022, datesModelSeq2023)

  val testObligationsViewModel: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDates = testQuarterlyObligationDates,
    finalDeclarationDates = Seq(datesModel),
    currentTaxYear = taxYear,
    showPrevTaxYears = false
  )

  def generateDatesModel(year: Int): DatesModel = {
    DatesModel(LocalDate.of(year, 1, 6),
      LocalDate.of(year, 4, 5),
      LocalDate.of(year, 5, 5), "EOPS", false, obligationType = "Quarterly")
  }

  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel("123", List(SingleObligationModel(
      LocalDate.of(taxYear, 1, 6),
      LocalDate.of(taxYear, 4, 5),
      LocalDate.of(taxYear, 5, 5),
      "Quarterly",
      None,
      "#001",
      StatusFulfilled),
      SingleObligationModel(
        LocalDate.of(taxYear, 1, 6),
        LocalDate.of(taxYear, 4, 5),
        LocalDate.of(taxYear, 5, 5),
        "Quarterly",
        None,
        "#002",
        StatusFulfilled
      )
    ))
  ))
}

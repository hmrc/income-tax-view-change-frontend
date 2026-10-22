/*
 * Copyright 2017 HM Revenue & Customs
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

package returns.controllers

import common.models.liabilitycalculation.{EndOfYearEstimate, IncomeSource}

object ForecastIncomeSummaryControllerTestConstants {
  val taxableIncome = 12500

  val endOfYearEstimate: EndOfYearEstimate = EndOfYearEstimate(
    incomeSource = Some(List(
      IncomeSource("01", Some("self-employment1"), taxableIncome),
      IncomeSource("01", Some("self-employment2"), taxableIncome),
      IncomeSource("02", None, taxableIncome),
      IncomeSource("03", None, taxableIncome),
      IncomeSource("04", None, taxableIncome),
      IncomeSource("05", Some("employment1"), taxableIncome),
      IncomeSource("05", Some("employment2"), taxableIncome),
      IncomeSource("06", None, taxableIncome),
      IncomeSource("07", None, taxableIncome),
      IncomeSource("08", None, taxableIncome),
      IncomeSource("09", None, taxableIncome),
      IncomeSource("10", None, taxableIncome),
      IncomeSource("11", None, taxableIncome),
      IncomeSource("12", None, taxableIncome),
      IncomeSource("13", None, taxableIncome),
      IncomeSource("14", None, taxableIncome),
      IncomeSource("15", None, taxableIncome),
      IncomeSource("16", None, taxableIncome),
      IncomeSource("17", None, taxableIncome),
      IncomeSource("18", None, taxableIncome),
      IncomeSource("19", None, taxableIncome),
      IncomeSource("20", None, taxableIncome),
      IncomeSource("21", None, taxableIncome),
      IncomeSource("22", None, taxableIncome),
      IncomeSource("98", None, taxableIncome)
    )),
    totalEstimatedIncome = Some(taxableIncome),
  )
}
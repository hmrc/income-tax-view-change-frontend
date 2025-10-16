/*
 * Copyright 2025 HM Revenue & Customs
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

sealed trait ConfirmedOptOutViewScenarios

case object CurrentYearNYQuarterlyOrAnnualScenario extends ConfirmedOptOutViewScenarios

case object CurrentYearNYMandatedScenario extends ConfirmedOptOutViewScenarios

case object NextYearCYMandatedOrQuarterlyScenario extends ConfirmedOptOutViewScenarios

case object NextYearCYAnnualScenario extends ConfirmedOptOutViewScenarios

case object PreviousAndNoStatusValidScenario extends ConfirmedOptOutViewScenarios

case object DefaultValidScenario extends ConfirmedOptOutViewScenarios

sealed trait ConfirmedOptOutViewScenariosError

case object UnableToDetermineContent extends ConfirmedOptOutViewScenariosError

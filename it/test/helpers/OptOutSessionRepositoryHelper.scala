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

package helpers

import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus
import models.optout.OptOutSessionData
import repositories.ITSAStatusRepositorySupport.statusToString
import repositories.{OptOutContextData, UIJourneySessionDataRepository}
import testConstants.BaseIntegrationTestConstants.testSessionId
import utils.OptOutJourney

class OptOutSessionRepositoryHelper(repository: UIJourneySessionDataRepository) extends CustomMatchers {

  def stubOptOutInitialState(currentTaxYear: TaxYear,
                             previousYearCrystallised: Boolean,
                             previousYearStatus: ITSAStatus.Value,
                             currentYearStatus: ITSAStatus.Value,
                             nextYearStatus: ITSAStatus.Value): Unit = {
    repository.set(
        UIJourneySessionData(testSessionId,
          OptOutJourney.Name,
          optOutSessionData =
            Some(OptOutSessionData(
              Some(OptOutContextData(
                currentYear = currentTaxYear.toString,
                previousYearCrystallised,
                statusToString(previousYearStatus),
                statusToString(currentYearStatus),
                statusToString(nextYearStatus))), None))))
      .futureValue shouldBe true
  }

}

/*
 * Copyright 2026 HM Revenue & Customs
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

package businessDetails.testConstants

import businessDetails.enums.IncomeSourceJourney.SelfEmployment
import businessDetails.models.incomeSourceDetails.{AddIncomeSourceData, CeaseIncomeSourceData, ManageIncomeSourceData}
import common.testConstants.BaseIntegrationTestConstants.{testPropertyIncomeId, testSelfEmploymentId, testSessionId}
import shared.models.UIJourneySessionData
import shared.enums.JourneyType.IncomeSourceJourneyType

object UIJourneySessionDataTestConstants {


  lazy val completedUIJourneySessionData: IncomeSourceJourneyType => UIJourneySessionData = (incomeSources: IncomeSourceJourneyType) => {
    incomeSources.operation.operationType match {
      case "ADD" => UIJourneySessionData(testSessionId, incomeSources.toString,
        addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceCreatedJourneyComplete = Some(true))))
      case "MANAGE" => if (incomeSources.businessType == SelfEmployment) UIJourneySessionData(testSessionId, incomeSources.toString,
        manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId),
          taxYear = Some(2024), reportingMethod = Some("annual"), journeyIsComplete = Some(true))))
      else UIJourneySessionData(testSessionId, incomeSources.toString,
        manageIncomeSourceData = Some(ManageIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId),
          taxYear = Some(2024), reportingMethod = Some("annual"), journeyIsComplete = Some(true))))
      case "CEASE" => UIJourneySessionData(testSessionId, incomeSources.toString,
        ceaseIncomeSourceData = Some(CeaseIncomeSourceData(journeyIsComplete = Some(true))))
    }
  }
  
  val emptyUIJourneySessionData: IncomeSourceJourneyType => UIJourneySessionData = incomeSources => {
    incomeSources.operation.operationType match {
      case "ADD" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = incomeSources.toString,
          addIncomeSourceData = Some(AddIncomeSourceData())
        )
      case "MANAGE" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = incomeSources.toString,
          manageIncomeSourceData = Some(ManageIncomeSourceData())
        )
      case "CEASE" =>
        UIJourneySessionData(
          sessionId = testSessionId,
          journeyType = incomeSources.toString,
          ceaseIncomeSourceData = Some(CeaseIncomeSourceData())
        )
    }
  }
}

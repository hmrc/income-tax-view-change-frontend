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

package controllers.agent.optOut

import helpers.OptOutSessionRepositoryHelper
import helpers.agent.AgentComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import play.api.http.Status.OK
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class ConfirmedOptOutControllerISpec extends AgentComponentSpecBase {
  private val isAgent: Boolean = true
  private val confirmedOptOutPageUrl = controllers.optOut.routes.ConfirmedOptOutController.show(isAgent).url
  private val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  private val helper = new OptOutSessionRepositoryHelper(repository)

  private val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  s"calling GET $confirmedOptOutPageUrl" should {
    s"render $confirmedOptOutPageUrl" when {
      s"following year to opt-out year is $Mandated" when {
        "User is authorised" in {
          stubAuthorisedAgentUser(authorised = true)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          helper.stubOptOutInitialState(currentTaxYear,
            previousYearCrystallised = false,
            previousYearStatus = Voluntary,
            currentYearStatus = Mandated,
            nextYearStatus = Mandated)

          val result = IncomeTaxViewChangeFrontend.getConfirmedOptOut(clientDetailsWithConfirmation)
          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(OK),
            pageTitleAgent("optout.confirmedOptOut.heading")
          )
        }
      }
    }
  }

}

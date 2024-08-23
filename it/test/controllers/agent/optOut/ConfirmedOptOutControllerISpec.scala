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

import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus._
import models.optout.OptOutSessionData
import play.api.http.Status.OK
import repositories.ITSAStatusRepositorySupport.statusToString
import repositories.{OptOutContextData, UIJourneySessionDataRepository}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse
import utils.OptOutJourney

class ConfirmedOptOutControllerISpec extends ComponentSpecBase {
  val isAgent: Boolean = true
  val confirmedOptOutPageUrl = controllers.optOut.routes.ConfirmedOptOutController.show(isAgent).url
  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val previousYear = currentTaxYear.addYears(-1)

  val expectedTitle = s"Confirm and opt out for the ${previousYear.startYear} to ${previousYear.endYear} tax year"
  val summary = "If you opt out, you can submit your tax return through your HMRC online account or software."
  val infoMessage = s"In future, you could be required to report quarterly again if, for example, your income increases or the threshold for reporting quarterly changes. If this happens, we’ll write to you to let you know."

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe(true)
  }

  s"calling GET $confirmedOptOutPageUrl" should {
    s"render $confirmedOptOutPageUrl" when {
      s"following year to opt-out year is $Mandated" when {
        "User is authorised" in {
          stubAuthorisedAgentUser(authorised = true)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          stubOptOutInitialState(currentTaxYear,
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

  private def stubOptOutInitialState(currentTaxYear: TaxYear,
                                     previousYearCrystallised: Boolean,
                                     previousYearStatus: Value,
                                     currentYearStatus: Value,
                                     nextYearStatus: Value): Unit = {
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
  }

}

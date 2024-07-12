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

import forms.optOut.ConfirmOptOutSingleTaxYearForm
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{NoStatus, Voluntary}
import models.optout.OptOutContextData.statusToString
import models.optout.{OptOutContextData, OptOutSessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse
import utils.OptOutJourney

class SingleYearOptOutWarningControllerISpec extends ComponentSpecBase {
  val isAgent: Boolean = true
  val singleYearOptOutWarningPageGETUrl = controllers.optOut.routes.SingleYearOptOutWarningController.show(isAgent).url
  val singleYearOptOutWarningPagePOSTUrl = controllers.optOut.routes.SingleYearOptOutWarningController.submit(isAgent).url
  val validYesForm = ConfirmOptOutSingleTaxYearForm(Some(true), "")
  val validNoForm = ConfirmOptOutSingleTaxYearForm(Some(false), "")
  val inValidForm = ConfirmOptOutSingleTaxYearForm(None, "")
  val confirmOptOutPageUrl = controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url
  val nextUpdatesPageUrl = controllers.routes.NextUpdatesController.showAgent.url

  val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val previousYear = currentTaxYear.addYears(-1)

  val expectedInsetText = s"From 6 April ${previousYear.endYear}, you’ll be required to send quarterly updates again through software."
  val expectedDetailText = s"You can only opt out and report annually for the ${previousYear.startYear} to ${previousYear.endYear} tax year."
  val expectedFormTitle = s"Do you still want to opt out for the ${previousYear.startYear} to ${previousYear.endYear} tax year?"
  val expectedErrorText = s"Select yes to opt out for the ${previousYear.startYear.toString} to ${previousYear.endYear.toString} tax year"

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe(true)
  }

  s"calling GET $singleYearOptOutWarningPageGETUrl" should {
    "render single tax year opt out confirmation pager" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        stubOptOutInitialState(previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = NoStatus,
          nextYearStatus = NoStatus)

        val result = IncomeTaxViewChangeFrontend.getSingleYearOptOutWarning(clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleAgent("optOut.confirmSingleYearOptOut.title"),
          elementTextByID("detail-text")(expectedDetailText),
          elementTextByID("warning-inset")(expectedInsetText),
          elementTextBySelector(".govuk-fieldset__legend--m")(expectedFormTitle)
        )

      }
    }
  }

  s"calling POST $singleYearOptOutWarningPagePOSTUrl" should {
    s"return status $BAD_REQUEST and render single tax year opt out confirmation pager with error message - $BAD_REQUEST " when {
      "invalid data is sent" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        stubOptOutInitialState(previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = NoStatus,
          nextYearStatus = NoStatus)

        val result = IncomeTaxViewChangeFrontend.postSingleYearOptOutWarning(clientDetailsWithConfirmation)(inValidForm)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleAgent("optOut.confirmSingleYearOptOut.title"),
          elementTextByID("detail-text")(expectedDetailText),
          elementTextByID("warning-inset")(expectedInsetText),
          elementTextBySelector(".govuk-fieldset__legend--m")(expectedFormTitle),
          elementTextBySelector(".govuk-error-summary__body")(expectedErrorText)
        )

      }
    }
    s"return status $SEE_OTHER with location $confirmOptOutPageUrl" when {
      "Yes response is sent" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        stubOptOutInitialState(previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = NoStatus,
          nextYearStatus = NoStatus)

        val result = IncomeTaxViewChangeFrontend.postSingleYearOptOutWarning(clientDetailsWithConfirmation)(validYesForm)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(confirmOptOutPageUrl)
        )

      }
    }

    s"return status $SEE_OTHER with location $nextUpdatesPageUrl" when {
      "Yes response is sent" in {
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        stubOptOutInitialState(previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = NoStatus,
          nextYearStatus = NoStatus)

        val result = IncomeTaxViewChangeFrontend.postSingleYearOptOutWarning(clientDetailsWithConfirmation)(validNoForm)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(nextUpdatesPageUrl)
        )
      }
    }
  }

  private def stubOptOutInitialState(previousYearCrystallised: Boolean,
                                     previousYearStatus: ITSAStatus.Value,
                                     currentYearStatus: ITSAStatus.Value,
                                     nextYearStatus: ITSAStatus.Value): Unit = {
    repository.set(
      UIJourneySessionData(testSessionId,
        OptOutJourney.Name,
        optOutSessionData =
          Some(OptOutSessionData(
            Some(OptOutContextData(
              previousYearCrystallised,
              statusToString(previousYearStatus),
              statusToString(currentYearStatus),
              statusToString(nextYearStatus))), None))))
  }

}

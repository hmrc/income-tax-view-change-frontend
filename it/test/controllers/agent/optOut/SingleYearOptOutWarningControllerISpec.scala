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
import helpers.OptOutSessionRepositoryHelper
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class SingleYearOptOutWarningControllerISpec extends ComponentSpecBase {

  private val validYesForm = ConfirmOptOutSingleTaxYearForm(Some(true), "")
  private val validNoForm = ConfirmOptOutSingleTaxYearForm(Some(false), "")
  private val inValidForm = ConfirmOptOutSingleTaxYearForm(None, "")

  private val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)

  private val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  private val helper = new OptOutSessionRepositoryHelper(repository)

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  "SingleYearOptOutWarningController" when {

    "is Agent Journey" when {

      "calling GET - /optout/single-taxyear-warning" should {

        "render single tax year opt out confirmation pager" when {

          "Agent User is authorised" in {

            stubAuthorisedAgentUser(authorised = true)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            helper.stubOptOutInitialState(currentTaxYear,
              previousYearCrystallised = false,
              previousYearStatus = Voluntary,
              currentYearStatus = NoStatus,
              nextYearStatus = NoStatus)

            val result = IncomeTaxViewChangeFrontend.getSingleYearOptOutWarning(clientDetailsWithConfirmation)

            result should have(
              httpStatus(OK),
              pageTitleAgent("optOut.confirmSingleYearOptOut.title")
            )

          }
        }
      }

      "calling POST - /agent/optout/single-taxyear-warning" should {

        "return status BAD_REQUEST - and render single tax year opt out confirmation pager with error message - 400 " when {

          "invalid data is sent" in {

            stubAuthorisedAgentUser(authorised = true)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            helper.stubOptOutInitialState(currentTaxYear,
              previousYearCrystallised = false,
              previousYearStatus = Voluntary,
              currentYearStatus = NoStatus,
              nextYearStatus = NoStatus)

            val result = IncomeTaxViewChangeFrontend.postSingleYearOptOutWarning(clientDetailsWithConfirmation)(inValidForm)

            verifyIncomeSourceDetailsCall(testMtditid)

            result should have(
              httpStatus(BAD_REQUEST),
              pageTitleAgent("optOut.confirmSingleYearOptOut.title")
            )

          }
        }

        "redirect to ConfirmOptOutPage - /agents/optout/review-confirm-taxyear - SEE_OTHER - 303" when {

          "Yes response is sent" in {

            val isAgent: Boolean = true
            val confirmOptOutPageUrl = controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url

            stubAuthorisedAgentUser(authorised = true)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            helper.stubOptOutInitialState(currentTaxYear,
              previousYearCrystallised = false,
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

        "redirect to OptOutCancelledPage - /agents/optout/cancelled - SEE_OTHER - 303" when {

          "user answers No" in {

            val optOutCancelledAgentUrl = controllers.optOut.routes.OptOutCancelledController.showAgent().url

            stubAuthorisedAgentUser(authorised = true)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

            helper.stubOptOutInitialState(currentTaxYear,
              previousYearCrystallised = false,
              previousYearStatus = Voluntary,
              currentYearStatus = NoStatus,
              nextYearStatus = NoStatus)

            val result = IncomeTaxViewChangeFrontend.postSingleYearOptOutWarning(clientDetailsWithConfirmation)(validNoForm)

            verifyIncomeSourceDetailsCall(testMtditid)

            result should have(
              httpStatus(SEE_OTHER),
              redirectURI(optOutCancelledAgentUrl)
            )
          }
        }
      }
    }
  }


}

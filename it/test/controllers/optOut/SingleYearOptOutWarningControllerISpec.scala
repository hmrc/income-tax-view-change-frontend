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

package controllers.optOut

import controllers.ControllerISpecHelper
import enums.MTDIndividual
import forms.optOut.ConfirmOptOutSingleTaxYearForm
import helpers.OptOutSessionRepositoryHelper
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.admin.{NavBarFs, OptOutFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class SingleYearOptOutWarningControllerISpec extends ControllerISpecHelper {
  private val isAgent: Boolean = false
  private val path = "/optout/single-taxyear-warning"
  private val validYesForm = getPageForm("true")
  private val validNoForm = getPageForm("false")
  private val inValidForm = getPageForm("")
  private val confirmOptOutPageUrl = controllers.optOut.routes.ConfirmOptOutController.show(isAgent).url
  private val optOutCancelledUrl = controllers.optOut.routes.OptOutCancelledController.show().url
  private val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  private val previousYear = currentTaxYear.addYears(-1)

  private val expectedInsetText = s"If you continue, from 6 April ${previousYear.endYear}. you’ll be required to send quarterly updates again through software."
  private val expectedDetailText = s"You can only opt out for the ${previousYear.startYear} to ${previousYear.endYear} tax year."
  private val expectedFormTitle = s"Do you still want to opt out for the ${previousYear.startYear} to ${previousYear.endYear} tax year?"
  private val expectedErrorText = s"Select yes to opt out for the ${previousYear.startYear.toString} to ${previousYear.endYear.toString} tax year"

  private val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  private val helper = new OptOutSessionRepositoryHelper(repository)

  private def getPageForm(value: String): Map[String, Seq[String]] = Map(
    ConfirmOptOutSingleTaxYearForm.confirmOptOutField -> Seq(value),
    ConfirmOptOutSingleTaxYearForm.csrfToken -> Seq("")
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    repository.clearSession(testSessionId).futureValue shouldBe true
  }

  s"calling GET $path" should {
    "render multi tax year opt out confirmation page" when {
      "User is authorised" in {
        enable(OptOutFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(
          currentTaxYear,
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Annual,
          nextYearStatus = Annual
        )

        val result = buildGETMTDClient(path).futureValue
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("optOut.confirmSingleYearOptOut.title"),
          elementTextByID("detail-text")(expectedDetailText),
          elementTextByID("warning-inset")(expectedInsetText),
          elementTextBySelector(".govuk-fieldset__legend--m")(expectedFormTitle)
        )

      }
    }

    testAuthFailures(path, MTDIndividual)
  }

  s"calling POST $path" should {
    s"return status $BAD_REQUEST and render multi tax year opt out confirmation page with error message - $BAD_REQUEST " when {
      "user answers with invalid data" in {
        enable(OptOutFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear,
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Annual,
          nextYearStatus = Annual)

        val result = buildPOSTMTDPostClient(path, body = inValidForm).futureValue
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        result should have(
          httpStatus(BAD_REQUEST),
          pageTitleIndividual("optOut.confirmSingleYearOptOut.title"),
          elementTextByID("detail-text")(expectedDetailText),
          elementTextByID("warning-inset")(expectedInsetText),
          elementTextBySelector(".govuk-fieldset__legend--m")(expectedFormTitle),
          elementTextBySelector(".govuk-error-summary__body")(expectedErrorText)
        )

      }
    }
    s"redirect to ConfirmOptOutPage - $confirmOptOutPageUrl with status $SEE_OTHER" when {
      "user answers Yes" in {
        enable(OptOutFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear,
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Annual,
          nextYearStatus = Annual)

        val result = buildPOSTMTDPostClient(path, body = validYesForm).futureValue
        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(confirmOptOutPageUrl)
        )

      }
    }

    s"redirect to OptOutCancelledPage - $optOutCancelledUrl with status $SEE_OTHER" when {
      "user answers No" in {
        enable(OptOutFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        helper.stubOptOutInitialState(currentTaxYear,
          previousYearCrystallised = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Annual,
          nextYearStatus = Annual)

        val result = buildPOSTMTDPostClient(path, body = validNoForm).futureValue

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(optOutCancelledUrl)
        )
      }
    }
  }

}

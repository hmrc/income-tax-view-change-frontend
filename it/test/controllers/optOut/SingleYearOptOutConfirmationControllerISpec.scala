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

import forms.optOut.ConfirmOptOutSingleTaxYearForm
import helpers.ComponentSpecBase
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class SingleYearOptOutConfirmationControllerISpec extends ComponentSpecBase {
  val isAgent: Boolean = false
  val singleYearOptOutConfirmationPageGETUrl = controllers.optOut.routes.SingleYearOptOutConfirmationController.show(isAgent).url
  val singleYearOptOutConfirmationPagePOSTUrl = controllers.optOut.routes.SingleYearOptOutConfirmationController.submit(isAgent).url
  val validYesForm = ConfirmOptOutSingleTaxYearForm(Some(true), "")
  val validNoForm = ConfirmOptOutSingleTaxYearForm(Some(false), "")
  val inValidForm = ConfirmOptOutSingleTaxYearForm(None, "")
  val checkPointPageUrl = controllers.optOut.routes.OptOutCheckpointController.show.url
  val nextUpdatePageUrl = controllers.routes.NextUpdatesController.show().url

  val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
  val previousYear = currentTaxYear.addYears(-1)

  val expectedInsetText = s"From 6 April ${previousYear.endYear}, you’ll be required to send quarterly updates again through software."
  val expectedDetailText = s"You can only opt out and report annually for the ${previousYear.startYear} to ${previousYear.endYear} tax year."
  val expectedFormTitle = s"Do you still want to opt out for the ${previousYear.startYear} to ${previousYear.endYear} tax year?"
  val expectedErrorText = s"Select yes to opt out for the ${previousYear.startYear.toString} to ${previousYear.endYear.toString} tax year"

  s"calling GET $singleYearOptOutConfirmationPageGETUrl" should {
    "render single tax year opt out confirmation pager" when {
      "User is authorised" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())


        val result = IncomeTaxViewChangeFrontendManageBusinesses.getSingleYearOptOutConfirmation()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("optOut.confirmSingleYearOptOut.title"),
          elementTextByID("detail-text")(expectedDetailText),
          elementTextByID("warning-inset")(expectedInsetText),
          elementTextBySelector(".govuk-fieldset__legend--m")(expectedFormTitle)
        )

      }
    }
  }

  s"calling POST $singleYearOptOutConfirmationPagePOSTUrl" should {
    s"return status $BAD_REQUEST and render single tax year opt out confirmation pager with error message - $BAD_REQUEST " when {
      "invalid data is sent" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postSingleYearOptOutConfirmation()(inValidForm)

        verifyIncomeSourceDetailsCall(testMtditid)

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
    s"return status $SEE_OTHER with location $checkPointPageUrl" when {
      "Yes response is sent" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postSingleYearOptOutConfirmation()(validYesForm)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkPointPageUrl)
        )

      }
    }

    s"return status $SEE_OTHER with location $nextUpdatePageUrl" when {
      "Yes response is sent" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val result = IncomeTaxViewChangeFrontendManageBusinesses.postSingleYearOptOutConfirmation()(validNoForm)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(nextUpdatePageUrl)
        )
      }
    }
  }

}

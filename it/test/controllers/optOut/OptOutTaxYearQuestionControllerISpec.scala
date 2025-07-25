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

package controllers.optOut

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{OptOutFs, ReportingFrequencyPage}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.propertyOnlyResponse

class OptOutTaxYearQuestionControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/optout"
  }

  object optOutTaxYearQuestionMessages {
    val currentYearHeading = "Opt out of Making Tax Digital for Income Tax from the current tax year"
    val currentYearTitle = "Opt out of Making Tax Digital for Income Tax from the current tax year - Manage your Self Assessment - GOV.UK"
    val currentYearDesc1 = "This would mean you no longer need to use software compatible with Making Tax Digital for Income Tax."
    val currentYearInset = "Quarterly updates that you’ve submitted will be deleted from our records if you opt out from that tax year. You’ll need to include any income from these updates in your tax return."
    val currentYearDesc2 = "You would need to go back to the way you have filed your tax return previously for all of your current businesses and any that you add in future."
    val currentYearDesc3 = "In future, you could be required to go back to using Making Tax Digital for Income Tax. If this happens, we will write to you to let you know."
    val currentYearQuestion = "Do you want to opt out from the current tax year?"

    val nextYearHeading = "Opt out of Making Tax Digital for Income Tax from the next tax year"
    val nextYearTitle = "Opt out of Making Tax Digital for Income Tax from the next tax year - Manage your Self Assessment - GOV.UK"
    val nextYearDesc1 = "From 6 April 2023, this would mean you would no longer need to use software compatible with Making Tax Digital for Income Tax."
    val nextYearDesc2 = "You will also need to go back to the way you have filed your tax return previously for all of your current businesses and any that you add in future."
    val nextYearDesc3 = "In future, you could be required to go back to using Making Tax Digital for Income Tax. If this happens, we will write to you to let you know."
    val nextYearQuestion = "Do you want to opt out from the next tax year?"
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)

    s"a user is a $mtdUserRole" that {
      "is authenticated with valid enrolment" should {
        "render the opt out tax year question page - CY Onwards" in {
          val currentYear = "2022"
          val taxYear = TaxYear(2022, 2023)
          enable(OptOutFs)
          enable(ReportingFrequencyPage)

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.Voluntary,
            itsaStatusCY = ITSAStatus.Voluntary,
            `itsaStatusCY+1` = ITSAStatus.Voluntary
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          val result = buildGETMTDClient(s"$path?taxYear=$currentYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("opt-out-question-heading")(optOutTaxYearQuestionMessages.currentYearHeading),
            elementTextByID("opt-out-question-desc-1")(optOutTaxYearQuestionMessages.currentYearDesc1),
            elementTextByID("opt-out-question-inset")(optOutTaxYearQuestionMessages.currentYearInset),
            elementTextByID("opt-out-question-desc-2")(optOutTaxYearQuestionMessages.currentYearDesc2),
            elementTextByID("opt-out-question-desc-3")(optOutTaxYearQuestionMessages.currentYearDesc3),
            elementTextByClass("govuk-fieldset__legend--m")(optOutTaxYearQuestionMessages.currentYearQuestion),
          )
        }

        "render the opt out tax year question page - CY+1 Onwards" in {
          val nextYear = "2023"
          val taxYear = TaxYear(2022, 2023)
          enable(OptOutFs)
          enable(ReportingFrequencyPage)

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.Voluntary,
            itsaStatusCY = ITSAStatus.Voluntary,
            `itsaStatusCY+1` = ITSAStatus.Voluntary
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

          val result = buildGETMTDClient(s"$path?taxYear=$nextYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("opt-out-question-heading")(optOutTaxYearQuestionMessages.nextYearHeading),
            elementTextByID("opt-out-question-desc-1")(optOutTaxYearQuestionMessages.nextYearDesc1),
            elementTextByID("opt-out-question-desc-2")(optOutTaxYearQuestionMessages.nextYearDesc2),
            elementTextByID("opt-out-question-desc-3")(optOutTaxYearQuestionMessages.nextYearDesc3),
            elementTextByClass("govuk-fieldset__legend--m")(optOutTaxYearQuestionMessages.nextYearQuestion),
          )
        }
        testAuthFailures(path, mtdUserRole)
      }

      "submit the answer to the opt out tax year question" in {
        val currentYear = "2022"
        val taxYear = TaxYear(2022, 2023)
        enable(OptOutFs)
        enable(ReportingFrequencyPage)

        stubAuthorised(mtdUserRole)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
          taxYear = taxYear,
          `itsaStatusCY-1` = ITSAStatus.Voluntary,
          itsaStatusCY = ITSAStatus.Voluntary,
          `itsaStatusCY+1` = ITSAStatus.Voluntary
        )
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val result = buildPOSTMTDPostClient(s"$path?taxYear=$currentYear", additionalCookies, Map("opt-out-tax-year-question" -> Seq("Yes"))).futureValue

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view$path?taxYear=$currentYear")
        )
      }

      "get an error message if the user incorrectly submits to the form" in {
        val currentYear = "2022"
        val taxYear = TaxYear(2022, 2023)
        enable(OptOutFs)
        enable(ReportingFrequencyPage)

        stubAuthorised(mtdUserRole)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
          taxYear = taxYear,
          `itsaStatusCY-1` = ITSAStatus.Voluntary,
          itsaStatusCY = ITSAStatus.Voluntary,
          `itsaStatusCY+1` = ITSAStatus.Voluntary
        )
        CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val result = buildPOSTMTDPostClient(s"$path?taxYear=$currentYear", additionalCookies, Map("opt-out-tax-year-question" -> Seq(""))).futureValue

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByClass("govuk-error-summary__title")("There is a problem"),
          elementTextByClass("govuk-error-summary__body")("Select yes to opt out for the 2022 to 2023 tax year")
        )
      }
    }
  }
}

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
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled, StatusOpen}
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
    // Multi
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

    // Single Year followed by Mandated
    val singleYearFollowedByMandatedHeading = "Opt out of Making Tax Digital for Income Tax for a single tax year"
    val singleYearFollowedByMandatedDesc1 = "You can only opt out for the 2022 to 2023 tax year. This would mean you no longer need software compatible with Making Tax Digital for Income Tax for this tax year."
    val singleYearFollowedByMandatedInset = "Even if you continue, from 6 April 2023, you will be required to go back to using Making Tax Digital for Income Tax."
    val singleYearFollowedByMandatedQuestion = "Do you still want to opt out for the 2022 to 2023 tax year?"

    val singleYearFollowedByMandatedUpdatesHeading = "Opt out of Making Tax Digital for Income Tax for a single tax year"
    val singleYearFollowedByMandatedUpdatesDesc1 = "You can only opt out for the 2022 to 2023 tax year."
    val singleYearFollowedByMandatedUpdatesInset = "If you continue, from 6 April 2023, you will be required to go back to using Making Tax Digital for Income Tax."
    val singleYearFollowedByMandatedUpdatesQuestion = "Do you still want to opt out for the 2022 to 2023 tax year?"

    // Single year followed by Annual
    val singleYearFollowedByAnnualHeading = "Opt out of Making Tax Digital for Income Tax for the 2022 to 2023 tax year"
    val singleYearFollowedByAnnualDesc1 = "This would mean you no longer need to use software compatible with Making Tax Digital for Income Tax."
    val singleYearFollowedByAnnualDesc2 = "In future, you could be required to go back to using Making Tax Digital for Income Tax. If this happens, we will write to you to let you know."
    val singleYearFollowedByAnnualQuestion = "Do you want to opt out for the 2022 to 2023 tax year?"

    val singleYearFollowedByAnnualUpdatesHeading = "Opt out of Making Tax Digital for Income Tax for the 2022 to 2023 tax year"
    val singleYearFollowedByAnnualUpdatesDesc1 = "This would mean you no longer need to use software compatible with Making Tax Digital for Income Tax."
    val singleYearFollowedByAnnualUpdatesInset = "You have 1 quarterly updates submitted for this tax year. If you continue, these updates will be deleted from our records. You will need to include any income from these updates in your tax return."
    val singleYearFollowedByAnnualUpdatesDesc2 = "In future, you could be required to go back to using Making Tax Digital for Income Tax. If this happens, we will write to you to let you know."
    val singleYearFollowedByAnnualUpdatesQuestion = "Do you want to opt out for the 2022 to 2023 tax year?"

    // Next year opt out
    val nextYearOptOutAnnualHeading = "Opt out of Making Tax Digital for Income Tax from the 2023 to 2024 tax year onwards"
    val nextYearOptOutAnnualDesc1 = "In future, you could be required to use Making Tax Digital for Income Tax. If this happens, we will write to you to let you know."
    val nextYearOptOutAnnualQuestion = "Do you want to opt out from the 2023 to 2024 tax year?"

    val nextYearOptOutMandatedHeading = "Opt out of Making Tax Digital for Income Tax from the 2023 to 2024 tax year onwards"
    val nextYearOptOutMandatedDesc1 = "From 6 April 2023, this would mean you would not need to use software compatible with Making Tax Digital for Income Tax."
    val nextYearOptOutMandatedDesc2 = "In future, you could be required to go back to using Making Tax Digital for Income Tax. If this happens, we will write to you to let you know."
    val nextYearOptOutMandatedQuestion = "Do you want to opt out from the 2023 to 2024 tax year?"
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
          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, taxYear.toFinancialYearStart, taxYear.toFinancialYearEnd, allObligations)

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
          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, taxYear.toFinancialYearStart, taxYear.toFinancialYearEnd, allObligations)

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

        "render the opt out tax year question page - Single Year Followed By Mandated" in {
          val currentYear = "2022"
          val taxYear = TaxYear(2022, 2023)
          enable(OptOutFs)
          enable(ReportingFrequencyPage)

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.NoStatus,
            itsaStatusCY = ITSAStatus.Voluntary,
            `itsaStatusCY+1` = ITSAStatus.Mandated
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, taxYear.toFinancialYearStart, taxYear.toFinancialYearEnd, allObligations)

          val result = buildGETMTDClient(s"$path?taxYear=$currentYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("opt-out-question-heading")(optOutTaxYearQuestionMessages.singleYearFollowedByMandatedHeading),
            elementTextByID("opt-out-question-desc-1")(optOutTaxYearQuestionMessages.singleYearFollowedByMandatedDesc1),
            elementTextByID("opt-out-question-inset")(optOutTaxYearQuestionMessages.singleYearFollowedByMandatedInset),
            elementTextByClass("govuk-fieldset__legend--m")(optOutTaxYearQuestionMessages.singleYearFollowedByMandatedQuestion),
          )
        }

        "render the opt out tax year question page - Single Year Followed By Mandated with quarterly updates" in {
          val currentYear = "2022"
          val taxYear = TaxYear(2022, 2023)
          enable(OptOutFs)
          enable(ReportingFrequencyPage)

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.NoStatus,
            itsaStatusCY = ITSAStatus.Voluntary,
            `itsaStatusCY+1` = ITSAStatus.Mandated
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, taxYear.toFinancialYearStart, taxYear.toFinancialYearEnd, obligationWithSubmittedQuarterlyUpdates)

          val result = buildGETMTDClient(s"$path?taxYear=$currentYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("opt-out-question-heading")(optOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesHeading),
            elementTextByID("opt-out-question-desc-1")(optOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesDesc1),
            elementTextByID("opt-out-question-inset")(optOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesInset),
            elementTextByClass("govuk-fieldset__legend--m")(optOutTaxYearQuestionMessages.singleYearFollowedByMandatedUpdatesQuestion),
          )
        }

        "render the opt out tax year question page - Single Year Followed By Annual" in {
          val currentYear = "2022"
          val taxYear = TaxYear(2022, 2023)
          enable(OptOutFs)
          enable(ReportingFrequencyPage)

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.NoStatus,
            itsaStatusCY = ITSAStatus.Voluntary,
            `itsaStatusCY+1` = ITSAStatus.Annual
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, taxYear.toFinancialYearStart, taxYear.toFinancialYearEnd, allObligations)

          val result = buildGETMTDClient(s"$path?taxYear=$currentYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("opt-out-question-heading")(optOutTaxYearQuestionMessages.singleYearFollowedByAnnualHeading),
            elementTextByID("opt-out-question-desc-1")(optOutTaxYearQuestionMessages.singleYearFollowedByAnnualDesc1),
            elementTextByID("opt-out-question-desc-2")(optOutTaxYearQuestionMessages.singleYearFollowedByAnnualDesc2),
            elementTextByClass("govuk-fieldset__legend--m")(optOutTaxYearQuestionMessages.singleYearFollowedByAnnualQuestion),
          )
        }

        "render the opt out tax year question page - Single Year Followed By Annual with quarterly updates" in {
          val currentYear = "2022"
          val taxYear = TaxYear(2022, 2023)
          enable(OptOutFs)
          enable(ReportingFrequencyPage)

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.NoStatus,
            itsaStatusCY = ITSAStatus.Voluntary,
            `itsaStatusCY+1` = ITSAStatus.Annual
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, taxYear.toFinancialYearStart, taxYear.toFinancialYearEnd, obligationWithSubmittedQuarterlyUpdates)

          val result = buildGETMTDClient(s"$path?taxYear=$currentYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("opt-out-question-heading")(optOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesHeading),
            elementTextByID("opt-out-question-desc-1")(optOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesDesc1),
            elementTextByID("opt-out-question-inset")(optOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesInset),
            elementTextByID("opt-out-question-desc-2")(optOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesDesc2),
            elementTextByClass("govuk-fieldset__legend--m")(optOutTaxYearQuestionMessages.singleYearFollowedByAnnualUpdatesQuestion),
          )
        }

        "render the opt out tax year question page - Next Year Opt Out with CY Annual" in {
          val nextYear = "2023"
          val taxYear = TaxYear(2022, 2023)
          enable(OptOutFs)
          enable(ReportingFrequencyPage)

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.NoStatus,
            itsaStatusCY = ITSAStatus.Annual,
            `itsaStatusCY+1` = ITSAStatus.Voluntary
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, taxYear.toFinancialYearStart, taxYear.toFinancialYearEnd, allObligations)

          val result = buildGETMTDClient(s"$path?taxYear=$nextYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("opt-out-question-heading")(optOutTaxYearQuestionMessages.nextYearOptOutAnnualHeading),
            elementTextByID("opt-out-question-desc-1")(optOutTaxYearQuestionMessages.nextYearOptOutAnnualDesc1),
            elementTextByClass("govuk-fieldset__legend--m")(optOutTaxYearQuestionMessages.nextYearOptOutAnnualQuestion),
          )
        }

        "render the opt out tax year question page - Next Year Opt Out with CY Mandated" in {
          val nextYear = "2023"
          val taxYear = TaxYear(2022, 2023)
          enable(OptOutFs)
          enable(ReportingFrequencyPage)

          stubAuthorised(mtdUserRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
            taxYear = taxYear,
            `itsaStatusCY-1` = ITSAStatus.NoStatus,
            itsaStatusCY = ITSAStatus.Mandated,
            `itsaStatusCY+1` = ITSAStatus.Voluntary
          )
          CalculationListStub.stubGetLegacyCalculationList(testNino, taxYear.startYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())
          IncomeTaxViewChangeStub.stubGetAllObligations(testNino, taxYear.toFinancialYearStart, taxYear.toFinancialYearEnd, allObligations)

          val result = buildGETMTDClient(s"$path?taxYear=$nextYear", additionalCookies).futureValue

          result should have(
            httpStatus(OK),
            elementTextByID("opt-out-question-heading")(optOutTaxYearQuestionMessages.nextYearOptOutMandatedHeading),
            elementTextByID("opt-out-question-desc-1")(optOutTaxYearQuestionMessages.nextYearOptOutMandatedDesc1),
            elementTextByID("opt-out-question-desc-2")(optOutTaxYearQuestionMessages.nextYearOptOutMandatedDesc2),
            elementTextByClass("govuk-fieldset__legend--m")(optOutTaxYearQuestionMessages.nextYearOptOutMandatedQuestion),
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

  val allObligations: ObligationsModel = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "1234",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#003",
          StatusOpen
        ))
    ),
    GroupedObligationsModel(
      identification = "1234",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#004",
          StatusOpen
        ))
    )
  ))

  val obligationWithSubmittedQuarterlyUpdates = ObligationsModel(Seq(
    GroupedObligationsModel(
      identification = "1234",
      obligations = List(
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(3),
          end = getCurrentTaxYearEnd,
          due = getCurrentTaxYearEnd,
          obligationType = "Quarterly",
          dateReceived = Some(getCurrentTaxYearEnd),
          periodKey = "#003",
          StatusFulfilled
        ),
        SingleObligationModel(
          start = getCurrentTaxYearEnd.minusMonths(6),
          end = getCurrentTaxYearEnd.minusMonths(3),
          due = getCurrentTaxYearEnd.minusMonths(3),
          obligationType = "1234",
          dateReceived = Some(getCurrentTaxYearEnd.minusMonths(3)),
          periodKey = "#002",
          StatusFulfilled
        )
      )
    )
  ))
}

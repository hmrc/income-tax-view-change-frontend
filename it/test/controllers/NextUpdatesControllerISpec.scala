/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import audit.models.NextUpdatesAuditing.NextUpdatesAuditModel
import auth.MtdItUser
import enums.MTDIndividual
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks._
import models.admin.{OptInOptOutContentUpdateR17, OptOutFs, ReportingFrequencyPage}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.obligations.ObligationsModel
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NextUpdatesIntegrationTestConstants._

class NextUpdatesControllerISpec extends ControllerISpecHelper {

  val path = "/next-updates"

  s"GET $path" when {
    val mtdUserRole = MTDIndividual
    val testPropertyOnlyUser: MtdItUser[_] = getTestUser(MTDIndividual, ukPropertyOnlyResponse)

    val testBusinessOnlyUser: MtdItUser[_] = getTestUser(MTDIndividual, businessOnlyResponse)

    val ceasedBusinessUser: MtdItUser[_] = getTestUser(MTDIndividual, foreignAndSoleTraderCeasedBusiness)

    testAuthFailures(path, mtdUserRole)

    "renderViewNextUpdates" when {
      "the user has no obligations" in {
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val res = buildGETMTDClient(path).futureValue

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        Then("the view displays the correct title, username and links")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("obligations.heading")
        )

        Then("the page displays no obligation dates")
        res should have(
          elementTextBySelector("p.govuk-body")(messagesAPI("obligations.noReports"))
        )
      }

      "the user has a quarterly property income obligation only" in {
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title, username and links")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

        Then("the page displays the property obligation dates")
        res should have(
          elementTextBySelector("#accordion-with-summary-sections-summary-1")(expectedValue = "Quarterly update"),
          elementTextBySelector("#accordion-with-summary-sections-heading-1")(expectedValue = "1 January 2018"),
        )

      }

      "the user has a quarterly business income obligation only" in {
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testSelfEmploymentId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testBusinessOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)

        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title, username and links")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

        Then("the page displays the property obligation dates")
        res should have(
          elementTextBySelector("#accordion-with-summary-sections-summary-1")(expectedValue = "Quarterly update"),
          elementTextBySelector("#accordion-with-summary-sections-heading-1")(expectedValue = "1 January 2018"),
        )

      }

      "the user has multiple quarterly business income obligations only" in {
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testSelfEmploymentId),
          singleObligationQuarterlyModel(otherTestSelfEmploymentId))))

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testBusinessOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title, username and links")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

        Then("the page displays all the business obligation dates")
        res should have(
          elementTextBySelector("#accordion-with-summary-sections-summary-1")(expectedValue = "Quarterly update"),
          elementTextBySelector("#accordion-with-summary-sections-heading-1")(expectedValue = "1 January 2018"),
        )

      }

      "the user has a Crystallised obligation only" in {
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(noObligationsModel(testSelfEmploymentId), crystallisedEOPSModel)))
        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testBusinessOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

      }

      "the user has a Opt Out Feature Switch Enabled" in {
        enable(OptOutFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(taxYear = dateService.getCurrentTaxYear)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title, username and links")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

        Then("the page displays the property obligation dates")
        res should have(
          elementTextBySelector("#accordion-with-summary-sections-summary-1")(expectedValue = "Quarterly update"),
          elementTextBySelector("#accordion-with-summary-sections-heading-1")(expectedValue = "1 January 2018"),
        )

        Then("the quarterly updates info sections")
        res should have(
          elementTextBySelector("#accordion-with-summary-sections-summary-1")(expectedValue = "Quarterly update"),
          elementTextBySelector("#accordion-with-summary-sections-heading-1")(expectedValue = "1 January 2018"),
          elementTextBySelector("#updates-software-heading")(expectedValue = "Submitting updates in software"),
          elementTextBySelector("#updates-software-link")
          (expectedValue = "Use your compatible record keeping software (opens in new tab) " +
            "to keep digital records of all your business income and expenses. You must submit these " +
            "updates through your software by each date shown."),
        )

      }

      "the user has a Opt Out R17 Feature Switch Enabled" in {
        enable(OptOutFs)
        enable(ReportingFrequencyPage)
        enable(OptInOptOutContentUpdateR17)

        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(upcomingAndMissedObligationModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(taxYear = dateService.getCurrentTaxYear)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title, username and links")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

        Then("the page displays the missed deadlines table")
        res should have(
          elementTextByID("missed-deadlines-warning")(expectedValue = "! Warning You have missed deadlines for one or more quarterly updates."),
          elementTextByID("missed-deadlines-table-heading")(expectedValue = "Missed deadlines"),
          elementTextByID("table-head-name-deadline-missed")(expectedValue = "Deadline"),
          elementTextByID("table-head-name-period-missed")(expectedValue = "Period"),
          elementTextByID("table-head-name-updates-due-missed")(expectedValue = "Income source updates due"),
          elementTextByID("quarterly-deadline-date-missed-0")(expectedValue = "31 Oct 2017"),
          elementTextByID("quarterly-period-missed-0")(expectedValue = "6 Apr 2017 to 5 Jul 2017"),
          elementTextByID("quarterly-income-sources-missed-0")(expectedValue = "Property business"),
        )

        Then("the page displays the upcoming deadlines table")
        res should have(
          elementTextByID("active-quarterly-table-heading")(expectedValue = "Upcoming deadlines"),
          elementTextByID("table-head-name-deadline")(expectedValue = "Deadline"),
          elementTextByID("table-head-name-period")(expectedValue = "Period"),
          elementTextByID("table-head-name-updates-due")(expectedValue = "Income source updates due"),
          elementTextByID("quarterly-deadline-date-upcoming-0")(expectedValue = "31 Aug 2026"),
          elementTextByID("quarterly-period-upcoming-0")(expectedValue = "6 Apr 2026 to 5 Jul 2026"),
          elementTextByID("quarterly-income-sources-upcoming-0")(expectedValue = "Property business"),
        )

        Then("the quarterly updates info sections")
        res should have(
          elementTextByID("active-quarterly-desc")(expectedValue = "This page shows your upcoming due dates and any missed deadlines."),
          elementTextByID("active-quarterly-subheading")(expectedValue = "Quarterly updates due"),
          elementTextByID("active-quarterly-subdesc")(expectedValue = "Every 3 months an update is due for each of your property and sole trader income sources."),
          elementTextByClass("govuk-details__summary-text")(expectedValue = "Find out more about quarterly updates"),
          elementTextByID("active-quarterly-dropdown-desc")(expectedValue = "Each quarterly update is a running total of income and expenses for the tax year so far. It combines:"),
          elementTextByClass("govuk-list govuk-list--bullet")(expectedValue = "new information and corrections made since the last update any information you’ve already provided that has not changed"),
          elementTextByID("active-quarterly-dropdown-desc2")(expectedValue = "This is done using software compatible with Making Tax Digital for Income Tax (opens in new tab).")
        )
      }

      "the user has a Opt Out R17 Feature Switch Enabled - All ceased businesses" in {
        enable(OptOutFs)
        enable(ReportingFrequencyPage)
        enable(OptInOptOutContentUpdateR17)

        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignAndSoleTraderCeasedBusiness)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(taxYear = dateService.getCurrentTaxYear)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(ceasedBusinessUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title, username and links")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

        Then("the annual info sections")
        res should have(
          elementTextByID("current-year-desc")(expectedValue = "This page shows your upcoming due dates and any missed deadlines."),
          elementTextByID("current-year-subheading")(expectedValue = "Tax return due"),
          elementTextByID("current-year-compatible-software-desc")(expectedValue = "As you are opted out of Making Tax Digital for Income Tax, you can find out here how you file your Self Assessment tax return (opens in new tab)."),
          elementTextByID("current-year-return-due-date")(expectedValue = "Your return for the 2022 to 2023 tax year is due by 31 January 2024.")
        )
      }

      "the user has a Opt Out Feature Switch Disabled" in {
        disable(OptOutFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title, username and links")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

        Then("the page displays the property obligation dates")
        res should have(
          elementTextBySelector("#accordion-with-summary-sections-summary-1")(expectedValue = "Quarterly update"),
          elementTextBySelector("#accordion-with-summary-sections-heading-1")(expectedValue = "1 January 2018"),
          isElementVisibleById("#updates-software-heading")(expectedValue = false),
          isElementVisibleById("#updates-software-link")(expectedValue = false),
        )
      }
    }

    "one year opt-out scenarios" when {

      "show reporting frequency message if reporting frequency FS is enabled" in {

        enable(OptOutFs)
        enable(ReportingFrequencyPage)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the quarterly updates info sections")
        res should have(
          elementTextBySelector("#what-the-user-can-do")(expectedValue = "Depending on your circumstances, you may be able to view and change your reporting obligations."),
          elementTextBySelector("#reporting-frequency-link")("you may be able to view and change your reporting obligations.")
        )

      }

      "show one year opt out message if reporting frequency FS is disabled" in {

        enable(OptOutFs)
        disable(ReportingFrequencyPage)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Annual, ITSAStatus.Annual)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the quarterly updates info sections")
        res should have(
          elementTextBySelector("#what-the-user-can-do")(expectedValue = "You are currently reporting quarterly on a voluntary basis for the 2021 to 2022 tax year. You can choose to opt out of quarterly updates and report annually instead."),
          elementTextBySelector("#confirm-opt-out-link")("opt out of quarterly updates and report annually instead.")
        )

      }

      "show one year opt out message if reporting frequency FS is disabled and its following mandated" in {

        enable(OptOutFs)
        disable(ReportingFrequencyPage)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Mandated, ITSAStatus.Mandated)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the quarterly updates info sections")
        res should have(
          elementTextBySelector("#what-the-user-can-do")(expectedValue = "You are currently reporting quarterly on a voluntary basis for the 2021 to 2022 tax year. You can choose to opt out of quarterly updates and report annually instead."),
          elementTextBySelector("#single-year-opt-out-warning-link")("opt out of quarterly updates and report annually instead.")
        )
      }
    }

    "multi year opt-out scenario" when {
      "show one year opt out message if reporting frequency FS is disabled" in {

        enable(OptOutFs)
        disable(ReportingFrequencyPage)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the quarterly updates info sections")
        res should have(
          elementTextBySelector("#what-the-user-can-do")(expectedValue = "You are currently reporting quarterly on a voluntary basis. You can choose to opt out of quarterly updates and report annually instead."),
          elementTextBySelector("#opt-out-link")("opt out of quarterly updates and report annually instead.")
        )
      }
    }

    "show internal server error page" when {
      "Opt Out feature switch is enabled" when {
        "ITSA Status API Failure" in {
          enable(OptOutFs)
          MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

          val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
          val previousYear = currentTaxYear.addYears(-1)


          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

          IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
          ITSAStatusDetailsStub.stubGetITSAStatusDetailsError(previousYear.formatAsShortYearRange, futureYears = true)
          CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())

          val res = buildGETMTDClient(path).futureValue

          AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

          IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
          IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
          IncomeTaxViewChangeStub.verifyGetObligations(testNino)

          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }

        "Calculation API Failure" in {
          enable(OptOutFs)
          MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

          val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
          val previousYear = currentTaxYear.addYears(-1)


          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

          IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
          ITSAStatusDetailsStub.stubGetITSAStatusDetails(previousYear.formatAsShortYearRange)
          CalculationListStub.stubGetLegacyCalculationListError(testNino, previousYear.endYear.toString)


          val res = buildGETMTDClient(path).futureValue

          AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

          IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
          IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
          IncomeTaxViewChangeStub.verifyGetObligations(testNino)

          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }

        "ITSA Status API Failure and Calculation API Failure" in {
          enable(OptOutFs)
          MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

          val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
          val previousYear = currentTaxYear.addYears(-1)


          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

          IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
          ITSAStatusDetailsStub.stubGetITSAStatusDetailsError(previousYear.formatAsShortYearRange)
          CalculationListStub.stubGetLegacyCalculationListError(testNino, previousYear.endYear.toString)


          val res = buildGETMTDClient(path).futureValue

          AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

          IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
          IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
          IncomeTaxViewChangeStub.verifyGetObligations(testNino)

          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }
}

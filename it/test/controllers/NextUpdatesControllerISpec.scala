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
import models.admin.OptOutFs
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

      "show opt-out message if the user has Previous Year as Voluntary, Current Year as NoStatus, Next Year as NoStatus" in {
        enable(OptOutFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the quarterly updates info sections")
        res should have(
          elementTextBySelector("#what-the-user-can-do")(expectedValue = "Depending on your circumstances, you may be able to view and change your reporting obligations.")
        )

      }

      "show multi year opt-out message if the user has Previous Year as Voluntary, Current Year as Voluntary, Next Year as Voluntary" in {
        enable(OptOutFs)
        MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        val threeYearStatus = ITSAYearStatus(ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetailsWithGivenThreeStatus(dateService.getCurrentTaxYearEnd, threeYearStatus)
        CalculationListStub.stubGetLegacyCalculationList(testNino,
          previousYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
        IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the quarterly updates info sections")
        res should have(
          elementTextBySelector("#what-the-user-can-do")(expectedValue = "Depending on your circumstances, you may be able to view and change your reporting obligations.")
        )
      }
    }

    "show Next updates page" when {
      "Opt Out feature switch is enabled" when {
        "ITSA Status API Failure" in {
          enable(OptOutFs)
          MTDIndividualAuthStub.stubAuthorisedAndMTDEnrolled()

          val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
          val previousYear = currentTaxYear.addYears(-1)


          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

          IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
          ITSAStatusDetailsStub.stubGetITSAStatusDetailsError(previousYear.formatAsShortYearRange)
          CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


          val res = buildGETMTDClient(path).futureValue

          AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

          IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
          IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)
          IncomeTaxViewChangeStub.verifyGetObligations(testNino)

          Then("the view displays the correct title even if the OptOut fail")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("nextUpdates.heading")
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

          Then("the view displays the correct title even if the OptOut fail")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("nextUpdates.heading")
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

          Then("the view displays the correct title even if the OptOut fail")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("nextUpdates.heading")
          )
        }
      }
    }
  }
}

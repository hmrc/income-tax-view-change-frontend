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
import helpers.servicemocks.ITSAStatusDetailsStub.ITSAYearStatus
import helpers.servicemocks._
import models.admin.OptOut
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.obligations.ObligationsModel
import play.api.http.Status._
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NextUpdatesIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class NextUpdatesControllerISpec extends ControllerISpecHelper {

  val path = "/next-updates"

  s"GET $path" when {

    val testPropertyOnlyUser: MtdItUser[_] = MtdItUser(
      testMtditid, testNino, None, ukPropertyOnlyResponse,
      None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
    )(FakeRequest())

    val testBusinessOnlyUser: MtdItUser[_] = MtdItUser(
      testMtditid, testNino, None, businessOnlyResponse,
      None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
    )(FakeRequest())

    testAuthFailuresForMTDIndividual(path)

    "renderViewNextUpdates" when {

      "the user has no obligations" in {
        MTDIndividualAuthStub.stubAuthorised()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val res = buildGETMTDClient(path).futureValue

        verifyIncomeSourceDetailsCall(testMtditid)

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
        MTDIndividualAuthStub.stubAuthorised()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyNextUpdatesCall(testNino)
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
        MTDIndividualAuthStub.stubAuthorised()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testSelfEmploymentId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testBusinessOnlyUser))

        verifyIncomeSourceDetailsCall(testMtditid)

        verifyNextUpdatesCall(testNino)

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
        MTDIndividualAuthStub.stubAuthorised()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testSelfEmploymentId),
          singleObligationQuarterlyModel(otherTestSelfEmploymentId))))

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testBusinessOnlyUser))

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyNextUpdatesCall(testNino)
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
        MTDIndividualAuthStub.stubAuthorised()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(noObligationsModel(testSelfEmploymentId), crystallisedEOPSModel)))
        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testBusinessOnlyUser))

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyNextUpdatesCall(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

      }

      "the user has a Opt Out Feature Switch Enabled" in {
        
        enable(OptOut)
        MTDIndividualAuthStub.stubAuthorised()

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(dateService.getCurrentTaxYearEnd)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyNextUpdatesCall(testNino)
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
        disable(OptOut)
        MTDIndividualAuthStub.stubAuthorised()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = buildGETMTDClient(path).futureValue

        AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyNextUpdatesCall(testNino)
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
        enable(OptOut)
        MTDIndividualAuthStub.stubAuthorised()

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

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyNextUpdatesCall(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the quarterly updates info sections")
        res should have(
          elementTextBySelector("#one-year-opt-out-message")(expectedValue = "You are currently reporting quarterly on a " +
            "voluntary basis for the 2021 to 2022 tax year. You can choose to opt out of quarterly updates and " +
            "report annually instead.")
        )

      }

      "show multi year opt-out message if the user has Previous Year as Voluntary, Current Year as Voluntary, Next Year as Voluntary" in {
        enable(OptOut)
        MTDIndividualAuthStub.stubAuthorised()

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

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyNextUpdatesCall(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the quarterly updates info sections")
        res should have(
          elementTextBySelector("#multi-year-opt-out-message")(expectedValue = "You are currently reporting quarterly on a " +
            "voluntary basis. You can choose to opt out of quarterly updates and report annually instead.")
        )
      }
    }

    "show Next updates page" when {
      "Opt Out feature switch is enabled" when {
        "ITSA Status API Failure" in {
          enable(OptOut)
          MTDIndividualAuthStub.stubAuthorised()

          val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
          val previousYear = currentTaxYear.addYears(-1)


          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

          IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
          ITSAStatusDetailsStub.stubGetITSAStatusDetailsError(previousYear.formatTaxYearRange)
          CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


          val res = buildGETMTDClient(path).futureValue

          AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyNextUpdatesCall(testNino)
          IncomeTaxViewChangeStub.verifyGetObligations(testNino)

          Then("the view displays the correct title even if the OptOut fail")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("nextUpdates.heading")
          )
        }

        "Calculation API Failure" in {
          enable(OptOut)
          MTDIndividualAuthStub.stubAuthorised()

          val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
          val previousYear = currentTaxYear.addYears(-1)


          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

          IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
          ITSAStatusDetailsStub.stubGetITSAStatusDetails(previousYear.formatTaxYearRange)
          CalculationListStub.stubGetLegacyCalculationListError(testNino, previousYear.endYear.toString)


          val res = buildGETMTDClient(path).futureValue

          AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyNextUpdatesCall(testNino)
          IncomeTaxViewChangeStub.verifyGetObligations(testNino)

          Then("the view displays the correct title even if the OptOut fail")
          res should have(
            httpStatus(OK),
            pageTitleIndividual("nextUpdates.heading")
          )
        }

        "ITSA Status API Failure and Calculation API Failure" in {
          enable(OptOut)
          MTDIndividualAuthStub.stubAuthorised()

          val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
          val previousYear = currentTaxYear.addYears(-1)


          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

          IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

          IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
          ITSAStatusDetailsStub.stubGetITSAStatusDetailsError(previousYear.formatTaxYearRange)
          CalculationListStub.stubGetLegacyCalculationListError(testNino, previousYear.endYear.toString)


          val res = buildGETMTDClient(path).futureValue

          AuditStub.verifyAuditEvent(NextUpdatesAuditModel(testPropertyOnlyUser))

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyNextUpdatesCall(testNino)
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

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
import config.featureswitch.OptOut
import helpers.ComponentSpecBase
import helpers.servicemocks.{AuditStub, CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.nextUpdates.ObligationsModel
import play.api.http.Status._
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NextUpdatesIntegrationTestConstants._
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

class NextUpdatesControllerISpec extends ComponentSpecBase {

  "Calling the NextUpdatesController" when {

    val testPropertyOnlyUser: MtdItUser[_] = MtdItUser(
      testMtditid, testNino, None, ukPropertyOnlyResponse,
      None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
    )(FakeRequest())

    val testBusinessOnlyUser: MtdItUser[_] = MtdItUser(
      testMtditid, testNino, None, businessOnlyResponse,
      None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
    )(FakeRequest())

    unauthorisedTest("/next-updates")

    "renderViewNextUpdates" when {

      "the user has no obligations" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

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
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

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
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testSelfEmploymentId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

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

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testSelfEmploymentId),
          singleObligationQuarterlyModel(otherTestSelfEmploymentId))))

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

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

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(noObligationsModel(testSelfEmploymentId), crystallisedEOPSModel)))
        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

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

        val currentTaxYear = dateService.getCurrentTaxYearEnd
        val previousYear = currentTaxYear - 1

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)
        ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(dateService.getCurrentTaxYearEnd)
        CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


        val res = IncomeTaxViewChangeFrontend.getNextUpdates

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
          elementTextBySelector("#updates-software-heading")(expectedValue = "Submitting updates in software"),
          elementTextBySelector("#updates-software-link")
          (expectedValue = "Use your compatible record keeping software (opens in new tab) " +
            "to keep digital records of all your business income and expenses. You must submit these " +
            "updates through your software by each date shown."),
        )
      }

      "the user has a Opt Out Feature Switch Disabled" in {
        disable(OptOut)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testPropertyIncomeId))))

        IncomeTaxViewChangeStub.stubGetFulfilledObligationsNotFound(testNino)

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

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
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be DISABLED" in {
      testIncomeSourceDetailsCaching(resetCacheAfterFirstCall = false, 2,
        () => IncomeTaxViewChangeFrontend.getNextUpdates)
    }
  }
}

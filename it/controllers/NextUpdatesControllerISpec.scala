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

import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.nextUpdates.ObligationsModel
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NextUpdatesIntegrationTestConstants._
import testConstants.messages.{NextUpdatesMessages => obligationsMessages}

class NextUpdatesControllerISpec extends ComponentSpecBase {

  "Calling the NextUpdatesController" when {

    unauthorisedTest("/next-updates")

    "renderViewNextUpdates" when {

      "the user has a eops property income obligation only" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, propertyOnlyResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationEOPSPropertyModel)))

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

        verifyIncomeSourceDetailsCall(testMtditid)

        verifyNextUpdatesCall(testNino)

        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title, username and links")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

        Then("the page displays one eops property income obligation")
        res should have(
          elementTextBySelector("#accordion-with-summary-sections-summary-1")("Tax year: 6 April 2017 to 5 July 2018"),
          elementTextBySelector("#accordion-with-summary-sections-heading-1")("1 January 2018"),
        )
      }

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

        IncomeTaxViewChangeStub.stubGetPreviousObligationsNotFound(testNino)

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

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
          elementTextBySelector("#accordion-with-summary-sections-summary-1")(expectedValue = "Update for: 6 April 2017 to 5 July 2017"),
          elementTextBySelector("#accordion-with-summary-sections-heading-1")(expectedValue = "1 January 2018"),
        )

      }

      "the user has a quarterly business income obligation only" in {
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testSelfEmploymentId))))

        IncomeTaxViewChangeStub.stubGetPreviousObligationsNotFound(testNino)

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

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
          elementTextBySelector("#accordion-with-summary-sections-summary-1")(expectedValue = "Update for: 6 April 2017 to 5 July 2017"),
          elementTextBySelector("#accordion-with-summary-sections-heading-1")(expectedValue = "1 January 2018"),
        )

      }

      "the user has multiple quarterly business income obligations only" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(singleObligationQuarterlyModel(testSelfEmploymentId),
          singleObligationQuarterlyModel(otherTestSelfEmploymentId))))

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

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
          elementTextBySelector("#accordion-with-summary-sections-summary-1")(expectedValue = "Update for: 6 April 2017 to 5 July 2017"),
          elementTextBySelector("#accordion-with-summary-sections-heading-1")(expectedValue = "1 January 2018"),
          elementTextBySelector("#accordion-with-summary-sections-content-1 > dl div:nth-child(1)")(expectedValue = "Quarterly update business"),
          elementTextBySelector("#accordion-with-summary-sections-content-1 > dl div:nth-child(2)")(expectedValue = "Quarterly update secondBusiness"),
        )

      }

      "the user has a eops SE income obligation only" in {
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(SEIncomeSourceEOPSModel(testSelfEmploymentId))))
        IncomeTaxViewChangeStub.stubGetPreviousObligationsNotFound(testNino)

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyNextUpdatesCall(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

        Then("the page displays SE income source obligation dates")
        res should have(
          elementTextBySelector("#accordion-with-summary-sections-summary-1")(expectedValue = "Tax year: 6 April 2017 to 5 April 2018"),
          elementTextBySelector("#accordion-with-summary-sections-heading-1")(expectedValue = "31 January 2018"),
        )

      }

      "the user has a Crystallised obligation only" in {

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, ObligationsModel(Seq(noObligationsModel(testSelfEmploymentId), crystallisedEOPSModel)))
        IncomeTaxViewChangeStub.stubGetPreviousObligationsNotFound(testNino)

        val res = IncomeTaxViewChangeFrontend.getNextUpdates

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyNextUpdatesCall(testNino)
        IncomeTaxViewChangeStub.verifyGetObligations(testNino)

        Then("the view displays the correct title")
        res should have(
          httpStatus(OK),
          pageTitleIndividual("nextUpdates.heading")
        )

      }

    }
  }

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be DISABLED" in {
      testIncomeSourceDetailsCaching(false, 2,
        () => IncomeTaxViewChangeFrontend.getNextUpdates)
    }
  }
}

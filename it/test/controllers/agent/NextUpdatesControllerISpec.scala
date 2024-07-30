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

package controllers.agent

import audit.models.NextUpdatesResponseAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.admin.OptOut
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, TaxYear}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel, StatusFulfilled}
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.address
import testConstants.CalculationListIntegrationTestConstants
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.Name

import java.time.LocalDate

class NextUpdatesControllerISpec extends ComponentSpecBase with FeatureSwitching {

  lazy val fixedDate: LocalDate = LocalDate.of(2024, 6, 5)

  val incomeSourceDetails: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
      None,
      None,
      Some(getCurrentTaxYearEnd),
      None,
      address = Some(address),
      cashOrAccruals = false
    )),
    properties = Nil
  )

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, Some(Name(Some("Test"), Some("User"))), incomeSourceDetails,
    None, Some("1234567890"), None, Some(Agent), Some("1")
  )(FakeRequest())

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  "Calling the NextUpdatesController" when {
    "the user is unauthorised" in {

      stubAuthorisedAgentUser(authorised = false)

      val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithoutConfirmation)

      Then("the page redirects to clients UTR page")
      res should have(
        httpStatus(SEE_OTHER)
        //          pageTitle("What is your client’s Unique Taxpayer Reference?") //need to add this check when page title is fixed
      )
    }
    "the user does not have confirmed client" in {

      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = incomeSourceDetails
      )

      val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithoutConfirmation)

      Then("the page redirects to clients UTR page")
      res should have(
        httpStatus(SEE_OTHER)
        //          pageTitle("What is your client’s Unique Taxpayer Reference?") //need to add this check when page title is fixed
      )
    }

    "the user has obligations" in {
      stubAuthorisedAgentUser(authorised = true)

      val currentObligations: ObligationsModel = ObligationsModel(Seq(
        NextUpdatesModel(
          identification = "testId",
          obligations = List(
            NextUpdateModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
          ))
      ))

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = incomeSourceDetails
      )

      IncomeTaxViewChangeStub.stubGetNextUpdates(
        nino = testNino,
        deadlines = currentObligations
      )

      val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)

      verifyNextUpdatesCall(testNino)

      Then("the next update view displays the correct title")
      res should have(
        httpStatus(OK),
        pageTitleAgent("nextUpdates.heading")
      )

      verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
    }

    "the user has no obligations" in {
      stubAuthorisedAgentUser(authorised = true)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = incomeSourceDetails
      )

      IncomeTaxViewChangeStub.stubGetNextUpdates(
        nino = testNino,
        deadlines = ObligationsModel(Seq())
      )

      val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)

      IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)

      Then("then Internal server error is returned")
      res should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }

    "the user has obligations and the Opt Out feature switch enabled" in {
      stubAuthorisedAgentUser(authorised = true)
      enable(OptOut)
      val currentTaxYear = dateService.getCurrentTaxYearEnd
      val previousYear = currentTaxYear - 1
      val currentObligations: ObligationsModel = ObligationsModel(Seq(
        NextUpdatesModel(
          identification = "testId",
          obligations = List(
            NextUpdateModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
          ))
      ))

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = incomeSourceDetails
      )

      IncomeTaxViewChangeStub.stubGetNextUpdates(
        nino = testNino,
        deadlines = currentObligations
      )
      ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(dateService.getCurrentTaxYearEnd)
      CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


      val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)

      verifyNextUpdatesCall(testNino)

      Then("the next update view displays the correct title")
      res should have(
        httpStatus(OK),
        pageTitleAgent("nextUpdates.heading"),
        elementTextBySelector("#updates-software-heading")(expectedValue = "Submitting updates in software"),
        elementTextBySelector("#updates-software-link")
        (expectedValue = "Use your compatible record keeping software (opens in new tab) " +
          "to keep digital records of all your business income and expenses. You must submit these " +
          "updates through your software by each date shown."),
      )

      verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
    }

    "the user has obligations and the Opt Out feature switch disabled" in {
      stubAuthorisedAgentUser(authorised = true)
      disable(OptOut)

      val currentObligations: ObligationsModel = ObligationsModel(Seq(
        NextUpdatesModel(
          identification = "testId",
          obligations = List(
            NextUpdateModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
          ))
      ))

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
        status = OK,
        response = incomeSourceDetails
      )

      IncomeTaxViewChangeStub.stubGetNextUpdates(
        nino = testNino,
        deadlines = currentObligations
      )

      val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)

      verifyNextUpdatesCall(testNino)

      Then("the next update view displays the correct title")
      res should have(
        httpStatus(OK),
        pageTitleAgent("nextUpdates.heading"),
        isElementVisibleById("#updates-software-heading")(expectedValue = false),
        isElementVisibleById("#updates-software-link")(expectedValue = false),
      )

      verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
    }

    "show Next updates page" when {
      "Opt Out feature switch is enabled" when {
        "ITSA Status API Failure" in {
          stubAuthorisedAgentUser(authorised = true)
          enable(OptOut)
          val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
          val previousYear = currentTaxYear.addYears(-1)
          val currentObligations: ObligationsModel = ObligationsModel(Seq(
            NextUpdatesModel(
              identification = "testId",
              obligations = List(
                NextUpdateModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
              ))
          ))

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            status = OK,
            response = incomeSourceDetails
          )

          IncomeTaxViewChangeStub.stubGetNextUpdates(
            nino = testNino,
            deadlines = currentObligations
          )
          ITSAStatusDetailsStub.stubGetITSAStatusDetailsError(previousYear.formatTaxYearRange, futureYears = true)
          CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


          val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithConfirmation)

          verifyIncomeSourceDetailsCall(testMtditid)

          verifyNextUpdatesCall(testNino)

          Then("the next update view displays the correct title even if the OptOut fail")
          res should have(
            httpStatus(OK),
            pageTitleAgent("nextUpdates.heading")
          )
        }

        "Calculation API Failure" in {
          stubAuthorisedAgentUser(authorised = true)
          enable(OptOut)
          val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
          val previousYear = currentTaxYear.addYears(-1)
          val currentObligations: ObligationsModel = ObligationsModel(Seq(
            NextUpdatesModel(
              identification = "testId",
              obligations = List(
                NextUpdateModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
              ))
          ))

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            status = OK,
            response = incomeSourceDetails
          )

          IncomeTaxViewChangeStub.stubGetNextUpdates(
            nino = testNino,
            deadlines = currentObligations
          )
          ITSAStatusDetailsStub.stubGetITSAStatusDetails(previousYear.formatTaxYearRange)
          CalculationListStub.stubGetLegacyCalculationListError(testNino, previousYear.endYear.toString)


          val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithConfirmation)

          verifyIncomeSourceDetailsCall(testMtditid)

          verifyNextUpdatesCall(testNino)

          Then("the next update view displays the correct title even if the OptOut fail")
          res should have(
            httpStatus(OK),
            pageTitleAgent("nextUpdates.heading")
          )
        }

        "ITSA Status API Failure and Calculation API Failure" in {
          stubAuthorisedAgentUser(authorised = true)
          enable(OptOut)
          val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
          val previousYear = currentTaxYear.addYears(-1)
          val currentObligations: ObligationsModel = ObligationsModel(Seq(
            NextUpdatesModel(
              identification = "testId",
              obligations = List(
                NextUpdateModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
              ))
          ))

          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            status = OK,
            response = incomeSourceDetails
          )

          IncomeTaxViewChangeStub.stubGetNextUpdates(
            nino = testNino,
            deadlines = currentObligations
          )
          ITSAStatusDetailsStub.stubGetITSAStatusDetailsError(previousYear.formatTaxYearRange, futureYears = true)
          CalculationListStub.stubGetLegacyCalculationListError(testNino, previousYear.endYear.toString)


          val res = IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithConfirmation)

          verifyIncomeSourceDetailsCall(testMtditid)

          verifyNextUpdatesCall(testNino)

          Then("the next update view displays the correct title even if the OptOut fail")
          res should have(
            httpStatus(OK),
            pageTitleAgent("nextUpdates.heading")
          )
        }
      }
    }
  }

  "API#1171 GetBusinessDetails Caching" when {
    "caching should be DISABLED" in {
      testIncomeSourceDetailsCaching(resetCacheAfterFirstCall = false, 2,
        () => IncomeTaxViewChangeFrontend.getAgentNextUpdates(clientDetailsWithConfirmation))
    }
  }
}

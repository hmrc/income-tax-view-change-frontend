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

import audit.models.TaxDueResponseAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditEvent
import helpers.servicemocks.AuthStub.titleInternalServer
import helpers.servicemocks._
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.liabilitycalculation.viewmodels.TaxDueSummaryViewModel
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessFull
import testConstants.NewCalcDataIntegrationTestConstants._
import testConstants.messages.TaxDueSummaryMessages._

import java.time.LocalDate

class TaxDueSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), None, Some("Agent"), Some("1")
  )(FakeRequest())

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]

  val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      Some("testId"),
      Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
      Some("Test Trading Name"),
      Some(getCurrentTaxYearEnd)
    )),
    property = Some(
      PropertyDetailsModel(
        Some("testId2"),
        Some(AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1))),
        Some(getCurrentTaxYearEnd)
      )
    )
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    And("I wiremock stub a successful income source Details response with single Business and Property income")
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
      status = OK,
      response = incomeSourceDetailsSuccess
    )
  }

  "Calling the TaxDueSummaryController.showIncomeSummary(taxYear)" when {
    s"redirect ($SEE_OTHER) to ${controllers.routes.SignInController.signIn().url}" when {
      "the user is not authenticated" in {
        stubAuthorisedAgentUser(authorised = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)()

        Then(s"The user is redirected to ${controllers.routes.SignInController.signIn().url}")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
    s"return $OK with technical difficulties" when {
      "the user is authenticated but doesn't have the agent enrolment" in {
        stubAuthorisedAgentUser(authorised = true, hasAgentEnrolment = false)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)()

        Then(s"Technical difficulties are shown with status OK")
        result should have(
          httpStatus(OK),
          pageTitleAgent(titleInternalServer)
        )
      }
    }
    s"return $SEE_OTHER" when {
      "the agent does not have client details in session" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)()

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
      "the agent has client details in session but no confirmation flag" in {
        stubAuthorisedAgentUser(authorised = true)

        val result: WSResponse = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(getCurrentTaxYearEnd.getYear)(clientDetailsWithoutConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.EnterClientsUTRController.show().url)
        )
      }
    }

    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid liability calculation response, " should {
      "return the correct tax due page with a full Calculation" in {
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
          status = OK,
          body = liabilityCalculationModelSuccessFull
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/agents/calculation/2018/tax-due")
        val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(testYearInt)(clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

        verifyAuditEvent(TaxDueResponseAuditModel(testUser, TaxDueSummaryViewModel(liabilityCalculationModelSuccessFull), testYearInt))

        res should have(
          httpStatus(OK),
          pageTitleAgent("taxCal_breakdown.heading")
        )
      }
    }

    "return the correct tax due summary page with just Gift Aid Additional charges" in {

      stubAuthorisedAgentUser(authorised = true)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationGiftAid
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(testYearInt)(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleAgent("taxCal_breakdown.heading"),
        elementTextByID("additional_charges")(additionCharges)
      )
    }

    "return the correct tax due summary page with just Pension Lump Sum Additional charges" in {

      stubAuthorisedAgentUser(authorised = true)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationPensionLumpSums
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(testYearInt)(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleAgent("taxCal_breakdown.heading"),
        elementTextByID("additional_charges")(additionCharges)
      )
    }

    "return the correct tax due summary page with just Pension Savings Additional charges" in {

      stubAuthorisedAgentUser(authorised = true)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationPensionSavings
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(testYearInt)(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleAgent("taxCal_breakdown.heading"),
        elementTextByID("additional_charges")(additionCharges)
      )
    }

    "return the correct tax due summary page using minimal calculation with no Additional Charges" in {

      stubAuthorisedAgentUser(authorised = true)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationMinimal
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(testYearInt)(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleAgent("taxCal_breakdown.heading")
      )

      res shouldNot have(
        elementTextByID("additional_charges")(additionCharges)
      )
    }

    "return class2VoluntaryContributions as false when the flag is missing from the calc data" in {

      stubAuthorisedAgentUser(authorised = true)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationNonVoluntaryClass2Nic
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(testYearInt)(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleAgent("taxCal_breakdown.heading"),
        elementTextBySelector("#national-insurance-contributions-table tbody:nth-child(3) td:nth-child(1)")(nonVoluntaryClass2Nics)
      )
    }

    "return class2VoluntaryContributions as true when the flag is returned in the calc data" in {

      stubAuthorisedAgentUser(authorised = true)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationVoluntaryClass2Nic
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxCalcBreakdown(testYearInt)(clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleAgent("taxCal_breakdown.heading"),
        elementTextBySelector("#national-insurance-contributions-table tbody:nth-child(3) td:nth-child(1)")(voluntaryClass2Nics)
      )
    }
  }
}

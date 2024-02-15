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

import audit.models.TaxDueResponseAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import helpers.ComponentSpecBase
import helpers.servicemocks.AuditStub.verifyAuditEvent
import helpers.servicemocks._
import models.liabilitycalculation.viewmodels.TaxDueSummaryViewModel
import play.api.http.Status._
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessful
import testConstants.NewCalcDataIntegrationTestConstants._
import testConstants.messages.TaxDueSummaryMessages.{additionCharges, nonVoluntaryClass2Nics, postgraduatePlan, studentPlan, voluntaryClass2Nics}
import uk.gov.hmrc.auth.core.AffinityGroup.Individual


class TaxDueSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some(Individual), None
  )(FakeRequest())


  "Calling the TaxDueSummaryController.showTaxDueSummary(taxYear)" when {
    "isAuthorisedUser with an active enrolment, valid nino and tax year, valid LiabilityCalculationResponse, " should {
      "return the correct tax due summary page" in {

        Given("I stub the auth endpoint")
        AuthStub.stubAuthorised()

        And("I wiremock stub a successful TaxDue Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

        And("I stub a successful liability calculation response")
        IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
          status = OK,
          body = liabilityCalculationModelSuccessful
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
        val res = IncomeTaxViewChangeFrontend.getTaxDueSummary(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

        verifyAuditEvent(TaxDueResponseAuditModel(testUser, TaxDueSummaryViewModel(liabilityCalculationModelSuccessful), testYearInt))

        res should have(
          httpStatus(OK),
          pageTitleIndividual("taxCal_breakdown.heading"),
          elementTextByID("additional_charges")(additionCharges),
          elementTextByID("student-repayment-plan0X")(studentPlan),
          elementTextByID("graduate-repayment-plan")(postgraduatePlan),
        )
      }
    }

    "return the correct tax due summary page with just Gift Aid Additional charges" in {

      And("I wiremock stub a successful TaxDue Details response with single Business and Property income")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationGiftAid
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxDueSummary(testYear)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleIndividual("taxCal_breakdown.heading"),
        elementTextByID("additional_charges")(additionCharges)
      )
    }

    "return the correct tax due summary page with just Pension Lump Sum Additional charges" in {

      And("I wiremock stub a successful TaxDue Details response with single Business and Property income")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationPensionLumpSums
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxDueSummary(testYear)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleIndividual("taxCal_breakdown.heading"),
        elementTextByID("additional_charges")(additionCharges)
      )
    }

    "return the correct tax due summary page with just Pension Savings Additional charges" in {

      And("I wiremock stub a successful TaxDue Details response with single Business and Property income")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationPensionSavings
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxDueSummary(testYear)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleIndividual("taxCal_breakdown.heading"),
        elementTextByID("additional_charges")(additionCharges)
      )
    }

    "return the correct tax due summary page using minimal calculation with no Additional Charges" in {

      And("I wiremock stub a successful TaxDue Details response with single Business and Property income")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationMinimal
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxDueSummary(testYear)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleIndividual("taxCal_breakdown.heading")
      )

      res shouldNot have(
        elementTextByID("additional_charges")(additionCharges)
      )
    }

    "return class2VoluntaryContributions as false when the flag is missing from the calc data" in {
      And("I wiremock stub a successful TaxDue Details response with single Business and Property income")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationNonVoluntaryClass2Nic
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxDueSummary(testYear)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleIndividual("taxCal_breakdown.heading"),
        elementTextBySelector("#national-insurance-contributions-table tbody:nth-child(3) td:nth-child(1)")(nonVoluntaryClass2Nics)


      )
    }

    "return class2VoluntaryContributions as true when the flag is returned in the calc data" in {
      And("I wiremock stub a successful TaxDue Details response with single Business and Property income")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

      And("I stub a successful liability calculation response")
      IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
        status = OK,
        body = liabilityCalculationVoluntaryClass2Nic
      )

      When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
      val res = IncomeTaxViewChangeFrontend.getTaxDueSummary(testYear)

      verifyIncomeSourceDetailsCall(testMtditid)
      IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

      res should have(
        httpStatus(OK),
        pageTitleIndividual("taxCal_breakdown.heading"),
        elementTextBySelector("#national-insurance-contributions-table tbody:nth-child(3) td:nth-child(1)")(voluntaryClass2Nics)
      )
    }
  }

  unauthorisedTest("/" + testYear + "/tax-calculation")

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getTaxDueSummary(testYear))
    }
  }
}

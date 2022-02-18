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
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessFull
import testConstants.NewCalcDataIntegrationTestConstants._
import testConstants.messages.TaxDueSummaryMessages.taxDueSummaryTitle
import testConstants.messages.{TaxDueSummaryMessages => messages}


class TaxDueSummaryControllerISpec extends ComponentSpecBase with FeatureSwitching {

  val testUser: MtdItUser[_] = MtdItUser(
    testMtditid, testNino, None, multipleBusinessesAndPropertyResponse,
    None, Some("1234567890"), Some("12345-credId"), Some("Individual"), None
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
          body = liabilityCalculationModelSuccessFull
        )

        When(s"I call GET /report-quarterly/income-and-expenses/view/calculation/$testYear/tax-due")
        val res = IncomeTaxViewChangeFrontend.getTaxDueSummary(testYear)

        verifyIncomeSourceDetailsCall(testMtditid)
        IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

        verifyAuditEvent(TaxDueResponseAuditModel(testUser, TaxDueSummaryViewModel(liabilityCalculationModelSuccessFull), testYearInt))

        res should have(
          httpStatus(OK),
          pageTitleIndividual(taxDueSummaryTitle),
          elementTextBySelector("h1")(messages.taxDueSummaryHeading ++ " " + "Tax calculation"),
          elementTextByID("additional_charges")("Additional charges")
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
        pageTitleIndividual(taxDueSummaryTitle),
        elementTextBySelector("h1")(messages.taxDueSummaryHeading ++ " " + "Tax calculation"),
        elementTextByID("additional_charges")("Additional charges")
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
        pageTitleIndividual(taxDueSummaryTitle),
        elementTextBySelector("h1")(messages.taxDueSummaryHeading ++ " " + "Tax calculation"),
        elementTextByID("additional_charges")("Additional charges")
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
        pageTitleIndividual(taxDueSummaryTitle),
        elementTextBySelector("h1")(messages.taxDueSummaryHeading ++ " " + "Tax calculation"),
        elementTextByID("additional_charges")("Additional charges")
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
        pageTitleIndividual(taxDueSummaryTitle),
        elementTextBySelector("h1")(messages.taxDueSummaryHeading ++ " " + "Tax calculation")
      )

      res shouldNot have(
        elementTextByID("additional_charges")("Additional charges")
      )
    }
  }

  unauthorisedTest("/calculation/" + testYear + "/tax-due")

  "API#1171 IncomeSourceDetails Caching" when {
    "caching should be ENABLED" in {
      testIncomeSourceDetailsCaching(false, 1,
        () => IncomeTaxViewChangeFrontend.getTaxDueSummary(testYear))
    }
  }
}

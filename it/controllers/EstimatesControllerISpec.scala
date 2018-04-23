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

import assets.BaseIntegrationTestConstants._
import assets.CalcDataIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.LastTaxCalcIntegrationTestContants._
import assets.ReportDeadlinesIntegrationTestConstants.multipleReportDeadlinesDataSuccessModel
import config.FrontendAppConfig
import enums.{Crystallised, Estimate}
import helpers.servicemocks._
import helpers.{ComponentSpecBase, GenericStubMethods}
import models.calculation.LastTaxCalculation
import play.api.http.Status._

class EstimatesControllerISpec extends ComponentSpecBase with GenericStubMethods {

  lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  "Calling the EstimatesController.viewEstimatedCalculations" when {

    "Estimates Feature switch is enabled" when {

      "isAuthorisedUser with an active enrolment, and a single, valid tax estimate" should {

        "return the correct page with tax links" in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, estimateLastTaxCalcResponse)

          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)
          verifyLastTaxCalculationCall(testNino, testYear)

          Then("The view should have the correct headings and a single tax estimate link")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.CalculationController.showCalculationForYear(testYearInt).url)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, and multiple valid tax estimates" should {
        "return the correct page with tax links" in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(otherTestSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, estimateLastTaxCalcResponse)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYearPlusOne, estimateLastTaxCalcResponse2)

          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testSelfEmploymentId, otherTestSelfEmploymentId, testPropertyIncomeId)
          verifyLastTaxCalculationCall(testNino, testYear)
          verifyLastTaxCalculationCall(testNino, testYearPlusOne)

          Then("The view should have the correct headings and two tax estimate links")
          res should have(
            httpStatus(OK),
            pageTitle("Estimates"),
            elementTextByID("view-estimates")("View your current estimates:"),
            elementTextByID(s"estimates-link-$testYearPlusOne")(s"${testYearPlusOneInt - 1} to $testYearPlusOne tax year"),
            elementTextByID(s"estimates-link-$testYear")(s"${testYearInt - 1} to $testYear tax year"),
            nElementsWithClass("estimates-link")(2)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, with a crystallised calculation and a tax estimate" should {
        "return the correct estimate page" in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(otherTestSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, crystallisedLastTaxCalcResponse)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYearPlusOne, estimateLastTaxCalcResponse)

          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testSelfEmploymentId, otherTestSelfEmploymentId, testPropertyIncomeId)
          verifyLastTaxCalculationCall(testNino, testYear)
          verifyLastTaxCalculationCall(testNino, testYearPlusOne)

          Then("The view should have the correct headings and a single tax estimate link")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.CalculationController.showCalculationForYear(testYearPlusOneInt).url)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, and no tax estimates" should {
        "return the correct page with no estimates found message" in {

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, crystallisedLastTaxCalcResponse)

          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)
          verifyLastTaxCalculationCall(testNino, testYear)

          Then("The view should have the correct headings and a single tax estimate link")
          res should have(
            httpStatus(OK),
            pageTitle("Estimates"),
            elementTextByID("no-estimates")("You don't have an estimate right now. We'll show your next Income Tax estimate when you submit a report using software."),
            nElementsWithClass("estimates-link")(0)
          )
        }
      }

      unauthorisedTest("/estimates")
    }

    "Estimates Feature is disabled" should {

      "redirect to home page" in {

        appConfig.features.estimatesEnabled(false)

        And("I wiremock stub a successful Income Source Details response with 1 Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        And("I wiremock stub multiple open and received obligations response")
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
        IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

        When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
        val res = IncomeTaxViewChangeFrontend.getEstimates

        verifyIncomeSourceDetailsCall(testMtditid)
        verifyReportDeadlinesCall(testSelfEmploymentId, testPropertyIncomeId)

        Then("The view should have the correct headings and a single tax estimate link")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.home().url)
        )
      }
    }
  }
}

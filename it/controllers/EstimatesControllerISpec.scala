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
import assets.messages.EstimatesMessages._
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

    "Estimates Feature is disabled" should {

      "redirect to home page" in {

        appConfig.features.estimatesEnabled(false)

        And("I wiremock stub a successful Income Source Details response with 1 Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
        val res = IncomeTaxViewChangeFrontend.getEstimates

        Then("The view should have the correct headings and a single tax estimate link")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.home().url)
        )
      }
    }


    "Estimates Feature switch is enabled" when {
      "isAuthorisedUser with an active enrolment, and a single, valid tax estimate" should {
        "return the correct page with tax links" in {

          appConfig.features.estimatesEnabled(true)

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            OK, businessAndPropertyResponse
          )

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse =
            LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Estimate)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

          And("I wiremock stub a successful Get CalculationData response")
          val calcBreakdownResponse = calculationDataSuccessWithEoYModel
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())

          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          Then("I verify the Income Source Details has been successfully wiremocked")
          IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

          Then("I verify the Estimated Tax Liability response has been wiremocked")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

          Then("The view should have the correct headings and a single tax estimate link")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.CalculationController.showCalculationForYear(2018).url)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, and multiple valid tax estimates" should {
        "return the correct page with tax links" in {

          appConfig.features.estimatesEnabled(true)

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            OK, multipleBusinessesAndPropertyResponse
          )

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(otherTestSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse =
            LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Estimate)
          val lastTaxCalcResponse2 =
            LastTaxCalculation(testCalcId2, "2017-07-06T12:34:56.789Z", calculationDataSuccessModel.totalIncomeTaxNicYtd, Estimate)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYearPlusOne, lastTaxCalcResponse2)

          And("I wiremock stub a successful Get CalculationData response")
          val calcBreakdownResponse = calculationDataSuccessWithEoYModel
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId2, calculationDataSuccessWithEoyJson.toString())

          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          Then("I verify the Income Source Details has been successfully wiremocked")
          IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

          Then("I verify the Estimated Tax Liability response has been wiremocked")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYearPlusOne)

          Then("The view should have the correct headings and two tax estimate links")
          res should have(
            httpStatus(OK),
            pageTitle(estimatesTitle),
            elementTextByID("view-estimates")(viewEstimates),
            elementTextByID(s"estimates-link-$testYearPlusOne")(estimatesLink(testYearPlusOneInt)),
            elementTextByID(s"estimates-link-$testYear")(estimatesLink(testYearInt)),
            nElementsWithClass("estimates-link")(2)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, with a crystallised calculation and a tax estimate" should {
        "return the correct estimate page" in {

          appConfig.features.estimatesEnabled(true)

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            OK, multipleBusinessesAndPropertyResponse
          )

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(otherTestSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val crystallisedLastTaxCalcResponse =
            LastTaxCalculation(
              testCalcId,
              "2017-07-06T12:34:56.789Z",
              calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd,
              Crystallised
            )
          val lastTaxCalcResponse =
            LastTaxCalculation(
              testCalcId2,
              "2017-07-06T12:34:56.789Z",
              calculationDataSuccessModel.totalIncomeTaxNicYtd,
              Estimate
            )
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, crystallisedLastTaxCalcResponse)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYearPlusOne, lastTaxCalcResponse)

          And("I wiremock stub a successful Get CalculationData response")
          val calcBreakdownResponse = calculationDataSuccessWithEoYModel
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId2, calculationDataSuccessWithEoyJson.toString())


          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          Then("I verify the Income Source Details has been successfully wiremocked")
          IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

          Then("I verify the Estimated Tax Liability response has been wiremocked")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYearPlusOne)

          Then("The view should have the correct headings and a single tax estimate link")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.CalculationController.showCalculationForYear(2019).url)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, and no tax estimates" should {
        "return the correct page with no estimates found message" in {

          appConfig.features.estimatesEnabled(true)

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            OK, businessAndPropertyResponse
          )

          And("I wiremock stub multiple open and received obligations response")
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testSelfEmploymentId, multipleReportDeadlinesDataSuccessModel)
          IncomeTaxViewChangeStub.stubGetReportDeadlines(testPropertyIncomeId, multipleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse =
            LastTaxCalculation(
              testCalcId,
              "2017-07-06T12:34:56.789Z",
              calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd,
              Crystallised
            )
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

          And("I wiremock stub a successful Get CalculationData response")
          val calcBreakdownResponse = calculationDataSuccessWithEoYModel
          SelfAssessmentStub.stubGetCalcData(testNino, testCalcId, calculationDataSuccessWithEoyJson.toString())


          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          Then("I verify the Income Source Details has been successfully wiremocked")
          IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

          Then("I verify the Estimated Tax Liability response has been wiremocked")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

          Then("The view should have the correct headings and a single tax estimate link")
          res should have(
            httpStatus(OK),
            pageTitle(estimatesTitle),
            elementTextByID("no-estimates")(noEstimates),
            nElementsWithClass("estimates-link")(0)
          )
        }
      }

      unauthorisedTest("/estimates")
    }
  }
}

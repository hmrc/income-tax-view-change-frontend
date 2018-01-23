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

import enums.Estimate
import helpers.IntegrationTestConstants.GetReportDeadlinesData._
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{IncomeTaxViewChangeStub, SelfAssessmentStub}
import helpers.{ComponentSpecBase, GenericStubMethods}
import models.LastTaxCalculation
import play.api.http.Status._
import utils.ImplicitDateFormatter

class BtaPartialControllerISpec extends ComponentSpecBase with ImplicitDateFormatter with GenericStubMethods {

  "calling the BtaPartialController" when {

    "isAuthorisedUser with na active enrolment" which {

      "has a combination of Received business and property obligations with met = true" should {

        "display the bta partial with the correct information" in {

          isAuthorisedUser(true)

          stubUserDetails()

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse = LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Estimate)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

          getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub a single business obligation response")
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)

          And("I wiremock stub a single property obligation response")
          SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, otherReportDeadlinesDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/partial")
          val res = IncomeTaxViewChangeFrontend.getBtaPartial

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          verifyPropObsCall()

          res should have(
            httpStatus(OK)
          )

          Then("the BTA page contains the text - Quarterly reporting")
          res should have(
            isElementVisibleById("it-quarterly-reporting-heading")(expectedValue = true)
          )

          Then("the BTA page contains the text - Your latest report has been received")
          res should have(
            elementTextByID("report-due")("Your latest report has been received")
          )

          Then("the BTA page contains the following links")
          res should have(
            elementTextByID("obligations-link")("View deadlines"),
            elementTextByID("estimates-link-2018")("View details")
          )

          Then("the BTA page contains the text - Your estimated tax amount is £90,500")
          res should have(
            elementTextByID("current-estimate-2018")("Your estimated tax amount is £90,500"),
            isElementVisibleById("current-estimate-2019")(expectedValue = false)
          )
        }
      }

      "has a combination of Received business and property obligations with met = false" should {

        "display the bta partial with the correct information" in {

          isAuthorisedUser(true)

          stubUserDetails()

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse =
            LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Estimate)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

          getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub obligation responses in different tax years")
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, singleObligationPlusYearOpenModel)
          SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, singleObligationOverdueModel)

          When("I call GET /report-quarterly/income-and-expenses/view/partial")
          val res = IncomeTaxViewChangeFrontend.getBtaPartial

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          verifyPropObsCall()

          res should have(
            httpStatus(OK)
          )

          Then("the BTA page contains the text - Quarterly reporting")
          res should have(
            isElementVisibleById("it-quarterly-reporting-heading")(expectedValue = true)
          )

          Then("the BTA page contains the text - You have an overdue report")
          res should have(
            elementTextByID("report-due")("You have an overdue report")
          )

          Then("the BTA page contains the text - Your estimated tax amount is £90,500")
          res should have(
            elementTextByID("current-estimate-2018")("Your estimated tax amount is £90,500"),
            isElementVisibleById("current-estimate-2019")(expectedValue = false)
          )
        }
      }

      "has a multiple estimates with different tax years" should {

        "display the bta partial with the correct information" in {

          isAuthorisedUser(true)

          stubUserDetails()

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse =
            LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Estimate)
          val lastTaxCalcResponsePlusYear =
            LastTaxCalculation(testCalcId, "2018-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Estimate)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, "2019", lastTaxCalcResponsePlusYear)

          getBizDeets(GetBusinessDetails.otherSuccessResponse(testSelfEmploymentId))

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub obligation responses in different tax years")
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, singleObligationPlusYearOpenModel)
          SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, singleObligationOverdueModel)

          When("I call GET /report-quarterly/income-and-expenses/view/partial")
          val res = IncomeTaxViewChangeFrontend.getBtaPartial

          Then(s"Verify that the last calc has been called for $testYear")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

          Then("Verify that the last calc has been called for 2019")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, "2019")

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          verifyPropObsCall()

          res should have(
            httpStatus(OK)
          )

          Then("the BTA page should contains the text - Quarterly reporting")
          res should have(
            isElementVisibleById("it-quarterly-reporting-heading")(expectedValue = true)
          )

          Then("the BTA page should contains the text - You have an overdue report")
          res should have(
            elementTextByID("report-due")("You have an overdue report")
          )

          Then("the BTA page should contains the links for 2018 and 2019 tax years")
          res should have(
            elementTextByID("current-estimate-2018")("Your estimated tax amount for 2017 to 2018 is £90,500"),
            elementTextByID("current-estimate-2019")("Your estimated tax amount for 2018 to 2019 is £90,500")
          )
        }
      }

      "has multiple businesses where the second business has an overdue obligation" should {

        "display the bta partial with the correct information" in {

          isAuthorisedUser(true)

          stubUserDetails()

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse =
            LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Estimate)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

          getBizDeets(GetBusinessDetails.multipleSuccessResponse(testSelfEmploymentId, otherTestSelfEmploymentId))

          getPropDeets(GetPropertyDetails.successResponse())

          And("I wiremock stub a single business obligation response for each business")
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, otherTestSelfEmploymentId, singleObligationOverdueModel)

          And("I wiremock stub a single property obligation response")
          SelfAssessmentStub.stubGetPropertyReportDeadlines(testNino, otherReportDeadlinesDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/partial")
          val res = IncomeTaxViewChangeFrontend.getBtaPartial

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId, otherTestSelfEmploymentId)

          verifyPropObsCall()

          res should have(
            httpStatus(OK)
          )

          Then("the BTA page contains the text - Quarterly reporting")
          res should have(
            isElementVisibleById("it-quarterly-reporting-heading")(expectedValue = true)
          )

          Then("the BTA page contains the text - You have an overdue report")
          res should have(
            elementTextByID("report-due")("You have an overdue report")
          )

          Then("the BTA page contains the following links")
          res should have(
            elementTextByID("obligations-link")("View deadlines"),
            elementTextByID("estimates-link-2018")("View details")
          )

          Then("the BTA page contains the text - Your estimated tax amount for 2017 to 2018 is £90,500")
          res should have(
            elementTextByID("current-estimate-2018")("Your estimated tax amount for 2017 to 2018 is £90,500"),
            elementTextByID("current-estimate-2019")(
              "Your estimated tax amount for 2018 to 2019 is £90,500")
          )

        }

      }

      "receives an error from Tax Estimate but valid obligations" should {

        "display the bta partial with the correct information" in {

          isAuthorisedUser(true)

          stubUserDetails()

          And("I wiremock stub an error response from Get Last Estimated Tax Liability")
          IncomeTaxViewChangeStub.stubGetLastCalcError(testNino, testYear)

          getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub a successful Property Details response")
          SelfAssessmentStub.stubGetNoPropertyDetails(testNino)

          And("I wiremock stub a successful business obligations response")
          SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)

          When("I call GET /report-quarterly/income-and-expenses/view/partial")
          val res = IncomeTaxViewChangeFrontend.getBtaPartial

          Then(s"Verify that the last calc has been called for $testYear")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          res should have(
            httpStatus(OK)
          )

          Then("the BTA page displays the text-  Quarterly reporting")
          res should have(
            isElementVisibleById("it-quarterly-reporting-heading")(expectedValue = true)
          )

          Then("the BTA page displays the text-  Your latest report has been received")
          res should have(
            elementTextByID("report-due")("Your latest report has been received")
          )

          Then("the BTA page displays the error text for non retrieved estimated tax amount")
          res should have(
            elementTextByID("estimate-error-p1")("We can't display your estimated tax amount at the moment."),
            elementTextByID("estimate-error-p2")("Try refreshing the page in a few minutes.")
          )
        }
      }

      "receives an error from ReportDeadlines but valid last tax estimate" should {

        "display the bta partial with the correct information" in {

          isAuthorisedUser(true)

          stubUserDetails()

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          val lastTaxCalcResponse =
            LastTaxCalculation(testCalcId, "2017-07-06T12:34:56.789Z", GetCalculationData.calculationDataSuccessWithEoYModel.totalIncomeTaxNicYtd, Estimate)
          IncomeTaxViewChangeStub.stubGetLastTaxCalc(testNino, testYear, lastTaxCalcResponse)

          getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub no Property Details response")
          SelfAssessmentStub.stubGetNoPropertyDetails(testNino)

          And("I wiremock stub an error for business obligations response")
          SelfAssessmentStub.stubBusinessReportDeadlinesError(testNino, testSelfEmploymentId)

          When("I call GET /report-quarterly/income-and-expenses/view/partial")
          val res = IncomeTaxViewChangeFrontend.getBtaPartial

          Then(s"Verify that the last calc has been called for $testYear")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          res should have(
            httpStatus(OK)
          )

          Then("the text Quarterly reporting is displayed on the BTA page")
          res should have(
            isElementVisibleById("it-quarterly-reporting-heading")(expectedValue = true)
          )

          Then("the BTA page displays the error text for non retrieved next report due date")
          res should have(
            elementTextByID("obligation-error-p1")("We can't display your next report due date at the moment."),
            elementTextByID("obligation-error-p2")("Try refreshing the page in a few minutes.")
          )

          Then("the BTA page displays the text Your estimated tax amount is £90,500")
          res should have(
            elementTextByID("current-estimate-2018")("Your estimated tax amount is £90,500")
          )
        }
      }


      "receives an error for both ReportDeadlines and last tax estimate" should {

        "display the bta partial with the correct information" in {

          isAuthorisedUser(true)

          stubUserDetails()

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLastCalcError(testNino, testYear)

          getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

          And("I wiremock stub no Property Details response")
          SelfAssessmentStub.stubGetNoPropertyDetails(testNino)

          And("I wiremock stub an error for business obligations response")
          SelfAssessmentStub.stubBusinessReportDeadlinesError(testNino, testSelfEmploymentId)

          When("I call GET /report-quarterly/income-and-expenses/view/partial")
          val res = IncomeTaxViewChangeFrontend.getBtaPartial

          Then(s"Verify that the last calc has been called for $testYear")
          IncomeTaxViewChangeStub.verifyGetLastTaxCalc(testNino, testYear)

          verifyBizDeetsCall()

          verifyPropDeetsCall()

          verifyBizObsCall(testSelfEmploymentId)

          Then("the result should have a HTTP status of OK")
          res should have(
            httpStatus(OK)
          )
          Then("the text Quarterly reporting is displayed on the BTA page")
          res should have(
            isElementVisibleById("it-quarterly-reporting-heading")(expectedValue = true)
          )
          Then("the BTA page displays the error text for non retrieved next report due date and estimated tax amount")
          res should have(
            elementTextByID("obligation-error-p1")("We can't display your next report due date at the moment."),
            elementTextByID("obligation-error-p2")("Try refreshing the page in a few minutes."),
            elementTextByID("estimate-error-p1")("We can't display your estimated tax amount at the moment."),
            elementTextByID("estimate-error-p2")("Try refreshing the page in a few minutes.")
          )
        }
      }
    }

  }

}

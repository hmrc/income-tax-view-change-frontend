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
import assets.IncomeSourceIntegrationTestConstants._
import assets.CalcDataIntegrationTestConstants._
import assets.messages.{BillsMessages => messages}
import config.FrontendAppConfig
import helpers.servicemocks._
import helpers.ComponentSpecBase
import play.api.http.Status._

class BillsControllerISpec extends ComponentSpecBase {

  lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  "Calling the BillsController" when {

    "the Bill Feature is enabled" when {

      "isAuthorisedUser with an active enrolment, and a single, valid crystallised estimate" should {

        "return the correct page with bills links" in {

          And("I wiremock stub a successful Income Source Details response with 1 Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationCrystallisedResponse)

          When(s"I call GET /report-quarterly/income-and-expenses/view/bills")
          val res = IncomeTaxViewChangeFrontend.getBills

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyLatestCalculationCall(testNino, testYear)

          Then("The view should have the correct headings and a single tax bill link")
          res should have(
            httpStatus(OK),
            pageTitle(messages.billsTitle),
            elementTextByID("finalised-bills")(messages.finalBills),
            elementTextByID(s"bills-link-$testYear")(messages.taxYearText(testYearInt)),
            nElementsWithClass("bills-link")(1),
            elementTextByID("earlier-bills")(messages.earlierBills)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, and multiple valid crystallised estimates" should {

        "return the correct page with tax links when calls successful" in {

          And("I wiremock stub a successful Income Source Details response with Multiple Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationCrystallisedResponse)
          IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYearPlusOne, taxCalculationCrystallisedResponse)

          When(s"I call GET /report-quarterly/income-and-expenses/view/bills")
          val res = IncomeTaxViewChangeFrontend.getBills

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyLatestCalculationCall(testNino, testYear)
          verifyLatestCalculationCall(testNino, testYearPlusOne)

          Then("The view should have the correct headings and two tax bill links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.billsTitle),
            elementTextByID("finalised-bills")(messages.finalBills),
            elementTextByID(s"bills-link-$testYearPlusOne")(messages.taxYearText(testYearPlusOneInt)),
            elementTextByID(s"bills-link-$testYear")(messages.taxYearText(testYearInt)),
            nElementsWithClass("bills-link")(2),
            elementTextByID("earlier-bills")(messages.earlierBills)
          )
        }

        "return the correct page with tax links when one response is not found" in {

          And("I wiremock stub a successful Income Source Details response with Multiple Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationCrystallisedResponse)
          IncomeTaxViewChangeStub.stubGetLatestCalcNotFound(testNino, testYearPlusOne)

          When(s"I call GET /report-quarterly/income-and-expenses/view/bills")
          val res = IncomeTaxViewChangeFrontend.getBills

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyLatestCalculationCall(testNino, testYear)
          verifyLatestCalculationCall(testNino, testYearPlusOne)

          Then("The view should have the correct headings and two tax bill links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.billsTitle),
            elementTextByID("finalised-bills")(messages.finalBills),
            elementTextByID(s"bills-link-$testYear")(messages.taxYearText(testYearInt)),
            nElementsWithClass("bills-link")(1),
            elementTextByID("earlier-bills")(messages.earlierBills)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, with a crystallised calculation and a tax estimate" should {
        "return the correct page with just the tax bill link" in {

          And("I wiremock stub a successful Income Source Details response with Multiple Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationCrystallisedResponse)
          IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYearPlusOne, taxCalculationResponse)

          When(s"I call GET /report-quarterly/income-and-expenses/view/bills")
          val res = IncomeTaxViewChangeFrontend.getBills

          verifyIncomeSourceDetailsCall(testMtditid)
          verifyLatestCalculationCall(testNino, testYear)
          verifyLatestCalculationCall(testNino, testYearPlusOne)

          Then("The view should have the correct headings and a single tax bill link")
          res should have(
            httpStatus(OK),
            pageTitle(messages.billsTitle),
            elementTextByID("finalised-bills")(messages.finalBills),
            elementTextByID(s"bills-link-$testYear")(messages.taxYearText(testYearInt)),
            nElementsWithClass("bills-link")(1),
            elementTextByID("earlier-bills")(messages.earlierBills)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, and no tax bills" should {
        "return the correct page with no bills found message" when {
          "there are no crystallised calculations returned" in {

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

            And("I wiremock stub a successful Get Last Estimated Tax Liability response")
            IncomeTaxViewChangeStub.stubGetLatestCalculation(testNino, testYear, taxCalculationResponse)

            When(s"I call GET /report-quarterly/income-and-expenses/view/bills")
            val res = IncomeTaxViewChangeFrontend.getBills

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyLatestCalculationCall(testNino, testYear)

            Then("The view should have the correct headings and a single tax estimate link")
            res should have(
              httpStatus(OK),
              pageTitle(messages.billsTitle),
              elementTextByID("no-bills")(messages.noBills),
              nElementsWithClass("bills-link")(0)
            )
          }

          "all calculations returned with not found from the connector" in {

            And("I wiremock stub a successful Income Source Details response with single Business and Property income")
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

            And("I wiremock stub a successful Get Last Estimated Tax Liability response")
            IncomeTaxViewChangeStub.stubGetLatestCalcNotFound(testNino, testYear)

            When(s"I call GET /report-quarterly/income-and-expenses/view/bills")
            val res = IncomeTaxViewChangeFrontend.getBills

            verifyIncomeSourceDetailsCall(testMtditid)
            verifyLatestCalculationCall(testNino, testYear)

            Then("The view should have the correct headings and a single tax estimate link")
            res should have(
              httpStatus(OK),
              pageTitle(messages.billsTitle),
              elementTextByID("no-bills")(messages.noBills),
              nElementsWithClass("bills-link")(0)
            )
          }
        }
      }

      unauthorisedTest("/bills")
    }

    "the Bills Feature is disabled" should {

      "redirect to home page" in {

        When("I wiremock stub a successful Income Source Details response with single Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        appConfig.features.billsEnabled(false)

        When(s"I call GET /report-quarterly/income-and-expenses/view/bills")
        val res = IncomeTaxViewChangeFrontend.getBills

        Then("I should be redirected to the Home Page")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.home().url)
        )
      }

      unauthorisedTest("/bills")
    }
  }
}

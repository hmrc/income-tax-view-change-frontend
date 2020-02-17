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

import java.time.LocalDateTime

import assets.BaseIntegrationTestConstants._
import assets.CalcDataIntegrationTestConstants._
import assets.IncomeSourceIntegrationTestConstants._
import assets.messages.{EstimatesMessages => messages}
import config.featureswitch.{Estimates, FeatureSwitching}
import helpers.ComponentSpecBase
import helpers.servicemocks._
import models.calculation.{CalculationItem, ListCalculationItems}
import play.api.http.Status._

class EstimatesControllerISpec extends ComponentSpecBase with FeatureSwitching {

  "Calling the EstimatesController.viewEstimatedCalculations" when {

    "Estimates Feature switch is enabled" when {

      "isAuthorisedUser with an active enrolment, and a single, valid tax estimate" should {

        "return the correct page with tax links" in {

          enable(Estimates)
          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I stub a successful calculation response for 2017-18")
          IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
            status = OK,
            body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
          )
          IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
            status = OK,
            body = estimatedCalculationFullJson
          )

          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          verifyIncomeSourceDetailsCall(testMtditid)
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

          Then("The view should have the correct headings and a single tax estimate link")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.CalculationController.renderCalculationPage(testYearInt).url)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, and multiple valid tax estimates" should {
        "return the correct page with tax links" in {

          enable(Estimates)
          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

          And("I wiremock stub a successful Get Last Estimated Tax Liability response")
          And("I stub a successful calculation response for 2017-18")
          IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
            status = OK,
            body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
          )
          IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
            status = OK,
            body = estimatedCalculationFullJson
          )
          And("I stub a successful calculation response for 2018-19")
          IndividualCalculationStub.stubGetCalculationList(testNino, "2018-19")(
            status = OK,
            body = ListCalculationItems(Seq(CalculationItem("idTwo", LocalDateTime.now())))
          )
          IndividualCalculationStub.stubGetCalculation(testNino, "idTwo")(
            status = OK,
            body = estimatedCalculationFullJson
          )

          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          verifyIncomeSourceDetailsCall(testMtditid)
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2018-19")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idTwo")

          Then("The view should have the correct headings and two tax estimate links")
          res should have(
            httpStatus(OK),
            pageTitle(messages.estimatesTitle),
            elementTextByID("view-estimates")(messages.viewEstimates),
            elementTextByID(s"estimates-link-$testYearPlusOne")(messages.estimatesLink(testYearPlusOneInt)),
            elementTextByID(s"estimates-link-$testYear")(messages.estimatesLink(testYearInt)),
            nElementsWithClass("estimates-link")(2)
          )
        }

        "return the correct estimate page when one response received a not found" in {

          enable(Estimates)

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

          And("I stub a successful calculation response for 2017-18")
          IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
            status = OK,
            body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
          )
          IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
            status = OK,
            body = estimatedCalculationFullJson
          )

          And("I stub a not found calculation response for 2018-19")
          IndividualCalculationStub.stubGetCalculationList(testNino, "2018-19")(
            status = OK,
            body = ListCalculationItems(Seq(CalculationItem("idTwo", LocalDateTime.now())))
          )
          IndividualCalculationStub.stubGetCalculationNotFound(testNino, "idTwo")

          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          verifyIncomeSourceDetailsCall(testMtditid)
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2018-19")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idTwo")

          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.CalculationController.renderCalculationPage(testYearInt).url)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, with a crystallised calculation and a tax estimate" should {
        "return the correct estimate page with successful responses" in {

          enable(Estimates)

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndPropertyResponse)

          And("I stub a successful crystallised calculation response for 2017-18")
          IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
            status = OK,
            body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
          )
          IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
            status = OK,
            body = crystallisedCalculationFullJson
          )
          And("I stub a successful estimated calculation response for 2018-19")
          IndividualCalculationStub.stubGetCalculationList(testNino, "2018-19")(
            status = OK,
            body = ListCalculationItems(Seq(CalculationItem("idTwo", LocalDateTime.now())))
          )
          IndividualCalculationStub.stubGetCalculation(testNino, "idTwo")(
            status = OK,
            body = estimatedCalculationFullJson
          )

          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          verifyIncomeSourceDetailsCall(testMtditid)
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2018-19")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idTwo")

          Then("The view should have the correct headings and a single tax estimate link")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(controllers.routes.CalculationController.renderCalculationPage(testYearPlusOneInt).url)
          )
        }
      }

      "isAuthorisedUser with an active enrolment, and no tax estimates" should {
        "return the correct page with no estimates found message" in {

          enable(Estimates)

          And("I wiremock stub a successful Income Source Details response with single Business and Property income")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

          And("I stub a successful crystallised calculation response for 2017-18")
          IndividualCalculationStub.stubGetCalculationList(testNino, "2017-18")(
            status = OK,
            body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.now())))
          )
          IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
            status = OK,
            body = crystallisedCalculationFullJson
          )
          When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
          val res = IncomeTaxViewChangeFrontend.getEstimates

          verifyIncomeSourceDetailsCall(testMtditid)
          IndividualCalculationStub.verifyGetCalculationList(testNino, "2017-18")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

          Then("The view should have the correct headings and a single tax estimate link")
          res should have(
            httpStatus(OK),
            pageTitle(messages.estimatesTitle),
            elementTextByID("no-estimates")(messages.noEstimates),
            nElementsWithClass("estimates-link")(0)
          )
        }
      }

      unauthorisedTest("/estimates")
    }

    "Estimates Feature is disabled" should {

      "redirect to home page" in {

        disable(Estimates)

        And("I wiremock stub a successful Income Source Details response with 1 Business and Property income")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        When(s"I call GET /report-quarterly/income-and-expenses/view/estimates")
        val res = IncomeTaxViewChangeFrontend.getEstimates

        verifyIncomeSourceDetailsCall(testMtditid)

        Then("The view should have the correct headings and a single tax estimate link")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.HomeController.home().url)
        )
      }
    }
  }
}

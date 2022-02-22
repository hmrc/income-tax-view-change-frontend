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

import forms.utils.SessionKeys
import helpers.ComponentSpecBase
import helpers.servicemocks._
import models.liabilitycalculation.LiabilityCalculationError
import play.api.http.Status._
import repositories.MongoLockRepositoryImpl
import testConstants.BaseIntegrationTestConstants._
import testConstants.NewCalcBreakdownItTestConstants._

import scala.concurrent.ExecutionContext

class CalculationPollingControllerISpec extends ComponentSpecBase {

  val mongoDbConnection = app.injector.instanceOf[MongoLockRepositoryImpl]
  implicit val ec = app.injector.instanceOf[ExecutionContext]

  unauthorisedTest(s"/calculation/$testYear/submitted")

  s"GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt, isFinalCalc = false).url}" when {
    "the user is authorised with an active enrolment" when {
      "redirects to calculation home page" in {
        Given("Calculation service returns a successful response back")

        IncomeTaxCalculationStub.stubGetCalculationResponseByCalcId(testNino, "idOne")(
          status = OK,
          body = liabilityCalculationModelSuccessFull
        )

        When(s"I call GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt, isFinalCalc = false).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculationPoller(testYear, Map(SessionKeys.calculationId -> "idOne"))

        Then("I check all calls expected were made")
        IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idOne")

        And("The expected result is returned")
        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url)
        )

        mongoDbConnection.repo.findById("idOne").futureValue shouldBe None
      }
      "calculation service returns non-retryable response back" in {
        Given("Calculation service returns a 500 error response back")

        IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idTwo")(INTERNAL_SERVER_ERROR,
          LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error"))

        When(s"I call GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt, isFinalCalc = false).url}")

        val res = IncomeTaxViewChangeFrontend.getCalculationPoller(testYear, Map(SessionKeys.calculationId -> "idTwo"))

        Then("I check all calls expected were made")
        IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idTwo")

        And("The expected result is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
        mongoDbConnection.repo.findById("idTwo").futureValue shouldBe None
      }
      "calculation service returns retryable response back" in {
        Given("Calculation service returns a 404 error response back during total duration of timeout interval")

        IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idThree")(NOT_FOUND,
          LiabilityCalculationError(NOT_FOUND, "not found"))

        When(s"I call GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt, isFinalCalc = false).url}")
        val res = IncomeTaxViewChangeFrontend.getCalculationPoller(testYear, Map(SessionKeys.calculationId -> "idThree"))

        Then("I check all calls expected were made")
        IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idThree", 8)

        And("The expected result is returned")
        res should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
        mongoDbConnection.repo.findById("idThree").futureValue shouldBe None
      }
      "calculation service returns retryable response back initially and then returns success response before interval time completed" in {
        Given("Calculation service returns a 404 error response back during total duration of timeout interval")

        IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idFour")(NOT_FOUND,
          LiabilityCalculationError(NOT_FOUND, "not found"))

        When(s"I call GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt, isFinalCalc = false).url}")

        val res = IncomeTaxViewChangeFrontend.getCalculationPollerWithoutAwait(testYear, Map(SessionKeys.calculationId -> "idFour"))

        Thread.sleep(100)
        mongoDbConnection.repo.findById("idFour").futureValue.get.id shouldBe "idFour"

        //After 1.5 seconds responding with success message
        Thread.sleep(1500)
        IncomeTaxCalculationStub.stubGetCalculationResponseByCalcId(testNino, "idFour")(
          status = OK,
          body = liabilityCalculationModelSuccessFull
        )

        And("The expected result is returned")
        res.futureValue should have(
          httpStatus(SEE_OTHER),
          redirectURI(routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url)
        )

        Then("I check all calls expected were made")
        IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idFour", 6)

        mongoDbConnection.repo.findById("idFour").futureValue shouldBe None
      }
      "calculation service returns retryable response back initially and then returns non-retryable error before interval time completed" in {
        Given("Calculation service returns a 404 error response back during total duration of timeout interval")

        IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idFive")(NOT_FOUND, LiabilityCalculationError(NOT_FOUND, "not found"))

        When(s"I call GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt, isFinalCalc = false).url}")

        val res = IncomeTaxViewChangeFrontend.getCalculationPollerWithoutAwait(testYear, Map(SessionKeys.calculationId -> "idFive"))

        Thread.sleep(100)
        mongoDbConnection.repo.findById("idFive").futureValue.get.id shouldBe "idFive"

        //After 1.5 seconds responding with success message
        Thread.sleep(1500)
        IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idFive")(INTERNAL_SERVER_ERROR,
          LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error"))

        And("The expected result is returned")
        res.futureValue should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )

        Then("I check all calls expected were made")
        IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idFive", 6)

        mongoDbConnection.repo.findById("idFive").futureValue shouldBe None
      }
    }
  }

  s"calling GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt, isFinalCalc = true).url}" when {

    "the user is authorised with an active enrolment" should {

      "redirect the user to the final tax calculation page" which {
        lazy val result = {
          IncomeTaxCalculationStub.stubGetCalculationResponseByCalcId(testNino, "idOne")(OK, liabilityCalculationModelSuccessFull)
          IncomeTaxViewChangeFrontend.getFinalTaxCalculationPoller(testYear, Map(SessionKeys.calculationId -> "idOne"))
        }

        "has the status of SEE_OTHER (303)" in {
          result.status shouldBe SEE_OTHER
        }

        s"redirect to '${controllers.routes.FinalTaxCalculationController.show(testTaxYear).url}''" in {
          result.header("Location").head shouldBe controllers.routes.FinalTaxCalculationController.show(testTaxYear).url
        }
      }
    }
  }
}

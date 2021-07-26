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
import forms.utils.SessionKeys
import helpers.ComponentSpecBase
import helpers.servicemocks._
import play.api.http.Status._
import repositories.MongoLockRepositoryImpl

import scala.concurrent.ExecutionContext

class CalculationPollingControllerISpec extends ComponentSpecBase {

  val mongoDbConnection = app.injector.instanceOf[MongoLockRepositoryImpl]
  implicit val ec = app.injector.instanceOf[ExecutionContext]

  unauthorisedTest(s"/calculation/$testYear/submitted")

  s"GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt).url}" when {
      "the user is authorised with an active enrolment" when {
        "redirects to calculation home page" in {
          Given("Calculation service returns a successful response back")

          IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
            status = OK,
            body = estimatedCalculationFullJson
          )

          When(s"I call GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt).url}")
          val res = IncomeTaxViewChangeFrontend.getCalculationPoller(testYear,Map(SessionKeys.calculationId -> "idOne"))

          Then("I check all calls expected were made")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idOne")

          And("The expected result is returned")
          res should have(
            httpStatus(SEE_OTHER),
            redirectURI(routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url)
          )

          await(mongoDbConnection.repo.findById("idOne")) shouldBe None
        }
        "calculation service returns non-retryable response back" in {
          Given("Calculation service returns a 500 error response back")

          IndividualCalculationStub.stubGetCalculationError(testNino, "idTwo")

          When(s"I call GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt).url}")

          val res = IncomeTaxViewChangeFrontend.getCalculationPoller(testYear,Map(SessionKeys.calculationId -> "idTwo"))

          Then("I check all calls expected were made")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idTwo")

          And("The expected result is returned")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
          await(mongoDbConnection.repo.findById("idTwo")) shouldBe None
        }
        "calculation service returns retryable response back" in {
          Given("Calculation service returns a 404 error response back during total duration of timeout interval")

          IndividualCalculationStub.stubGetCalculationListNotFound(testNino,"idThree")

          When(s"I call GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt).url}")
          val res = IncomeTaxViewChangeFrontend.getCalculationPoller(testYear,Map(SessionKeys.calculationId -> "idThree"))

          Then("I check all calls expected were made")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idThree")

          And("The expected result is returned")
          res should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
          await(mongoDbConnection.repo.findById("idThree")) shouldBe None
        }
        "calculation service returns retryable response back initially and then returns success response before interval time completed" in {
          Given("Calculation service returns a 404 error response back during total duration of timeout interval")

          IndividualCalculationStub.stubGetCalculationListNotFound(testNino, "idFour")

          When(s"I call GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt).url}")

          val res = IncomeTaxViewChangeFrontend.getCalculationPollerWithoutAwait(testYear,Map(SessionKeys.calculationId -> "idFour"))

          Thread.sleep(100)
          await(mongoDbConnection.repo.findById("idFour")).get.id shouldBe "idFour"

          //After 1.5 seconds responding with success message
          Thread.sleep(1500)
          IndividualCalculationStub.stubGetCalculation(testNino, "idFour")(
            status = OK,
            body = estimatedCalculationFullJson
          )

          And("The expected result is returned")
          await(res) should have(
            httpStatus(SEE_OTHER),
            redirectURI(routes.TaxYearOverviewController.renderTaxYearOverviewPage(testYearInt).url)
          )

          Then("I check all calls expected were made")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idFour")

          await(mongoDbConnection.repo.findById("idFour")) shouldBe None
        }
        "calculation service returns retryable response back initially and then returns non-retryable error before interval time completed" in {
          Given("Calculation service returns a 404 error response back during total duration of timeout interval")

          IndividualCalculationStub.stubGetCalculationListNotFound(testNino, "idFive")

          When(s"I call GET ${controllers.routes.CalculationPollingController.calculationPoller(testYearInt).url}")

          val res = IncomeTaxViewChangeFrontend.getCalculationPollerWithoutAwait(testYear,Map(SessionKeys.calculationId -> "idFive"))

          Thread.sleep(100)
          await(mongoDbConnection.repo.findById("idFive")).get.id shouldBe "idFive"

          //After 1.5 seconds responding with success message
          Thread.sleep(1500)
          IndividualCalculationStub.stubGetCalculationError(testNino, "idFive")

          And("The expected result is returned")
          await(res) should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )

          Then("I check all calls expected were made")
          IndividualCalculationStub.verifyGetCalculation(testNino, "idFive")

          await(mongoDbConnection.repo.findById("idFive")) shouldBe None
        }
    }
  }

}

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

import config.FrontendAppConfig
import helpers.IntegrationTestConstants.GetReportDeadlinesData.singleReportDeadlinesDataSuccessModel
import helpers.servicemocks.{AuthStub, IncomeTaxViewChangeStub, SelfAssessmentStub}
import helpers.{ComponentSpecBase, GenericStubMethods, IntegrationTestConstants}
import models.{Nino, NinoResponseError}
import play.api.http.Status._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Environment, Mode}
import utils.ImplicitDateFormatter

class HomeControllerISpec extends ComponentSpecBase with GenericStubMethods with ImplicitDateFormatter {

  lazy val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config ++ Map("features.homePageEnabled" -> true))
    .build

  "Navigating to /report-quarterly/income-and-expenses/view" when {

    "Authorised, should show the Income Tax Home page" in {

      isAuthorisedUser(true)
      stubUserDetails()
      stubPartial()

      When("I call GET /report-quarterly/income-and-expenses/view")
      val res = IncomeTaxViewChangeFrontend.getHome

      Then("the result should have a HTTP status of OK (200) and the Income Tax home page")
      res should have(

        //Check Status OK (200) Result
        httpStatus(OK),

        //Check Redirect Location
        pageTitle("Your Income Tax")
      )
    }

    "unauthorised" should {

      "redirect to sign in" in {

        isAuthorisedUser(false)

        When("I call GET /report-quarterly/income-and-expenses/view")
        val res = IncomeTaxViewChangeFrontend.getHome

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI(controllers.routes.SignInController.signIn().url)
        )
      }
    }
  }

  "Navigating to /report-quarterly/income-and-expenses/view/obligations" when {
    "a user is without a HMRC-NI enrolment" should {
      "redirect to the report deadlines page" in {

        val f = IntegrationTestConstants
        import f._

        Given("I wiremock stub an authorised with no Nino user response")
        AuthStub.stubAuthorisedNoNino()

        IncomeTaxViewChangeStub.stubGetNinoResponse(testMtditid, Nino(testNino))

        stubUserDetails()

        stubPartial()

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        And("I wiremock stub a single business obligation response")
        SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)

        When("I call GET /report-quarterly/income-and-expenses/view/obligations")
        val res = IncomeTaxViewChangeFrontend.getReportDeadlines

        Then("Verify NINO lookup has been called")
        IncomeTaxViewChangeStub.verifyGetNino(testMtditid)

        res should have(
          httpStatus(SEE_OTHER),
          redirectURI("/report-quarterly/income-and-expenses/view/obligations")
        )
      }

      "be displayed a technical error page" in {
        val f = IntegrationTestConstants
        import f._

        Given("I wiremock stub an authorised with no Nino user response")
        AuthStub.stubAuthorisedNoNino()

        IncomeTaxViewChangeStub.stubGetNinoError(testMtditid, NinoResponseError(INTERNAL_SERVER_ERROR, "Error Message"))

        stubUserDetails()

        stubPartial()

        getBizDeets(GetBusinessDetails.successResponse(testSelfEmploymentId))

        getPropDeets(GetPropertyDetails.successResponse())

        And("I wiremock stub a single business obligation response")
        SelfAssessmentStub.stubGetBusinessReportDeadlines(testNino, testSelfEmploymentId, singleReportDeadlinesDataSuccessModel)

        When("I call GET /report-quarterly/income-and-expenses/view/obligations")
        val res = IncomeTaxViewChangeFrontend.getReportDeadlines

        Then("Verify NINO lookup has been called")
        IncomeTaxViewChangeStub.verifyGetNino(testMtditid)

        res should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitle("Sorry, we are experiencing technical difficulties - 500")
        )
      }
    }
  }
}
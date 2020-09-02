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
  import assets.PreviousObligationsIntegrationTestConstants._
  import config.featureswitch.{FeatureSwitching, ObligationsPage}
  import helpers.ComponentSpecBase
  import helpers.servicemocks.IncomeTaxViewChangeStub
  import implicits.ImplicitDateFormatter
  import models.reportDeadlines.ObligationsModel
  import play.api.http.Status._
  import play.api.libs.ws.WSResponse

class PreviousObligationsControllerISpec extends ComponentSpecBase with ImplicitDateFormatter with FeatureSwitching {


  s"${controllers.routes.PreviousObligationsController.getPreviousObligations().url}" should {
    "display no previous obligations when there are none" in {
      enable(ObligationsPage)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

      IncomeTaxViewChangeStub.stubGetPreviousObligationsNotFound(testNino)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPreviousObligations

      result should have(
        httpStatus(OK),
        pageTitle("Updates - Business Tax account - GOV.UK"),
        elementTextByID("no-previous-obligations")("No previously submitted updates"),
        isElementVisibleById("income-source-1")(expectedValue = false),
        isElementVisibleById("obligation-type-1")(expectedValue = false),
        isElementVisibleById("date-from-to-1")(expectedValue = false),
        isElementVisibleById("was-due-on-1")(expectedValue = false),
        isElementVisibleById("submitted-on-date-1")(expectedValue = false)
      )
    }

    "display the previous obligations returned from the backend" in {
      enable(ObligationsPage)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

      IncomeTaxViewChangeStub.stubGetPreviousObligations(testNino, ObligationsModel(Seq(
        previousQuarterlyObligation(testSelfEmploymentId),
        previousEOPSObligation(testPropertyIncomeId),
        previousCrystallisationObligation
      )))

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPreviousObligations

      IncomeTaxViewChangeStub.verifyGetPreviousObligations(testNino)

      result should have(
        httpStatus(OK),
        pageTitle("Updates - Business Tax account - GOV.UK"),
        isElementVisibleById("no-previous-obligations")(expectedValue = false),
        elementTextByID("income-source-0")("Tax year - Final check"),
        elementTextByID("obligation-type-0")("Declaration"),
        elementTextByID("date-from-to-0")("1 May 2017 to 1 June 2017"),
        elementTextByID("was-due-on-0")("Was due on 1 July 2017"),
        elementTextByID("submitted-on-date-0")("1 June 2017"),
        elementTextByID("income-source-1")("Property Income"),
        elementTextByID("obligation-type-1")("Annual update"),
        elementTextByID("date-from-to-1")("1 March 2017 to 1 April 2017"),
        elementTextByID("was-due-on-1")("Was due on 1 May 2017"),
        elementTextByID("submitted-on-date-1")("1 April 2017"),
        elementTextByID("income-source-2")("business"),
        elementTextByID("obligation-type-2")("Quarterly update"),
        elementTextByID("date-from-to-2")("1 January 2017 to 1 February 2017"),
        elementTextByID("was-due-on-2")("Was due on 1 March 2017"),
        elementTextByID("submitted-on-date-2")("1 February 2017")
      )
    }

    "redirect back to the home page if the feature switch is disabled" in {
      disable(ObligationsPage)

      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

      val result: WSResponse = IncomeTaxViewChangeFrontend.getPreviousObligations

      result should have(
        httpStatus(SEE_OTHER),
        redirectURI(controllers.routes.HomeController.home().url)
      )
    }
  }
}

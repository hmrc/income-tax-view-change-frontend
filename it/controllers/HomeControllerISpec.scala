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
import helpers.ComponentSpecBase
import play.api.http.Status._
import utils.ImplicitDateFormatter

class HomeControllerISpec extends ComponentSpecBase with ImplicitDateFormatter {

  lazy val appConfig: FrontendAppConfig = fakeApplication().injector.instanceOf[FrontendAppConfig]

  "Navigating to /report-quarterly/income-and-expenses/view" should {

    "redirect to the Business Tax Account" in {

      When("I call GET /report-quarterly/income-and-expenses/view")
      val res = IncomeTaxViewChangeFrontend.getHome

      Then("the result should have a HTTP status of SEE_OTHER (303) and redirect me to BTA home")
      res should have(

        //Check Status OK (200) Result
        httpStatus(SEE_OTHER),

        //Check Redirect Location
        redirectURI(appConfig.businessTaxAccount)
      )
    }
  }
}
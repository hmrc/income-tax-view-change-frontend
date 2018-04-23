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

import assets.messages.HomeMessages._
import helpers.ComponentSpecBase
import play.api.http.Status._
import utils.ImplicitDateFormatter

class HomeControllerISpec extends ComponentSpecBase with ImplicitDateFormatter {

  "Navigating to /report-quarterly/income-and-expenses/view" when {

    "Authorised" should {

      "render the home page" in {

        When("I call GET /report-quarterly/income-and-expenses/view")
        val res = IncomeTaxViewChangeFrontend.getHome

        Then("the result should have a HTTP status of OK (200) and the Income Tax home page")
        res should have(
          httpStatus(OK),
          pageTitle(title)
        )
      }
    }
    unauthorisedTest("")
  }
}
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

import helpers.ComponentSpecBase
import play.api.http.Status._

class BtaPartialControllerISpec extends ComponentSpecBase {

  "calling the BtaPartialController" when {

    "Is authenticated with an active enrolment" should {

      "display the bta partial with the correct information" in {

        When("I call GET /report-quarterly/income-and-expenses/view/partial")
        val res = IncomeTaxViewChangeFrontend.getBtaPartial

        res should have(
          httpStatus(OK)
        )

        Then("The BTA Partial is rendered")
        res should have(
          isElementVisibleById("it-quarterly-reporting-heading")(expectedValue = true)
        )
      }
    }

    unauthorisedTest("/partial")
  }
}

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

package routes

import utils.TestSupport

class RoutesSpec extends TestSupport {

  val contextRoute: String = "/check-your-income-tax-and-expenses"

  // Timeout routes
  "The URL for the SessionTimeoutController.timeout action" should {
    s"be equal to $contextRoute/session-timeout" in {
      controllers.timeout.routes.SessionTimeoutController.timeout().url shouldBe s"$contextRoute/session-timeout"
    }
  }

  //Obligation route
  "The URL for the ObligationsController.getObligations action" should {
    s"be equal tp $contextRoute/obligations" in {
      controllers.routes.ObligationsController.getObligations().url shouldBe s"$contextRoute/obligations"
    }
  }

  //Estimated Tax Liability
  "The URL for the EstimatedTaxLiabilityController.getEstimatedTaxLiability action" should {
    s"be equal to $contextRoute/estimated-tax-liability" in {
      controllers.routes.EstimatedTaxLiabilityController.getEstimatedTaxLiability().url shouldBe s"$contextRoute/estimated-tax-liability"
    }
  }
}
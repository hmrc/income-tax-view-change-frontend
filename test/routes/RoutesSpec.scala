/*
 * Copyright 2021 HM Revenue & Customs
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

import testUtils.TestSupport

class RoutesSpec extends TestSupport {

  val contextRoute: String = "/report-quarterly/income-and-expenses/view"

  //Context Route
  "The 'home' url for the HomeController.show action" should {
    s"be equal to $contextRoute" in {
      controllers.routes.HomeController.home().url shouldBe contextRoute
    }
  }

  // Timeout routes
  "The URL for the SessionTimeoutController.timeout action" should {
    s"be equal to $contextRoute/session-timeout" in {
      controllers.timeout.routes.SessionTimeoutController.timeout().url shouldBe s"$contextRoute/session-timeout"
    }
  }

  //Obligation route
  "The URL for the ReportDeadlinesController.getNextObligation action" should {
    s"be equal tp $contextRoute/obligations" in {
      controllers.routes.ReportDeadlinesController.getReportDeadlines().url shouldBe s"$contextRoute/obligations"
    }
  }

  //Estimates route
  "The URL for the TaxYearsController.viewTaxYears() action" should {
    s"be equal to $contextRoute/estimates" in {
      controllers.routes.TaxYearsController.viewTaxYears().url shouldBe s"$contextRoute/tax-years"
    }
  }

  //Calculation
  "The URL for the CalculationController.renderCalculationPage(year) action" should {
    s"be equal to $contextRoute/calculation/2018" in {
      controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(2018).url shouldBe s"$contextRoute/calculation/2018"
    }
  }

  //Not-Enrolled route
  "The URL for the NotEnrolledController.show action" should {
    s"be equal to $contextRoute/not-enrolled" in {
      controllers.errors.routes.NotEnrolledController.show().url shouldBe s"$contextRoute/not-enrolled"
    }
  }

  //Language route
  "The URL for the ItvcLanguageController.switchToLanguage" should {
    s"be equal to $contextRoute/language/en" in {
      controllers.routes.ItvcLanguageController.switchToLanguage("en").url shouldBe s"$contextRoute/language/en"
    }
  }
}

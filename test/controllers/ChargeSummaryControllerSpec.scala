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

package controllers

import assets.FinancialDetailsTestConstants._
import audit.mocks.MockAuditingService
import config.featureswitch.{FeatureSwitching, NewFinancialDetailsApi}
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import models.financialDetails.FinancialDetailsResponseModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation}
import services.FinancialDetailsService
import testUtils.TestSupport

import scala.concurrent.Future

class ChargeSummaryControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with ImplicitDateFormatter
  with MockIncomeSourceDetailsService
  with MockAuditingService
  with FeatureSwitching
  with TestSupport {
  class Setup(financialDetails: FinancialDetailsResponseModel, featureSwitch: Boolean = true) {
    val financialDetailsService: FinancialDetailsService = mock[FinancialDetailsService]

    when(financialDetailsService.getFinancialDetails(any(), any())(any()))
      .thenReturn(Future.successful(financialDetails))

    mockBothIncomeSources()

    if (featureSwitch) enable(NewFinancialDetailsApi)
    else disable(NewFinancialDetailsApi)

    val controller = new ChargeSummaryController(
      MockAuthenticationPredicate,
      app.injector.instanceOf[SessionTimeoutPredicate],
      app.injector.instanceOf[NinoPredicate],
      MockIncomeSourceDetailsPredicate,
      financialDetailsService,
      mockAuditingService,
      app.injector.instanceOf[ItvcErrorHandler]
    )(
      app.injector.instanceOf[FrontendAppConfig],
      languageUtils,
      app.injector.instanceOf[MessagesControllerComponents],
      ec,
      app.injector.instanceOf[ImplicitDateFormatterImpl]
    )
  }

  val errorHeading = "Sorry, there is a problem with the service"
  val successHeading = "Tax year 6 April 2017 to 5 April 2018 Payment on account 1 of 2"

  "The ChargeSummaryController" should {

    "redirect a user back to the home page" when {

      "the financial api switch is disabled" in new Setup(financialDetailsModel(2018), false) {
        val result: Result = await(controller.showChargeSummary(2018, "1040000123")(fakeRequestWithActiveSession))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.home().url)
      }

      "the charge id provided does not match any charges in the response" in new Setup(financialDetailsModel(2018)) {
        val result: Result = await(controller.showChargeSummary(2018, "fakeId")(fakeRequestWithActiveSession))

        status(result) shouldBe Status.SEE_OTHER
        redirectLocation(result) shouldBe Some(controllers.routes.HomeController.home().url)
      }
    }

    "load the page" when {

      "provided with an id that matches a charge in the financial response" in new Setup(financialDetailsModel(2018)) {
        val result: Result = await(controller.showChargeSummary(2018, "1040000123")(fakeRequestWithActiveSession))

        status(result) shouldBe Status.OK
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe successHeading
      }
    }

    "load an error page" when {

      "the financial details response is an error" in new Setup(testFinancialDetailsErrorModelParsing) {
        val result: Result = await(controller.showChargeSummary(2018, "1040000123")(fakeRequestWithActiveSession))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        JsoupParse(result).toHtmlDocument.select("h1").text() shouldBe errorHeading
      }
    }
  }
}

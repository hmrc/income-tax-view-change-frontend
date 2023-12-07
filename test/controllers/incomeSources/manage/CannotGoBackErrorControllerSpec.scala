/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.incomeSources.manage

import config.featureswitch.IncomeSources
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import controllers.predicates.{NavBarPredicate, SessionTimeoutPredicate}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import org.scalatest.Assertion
import play.api.http.Status.OK
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import testConstants.BaseTestConstants.testSelfEmploymentId
import testUtils.TestSupport
import views.html.incomeSources.YouCannotGoBackError

class CannotGoBackErrorControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate {

  object TestCannotGoBackController extends CannotGoBackErrorController(
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockAuthService,
    MockIncomeSourceDetailsPredicate,
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate],
    app.injector.instanceOf[YouCannotGoBackError]
  )(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler])

  val annualReportingMethod = "annual"
  val quarterlyReportingMethod = "quarterly"
  val taxYear = "2022-2023"

  def setupOKTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
    disableAllSwitches()
    enable(IncomeSources)
    setupMockAuthorisationSuccess(isAgent)
    mockUKPropertyIncomeSourceWithLatency2024()

    val result = (isAgent, incomeSourceType) match {
      case (true, SelfEmployment) => TestCannotGoBackController.show(isAgent, incomeSourceType, annualReportingMethod, taxYear, Some(testSelfEmploymentId))(fakeRequestConfirmedClient())
      case (false, SelfEmployment) => TestCannotGoBackController.show(isAgent, incomeSourceType, annualReportingMethod, taxYear, Some(testSelfEmploymentId))(fakeRequestWithActiveSession)
      case (true, _) => TestCannotGoBackController.show(isAgent, incomeSourceType, annualReportingMethod, taxYear, None)(fakeRequestConfirmedClient())
      case (false, _) => TestCannotGoBackController.show(isAgent, incomeSourceType, annualReportingMethod, taxYear, None)(fakeRequestWithActiveSession)
    }

    status(result) shouldBe OK
  }

  "CannotGoBackErrorController" should {
    "return 200 OK" when {
      "Self Employment - Individual" in {
        setupOKTest(isAgent = false, SelfEmployment)
      }
      "Self Employment - Agent" in {
        setupOKTest(isAgent = true, SelfEmployment)
      }
      "UK Property - Individual" in {
        setupOKTest(isAgent = false, UkProperty)
      }
      "UK Property - Agent" in {
        setupOKTest(isAgent = true, UkProperty)
      }
      "Foreign Property - Individual" in {
        setupOKTest(isAgent = false, ForeignProperty)
      }
      "Foreign Property - Agent" in {
        setupOKTest(isAgent = true, ForeignProperty)
      }
    }
  }
}


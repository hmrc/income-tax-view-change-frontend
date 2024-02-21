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

package controllers.manageBusinesses.manage

import config.featureswitch.IncomeSources
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.MockSessionService
import org.scalatest.Assertion
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{completedUIJourneySessionData, emptyUIJourneySessionData}
import testUtils.TestSupport
import views.html.incomeSources.YouCannotGoBackError

class CannotGoBackErrorControllerSpec extends TestSupport with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with MockSessionService {

  object TestCannotGoBackController extends CannotGoBackErrorController(
    mockAuthService,
    app.injector.instanceOf[YouCannotGoBackError],
    mockSessionService,
    testAuthenticator
  )(appConfig,
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler])

  val annualReportingMethod = "annual"
  val quarterlyReportingMethod = "quarterly"
  val taxYear = "2022-2023"

  def setupMockCalls(isAgent: Boolean): Unit = {
    disableAllSwitches()
    enable(IncomeSources)
    setupMockAuthorisationSuccess(isAgent)
    mockUKPropertyIncomeSourceWithLatency2024()
  }

  def setupOKTest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
    setupMockCalls(isAgent)
    setupMockGetMongo(Right(Some(completedUIJourneySessionData(JourneyType(Manage, incomeSourceType)))))

    val result = if (isAgent) {
      TestCannotGoBackController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
    } else {
      TestCannotGoBackController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)
    }

    status(result) shouldBe OK
  }

  def setupISETest(isAgent: Boolean, incomeSourceType: IncomeSourceType): Assertion = {
    setupMockCalls(isAgent)
    setupMockGetMongo(Right(Some(emptyUIJourneySessionData(JourneyType(Manage, incomeSourceType)))))

    val result = if (isAgent) {
      TestCannotGoBackController.show(isAgent, incomeSourceType)(fakeRequestConfirmedClient())
    } else {
      TestCannotGoBackController.show(isAgent, incomeSourceType)(fakeRequestWithActiveSession)
    }

    status(result) shouldBe INTERNAL_SERVER_ERROR
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
    "return 500 ISE" when {
      "Required Mongo data is missing - Self Employment - Individual" in {
        setupISETest(isAgent = false, SelfEmployment)
      }
      "Required Mongo data is missing - Self Employment - Agent" in {
        setupISETest(isAgent = true, SelfEmployment)
      }
      "Required Mongo data is missing - UK Property - Individual" in {
        setupISETest(isAgent = false, UkProperty)
      }
      "Required Mongo data is missing - UK Property - Agent" in {
        setupISETest(isAgent = true, UkProperty)
      }
      "Required Mongo data is missing - Foreign Property - Individual" in {
        setupISETest(isAgent = false, ForeignProperty)
      }
      "Required Mongo data is missing - Foreign Property - Agent" in {
        setupISETest(isAgent = true, ForeignProperty)
      }
    }
  }
}


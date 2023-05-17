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

package controllers.incomeSources.cease

import config.featureswitch.FeatureSwitch.switches
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitching
import controllers.incomeSources.add.AddIncomeSourceController
import controllers.predicates.{NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import play.api.mvc.MessagesControllerComponents
import testUtils.TestSupport

class CeaseIncomeSourceControllerSpec extends MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter
  with MockIncomeSourceDetailsService with MockNavBarEnumFsPredicate with MockFrontendAuthorisedFunctions with FeatureSwitching with TestSupport {

  val controller = new CeaseIncomeSourceController(
    app.injector.instanceOf[views.html.incomeSources.cease.CeaseIncomeSources],
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    mockAuthService,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[NavBarPredicate]
  )(
    ec,
    app.injector.instanceOf[MessagesControllerComponents],
    app.injector.instanceOf[FrontendAppConfig]
  )

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  "The CeaseIncomeSourcesController" should {

    "redirect an individual back to the home page" when {

      "the IncomeSources FS is disabled" in {

      }

    }

    "redirect an agent back to the home page" when {

      "the IncomeSources FS is disabled" in {

      }

    }

    "redirect an individual to the cease a sole trader page" when {
      "user has a Sole Trader Businesses and a UK property" in {

      }
    }

    "redirect an agent to the add income source page" when {
      "agent has a Sole Trader Businesses and a UK property" in {

      }
    }

  }
}

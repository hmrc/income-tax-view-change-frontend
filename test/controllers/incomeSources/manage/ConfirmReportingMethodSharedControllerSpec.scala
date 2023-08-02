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

import config.featureswitch.FeatureSwitch.switches
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import exceptions.MissingFieldException
import implicits.ImplicitDateFormatter
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.MockIncomeSourceDetailsService
import models.incomeSourceDetails.viewmodels.ViewIncomeSourcesViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.http.Status
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{IncomeSourceDetailsService, UpdateIncomeSourceService}
import testConstants.BaseTestConstants.{testAgentAuthRetrievalSuccess, testIndividualAuthSuccessWithSaUtrResponse}
import testConstants.BusinessDetailsTestConstants.viewBusinessDetailsViewModel
import testConstants.PropertyDetailsTestConstants.viewUkPropertyDetailsViewModel
import testUtils.TestSupport
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.manage.{ConfirmReportingMethod, ManageIncomeSources}

import scala.concurrent.Future

class ConfirmReportingMethodSharedControllerSpec extends MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate
  with ImplicitDateFormatter
  with MockIncomeSourceDetailsService
  with MockNavBarEnumFsPredicate
  with MockFrontendAuthorisedFunctions
  with FeatureSwitching
  with TestSupport {

  object TestManageIncomeSourceController
    extends ConfirmReportingMethodSharedController(
      manageIncomeSources = app.injector.instanceOf[ManageIncomeSources],
      checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
      MockAuthenticationPredicate,
      authorisedFunctions = mockAuthService,
      retrieveNino = app.injector.instanceOf[NinoPredicate],
      retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
      retrieveBtaNavBar = MockNavBarPredicate,
      itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
      itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
      incomeSourceDetailsService = mockIncomeSourceDetailsService,
      updateIncomeSourceService = mock(classOf[UpdateIncomeSourceService]),
      customNotFoundErrorView = app.injector.instanceOf[CustomNotFoundError],
      confirmReportingMethod = app.injector.instanceOf[ConfirmReportingMethod]
    )(
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      appConfig = app.injector.instanceOf[FrontendAppConfig],
      ec = ec
    )

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  val redirectController = controllers.incomeSources.manage.routes.ManageObligationsController

  "ConfirmReportingMethodSharedController.show" should {
    "show the Custom Not Found Error Page" when {
      "the IncomeSources FS is disabled" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "changeTo parameter has an invalid format" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "the given incomeSourceId is can not be found in the user's income sources" in {
      }
    }
    s"return ${Status.OK}" when {
      "all query parameters are valid" in {
      }
    }
  }

  "ConfirmReportingMethodSharedController.submit" should {
    "show the Custom Not Found Error Page" when {
      "the IncomeSources FS is disabled" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "changeTo parameter has an invalid format" in {
      }
    }
    s"return ${Status.BAD_REQUEST}" when {
      "the form is empty" in {
      }
    }
    "redirect to the Manage Obligations page for a UK property" when {
      "the user's UK property reporting method is updated to annual" in {
      }
    }
    "redirect to the Manage Obligations page for a UK property" when {
      "the user's UK property reporting method is updated to quarterly" in {
      }
    }
    "redirect to the Manage Obligations page for a Foreign property" when {
      "the user's Foreign property reporting method is updated to annual" in {
      }
    }
    "redirect to the Manage Obligations page for a Foreign property" when {
      "the user's Foreign property reporting method is updated to quarterly" in {
      }
    }
    "redirect to the Manage Obligations page for a Sole Trader Business" when {
      "the user's Sole Trader Business reporting method is updated to annual" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "the user has no income sources matching the incomeSourceId query parameter" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "UpdateIncomeSourceService returns a UpdateIncomeSourceResponseError response" in {
      }
    }
  }

  "ConfirmReportingMethodSharedController.showAgent" should {
    "show the Custom Not Found Error Page" when {
      "the IncomeSources FS is disabled" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "changeTo parameter has an invalid format" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "the given incomeSourceId is can not be found in the user's income sources" in {
      }
    }
    s"return ${Status.OK}" when {
      "all query parameters are valid" in {
      }
    }
  }

  "ConfirmReportingMethodSharedController.submitAgent" should {
    "show the Custom Not Found Error Page" when {
      "the IncomeSources FS is disabled" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "taxYear parameter has an invalid format" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "changeTo parameter has an invalid format" in {
      }
    }
    s"return ${Status.BAD_REQUEST}" when {
      "the form is empty" in {
      }
    }
    "redirect to the Manage Obligations page for a UK property" when {
      "the user's UK property reporting method is updated to annual" in {
      }
    }
    "redirect to the Manage Obligations page for a UK property" when {
      "the user's UK property reporting method is updated to quarterly" in {
      }
    }
    "redirect to the Manage Obligations page for a Foreign property" when {
      "the user's Foreign property reporting method is updated to annual" in {
      }
    }
    "redirect to the Manage Obligations page for a Foreign property" when {
      "the user's Foreign property reporting method is updated to quarterly" in {
      }
    }
    "redirect to the Manage Obligations page for a Sole Trader Business" when {
      "the user's Sole Trader Business reporting method is updated to annual" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "the user has no income sources matching the incomeSourceId query parameter" in {
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "UpdateIncomeSourceService returns a UpdateIncomeSourceResponseError response" in {
      }
    }
  }
}

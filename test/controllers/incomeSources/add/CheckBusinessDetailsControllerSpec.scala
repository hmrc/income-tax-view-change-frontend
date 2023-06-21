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

package controllers.incomeSources.add

import config.featureswitch.FeatureSwitch.switches
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import forms.utils.SessionKeys
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import org.mockito.Mockito.mock
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.{MessagesControllerComponents, Result}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import testConstants.BaseTestConstants
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpClient
import views.html.incomeSources.add.CheckBusinessDetails

class CheckBusinessDetailsControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockIncomeSourceDetailsPredicate with MockNavBarEnumFsPredicate with FeatureSwitching {

  def disableAllSwitches(): Unit = {
    switches.foreach(switch => disable(switch))
  }

  val testBusinessName: String = "Test Business"
  val testBusinessStartDate: String = "2022-11-11"
  val testBusinessTrade: String = "Plumbing"
  val testBusinessAddressLine1: String = "123 Main Street"
  val testBusinessPostCode: String = "AB123CD"
  val testBusinessAccountingMethod = "Quarterly"


  val testBusinessAccountingPeriodStartDate: String = "2022-11-11"


  val mockHttpClient: HttpClient = mock(classOf[HttpClient])
  val mockCheckBusinessDetails: CheckBusinessDetails = app.injector.instanceOf[CheckBusinessDetails]

  object TestCheckBusinessDetailsController extends CheckBusinessDetailsController(
    checkBusinessDetails = app.injector.instanceOf[CheckBusinessDetails],
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    authenticate = MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    retrieveBtaNavBar = MockNavBarPredicate
  )(ec, mcc = app.injector.instanceOf[MessagesControllerComponents],
    appConfig = app.injector.instanceOf[FrontendAppConfig]
  )

  "CheckBusinessDetailsController" should {

    "return 200 OK" when {
        "the session contains full business details" in {
          disableAllSwitches()
          enable(IncomeSources)

          mockNoIncomeSources()
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val result = TestCheckBusinessDetailsController.show()(
            fakeRequestWithActiveSession
              .withSession(
                SessionKeys.businessName -> testBusinessStartDate,
                SessionKeys.businessStartDate -> testBusinessStartDate,
                SessionKeys.businessTrade -> testBusinessTrade,
                SessionKeys.addBusinessAddressLine1 -> testBusinessAddressLine1,
                SessionKeys.addBusinessPostCode -> testBusinessPostCode,
                SessionKeys.addBusinessAccountingMethod -> testBusinessAccountingMethod
              ))

          status(result) shouldBe OK
      }
    }

    "return 500 INTERNAL_SERVER_ERROR" when {
      "there is session data missing" in {
        disableAllSwitches()
        enable(IncomeSources)

        mockNoIncomeSources()
        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        val result = TestCheckBusinessDetailsController.show()(
          fakeRequestWithActiveSession
            .withSession(
              SessionKeys.businessName -> testBusinessStartDate,
              SessionKeys.businessStartDate -> testBusinessStartDate,
              SessionKeys.businessTrade -> testBusinessTrade,
              SessionKeys.addBusinessAddressLine1 -> testBusinessAddressLine1,
              SessionKeys.addBusinessPostCode -> testBusinessPostCode
            ))

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}



/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.optOut

import auth.FrontendAuthorisedFunctions
import auth.authV2.actions._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import mocks.auth.MockOldAuthActions
import mocks.controllers.predicates.MockIncomeSourceDetailsPredicate
import models.admin.ReportingFrequencyPage
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.OK
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testNino
import testConstants.BusinessDetailsTestConstants.{business1, testMtdItId}
import views.html.optOut.OptOutCancelledView

import scala.concurrent.Future


class OptOutCancelledControllerSpec extends MockOldAuthActions with MockIncomeSourceDetailsPredicate with MockitoSugar {

  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  val mockFrontendAuthorisedFunctions: FrontendAuthorisedFunctions = mock[FrontendAuthorisedFunctions]
  val mockNonAgentItvcErrorHandler: ItvcErrorHandler = mock[ItvcErrorHandler]
  val mockAgentItvcErrorHandler: AgentItvcErrorHandler = mock[AgentItvcErrorHandler]
  val mockAuthoriseAndRetrieve: AuthoriseAndRetrieve = mock[AuthoriseAndRetrieve]

  val optOutCancelledView: OptOutCancelledView = app.injector.instanceOf[OptOutCancelledView]

  val controller =
    new OptOutCancelledController(
      authorisedFunctions = mockFrontendAuthorisedFunctions,
      auth = mockAuthActions,
      dateService = dateService,
      view = optOutCancelledView
    )(
      appConfig = mockFrontendAppConfig,
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      ec = ec,
      itvcErrorHandler = mockNonAgentItvcErrorHandler,
      itvcErrorHandlerAgent = mockAgentItvcErrorHandler
    )


  "OptOutCancelledPageController" when {

    ".show()" should {

      "show the OptOutCancelled view and return OK response status" in {

        disableAllSwitches()

        val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtdItId, Some("2017"), List(business1), Nil)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(mockFrontendAppConfig.readFeatureSwitchesFromMongo).thenReturn(false)

        when(
          mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
        ).thenReturn(Future(singleBusinessIncome))

        val result = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentAsString(result) shouldBe
          optOutCancelledView(
            isAgent = false,
            currentTaxYearStart = dateService.getCurrentTaxYear.startYear.toString,
            currentTaxYearEnd = dateService.getCurrentTaxYear.endYear.toString
          ).toString
      }
    }
  }
}

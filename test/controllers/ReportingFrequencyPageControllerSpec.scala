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

package controllers

import auth.FrontendAuthorisedFunctions
import auth.authV2.actions._
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import mocks.auth.MockOldAuthActions
import mocks.controllers.predicates.MockIncomeSourceDetailsPredicate
import models.admin.ReportingFrequencyPage
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.optIn.OptInService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testNino
import testConstants.BusinessDetailsTestConstants.{business1, testMtdItId}
import views.html.errorPages.templates.ErrorTemplate

import scala.concurrent.Future


class ReportingFrequencyPageControllerSpec extends MockOldAuthActions with MockIncomeSourceDetailsPredicate with MockitoSugar {

  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val mockOptInService: OptInService = mock[OptInService]
  val mockFrontendAuthorisedFunctions: FrontendAuthorisedFunctions = mock[FrontendAuthorisedFunctions]
  val mockNonAgentItvcErrorHandler: ItvcErrorHandler = mock[ItvcErrorHandler]
  val mockAgentItvcErrorHandler: AgentItvcErrorHandler = mock[AgentItvcErrorHandler]

  val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]

  val mockAuthoriseAndRetrieve: AuthoriseAndRetrieve = mock[AuthoriseAndRetrieve]

  val controller =
    new ReportingFrequencyPageController(
      optInService = mockOptInService,
      authorisedFunctions = mockFrontendAuthorisedFunctions,
      auth = mockAuthActions,
      errorTemplate = errorTemplateView
    )(
      appConfig = mockFrontendAppConfig,
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      ec = ec,
      itvcErrorHandler = mockNonAgentItvcErrorHandler,
      itvcErrorHandlerAgent = mockAgentItvcErrorHandler
    )


  "ReportingFrequencyPageController" when {

    ".show()" when {

      "Reporting Frequency feature switch is enabled" should {

        "show the ReportingFrequencyPage" in {

          disableAllSwitches()
          enable(ReportingFrequencyPage)

          val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtdItId, Some("2017"), List(business1), Nil)

          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          when(mockFrontendAppConfig.readFeatureSwitchesFromMongo).thenReturn(false)

          when(
            mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
          ).thenReturn(Future(singleBusinessIncome))

          val result = controller.show()(fakeRequestWithActiveSession)

          status(result) shouldBe OK
          contentAsString(result) shouldBe "Reporting Frequency Page - Placeholder"
        }
      }

      "Reporting Frequency feature switch is disabled" should {

        "show an Error page" in {

          disableAllSwitches()
          disable(ReportingFrequencyPage)

          val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtdItId, Some("2017"), List(business1), Nil)

          when(mockFrontendAppConfig.readFeatureSwitchesFromMongo)
            .thenReturn(false)

          when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
            .thenReturn(Future(singleBusinessIncome))

          val result = controller.show()(fakeRequestWithActiveSession)

          status(result) shouldBe INTERNAL_SERVER_ERROR
          contentAsString(result).contains("Sorry, there is a problem with the service") shouldBe true
        }
      }

    }
  }
}

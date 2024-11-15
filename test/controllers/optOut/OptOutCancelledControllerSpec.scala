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
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus.{Mandated, Voluntary}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{OK, INTERNAL_SERVER_ERROR}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.optout.{OptOutProposition, OptOutService}
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testNino
import testConstants.BusinessDetailsTestConstants.{business1, testMtdItId}
import views.html.errorPages.templates.ErrorTemplate
import views.html.optOut.OptOutCancelledView

import scala.concurrent.Future


class OptOutCancelledControllerSpec extends MockOldAuthActions with MockIncomeSourceDetailsPredicate with MockitoSugar {

  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val mockOptOutService: OptOutService = mock[OptOutService]

  val mockFrontendAuthorisedFunctions: FrontendAuthorisedFunctions = mock[FrontendAuthorisedFunctions]
  val mockNonAgentItvcErrorHandler: ItvcErrorHandler = mock[ItvcErrorHandler]
  val mockAgentItvcErrorHandler: AgentItvcErrorHandler = mock[AgentItvcErrorHandler]
  val mockAuthoriseAndRetrieve: AuthoriseAndRetrieve = mock[AuthoriseAndRetrieve]

  val optOutCancelledView: OptOutCancelledView = app.injector.instanceOf[OptOutCancelledView]
  val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]

  val controller =
    new OptOutCancelledController(
      authorisedFunctions = mockFrontendAuthorisedFunctions,
      auth = mockAuthActions,
      optOutService = mockOptOutService,
      view = optOutCancelledView,
      errorTemplate = errorTemplateView
    )(
      appConfig = mockFrontendAppConfig,
      mcc = app.injector.instanceOf[MessagesControllerComponents],
      ec = ec,
      itvcErrorHandler = mockNonAgentItvcErrorHandler,
      itvcErrorHandlerAgent = mockAgentItvcErrorHandler
    )


  "OptOutCancelledPageController" when {

    ".show()" should {

      "show the OptOutCancelled view and return OK - 200" in {

        disableAllSwitches()

        val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtdItId, Some("2017"), List(business1), Nil)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(mockFrontendAppConfig.readFeatureSwitchesFromMongo).thenReturn(false)

        when(
          mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
        ).thenReturn(Future(singleBusinessIncome))

        when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
          Future(
            OptOutProposition.createOptOutProposition(
              currentYear = TaxYear(2024, 2025),
              previousYearCrystallised = false,
              previousYearItsaStatus = Mandated,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Mandated
            )
          )
        )

        val result = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe OK
        contentAsString(result) shouldBe
          optOutCancelledView(
            isAgent = false,
            currentTaxYearStart = "2024",
            currentTaxYearEnd = "2025"
          ).toString
      }

      "show the Error Template view and return Internal Server Error - 500" in {

        disableAllSwitches()

        val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtdItId, Some("2017"), List(business1), Nil)

        setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

        when(mockFrontendAppConfig.readFeatureSwitchesFromMongo).thenReturn(false)

        when(
          mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
        ).thenReturn(Future(singleBusinessIncome))

        when(mockOptOutService.fetchOptOutProposition()(any(), any(), any())).thenReturn(
          Future(
            OptOutProposition.createOptOutProposition(
              currentYear = TaxYear(2024, 2025),
              previousYearCrystallised = false,
              previousYearItsaStatus = Mandated,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Voluntary
            )
          )
        )

        val result = controller.show()(fakeRequestWithActiveSession)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsString(result).contains("Sorry, there is a problem with the service") shouldBe true
      }
    }
  }
}

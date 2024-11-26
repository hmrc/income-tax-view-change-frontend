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
import models.ReportingFrequencyViewModel
import models.admin.ReportingFrequencyPage
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.optout.{NextUpdatesQuarterlyReportingContentChecks, OptOutMultiYearViewModel}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, status}
import services.DateService
import services.optout.OptOutService
import testConstants.BaseTestConstants
import testConstants.BaseTestConstants.testNino
import testConstants.BusinessDetailsTestConstants.{business1, testMtdItId}
import views.html.ReportingFrequencyView
import views.html.errorPages.templates.ErrorTemplate

import scala.concurrent.Future


class ReportingFrequencyPageControllerSpec extends MockOldAuthActions with MockIncomeSourceDetailsPredicate with MockitoSugar {

  val mockFrontendAppConfig: FrontendAppConfig = mock[FrontendAppConfig]
  val mockOptOutService: OptOutService = mock[OptOutService]
  val mockFrontendAuthorisedFunctions: FrontendAuthorisedFunctions = mock[FrontendAuthorisedFunctions]
  val mockNonAgentItvcErrorHandler: ItvcErrorHandler = mock[ItvcErrorHandler]
  val mockAgentItvcErrorHandler: AgentItvcErrorHandler = mock[AgentItvcErrorHandler]
  val mockDateService: DateService = mock[DateService]

  val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]
  val reportingFrequencyView: ReportingFrequencyView = app.injector.instanceOf[ReportingFrequencyView]

  val controller =
    new ReportingFrequencyPageController(
      optOutService = mockOptOutService,
      authorisedFunctions = mockFrontendAuthorisedFunctions,
      auth = mockAuthActions,
      dateService = dateService,
      errorTemplate = errorTemplateView,
      view = reportingFrequencyView
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

          when(mockOptOutService.nextUpdatesPageOptOutViewModels()(any(), any(), any())).thenReturn(
            Future(
              (
                NextUpdatesQuarterlyReportingContentChecks(currentYearItsaStatus = true, previousYearItsaStatus = true, previousYearCrystallisedStatus = true),
                Some(OptOutMultiYearViewModel())
              )
            )
          )

          when(mockFrontendAppConfig.readFeatureSwitchesFromMongo).thenReturn(false)

          when(
            mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
          ).thenReturn(Future(singleBusinessIncome))

          val result = controller.show(false)(fakeRequestWithActiveSession)

          status(result) shouldBe OK
          contentAsString(result) shouldBe
            reportingFrequencyView(
              ReportingFrequencyViewModel(
                isAgent = false,
                currentTaxYear = TaxYear(2023, 2024),
                nextTaxYear = TaxYear(2024, 2025),
                Some(controllers.optOut.routes.OptOutChooseTaxYearController.show(false).url)
              )
            ).toString
        }
      }

      "Reporting Frequency feature switch is disabled" should {

        "show an Error page" in {

          disableAllSwitches()
          disable(ReportingFrequencyPage)
          setupMockAuthRetrievalSuccess(BaseTestConstants.testIndividualAuthSuccessWithSaUtrResponse())

          val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtdItId, Some("2017"), List(business1), Nil)

          when(mockOptOutService.nextUpdatesPageOptOutViewModels()(any(), any(), any())).thenReturn(
            Future(
              (
                NextUpdatesQuarterlyReportingContentChecks(currentYearItsaStatus = true, previousYearItsaStatus = true, previousYearCrystallisedStatus = true),
                Some(OptOutMultiYearViewModel())
              )
            )
          )

          when(mockFrontendAppConfig.readFeatureSwitchesFromMongo)
            .thenReturn(false)

          when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
            .thenReturn(Future(singleBusinessIncome))

          val result = controller.show(false)(fakeRequestWithActiveSession)

          status(result) shouldBe INTERNAL_SERVER_ERROR
          contentAsString(result).contains("Sorry, there is a problem with the service") shouldBe true
        }
      }

    }
  }
}

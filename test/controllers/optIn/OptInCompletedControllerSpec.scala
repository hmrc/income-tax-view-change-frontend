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

package controllers.optIn

import cats.data.OptionT
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import mocks.controllers.predicates.MockAuthenticationPredicate
import mocks.services.{MockOptInService, MockOptOutService}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.Voluntary
import models.optin.OptInCompletedViewModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import services.optIn.core.OptInProposition.createOptInProposition
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome
import testUtils.TestSupport
import views.html.optIn.OptInCompletedView

import scala.concurrent.Future

class OptInCompletedControllerSpec extends TestSupport
  with MockAuthenticationPredicate with MockOptOutService with MockOptInService {

  val view: OptInCompletedView = app.injector.instanceOf[OptInCompletedView]
  val messagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]
  val itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler]
  val agentItvcErrorHandler = app.injector.instanceOf[AgentItvcErrorHandler]

  val controller =
    new OptInCompletedController(
      view = view,
      mockOptInService,
      authorisedFunctions = mockAuthService,
      auth = testAuthenticator,
    )(
      appConfig = appConfig,
      mcc = messagesControllerComponents,
      ec = ec,
      itvcErrorHandler = itvcErrorHandler,
      itvcErrorHandlerAgent = agentItvcErrorHandler
    )

  val endTaxYear = 2023
  val taxYear2023 = TaxYear.forYearEnd(endTaxYear)

  "OptInCompletedController - Individual" when {

    ".show()" when {

      "the user selected a tax year and OptInCompletedViewModel is defined" should {

        "return OK 200 status" in {

          val isAgent = false

          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          val proposition = createOptInProposition(taxYear2023, ITSAStatus.Annual, ITSAStatus.Annual)

          when(mockOptInService.optinCompletedPageModel(any())(any(), any(), any()))
            .thenReturn(
              OptionT[Future, OptInCompletedViewModel](
                Future(Some(
                  OptInCompletedViewModel(
                    isAgent = isAgent,
                    optInTaxYear = taxYear2023,
                    showAnnualReportingAdvice = proposition.showAnnualReportingAdvice(taxYear2023),
                    isCurrentYear = proposition.isCurrentTaxYear(taxYear2023),
                    optInIncludedNextYear = proposition.nextTaxYear.status == Voluntary
                  )
                ))
              )
            )

          val request = fakeRequestWithNinoAndOrigin("PTA")
          val result =
            controller
              .show()
              .apply(request)

          status(result) shouldBe Status.OK
        }
      }

      "the user has not selected a tax year and OptInCompletedViewModel is not defined" should {

        "return INTERNAL_SERVER_ERROR 500 status" in {

          val isAgent = false

          setupMockAuthorisationSuccess(isAgent)
          setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

          when(mockOptInService.optinCompletedPageModel(any())(any(), any(), any()))
            .thenReturn(
              OptionT[Future, OptInCompletedViewModel](Future(None))
            )

          val request = fakeRequestWithNinoAndOrigin("PTA")
          val result =
            controller
              .show()
              .apply(request)

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    "OptInCompletedController - Agent" when {

      ".show()" when {

        "the user selected a tax year and OptInCompletedViewModel is defined" should {

          "return OK 200 status" in {

            val isAgent = true

            setupMockAuthorisationSuccess(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

            val proposition = createOptInProposition(taxYear2023, ITSAStatus.Annual, ITSAStatus.Annual)

            when(mockOptInService.optinCompletedPageModel(any())(any(), any(), any()))
              .thenReturn(
                OptionT[Future, OptInCompletedViewModel](
                  Future(Some(
                    OptInCompletedViewModel(
                      isAgent = isAgent,
                      optInTaxYear = taxYear2023,
                      showAnnualReportingAdvice = proposition.showAnnualReportingAdvice(taxYear2023),
                      isCurrentYear = proposition.isCurrentTaxYear(taxYear2023),
                      optInIncludedNextYear = proposition.nextTaxYear.status == Voluntary
                    )
                  ))
                )
              )

            val request = fakeRequestConfirmedClient()
            val result =
              controller
                .show(isAgent)
                .apply(request)

            status(result) shouldBe Status.OK
          }
        }

        "the user has not selected a tax year and OptInCompletedViewModel is not defined" should {

          "return INTERNAL_SERVER_ERROR 500 status" in {

            val isAgent = true

            setupMockAuthorisationSuccess(isAgent)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

            when(mockOptInService.optinCompletedPageModel(any())(any(), any(), any()))
              .thenReturn(
                OptionT[Future, OptInCompletedViewModel](Future(None))
              )

            val request = fakeRequestConfirmedClient()
            val result =
              controller
                .show(isAgent)
                .apply(request)

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }
    }
  }
}



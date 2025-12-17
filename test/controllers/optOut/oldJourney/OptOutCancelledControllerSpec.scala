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

package controllers.optOut.oldJourney

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockOptOutService
import models.admin.OptOutFs
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus.{Mandated, Voluntary}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.DateServiceInterface
import services.optout.{OptOutProposition, OptOutService}
import testConstants.BaseTestConstants.{testMtditid, testNino}
import testConstants.BusinessDetailsTestConstants.business1
import views.html.optOut.oldJourney.OptOutCancelledView

import scala.concurrent.Future


class OptOutCancelledControllerSpec extends MockAuthActions with MockOptOutService with MockitoSugar {

  def config: Map[String, Object] = Map(
    "feature-switches.read-from-mongo" -> "false"
  )

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[OptOutService].toInstance(mockOptOutService),
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
      )
      .configure(config)
      .build()

  lazy val testController = app.injector.instanceOf[OptOutCancelledController]
  lazy val optOutCancelledView = app.injector.instanceOf[OptOutCancelledView]

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual

    s"show(isAgent = $isAgent)" when {

      val action = if (isAgent) testController.showAgent() else testController.show()
      val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)

      s"the user is authenticated as a $mtdRole" when {

        "a tax year is determined from a single year / multi year chosen intent tax year" should {

          "render the opt out cancelled page" in {
            enable(OptOutFs)
            mockItsaStatusRetrievalAction()
            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)
            setupMockSuccess(mtdRole)

            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))


            when(mockOptOutService.getTaxYearForOptOutCancelled()(any(), any(), any()))
              .thenReturn(
                Future(Some(TaxYear(2024, 2025)))
              )

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

            val result = action(fakeRequest)

            status(result) shouldBe OK
            contentAsString(result) shouldBe
              optOutCancelledView(
                isAgent = isAgent,
                taxYearOpt = Some(TaxYear(2024, 2025))
              ).toString
          }
        }


        "user hits the cancel page before a tax year is selected for a multi year scenario and no tax year is returned" should {

          "render the opt out cancelled page - OK 200" in {
            enable(OptOutFs)

            val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)

            setupMockSuccess(mtdRole)
            mockItsaStatusRetrievalAction()
            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future(singleBusinessIncome))

            when(mockOptOutService.getTaxYearForOptOutCancelled()(any(), any(), any()))
              .thenReturn(
                Future(None)
              )

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

            val result = action(fakeRequest)

            status(result) shouldBe OK
            contentAsString(result) shouldBe
              optOutCancelledView(
                isAgent = isAgent,
                taxYearOpt = None
              ).toString
          }
        }

        "show the Error Template view" when {

          "there is some unexpected error" should {

            "recover and return error template Internal Server Error - 500" in {
              enable(OptOutFs)

              val singleBusinessIncome = IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)

              setupMockSuccess(mtdRole)

              when(
                mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
              ).thenReturn(Future(singleBusinessIncome))

              when(mockOptOutService.getTaxYearForOptOutCancelled()(any(), any(), any()))
                .thenReturn(
                  Future(throw new Exception("some fake error"))
                )

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

              val result = action(fakeRequest)

              status(result) shouldBe INTERNAL_SERVER_ERROR
              contentAsString(result).contains("Sorry, there is a problem with the service") shouldBe true
            }
          }
        }
        "redirect to the home page" when {

          "the feature switch is disabled" in {

            disable(OptOutFs)
            setupMockSuccess(mtdRole)
            mockItsaStatusRetrievalAction()
            when(
              mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any())
            ).thenReturn(Future.successful(IncomeSourceDetailsModel(testNino, testMtditid, Some("2017"), List(business1), Nil)))

            val result = action(fakeRequest)

            val redirectUrl = if (isAgent) {
              "/report-quarterly/income-and-expenses/view/agents/client-income-tax"
            } else {
              "/report-quarterly/income-and-expenses/view"
            }

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(redirectUrl)
          }
        }
      }
testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }
  }
}

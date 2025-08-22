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

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.MTDIndividual
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import models.incomeSourceDetails.ManageIncomeSourceData
import models.updateIncomeSource.{UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api
import play.api.Application
import play.api.http.Status
import play.api.http.Status.SEE_OTHER
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{SessionService, UpdateIncomeSourceService}
import testConstants.BaseTestConstants.testSelfEmploymentId
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{completedUIJourneySessionData, notCompletedUIJourneySessionData}

import scala.concurrent.Future

class CheckYourAnswersControllerSpec extends MockAuthActions
  with ImplicitDateFormatter
  with MockSessionService {

  lazy val mockUpdateIncomeSourceService = mock(classOf[UpdateIncomeSourceService])

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[UpdateIncomeSourceService].toInstance(mockUpdateIncomeSourceService)
    ).build()

  lazy val testController = app.injector.instanceOf[CheckYourAnswersController]
  val incomeSourceTypes = List(SelfEmployment, UkProperty, ForeignProperty)

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    incomeSourceTypes.foreach { incomeSourceType =>
      s"show(isAgent = $isAgent, incomeSourceType = $incomeSourceType)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = testController.show(isAgent, incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          "render the check your answers page" when {
            "the session contains all relevant data" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusinessWithLatency()

              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType))
                .copy(manageIncomeSourceData = Some(ManageIncomeSourceData(
                  incomeSourceId = Some(testSelfEmploymentId),
                  reportingMethod = Some(testChangeToAnnual),
                  taxYear = Some(testTaxYear.toInt))
                )))
              ))

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
            }
          }

          "redirect to the Cannot Go Back page" when {
            "the journey is complete" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))

              val result = action(fakeRequest)
              val expectedRedirectUrl = routes.CannotGoBackErrorController.show(isAgent = isAgent, incomeSourceType).url

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(expectedRedirectUrl)
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }

      s"submit(isAgent = $isAgent, incomeSourceType = $incomeSourceType)" when {
        val fakeRequest = fakePostRequestBasedOnMTDUserType(mtdRole)
        val action = testController.submit(isAgent, incomeSourceType)
        s"the user is authenticated as a $mtdRole" should {
          "redirect to the manage obligations page" when {
            "the reporting method is updated to annual" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType))
                .copy(manageIncomeSourceData = Some(ManageIncomeSourceData(
                  incomeSourceId = Some(testSelfEmploymentId),
                  reportingMethod = Some(testChangeToAnnual),
                  taxYear = Some(testTaxYear.toInt))
                )))
              ))
              setupMockSetMongoData(true)

              when(mockUpdateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
                .thenReturn(
                  Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z"))
                )

              val result = action(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe
                Some(
                  controllers.manageBusinesses.manage.routes
                    .ManageObligationsController.show(isAgent, incomeSourceType).url
                )
            }

            "the reporting method is updated to quarterly" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType))
                .copy(manageIncomeSourceData = Some(ManageIncomeSourceData(
                  incomeSourceId = Some(testSelfEmploymentId),
                  reportingMethod = Some(testChangeToQuarterly),
                  taxYear = Some(testTaxYear.toInt))
                )))
              ))
              setupMockSetMongoData(true)

              when(mockUpdateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
                .thenReturn(
                  Future(UpdateIncomeSourceResponseModel("2022-01-31T09:26:17Z"))
                )

              val result = action(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe
                Some(
                  controllers.manageBusinesses.manage.routes
                    .ManageObligationsController.show(isAgent, incomeSourceType).url
                )
            }
          }

          "redirect to ReportingMethodChangeErrorController" when {
            "UpdateIncomeSourceService returns a UpdateIncomeSourceResponseError response" in {
              setupMockSuccess(mtdRole)
              mockBothPropertyBothBusiness()
              setupMockCreateSession(true)
              setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType))
                .copy(manageIncomeSourceData = Some(ManageIncomeSourceData(
                  incomeSourceId = Some(testSelfEmploymentId),
                  reportingMethod = Some(testChangeToQuarterly),
                  taxYear = Some(testTaxYear.toInt))
                )))
              ))
              setupMockSetMongoData(true)

              when(mockUpdateIncomeSourceService.updateTaxYearSpecific(any(), any(), any())(any(), any()))
                .thenReturn(
                  Future(UpdateIncomeSourceResponseError("INTERNAL_SERVER_ERROR", "Dummy message"))
                )

              val result = action(fakeRequest)
              status(result) shouldBe Status.SEE_OTHER
              redirectLocation(result) shouldBe Some(routes.ReportingMethodChangeErrorController.show(isAgent, incomeSourceType).url)
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }

  private lazy val testTaxYear = "2023"
  private lazy val testChangeToAnnual = "annual"
  private lazy val testChangeToQuarterly = "quarterly"
}

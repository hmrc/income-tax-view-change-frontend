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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.MockSessionService
import play.api
import play.api.Application
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.{DateServiceInterface, SessionService}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{completedUIJourneySessionData, emptyUIJourneySessionData, singleUKPropertyIncome2024}

class CannotGoBackErrorControllerSpec extends MockAuthActions with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[SessionService].toInstance(mockSessionService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[CannotGoBackErrorController]

  val annualReportingMethod = "annual"
  val quarterlyReportingMethod = "quarterly"
  val taxYear = "2022-2023"

  val incomeSourceTypes = List(SelfEmployment, UkProperty, ForeignProperty)

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    incomeSourceTypes.foreach { incomeSourceType =>
      s"show(isAgent = $isAgent, incomeSourceType = $incomeSourceType)" when {
        val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
        val action = testController.show(isAgent, incomeSourceType)
        val actionForNoMongoSession = testController.noJourneySessionShow(isAgent)
        s"the user is authenticated as a $mtdRole" should {
          "render the manage income sources page" in {
            setupMockSuccess(mtdRole)
            mockItsaStatusRetrievalAction(singleUKPropertyIncome2024)
            mockUKPropertyIncomeSourceWithLatency2024()
            setupMockGetMongo(Right(Some(completedUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))

            val result = action(fakeRequest)
            status(result) shouldBe OK
          }

          "render the error page" when {
            "Required Mongo data is missing" in {
              setupMockSuccess(mtdRole)
              mockItsaStatusRetrievalAction(singleUKPropertyIncome2024)
              mockUKPropertyIncomeSourceWithLatency2024()
              setupMockGetMongo(Right(Some(emptyUIJourneySessionData(IncomeSourceJourneyType(Manage, incomeSourceType)))))

              val result = action(fakeRequest)
              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }

          "render the cannot go back page" when {
            "there is no journey session data in mongo" in {
              setupMockSuccess(mtdRole)
              mockUKPropertyIncomeSourceWithLatency2024()
              setupMockGetMongo(Right(None))

              val result = actionForNoMongoSession(fakeRequest)
              status(result) shouldBe OK
            }
          }
        }
        testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
      }
    }
  }
}


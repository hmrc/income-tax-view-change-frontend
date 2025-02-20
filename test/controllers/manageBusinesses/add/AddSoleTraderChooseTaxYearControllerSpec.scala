/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.manageBusinesses.add

import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.{MockDateService, MockSessionService}
import models.admin.IncomeSourcesNewJourney
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import play.api
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import services.{DateService, SessionService}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessesAndPropertyIncome

import scala.concurrent.Future

class AddSoleTraderChooseTaxYearControllerSpec extends MockAuthActions with MockDateService with MockSessionService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[DateService].toInstance(mockDateService),
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val controller = app.injector.instanceOf[AddSoleTraderChooseTaxYearController]

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    ".show()" when {
      s"the user is authenticated as a $mtdRole" should {
        "return 200 OK" when {
          "feature switch is enabled" in {
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            setupMockGetCurrentTaxYear(TaxYear(2024, 2025))
            enable(IncomeSourcesNewJourney)

            val result: Future[Result] = controller.show(isAgent)(fakeRequestWithActiveSession)

            status(result) shouldBe OK
          }
        }
        "return 303 seeOther" when {
          "feature switch is disabled" in {
            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            setupMockGetCurrentTaxYear(TaxYear(2024, 2025))
            disable(IncomeSourcesNewJourney)

            val result: Future[Result] = controller.show(isAgent)(fakeRequestWithActiveSession)

            val homeUrl = if (mtdRole == MTDIndividual) {
              controllers.routes.HomeController.show().url
            } else {
              controllers.routes.HomeController.showAgent.url
            }

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(homeUrl)
          }
        }
        "return 500 internalServerError" when {
          "the request fails" in {
            setupMockSuccess(mtdRole)
            mockSingleBusinessIncomeSourceError()
            enable(IncomeSourcesNewJourney)

            val result: Future[Result] = controller.show(isAgent)(fakeRequestWithActiveSession)

            status(result) shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }
    ".submit()" when {
      s"the user is authenticated as a $mtdRole" should {
        "return 200 OK" when {
          s"user submits a valid request" in {
            val sessionId = "Session-ID"
            val journeyType = "Journey Type"

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            setupMockGetCurrentTaxYear(TaxYear(2024, 2025))
            setupMockGetMongo(Right(Some(UIJourneySessionData(sessionId, journeyType))))
            setupMockSetMongoData(true)

            val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")

            val result: Future[Result] = controller.submit(isAgent)(fakeRequest.withFormUrlEncodedBody(
              "current-year-checkbox" -> "true",
              "next-year-checkbox" -> "true"
            ))

            status(result) shouldBe OK
          }
        }
        "return 400 BadRequest" when {
          "user submits an invalid request" in {
            val sessionId = "Session-ID"
            val journeyType = "Journey Type"

            setupMockSuccess(mtdRole)
            setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)
            setupMockGetCurrentTaxYear(TaxYear(2024, 2025))
            setupMockGetMongo(Right(Some(UIJourneySessionData(sessionId, journeyType))))
            setupMockSetMongoData(true)

            val result: Future[Result] = controller.submit(isAgent)(fakeRequestWithActiveSession)

            status(result) shouldBe BAD_REQUEST
          }
        }
        "return 500 InternalServerError" when {
          "the request fails" in {
            val sessionId = "Session-ID"
            val journeyType = "Journey Type"

            setupMockSuccess(mtdRole)
            mockSingleBusinessIncomeSourceError()
            setupMockGetCurrentTaxYear(TaxYear(2024, 2025))
            setupMockGetMongo(Right(Some(UIJourneySessionData(sessionId, journeyType))))
            setupMockSetMongoData(true)

            val result: Future[Result] = controller.submit(isAgent)(fakeRequestWithActiveSession)

            status(result) shouldBe INTERNAL_SERVER_ERROR
          }
        }
      }
    }
  }
}

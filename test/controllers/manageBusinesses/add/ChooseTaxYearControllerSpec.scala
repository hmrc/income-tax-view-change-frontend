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

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import mocks.services.{MockDateService, MockIncomeSourceRFService, MockSessionService}
import models.UIJourneySessionData
import models.incomeSourceDetails.{IncomeSourceReportingFrequencySourceData, TaxYear}
import play.api
import play.api.Application
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.mvc.Result
import play.api.test.Helpers.{defaultAwaitTimeout, status}
import services.manageBusinesses.IncomeSourceRFService
import services.{DateService, SessionService}

import scala.concurrent.Future

class ChooseTaxYearControllerSpec extends MockAuthActions with MockDateService with MockSessionService with MockIncomeSourceRFService {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[IncomeSourceRFService].toInstance(mockIncomeSourceRFService),
      api.inject.bind[DateService].toInstance(mockDateService),
      api.inject.bind[SessionService].toInstance(mockSessionService)
    ).build()

  lazy val controller = app.injector.instanceOf[ChooseTaxYearController]

  val incomeSourceTypes: Seq[IncomeSourceType] = List(SelfEmployment, UkProperty, ForeignProperty)

  val displayList = List((true, true), (true, false), (false, true), (false, false))

  def setupMockIncomeSourceDetailsCall(incomeSourceType: IncomeSourceType): Unit = incomeSourceType match {
    case UkProperty => mockUKPropertyIncomeSource()
    case ForeignProperty => mockForeignPropertyIncomeSource()
    case SelfEmployment => mockSingleBusinessIncomeSource()
  }

  mtdAllRoles.foreach { mtdRole =>
    val isAgent = mtdRole != MTDIndividual
    incomeSourceTypes.foreach { incomeSourceType =>
      s".show($isAgent, $incomeSourceType)" when {
        s"the user is authenticated as a $mtdRole" should {
          "return 200 OK" when {
            displayList.foreach { displayYears =>
              s"$displayYears - isChange" in {
                setupMockSuccess(mtdRole)
                setupMockIncomeSourceDetailsCall(incomeSourceType)
                setupMockGetCurrentTaxYear(TaxYear(2024, 2025))
                setupMockGetCurrentTaxYearEnd(2025)
                mockRedirectChecksForIncomeSourceRF()
                setupMockGetMongo(Right(Some(UIJourneySessionData("", "", incomeSourceReportingFrequencyData = Some(IncomeSourceReportingFrequencySourceData(displayYears._1, displayYears._2, true, true))))))

                val result: Future[Result] = controller.show(isAgent, isChange = true, incomeSourceType)(fakeGetRequestBasedOnMTDUserType(mtdRole))

                status(result) shouldBe OK
              }
            }

            s"no change" in {
              setupMockSuccess(mtdRole)
              setupMockIncomeSourceDetailsCall(incomeSourceType)
              setupMockGetCurrentTaxYear(TaxYear(2024, 2025))
              mockRedirectChecksForIncomeSourceRF()
              setupMockGetMongo(Right(Some(UIJourneySessionData("", ""))))

              val result: Future[Result] = controller.show(isAgent, isChange = false, incomeSourceType)(fakeGetRequestBasedOnMTDUserType(mtdRole))

              status(result) shouldBe OK
            }
          }

          "return 500 internalServerError" when {
            "the request fails" in {
              setupMockSuccess(mtdRole)
              mockSingleBusinessIncomeSourceError()

              val result: Future[Result] = controller.show(isAgent, false, incomeSourceType)(fakeGetRequestBasedOnMTDUserType(mtdRole))

              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      s".submit($isAgent, $incomeSourceType)" when {
        s"the user is authenticated as a $mtdRole" should {
          "return 303 SEE_OTHER" when {
            s"user submits a valid request" in {
              val sessionId = "Session-ID"
              val journeyType = "Journey Type"

              setupMockSuccess(mtdRole)
              setupMockIncomeSourceDetailsCall(incomeSourceType)
              setupMockGetCurrentTaxYear(TaxYear(2024, 2025))
              setupMockGetMongo(Right(Some(UIJourneySessionData(sessionId, journeyType))))
              setupMockSetMongoData(true)

              val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole).withMethod("POST")

              val result: Future[Result] = controller.submit(isAgent, false, incomeSourceType)(fakeRequest.withFormUrlEncodedBody(
                "current-year-checkbox" -> "true",
                "next-year-checkbox" -> "true"
              ))

              status(result) shouldBe SEE_OTHER
            }
          }
          "return 400 BadRequest" when {
            "user submits an invalid request" in {
              val sessionId = "Session-ID"
              val journeyType = "Journey Type"

              setupMockSuccess(mtdRole)
              setupMockIncomeSourceDetailsCall(incomeSourceType)
              setupMockGetCurrentTaxYear(TaxYear(2024, 2025))
              setupMockGetMongo(Right(Some(UIJourneySessionData(sessionId, journeyType))))
              setupMockSetMongoData(true)

              val result: Future[Result] = controller.submit(isAgent, false, incomeSourceType)(fakeGetRequestBasedOnMTDUserType(mtdRole))

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

              val result: Future[Result] = controller.submit(isAgent, false, incomeSourceType)(fakeGetRequestBasedOnMTDUserType(mtdRole))

              status(result) shouldBe INTERNAL_SERVER_ERROR
            }
          }
        }
      }
    }
  }
}

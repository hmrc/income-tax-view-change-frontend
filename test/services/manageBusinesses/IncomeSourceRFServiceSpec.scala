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

package services.manageBusinesses

import auth.MtdItUser
import authV2.AuthActionsSpecHelper
import authV2.AuthActionsTestData.{defaultMTDITUser, getMinimalMTDITUser}
import enums.IncomeSourceJourney.{AfterSubmissionPage, SelfEmployment}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockSessionService}
import models.admin.IncomeSourcesNewJourney
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus.Annual
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.mvc.{Result, Results}
import services.optIn.OptInServiceSpec.statusDetailWith
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{addedIncomeSourceUIJourneySessionData, completedUIJourneySessionData, emptyUIJourneySessionData, noIncomeDetails, notCompletedUIJourneySessionData, singleBusinessIncomeWithLatency2019}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class IncomeSourceRFServiceSpec extends TestSupport
  with MockSessionService
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with AuthActionsSpecHelper {

  val incomeSourceRFService = new IncomeSourceRFService(
    mockSessionService,
    mockITSAStatusService,
    mockCalculationListService,
    mockItvcErrorHandler,
    mockAgentErrorHandler,
    mockDateService,
    mockAppConfig
  )

  val incomeSourceType = SelfEmployment
  val incomeSourceJourneyType = IncomeSourceJourneyType(Add, incomeSourceType)
  val journeyState = AfterSubmissionPage
  val currentTaxYearEnd = mockDateService.getCurrentTaxYearEnd
  val isAgent = false
  val isChange = false
  def journeySessionCodeBlock: UIJourneySessionData => Future[Result] = (_) => Future.successful(Results.Ok("Successful"))

  val forYearEnd = 2023
  val currentTaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear = currentTaxYear.nextYear

  val testUser: MtdItUser[_] = defaultMTDITUser(Some(Individual), singleBusinessIncomeWithLatency2019)
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  override def beforeEach(): Unit = {
    super.beforeEach()
    setupMockGetCurrentTaxYear(TaxYear(2025, 2026))
    setupMockGetCurrentTaxYearEnd(2023)
  }

  "redirectChecksForIncomeSourceRF" should {
    "redirect to the income source added page" when {
      "business latency status is unknown" in {

      }
      "business is annual for CY and the status reason for CY is Rollover" in {
        setupMockGetMongo(Right(Some(notCompletedUIJourneySessionData(incomeSourceJourneyType))))
        enable(IncomeSourcesNewJourney)

        when(mockITSAStatusService.getStatusTillAvailableFutureYears(any())(any(), any(), any()))
          .thenReturn(Future.successful(
            Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Annual))
          ))

        setupMockGetCurrentTaxYearEnd(2023)
        setupMockGetCurrentTaxYear(TaxYear(2025, 2026))

        when(mockDateService.getCurrentTaxYear).thenReturn(TaxYear(2025, 2056))
        when(mockDateService.getNextTaxYear).thenReturn(TaxYear(2026, 2027))


        when(mockCalculationListService.determineTaxYearCrystallised(ArgumentMatchers.anyInt())(any(), any())).thenReturn(Future.successful(false))

        val result = incomeSourceRFService.redirectChecksForIncomeSourceRF(
          incomeSourceJourneyType,
          journeyState,
          incomeSourceType,
          currentTaxYearEnd,
          isAgent,
          isChange
        )(journeySessionCodeBlock)(testUser, hc)

        result.value shouldBe ""

      }
    }
    "redirect to reporting frequency page" when {
      "business is in latency for CY & CY+1 and CY is crystallised" in {

      }
      "business is in latency for CY & CY+1 and CY is NOT crystallised" in {

      }
      "business is annual for CY and the status reason is NOT Rollover" in {

      }
      "business is annual for CY and mandated or voluntary for CY+1" in {

      }
      "business is mandated/voluntary for CY and annual for CY+1" in {

      }
    }
  }



}
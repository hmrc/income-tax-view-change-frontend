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

package services.optout

import auth.MtdItUser
import connectors.optout.ITSAStatusUpdateConnector
import connectors.optout.OptOutUpdateRequestModel.OptOutUpdateResponseSuccess
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockITSAStatusUpdateConnector}
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.{ITSAStatus, StatusDetail}
import models.optout.{OptOutMultiYearViewModel, OptOutOneYearViewModel, OptOutSessionData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.BeforeAndAfter
import repositories.UIJourneySessionDataRepository
import services.NextUpdatesService
import testUtils.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.OptOutJourney

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OptOutServiceNextUpdatesPageOptOutViewModelSpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with MockITSAStatusUpdateConnector {

  val optOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val nextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  val repository: UIJourneySessionDataRepository = mock(classOf[UIJourneySessionDataRepository])

  val service: OptOutService = new OptOutService(optOutConnector, mockITSAStatusService, mockCalculationListService,
    nextUpdatesService, mockDateService, repository)

  implicit val user: MtdItUser[_] = mock(classOf[MtdItUser[_]])
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  val sessionIdValue = "123"

  "OptOutService.makeOptOutUpdateRequest" when {

    "one year available for opt-out; end-year 2023" should {
      "return successful response" in {

        val correlationId = "123"
        val taxableEntityId = "456"
        val currentYearNum = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYearNum)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear
        val nextTaxYear: TaxYear = currentTaxYear.nextYear

        when(user.nino).thenReturn(taxableEntityId)

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          previousTaxYear -> StatusDetail("", ITSAStatus.Voluntary, ""),
          currentTaxYear -> StatusDetail("", ITSAStatus.Mandated, ""),
          nextTaxYear -> StatusDetail("", ITSAStatus.Mandated, ""),
        )
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousTaxYear)).thenReturn(Future.successful(taxYearStatusDetailMap))
        when(mockCalculationListService.isTaxYearCrystallised(previousTaxYear)).thenReturn(Future.successful(false))

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val optOutSessionData = OptOutSessionData(Some(previousTaxYear.toString))
        val sessionData = Some(UIJourneySessionData(sessionIdValue, OptOutJourney.Name, optOutSessionData = Some(optOutSessionData)))
        when(repository.get(any(), any())).thenReturn(Future.successful(sessionData))

        when(optOutConnector.requestOptOutForTaxYear(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))

        val result = service.getOptOutViewModel()

        result.futureValue.get.isInstanceOf[OptOutOneYearViewModel] shouldBe true
      }
    }

    "three years available for opt-out; end-year 2023, 2024, 2025" should {
      "return successful response" in {

        val correlationId = "123"
        val taxableEntityId = "456"
        val currentYearNum = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYearNum)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear
        val nextTaxYear: TaxYear = currentTaxYear.nextYear

        when(user.nino).thenReturn(taxableEntityId)

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          previousTaxYear -> StatusDetail("", ITSAStatus.Voluntary, ""),
          currentTaxYear -> StatusDetail("", ITSAStatus.Voluntary, ""),
          nextTaxYear -> StatusDetail("", ITSAStatus.Voluntary, ""),
        )
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousTaxYear)).thenReturn(Future.successful(taxYearStatusDetailMap))
        when(mockCalculationListService.isTaxYearCrystallised(previousTaxYear)).thenReturn(Future.successful(false))

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val optOutSessionData = OptOutSessionData(Some(previousTaxYear.toString))
        val sessionData = Some(UIJourneySessionData(sessionIdValue, OptOutJourney.Name, optOutSessionData = Some(optOutSessionData)))
        when(repository.get(any(), any())).thenReturn(Future.successful(sessionData))

        when(optOutConnector.requestOptOutForTaxYear(any(), any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))

        val result = service.getOptOutViewModel()

        result.futureValue.get.isInstanceOf[OptOutMultiYearViewModel] shouldBe true
      }
    }
  }
}
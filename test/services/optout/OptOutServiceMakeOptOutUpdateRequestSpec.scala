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

import audit.AuditingService
import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockITSAStatusUpdateConnector}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.BeforeAndAfter
import play.api.http.Status.NO_CONTENT
import repositories.OptOutSessionDataRepository
import services.NextUpdatesService
import services.optout.OptOutProposition.createOptOutProposition
import testUtils.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OptOutServiceMakeOptOutUpdateRequestSpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with MockITSAStatusUpdateConnector {

  val optOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val nextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  val repository: OptOutSessionDataRepository = mock(classOf[OptOutSessionDataRepository])
  val auditingService: AuditingService = mock(classOf[AuditingService])

  val service: OptOutService = new OptOutService(optOutConnector, mockITSAStatusService, mockCalculationListService,
    nextUpdatesService, mockDateService, repository, auditingService)

  implicit val user: MtdItUser[_] = mock(classOf[MtdItUser[_]])
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  val sessionIdValue = "123"

  "OptOutService.makeOptOutUpdateRequest" when {

    "one year available for opt-out; end-year 2023" should {
      "return successful response" in {

        val taxableEntityId = "456"
        val currentYearNum = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYearNum)

        when(user.nino).thenReturn(taxableEntityId)

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))

        when(repository.recallOptOutProposition()).thenReturn(
          Future.successful(Some(
            createOptOutProposition(
              currentYear = currentTaxYear,
              previousYearCrystallised = false,
              previousYearItsaStatus = Voluntary,
              currentYearItsaStatus = Mandated,
              nextYearItsaStatus = Mandated))))

        when(optOutConnector.optOut(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(
          ITSAStatusUpdateResponseSuccess()
        ))

        val result = service.makeOptOutUpdateRequest()

        result.futureValue shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)
      }
    }

    "three years available for opt-out; end-year 2023, 2024, 2025" should {
      "return successful response" in {

        val taxableEntityId = "456"
        val currentYearNum = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYearNum)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear

        when(user.nino).thenReturn(taxableEntityId)

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))

        when(repository.recallOptOutProposition()).thenReturn(
          Future.successful(Some(
            createOptOutProposition(
              currentYear = currentTaxYear,
              previousYearCrystallised = false,
              previousYearItsaStatus = Voluntary,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Voluntary))))

        when(repository.fetchSavedIntent()).thenReturn(Future.successful(Some(previousTaxYear)))

        when(optOutConnector.optOut(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(
          ITSAStatusUpdateResponseSuccess()
        ))

        val result = service.makeOptOutUpdateRequest()

        result.futureValue shouldBe ITSAStatusUpdateResponseSuccess(204)
      }
    }

    "three years available for opt-out; end-year 2023, 2024, 2025" should {
      "return fail response" in {

        val taxableEntityId = "456"
        val currentYearNum = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYearNum)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear

        when(user.nino).thenReturn(taxableEntityId)

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        when(repository.fetchSavedIntent()).thenReturn(Future.successful(Some(previousTaxYear)))

        when(repository.recallOptOutProposition()).thenReturn(
          Future.successful(Some(
            createOptOutProposition(
              currentYear = currentTaxYear,
              previousYearCrystallised = false,
              previousYearItsaStatus = Voluntary,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Voluntary))))

        when(optOutConnector.optOut(any(), any())(any[HeaderCarrier])).thenReturn(Future.successful(
          ITSAStatusUpdateResponseFailure.defaultFailure()
        ))

        val result = service.makeOptOutUpdateRequest()

        result.futureValue.isInstanceOf[ITSAStatusUpdateResponseFailure] shouldBe true
      }
    }


  }

}

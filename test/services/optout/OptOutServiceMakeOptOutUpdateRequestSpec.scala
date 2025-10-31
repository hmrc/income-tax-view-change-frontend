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
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ErrorItem, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockITSAStatusUpdateConnector}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalatest.BeforeAndAfter
import play.api.http.Status.NO_CONTENT
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.OptOutSessionDataRepository
import services.NextUpdatesService
import services.optout.OptOutProposition.createOptOutProposition
import testUtils.TestSupport
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class OptOutServiceMakeOptOutUpdateRequestSpec
  extends TestSupport
    with BeforeAndAfter
    with MockITSAStatusService
    with MockCalculationListService
    with MockDateService
    with MockITSAStatusUpdateConnector {

  val optOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val nextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  val repository: OptOutSessionDataRepository = mock(classOf[OptOutSessionDataRepository])
  val auditingService: AuditingService = mock(classOf[AuditingService])

  val service: OptOutService =
    new OptOutService(optOutConnector, mockITSAStatusService, mockCalculationListService, nextUpdatesService, mockDateService, repository, auditingService)

  implicit val user: MtdItUser[_] = tsTestUser

  val sessionIdValue = "123"

  "OptOutService.makeOptOutUpdateRequest" when {

    "one year available for opt-out; end-year 2023" should {
      "return successful response" in {

        val currentYearNum = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYearNum)

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        when(repository.recallOptOutPropositionWithIntent()).thenReturn(
          Future(Some(
            createOptOutProposition(
              currentYear = currentTaxYear,
              previousYearCrystallised = false,
              previousYearItsaStatus = Voluntary,
              currentYearItsaStatus = Mandated,
              nextYearItsaStatus = Mandated), None)))

        when(optOutConnector.optOut(any(), any())(any[HeaderCarrier]))
          .thenReturn(
            Future(
              ITSAStatusUpdateResponseSuccess()
            ))

        val result = service.makeOptOutUpdateRequest()

        result.futureValue shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)
      }
    }

    "make opt-out update request for one year, PY is offered" should {

      "successful update request was made" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear

        when(repository.recallOptOutPropositionWithIntent())
          .thenReturn(Future(Some(
            createOptOutProposition(currentTaxYear, false, ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus), None))
          )

        when(optOutConnector.optOut(previousTaxYear, taxableEntityId))
          .thenReturn(Future(
            ITSAStatusUpdateResponseSuccess()
          ))

        val result = service.makeOptOutUpdateRequest()

        result.futureValue shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)
      }
    }

    "three years available for opt-out; end-year 2023, 2024, 2025" should {

      "return successful response" in {

        val currentYearNum = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYearNum)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        when(repository.recallOptOutPropositionWithIntent()).thenReturn(
          Future(Some(
            createOptOutProposition(
              currentYear = currentTaxYear,
              previousYearCrystallised = false,
              previousYearItsaStatus = Voluntary,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Voluntary), Some(previousTaxYear))))

        when(optOutConnector.optOut(any(), any())(any[HeaderCarrier]))
          .thenReturn(
            Future(ITSAStatusUpdateResponseSuccess())
          )

        val result = service.makeOptOutUpdateRequest()

        await(result) shouldBe ITSAStatusUpdateResponseSuccess(204)
      }
    }

    "three years available for opt-out; end-year 2023, 2024, 2025" should {

      "return fail response" in {

        val currentYearNum = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYearNum)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        when(repository.recallOptOutPropositionWithIntent()).thenReturn(
          Future(Some(
            createOptOutProposition(
              currentYear = currentTaxYear,
              previousYearCrystallised = false,
              previousYearItsaStatus = Voluntary,
              currentYearItsaStatus = Voluntary,
              nextYearItsaStatus = Voluntary), Some(previousTaxYear)))
        )

        when(optOutConnector.optOut(any(), any())(any[HeaderCarrier]))
          .thenReturn(
            Future(ITSAStatusUpdateResponseFailure.defaultFailure())
          )

        val result = service.makeOptOutUpdateRequest()

        await(result) shouldBe
          ITSAStatusUpdateResponseFailure(List(ErrorItem("INTERNAL_SERVER_ERROR", "Request failed due to unknown reason")))
      }
    }

    "make opt-out update request for multi year, PY, CY, NY is offered and one fails" should {

      "successful update request was made" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear
        val nextTaxYear: TaxYear = currentTaxYear.nextYear

        when(optOutConnector.optOut(previousTaxYear, taxableEntityId)).thenReturn(Future.successful(
          ITSAStatusUpdateResponseFailure.defaultFailure()
        ))
        when(optOutConnector.optOut(currentTaxYear, taxableEntityId)).thenReturn(Future.successful(
          ITSAStatusUpdateResponseSuccess()
        ))
        when(optOutConnector.optOut(nextTaxYear, taxableEntityId)).thenReturn(Future.successful(
          ITSAStatusUpdateResponseSuccess()
        ))

        when(repository.recallOptOutPropositionWithIntent()).thenReturn(Future.successful(Some(
          createOptOutProposition(currentTaxYear, false, ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus), None)))

        val result = service.makeOptOutUpdateRequest()

        result.futureValue shouldBe ITSAStatusUpdateResponseFailure.defaultFailure()
      }
    }
  }
}

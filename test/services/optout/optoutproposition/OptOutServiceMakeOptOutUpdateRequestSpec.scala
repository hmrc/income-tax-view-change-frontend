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

package services.optout.optoutproposition

import auth.MtdItUser
import connectors.optout.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess, optOutUpdateReason}
import connectors.optout.{ITSAStatusUpdateConnector, ITSAStatusUpdateConnectorModel}
import mocks.services._
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfter
import play.mvc.Http.Status.NO_CONTENT
import repositories.OptOutSessionDataRepository
import services.optout.OptOutProposition.createOptOutProposition
import services.optout.OptOutService
import services.{CalculationListService, DateServiceInterface, ITSAStatusService, NextUpdatesService}
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
  val itsaStatusService: ITSAStatusService = mockITSAStatusService
  val calculationListService: CalculationListService = mockCalculationListService
  val nextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  val dateService: DateServiceInterface = mockDateService
  val repository: OptOutSessionDataRepository = mock(classOf[OptOutSessionDataRepository])
  val service = new OptOutService(optOutConnector, itsaStatusService, calculationListService, nextUpdatesService, dateService, repository)

  implicit val user: MtdItUser[_] = mock(classOf[MtdItUser[_]])
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  val sessionIdValue = "123"

  before {
    reset(optOutConnector, itsaStatusService, calculationListService, dateService, user, hc)
  }

  "OptOutService.makeOptOutUpdateRequest" when {

    "make opt-out update request for one year, PY is offered" should {

      "successful update request was made" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear

        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.makeITSAStatusUpdate(previousTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          ITSAStatusUpdateResponseSuccess()
        ))

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        when(repository.recallOptOutProposition()).thenReturn(Future.successful(Some(
          createOptOutProposition(currentTaxYear, false, ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus))))

        val result = service.makeOptOutUpdateRequest()

        result.futureValue shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)
      }
    }

    "make opt-out update request for multi year, PY, CY, NY is offered" should {

      "successful update request was made" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear
        val nextTaxYear: TaxYear = currentTaxYear.nextYear

        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.makeITSAStatusUpdate(previousTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          ITSAStatusUpdateResponseSuccess()
        ))
        when(optOutConnector.makeITSAStatusUpdate(currentTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          ITSAStatusUpdateResponseSuccess()
        ))
        when(optOutConnector.makeITSAStatusUpdate(nextTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          ITSAStatusUpdateResponseSuccess()
        ))

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        when(repository.recallOptOutProposition()).thenReturn(Future.successful(Some(
          createOptOutProposition(currentTaxYear, false, ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus))))


        val result = service.makeOptOutUpdateRequest()

        result.futureValue shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)
      }
    }

    "make opt-out update request for multi year, PY, CY, NY is offered and one fails" should {

      "successful update request was made" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear
        val nextTaxYear: TaxYear = currentTaxYear.nextYear

        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.makeITSAStatusUpdate(previousTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          ITSAStatusUpdateResponseFailure.defaultFailure()
        ))
        when(optOutConnector.makeITSAStatusUpdate(currentTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          ITSAStatusUpdateResponseSuccess()
        ))
        when(optOutConnector.makeITSAStatusUpdate(nextTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          ITSAStatusUpdateResponseSuccess()
        ))

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        when(repository.recallOptOutProposition()).thenReturn(Future.successful(Some(
          createOptOutProposition(currentTaxYear, false, ITSAStatus.Voluntary, ITSAStatus.NoStatus, ITSAStatus.NoStatus))))

        val result = service.makeOptOutUpdateRequest()

        result.futureValue shouldBe ITSAStatusUpdateResponseFailure.defaultFailure()
      }
    }

  }
}

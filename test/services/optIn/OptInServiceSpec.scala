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

package services.optIn

import auth.MtdItUser
import connectors.optout.ITSAStatusUpdateConnector
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockITSAStatusUpdateConnector}
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Voluntary}
import models.itsaStatus.StatusDetail
import models.optin.{OptInContextData, OptInSessionData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, OneInstancePerTest}
import repositories.UIJourneySessionDataRepository
import services.NextUpdatesService
import services.optIn.OptInServiceSpec.statusDetailWith
import testUtils.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.OptInJourney

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object OptInServiceSpec {
  def statusDetailWith(status: ITSAStatus): StatusDetail = {
    StatusDetail("", status = status, statusReason = "", businessIncomePriorTo2Years = None)
  }
}

class OptInServiceSpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with OneInstancePerTest
  with MockITSAStatusUpdateConnector {

  implicit val user: MtdItUser[_] = mock(classOf[MtdItUser[_]])
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  val optOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val nextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  val repository: UIJourneySessionDataRepository = mock(classOf[UIJourneySessionDataRepository])

  val service: OptInService = new OptInService(optOutConnector, mockITSAStatusService,
    mockCalculationListService, nextUpdatesService, mockDateService, repository)

  val forYearEnd = 2023
  val currentTaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear = currentTaxYear.nextYear

  override def beforeEach(): Unit = {

    reset(hc)
    reset(repository)

    when(hc.sessionId).thenReturn(Some(SessionId("123")))
    when(repository.set(any())).thenReturn(Future.successful(true))
  }

  "OptInService.saveIntent" should {
    "save selectedOptInYear in session data" in {

      val data = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = OptInJourney.Name,
        optInSessionData = Some(OptInSessionData(None, selectedOptInYear = None))
      )
      when(repository.get(hc.sessionId.get.value, OptInJourney.Name)).thenReturn(Future.successful(Some(data)))

      val dataAfter = data.copy(optInSessionData = Some(data.optInSessionData.get.copy(selectedOptInYear = Some(currentTaxYear.toString))))
      when(repository.set(dataAfter)).thenReturn(Future.successful(true))

      val result = service.saveIntent(currentTaxYear)
      result.futureValue shouldBe true

      verify(repository).set(dataAfter)
    }
  }

  "OptInService.saveIntent and no session data" should {
    "save selectedOptInYear in session data" in {

      when(repository.get(hc.sessionId.get.value, OptInJourney.Name)).thenReturn(Future.successful(None))

      val result = service.saveIntent(currentTaxYear)
      result.futureValue shouldBe true

      verify(repository, times(0)).set(any())
    }
  }

  "OptInService.availableOptInTaxYear" when {

    "session data is not-saved" should {

      "return tax years ending 2023, 2024" in {

        when(repository.get(hc.sessionId.get.value, OptInJourney.Name)).thenReturn(Future.successful(None))
        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(currentTaxYear))
          .thenReturn(Future.successful(
            Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Annual))
          ))

        val result = service.availableOptInTaxYear()
        result.futureValue shouldBe Seq(currentTaxYear, nextTaxYear)
      }

      "return tax year ending 2023" in {

        when(repository.get(hc.sessionId.get.value, OptInJourney.Name)).thenReturn(Future.successful(None))
        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(currentTaxYear))
          .thenReturn(Future.successful(
            Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Voluntary))
          ))

        val result = service.availableOptInTaxYear()
        result.futureValue shouldBe Seq(currentTaxYear)
      }

    }

    "session data is saved" should {

      "return tax years ending 2023, 2024" in {

        //mockRepository(Annual, Annual)
        mockRepository(Some(OptInContextData(currentTaxYear.toString, OptInContextData.statusToString(Annual), nextTaxYear.toString, OptInContextData.statusToString(Annual))))
        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        val result = service.availableOptInTaxYear()
        result.futureValue shouldBe Seq(currentTaxYear, nextTaxYear)

        verify(mockITSAStatusService, times(0)).getStatusTillAvailableFutureYears(any())(any(), any(), any())
      }

      "return tax year ending 2023" in {

        mockRepository(Some(OptInContextData(currentTaxYear.toString, OptInContextData.statusToString(Annual), nextTaxYear.toString, OptInContextData.statusToString(Voluntary))))
        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        val result = service.availableOptInTaxYear()
        result.futureValue shouldBe Seq(currentTaxYear)

        verify(mockITSAStatusService, times(0)).getStatusTillAvailableFutureYears(any())(any(), any(), any())
      }
    }
  }

  "OptInService.fetchSavedChosenTaxYear" should {

    "fetch saved choice when there is choice saved" in {

      mockRepository(selectedOptInYear = Some(TaxYear.forYearEnd(2023).toString))
      val result = service.fetchSavedChosenTaxYear()
      val choice = result.futureValue.get

      choice shouldBe TaxYear.forYearEnd(2023)
    }

    "fetch saved choice when there is no choice saved" in {

      mockRepository()
      val result = service.fetchSavedChosenTaxYear()
      val choice = result.futureValue

      choice shouldBe None
    }
  }

  def mockRepository(optInContextData: Option[OptInContextData] = None, selectedOptInYear: Option[String] = None): Unit = {

    val sessionData = UIJourneySessionData(hc.sessionId.get.value, OptInJourney.Name,
      optInSessionData = Some(OptInSessionData(optInContextData, selectedOptInYear)))

    when(repository.get(hc.sessionId.get.value, OptInJourney.Name)).thenReturn(Future.successful(Some(sessionData)))
  }
}
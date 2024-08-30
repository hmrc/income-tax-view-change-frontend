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
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel._
import controllers.optIn.routes.ReportingFrequencyPageController
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockITSAStatusUpdateConnector}
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Voluntary}
import models.itsaStatus.StatusDetail
import models.optin.{MultiYearCheckYourAnswersViewModel, OptInContextData, OptInSessionData}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.{BeforeAndAfter, OneInstancePerTest}
import repositories.ITSAStatusRepositorySupport._
import repositories.UIJourneySessionDataRepository
import services.NextUpdatesService
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.optIn.OptInServiceSpec.statusDetailWith
import services.optIn.core.{CurrentOptInTaxYear, NextOptInTaxYear, OptInProposition}
import services.reportingfreq.ReportingFrequency.QuarterlyUpdatesCountForTaxYearModel
import testUtils.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.OptInJourney

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

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

      val jsd = UIJourneySessionData(hc.sessionId.get.value, OptInJourney.Name)

      when(repository.get(hc.sessionId.get.value, OptInJourney.Name)).thenReturn(Future.successful(Some(jsd)))

      val result = service.saveIntent(currentTaxYear)
      result.futureValue shouldBe true

      verify(repository, times(1)).set(jsd)
    }
  }

  "OptInService.availableOptInTaxYear" when {

    "session data is not-saved" should {

      "return tax years ending 2023, 2024" in {

        when(repository.get(hc.sessionId.get.value, OptInJourney.Name)).thenReturn(Future.successful(None))
        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(currentTaxYear.previousYear))
          .thenReturn(Future.successful(
            Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Annual))
          ))

        val result = service.availableOptInTaxYear()
        result.futureValue shouldBe Seq(currentTaxYear, nextTaxYear)
      }

      "return tax year ending 2023" in {

        when(repository.get(hc.sessionId.get.value, OptInJourney.Name)).thenReturn(Future.successful(None))
        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(currentTaxYear.previousYear))
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
        mockRepository(Some(OptInContextData(currentTaxYear.toString, statusToString(Annual), statusToString(Annual))))
        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        val result = service.availableOptInTaxYear()
        result.futureValue shouldBe Seq(currentTaxYear, nextTaxYear)

        verify(mockITSAStatusService, times(0)).getStatusTillAvailableFutureYears(any())(any(), any(), any())
      }

      "return tax year ending 2023" in {

        mockRepository(Some(OptInContextData(currentTaxYear.toString, statusToString(Annual), statusToString(Voluntary))))
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

  "OptInService.makeOptInCall" should {

    "success response case" in {

      mockRepository(selectedOptInYear = Some(currentTaxYear.toString))

      when(optOutConnector.optIn(any(), any())(any()))
        .thenReturn(Future.successful(ITSAStatusUpdateResponseSuccess()))

      val result = service.makeOptInCall()(user, hc, executionContext()).futureValue

      result.isInstanceOf[ITSAStatusUpdateResponseSuccess] shouldBe true
    }

    "fail response case" in {

      mockRepository(selectedOptInYear = Some(currentTaxYear.toString))

      when(optOutConnector.optIn(any(), any())(any()))
        .thenReturn(Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure()))

      val result = service.makeOptInCall()(user, hc, executionContext()).futureValue

      result.isInstanceOf[ITSAStatusUpdateResponseFailure] shouldBe true
    }

    "fail where missing intent tax-year case" in {

      mockRepository(selectedOptInYear = None)

      when(optOutConnector.makeITSAStatusUpdate(any(), any(), any())(any()))
        .thenReturn(Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure()))

      val result = service.makeOptInCall()(user, hc, executionContext()).futureValue

      result.isInstanceOf[ITSAStatusUpdateResponseFailure] shouldBe true
    }

  }

  "OptInService.cumulativeQuarterlyUpdateCounts" should {

    "for proposition with opt-in for only next-tax-year" in {

      val currentOptInTaxYear = CurrentOptInTaxYear(Voluntary, currentTaxYear)
      val nextOptInTaxYear = NextOptInTaxYear(Annual, nextTaxYear, currentOptInTaxYear)
      val proposition = OptInProposition(currentOptInTaxYear, nextOptInTaxYear)

      val result = service.getQuarterlyUpdatesCountForOfferedYears(proposition)

      result.futureValue shouldBe QuarterlyUpdatesCountForTaxYearModel(Seq(QuarterlyUpdatesCountForTaxYear(nextTaxYear, 0)))
    }

    "for proposition with opt-in for current-tax-year" in {

      val currentOptInTaxYear = CurrentOptInTaxYear(Annual, currentTaxYear)
      val nextOptInTaxYear = NextOptInTaxYear(Voluntary, nextTaxYear, currentOptInTaxYear)
      val proposition = OptInProposition(currentOptInTaxYear, nextOptInTaxYear)

      when(nextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentOptInTaxYear.taxYear))(any(), any()))
        .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentOptInTaxYear.taxYear, 1)))

      val result = service.getQuarterlyUpdatesCountForOfferedYears(proposition)

      result.futureValue shouldBe QuarterlyUpdatesCountForTaxYearModel(Seq(QuarterlyUpdatesCountForTaxYear(currentOptInTaxYear.taxYear, 1)))
    }
  }

  "OptInService.getMultiYearCheckYourAnswersViewModel" should {

    "return model when intent is current tax-year" in {

      val isAgent = false
      val optInContext = Some(OptInContextData(currentTaxYear.toString, statusToString(Annual), statusToString(Annual)))
      mockRepository(optInContextData = optInContext, selectedOptInYear = Some(currentTaxYear.toString))
      when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

      when(nextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear))(any(), any())).thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear, 1)))

      val result = service.getMultiYearCheckYourAnswersViewModel(isAgent)

      result.futureValue.get shouldBe MultiYearCheckYourAnswersViewModel(
        intentTaxYear = currentTaxYear,
        isAgent = isAgent,
        cancelURL = ReportingFrequencyPageController.show(isAgent).url,
        intentIsNextYear = false
      )
    }

    "return model when intent is next tax-year" in {

      val isAgent = false
      val optInContext = Some(OptInContextData(currentTaxYear.toString, statusToString(Annual), statusToString(Annual)))
      mockRepository(optInContextData = optInContext, selectedOptInYear = Some(currentTaxYear.nextYear.toString))
      when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

      when(nextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear))(any(), any())).thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear, 1)))

      val result = service.getMultiYearCheckYourAnswersViewModel(isAgent)

      result.futureValue.get shouldBe MultiYearCheckYourAnswersViewModel(
        intentTaxYear = currentTaxYear.nextYear,
        isAgent = isAgent,
        cancelURL = ReportingFrequencyPageController.show(isAgent).url,
        intentIsNextYear = true
      )
    }

  }

  def executionContext()(implicit executionContext: ExecutionContext): ExecutionContext = executionContext

  def mockRepository(optInContextData: Option[OptInContextData] = None, selectedOptInYear: Option[String] = None): Unit = {

    val sessionData = UIJourneySessionData(hc.sessionId.get.value, OptInJourney.Name,
      optInSessionData = Some(OptInSessionData(optInContextData, selectedOptInYear)))

    when(repository.get(hc.sessionId.get.value, OptInJourney.Name)).thenReturn(Future.successful(Some(sessionData)))
  }
}
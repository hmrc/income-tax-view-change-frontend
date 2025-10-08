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

package repositories

import auth.MtdItUser
import connectors.itsastatus.ITSAStatusUpdateConnector
import enums.JourneyType.OptOutJourney
import mocks.services._
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import models.optout._
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, OneInstancePerTest, Succeeded}
import services.NextUpdatesService
import services.optout.OptOutProposition.createOptOutProposition
import services.optout.OptOutTestSupport._
import testUtils.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OptOutSessionDataRepositorySpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with OneInstancePerTest
  with MockITSAStatusUpdateConnector {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(5, Millis))

  val optOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val nextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  val repository: UIJourneySessionDataRepository = mock(classOf[UIJourneySessionDataRepository])

  implicit val user: MtdItUser[_] = mock(classOf[MtdItUser[_]])
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  val taxYear2022_2023 = TaxYear.forYearEnd(2023)
  val taxYear2023_2024 = taxYear2022_2023.nextYear
  val taxYear2024_2025 = taxYear2023_2024.nextYear

  val taxYear: TaxYear = TaxYear.forYearEnd(2021)
  val previousTaxYear: TaxYear = taxYear.previousYear
  val crystallised: Boolean = true

  val sessionIdValue = "123"
  val error = new RuntimeException("Some Error")


  val optOutRepository: OptOutSessionDataRepository = new OptOutSessionDataRepository(repository)

  val noOptOutOptionAvailable: Option[Nothing] = None

  val apiError: String = "some api error"

  "OptOutService.initialiseOptOutJourney" should {
    "write the opt out data and no customer intent to the journey repository" in {

      val forYearEnd = 2024

      when(hc.sessionId).thenReturn(Some(SessionId("123")))
      when(repository.set(any())).thenReturn(Future.successful(true))

      val f = for {
        isReset <- optOutRepository.initialiseOptOutJourney(buildOneYearOptOutPropositionForPreviousYear(forYearEnd))
      } yield {
        verify(repository).set(any())
        isReset shouldBe true
      }

      f.futureValue shouldBe Succeeded
    }
  }

  "OptOutService.saveIntent" should {
    "overwrite the customer intent, but preserve the Opt Out Data" in {

      val forYearEnd = 2024

      when(hc.sessionId).thenReturn(Some(SessionId("123")))
      when(repository.set(any())).thenReturn(Future.successful(true))

      val data = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = OptOutJourney.toString,
        optOutSessionData = Some(OptOutSessionData(None, selectedOptOutYear = None))
      )
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(data)))

      val f = for {
        isSaved <- optOutRepository.saveIntent(TaxYear.forYearEnd(forYearEnd))
      } yield {
        verify(repository).set(any())
        isSaved shouldBe true
      }

      f.futureValue shouldBe Succeeded
    }
  }

  "OptOutService.recallOptOutPropositionWithIntent" should {
    "retrieve the opt out proposition with no intent if the user hasn't selected a year" in {

      when(hc.sessionId).thenReturn(Some(SessionId("123")))

      val data = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = OptOutJourney.toString,
        optOutSessionData = Some(OptOutSessionData(Some(
          OptOutContextData(
            currentYear = "2023-2024",
            crystallisationStatus = true,
            previousYearITSAStatus = "V",
            currentYearITSAStatus = "V",
            nextYearITSAStatus = "A")),
          selectedOptOutYear = None))
      )

      when(repository.get(any(), any())).thenReturn(Future.successful(Some(data)))

      val initialState = optOutRepository.recallOptOutPropositionWithIntent()

      initialState.futureValue.isDefined shouldBe true
      initialState.futureValue.get shouldBe (
        createOptOutProposition(
          currentYear = taxYear2023_2024,
          previousYearCrystallised = true,
          previousYearItsaStatus = Voluntary,
          currentYearItsaStatus = Voluntary,
          nextYearItsaStatus = Annual
        ), None)
    }

    "retrieve the opt out proposition with an intent if a tax year has been selected" in {

      when(hc.sessionId).thenReturn(Some(SessionId("123")))

      val data = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = OptOutJourney.toString,
        optOutSessionData = Some(OptOutSessionData(Some(
          OptOutContextData(
            currentYear = "2023-2024",
            crystallisationStatus = true,
            previousYearITSAStatus = "V",
            currentYearITSAStatus = "V",
            nextYearITSAStatus = "A")),
          selectedOptOutYear = Some("2023-2024")))
      )

      when(repository.get(any(), any())).thenReturn(Future.successful(Some(data)))

      val initialState = optOutRepository.recallOptOutPropositionWithIntent()

      initialState.futureValue.isDefined shouldBe true
      initialState.futureValue.get shouldBe (
        createOptOutProposition(
          currentYear = taxYear2023_2024,
          previousYearCrystallised = true,
          previousYearItsaStatus = Voluntary,
          currentYearItsaStatus = Voluntary,
          nextYearItsaStatus = Annual
        ), Some(TaxYear(2023, 2024)))
    }
  }

  "OptOutService.fetchSavedIntent" should {
    "retrieve the intent year only" in {

      val forYearEnd = 2024
      val customerIntent = TaxYear.forYearEnd(forYearEnd)

      when(hc.sessionId).thenReturn(Some(SessionId("123")))

      val data = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = OptOutJourney.toString,
        optOutSessionData = Some(OptOutSessionData(None, selectedOptOutYear = Some(customerIntent.toString)))
      )
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(data)))

      val savedIntent = optOutRepository.fetchSavedIntent()

      savedIntent.futureValue.isDefined shouldBe true
      savedIntent.futureValue.get shouldBe customerIntent
    }
  }

}

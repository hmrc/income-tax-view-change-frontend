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

package obligations.repositories

import common.auth.MtdItUser
import common.enums.JourneyType.{Opt, SignUpJourney}
import common.mocks.services.MockDateService
import common.models.incomeSourceDetails.TaxYear
import common.testUtils.UnitSpec
import obligations.models.reportingObligations.optOut.OptOutSessionData
import obligations.models.reportingObligations.signUp.{SignUpContextData, SignUpSessionData}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, OneInstancePerTest}
import shared.models.UIJourneySessionData
import shared.repositories.UIJourneySessionDataRepository
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SignUpSessionDataRepositorySpec extends UnitSpec
  with BeforeAndAfter
  with MockDateService
  with OneInstancePerTest {

  override def beforeEach(): Unit = {
    reset(hc)
    reset(repository)
    when(hc.sessionId).thenReturn(Some(SessionId("123")))
    when(repository.set(any())).thenReturn(Future.successful(true))
  }

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(10, Seconds), interval = Span(5, Millis))
  val repository: UIJourneySessionDataRepository = mock(classOf[UIJourneySessionDataRepository])

  implicit val user: MtdItUser[_] = mock(classOf[MtdItUser[_]])
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  val forYearEnd = 2023
  val currentTaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear = currentTaxYear.nextYear
  val crystallised: Boolean = true

  val sessionIdValue = "123"
  val error = new RuntimeException("Some Error")
  val defaultUIJourneySessionData: UIJourneySessionData = UIJourneySessionData(
    sessionId = sessionIdValue,
    journeyType = Opt(SignUpJourney).toString,
    signUpSessionData = Some(SignUpSessionData(None, None, Some(false)))
  )

  val signupRepository: SignUpSessionDataRepository = new SignUpSessionDataRepository(repository) {
    override def createDefaultUIJourneySessionData()(implicit hc: HeaderCarrier): UIJourneySessionData = {
      defaultUIJourneySessionData
    }
  }

  val noOptOutOptionAvailable: Option[Nothing] = None

  val apiError: String = "some api error"

  ".saveIntent()" should {
    "save selectedOptInYear in session data" in {

      val data = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = Opt(SignUpJourney).toString,
        signUpSessionData = Some(SignUpSessionData(None, selectedSignUpYear = None))
      )

      when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney))).thenReturn(Future.successful(Some(data)))

      val dataAfter = data.copy(signUpSessionData = Some(data.signUpSessionData.get.copy(selectedSignUpYear = Some(currentTaxYear.toString))))
      when(repository.set(dataAfter)).thenReturn(Future.successful(true))

      val result = signupRepository.saveIntent(currentTaxYear)
      result.futureValue shouldBe true

      verify(repository).set(dataAfter)
    }
  }

  ".saveIntent() and no session data" should {
    "save selectedOptInYear in session data" in {

      val jsd = UIJourneySessionData(hc.sessionId.get.value, Opt(SignUpJourney).toString)

      when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney))).thenReturn(Future.successful(Some(jsd)))

      val result = signupRepository.saveIntent(currentTaxYear)
      result.futureValue shouldBe true

      verify(repository, times(1)).set(jsd)
    }
  }

  "fetchSigninSessionData" when {

    "there is data pre-existing in db" should {

      "return the correct session data" in {

        val optInContextData =
          SignUpContextData(
            currentTaxYear = "2025-2026",
            currentYearITSAStatus = "MTD Voluntary",
            nextYearITSAStatus = "MTD Voluntary"
          )

        val selectedOptInYear = Some("2025-2026")
        val optInSessionData = SignUpSessionData(Some(optInContextData), selectedSignUpYear = selectedOptInYear)

        val retrievedUiSessionData =
          UIJourneySessionData(
            sessionId = hc.sessionId.get.value,
            journeyType = Opt(SignUpJourney).toString,
            signUpSessionData = Some(optInSessionData)
          )

        when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney)))
          .thenReturn(Future(Some(retrievedUiSessionData)))

        val request = signupRepository.fetchSignUpSessionData()
        whenReady(request) { result => result shouldBe Some(optInSessionData) }
      }
    }

    "there is NO data present in db" should {
      "return None" in {

        when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney)))
          .thenReturn(Future(None))

        val request = signupRepository.fetchSignUpSessionData()
        whenReady(request) { result => result shouldBe None }
      }
    }
  }

  "setupSessionData" should {
    "write the initial session data to the repository" in {

      when(repository.set(defaultUIJourneySessionData)).thenReturn(Future.successful(true))

      val result = signupRepository.setupSessionData()
      result.futureValue shouldBe true

      verify(repository).set(defaultUIJourneySessionData)
    }
  }

  "setJourneyCompleteStatus" should {
    "update the journeyIsComplete flag in session data" in {

      val initialSessionData = defaultUIJourneySessionData

      val updatedSessionData = initialSessionData.copy(
        signUpSessionData = initialSessionData.signUpSessionData.map(_.copy(journeyIsComplete = Some(true)))
      )

      when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney))).thenReturn(Future.successful(Some(initialSessionData)))
      when(repository.set(updatedSessionData)).thenReturn(Future.successful(true))

      val result = signupRepository.setJourneyCompleteStatus(true)
      result.futureValue shouldBe true

      verify(repository).set(updatedSessionData)
    }

    "handle failure to update session data" in {

      val initialSessionData = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = Opt(SignUpJourney).toString,
        signUpSessionData = Some(SignUpSessionData(None, None, Some(false)))
      )

      val updatedSessionData = initialSessionData.copy(
        signUpSessionData = initialSessionData.signUpSessionData.map(_.copy(journeyIsComplete = Some(true)))
      )

      when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney))).thenReturn(Future.successful(Some(initialSessionData)))
      when(repository.set(updatedSessionData)).thenReturn(Future.successful(false))

      val result = signupRepository.setJourneyCompleteStatus(true)
      result.futureValue shouldBe false

      verify(repository).set(updatedSessionData)
    }
  }

  "setUpdatedSessionDataStatus" should {
    "write the provided session data to the repository" in {

      val sessionData = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = Opt(SignUpJourney).toString,
        signUpSessionData = Some(SignUpSessionData(None, None, Some(false)))
      )

      when(repository.set(sessionData)).thenReturn(Future.successful(true))

      val result = signupRepository.setUpdatedSessionDataStatus(sessionData)
      result.futureValue shouldBe true

      verify(repository).set(sessionData)
    }
  }
}
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

package obligations.services.reportingObligations.signUp

import enums.JourneyType.{Opt, SignUpJourney}
import mocks.services.{MockDateService, MockITSAStatusService}
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.*
import models.itsaStatus.{StatusDetail, StatusReason}
import obligations.models.reportingObligations.signUp.{SignUpContextData, SignUpSessionData, SignUpTaxYearQuestionViewModel}
import obligations.repositories.SignUpSessionDataRepository
import obligations.services.reportingObligations.signUp.core.{CurrentSignUpTaxYear, NextSignUpTaxYear, SignUpProposition}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfter, OneInstancePerTest}
import testUtils.{TestSupport, UnitSpec}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.{ExecutionContext, Future}

class SignUpServiceSpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockDateService
  with OneInstancePerTest
  with Eventually
  with TestSupport {

  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])
  val repository: SignUpSessionDataRepository = mock(classOf[SignUpSessionDataRepository])

  val service: SignUpService = new SignUpService(mockITSAStatusService, mockDateService, repository)

  val forYearEnd = 2023
  val currentTaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear = currentTaxYear.nextYear

  override def beforeEach(): Unit = {

    reset(hc)
    reset(repository)
    when(hc.sessionId).thenReturn(Some(SessionId("123")))
  }

  def statusDetailWith(status: ITSAStatus): StatusDetail = {
    StatusDetail("", status = status, statusReason = StatusReason.Rollover, businessIncomePriorTo2Years = None)
  }

  "saveIntent()" should {

    "save the intent to repository" in {

      when(repository.saveIntent(currentTaxYear)).thenReturn(Future.successful(true))

      val result = service.saveIntent(currentTaxYear)
      result.futureValue shouldBe true
    }
  }

  "availableSignUpTaxYear()" should {

    "return available tax years from the proposition" in {

      val proposition = SignUpProposition(
        currentTaxYear = CurrentSignUpTaxYear(Annual, currentTaxYear),
        nextTaxYear = NextSignUpTaxYear(Annual, nextTaxYear, CurrentSignUpTaxYear(Annual, currentTaxYear))
      )

      when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any))
        .thenReturn(Future.successful(
          Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Annual))
        ))

      mockRepository(optInContextData = Some(SignUpContextData(currentTaxYear.toString, Annual.toString, Annual.toString)), selectedOptInYear = Some(currentTaxYear.toString))

      val result = service.availableSignUpTaxYear()
      result.futureValue shouldBe Seq(currentTaxYear, nextTaxYear)
    }
  }

  ".fetchSavedChosenTaxYear()" should {

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

  "isSignUpTaxYearValid" should {
    "return a SignUpTaxQuestionViewModel" when {
      "the current tax year is submitted" in {
        val queryTaxYear = "2022"

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any))
          .thenReturn(Future.successful(
            Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Annual))
          ))

        mockRepository(Some(SignUpContextData(queryTaxYear, Annual.toString, Annual.toString)), Some(queryTaxYear))

        val result = service.isSignUpTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(SignUpTaxYearQuestionViewModel(CurrentSignUpTaxYear(Annual, TaxYear(2022, 2023))))
      }
      "the next tax year is submitted" in {
        val queryTaxYear = "2023"

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any))
          .thenReturn(Future.successful(
            Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Annual))
          ))

        mockRepository(Some(SignUpContextData(queryTaxYear, Annual.toString, Annual.toString)), Some(queryTaxYear))

        val result = service.isSignUpTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(SignUpTaxYearQuestionViewModel(NextSignUpTaxYear(Annual, TaxYear(2023, 2024), CurrentSignUpTaxYear(Annual, TaxYear(2022, 2023)))))
      }
    }

    "return None" when {
      "no tax year is submitted" in {
        val result = service.isSignUpTaxYearValid(None)

        result.futureValue shouldBe None
      }

      "the previous tax year is submitted" in {
        val queryTaxYear = "2021"

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any))
          .thenReturn(Future.successful(
            Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Annual))
          ))

        mockRepository(Some(SignUpContextData(queryTaxYear, Annual.toString, Annual.toString)), Some(queryTaxYear))

        val result = service.isSignUpTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe None
      }
      "the current tax year is submitted but the status is not Annual" in {
        val queryTaxYear = "2022"

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any))
          .thenReturn(Future.successful(
            Map(currentTaxYear -> statusDetailWith(Voluntary), nextTaxYear -> statusDetailWith(Annual))
          ))

        mockRepository(Some(SignUpContextData(queryTaxYear, Voluntary.toString, Annual.toString)), Some(queryTaxYear))

        val result = service.isSignUpTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe None
      }

      "the next tax year is submitted but the status is not Annual" in {
        val queryTaxYear = "2023"

        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any))
          .thenReturn(Future.successful(
            Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Voluntary))
          ))

        mockRepository(Some(SignUpContextData(queryTaxYear, Annual.toString, Voluntary.toString)), Some(queryTaxYear))

        val result = service.isSignUpTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe None
      }
    }
  }

  ".initialiseOptInContextData()" should {

    "return true and not call ITSA when optInContextData is already present" in {
      val existingContext = SignUpContextData(currentTaxYear.toString, Annual.toString, Voluntary.toString)

      val sessionData = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = Opt(SignUpJourney).toString,
        signUpSessionData = Some(SignUpSessionData(signUpContextData = Some(existingContext), selectedSignUpYear = None))
      )

      when(repository.fetchExistingUIJourneySessionDataOrInit(any())(any(), any())).thenReturn(Future.successful(Some(sessionData)))

      val result = service.initialiseOptInContextData()

      result.futureValue shouldBe true
      verify(mockITSAStatusService, times(0)).getStatusTillAvailableFutureYears(any())(any(), any(), any())
      verify(repository, times(1)).fetchExistingUIJourneySessionDataOrInit(any())(any(), any())
    }

    "fetch ITSA statuses and store OptInContextData in session when missing" in {
      val sessionData = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = Opt(SignUpJourney).toString,
        signUpSessionData = Some(SignUpSessionData(signUpContextData = None, selectedSignUpYear = None))
      )

      when(repository.fetchExistingUIJourneySessionDataOrInit()(hc, ec)).thenReturn(Future.successful(Some(sessionData)))
      when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

      when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any))
        .thenReturn(Future.successful(
          Map(
            currentTaxYear -> statusDetailWith(Annual),
            nextTaxYear -> statusDetailWith(Voluntary)
          )
        ))

      val expectedContext = SignUpContextData(
        currentTaxYear = currentTaxYear.toString,
        currentYearITSAStatus = Annual.toString,
        nextYearITSAStatus = Voluntary.toString
      )

      val expectedUpdatedSession = sessionData.copy(
        signUpSessionData = Some(sessionData.signUpSessionData.get.copy(signUpContextData = Some(expectedContext)))
      )

      when(repository.setUpdatedSessionDataStatus(expectedUpdatedSession)).thenReturn(Future.successful(true))

      val result = service.initialiseOptInContextData()

      result.futureValue shouldBe true
      verify(mockDateService, times(1)).getCurrentTaxYear
      verify(mockITSAStatusService, times(1)).getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any)
      verify(repository, times(1)).setUpdatedSessionDataStatus(expectedUpdatedSession)
    }

    "initialise Sign-up session and store context data when optInSessionData is missing" in {
      val sessionId = hc.sessionId.get.value

      val sessionWithoutOptInData = UIJourneySessionData(
        sessionId = sessionId,
        journeyType = Opt(SignUpJourney).toString,
        signUpSessionData = None
      )

      val sessionAfterSetup = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = Opt(SignUpJourney).toString,
        signUpSessionData = Some(SignUpSessionData(signUpContextData = None, selectedSignUpYear = None))
      )

      when(repository.fetchExistingUIJourneySessionDataOrInit()(hc, ec))
        .thenReturn(Future.successful(Some(sessionWithoutOptInData)))
        .thenReturn(Future.successful(Some(sessionAfterSetup)))

      when(repository.setupSessionData()(hc)).thenReturn(Future.successful(true))
      when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

      when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any))
        .thenReturn(Future.successful(
          Map(
            currentTaxYear -> statusDetailWith(Annual),
            nextTaxYear -> statusDetailWith(Voluntary)
          )
        ))

      val expectedContext = SignUpContextData(
        currentTaxYear = currentTaxYear.toString,
        currentYearITSAStatus = Annual.toString,
        nextYearITSAStatus = Voluntary.toString
      )

      val expectedUpdatedSession = sessionAfterSetup.copy(
        signUpSessionData = Some(sessionAfterSetup.signUpSessionData.get.copy(signUpContextData = Some(expectedContext)))
      )

      when(repository.setUpdatedSessionDataStatus(expectedUpdatedSession)).thenReturn(Future.successful(true))

      val result = service.initialiseOptInContextData()
      result.futureValue shouldBe true

      verify(repository, times(1)).setupSessionData()(any())
      verify(mockITSAStatusService, times(1))
        .getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any)
      verify(repository, times(1)).setUpdatedSessionDataStatus(any())(any(), any())
    }
  }

  def ec(implicit executionContext: ExecutionContext): ExecutionContext = executionContext

  def mockRepository(optInContextData: Option[SignUpContextData] = None, selectedOptInYear: Option[String] = None): Unit = {

    val sessionData = UIJourneySessionData(hc.sessionId.get.value, Opt(SignUpJourney).toString,
      signUpSessionData = Some(SignUpSessionData(optInContextData, selectedOptInYear)))

    when(repository.fetchExistingUIJourneySessionDataOrInit()(hc, ec)).thenReturn(Future.successful(Some(sessionData)))
  }
}
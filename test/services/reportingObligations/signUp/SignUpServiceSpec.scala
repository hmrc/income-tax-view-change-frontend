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

package services.reportingObligations.signUp

import audit.mocks.MockAuditingService
import audit.models.OptInAuditModel
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import controllers.routes
import enums.JourneyType.{Opt, SignUpJourney}
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockITSAStatusUpdateConnector}
import models.UIJourneySessionData
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.*
import models.itsaStatus.{StatusDetail, StatusReason}
import models.reportingObligations.signUp.{SignUpContextData, SignUpSessionData, SignUpTaxYearQuestionViewModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfter, OneInstancePerTest}
import play.api.http.Status.OK
import repositories.UIJourneySessionDataRepository
import services.NextUpdatesService
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.reportingObligations.signUp.SignUpService
import services.reportingObligations.signUp.core.{CurrentSignUpTaxYear, NextSignUpTaxYear, SignUpProposition}
import testUtils.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

import scala.concurrent.{ExecutionContext, Future}

class SignUpServiceSpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with OneInstancePerTest
  with MockITSAStatusUpdateConnector
  with MockAuditingService
  with Eventually {

  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  val optOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val nextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  val repository: UIJourneySessionDataRepository = mock(classOf[UIJourneySessionDataRepository])

  val service: SignUpService = new SignUpService(optOutConnector, mockITSAStatusService, mockDateService, repository, mockAuditingService)

  val forYearEnd = 2023
  val currentTaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear = currentTaxYear.nextYear

  override def beforeEach(): Unit = {

    reset(hc)
    reset(repository)
    reset(optOutConnector)
    reset(mockAuditingService)

    when(hc.sessionId).thenReturn(Some(SessionId("123")))
    when(repository.set(any())).thenReturn(Future.successful(true))
  }

  def statusDetailWith(status: ITSAStatus): StatusDetail = {
    StatusDetail("", status = status, statusReason = StatusReason.Rollover, businessIncomePriorTo2Years = None)
  }

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

      val result = service.saveIntent(currentTaxYear)
      result.futureValue shouldBe true

      verify(repository).set(dataAfter)
    }
  }

  ".saveIntent() and no session data" should {
    "save selectedOptInYear in session data" in {

      val jsd = UIJourneySessionData(hc.sessionId.get.value, Opt(SignUpJourney).toString)

      when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney))).thenReturn(Future.successful(Some(jsd)))

      val result = service.saveIntent(currentTaxYear)
      result.futureValue shouldBe true

      verify(repository, times(1)).set(jsd)
    }
  }

  ".availableOptInTaxYear()" when {

    "session data is not-saved" should {

      "return tax years ending 2023, 2024" in {

        when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney))).thenReturn(Future.successful(None))
        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any))
          .thenReturn(Future.successful(
            Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Annual))
          ))

        val result = service.availableSignUpTaxYear()
        result.futureValue shouldBe Seq(currentTaxYear, nextTaxYear)
      }

      "return tax year ending 2023" in {

        when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney))).thenReturn(Future.successful(None))
        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any))
          .thenReturn(Future.successful(
            Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Voluntary))
          ))

        val result = service.availableSignUpTaxYear()
        result.futureValue shouldBe Seq(currentTaxYear)
      }

    }

    "session data is saved" should {

      "return tax years ending 2023, 2024" in {

        mockRepository(Some(SignUpContextData(currentTaxYear.toString, Annual.toString, Annual.toString)))
        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        val result = service.availableSignUpTaxYear()
        result.futureValue shouldBe Seq(currentTaxYear, nextTaxYear)

        verify(mockITSAStatusService, times(0)).getStatusTillAvailableFutureYears(any())(any(), any(), any())
      }

      "return tax year ending 2023" in {

        mockRepository(Some(SignUpContextData(currentTaxYear.toString, Annual.toString, Voluntary.toString)))
        when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

        val result = service.availableSignUpTaxYear()
        result.futureValue shouldBe Seq(currentTaxYear)

        verify(mockITSAStatusService, times(0)).getStatusTillAvailableFutureYears(any())(any(), any(), any())
      }
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

  ".makeOptInCall()" should {

    "success response case" in {
      val optInContext = Some(SignUpContextData(currentTaxYear.toString, Annual.toString, Voluntary.toString))
      mockRepository(optInContext, Some(currentTaxYear.toString))

      when(optOutConnector.signUp(any(), any())(any()))
        .thenReturn(Future.successful(ITSAStatusUpdateResponseSuccess(OK)))

      whenReady(service.makeOptInCall()) { result =>

        val currentTaxYearOptIn: CurrentSignUpTaxYear = CurrentSignUpTaxYear(Annual, currentTaxYear)

        eventually {
          verifyExtendedAudit(
            OptInAuditModel(
              SignUpProposition(
                currentTaxYearOptIn,
                NextSignUpTaxYear(Voluntary, nextTaxYear, currentTaxYearOptIn)
              ),
              currentTaxYear,
              ITSAStatusUpdateResponseSuccess(OK)
            )
          )
        }

        result.isInstanceOf[ITSAStatusUpdateResponseSuccess] shouldBe true
      }
    }

    "fail response case" in {

      val optInContext = Some(SignUpContextData(currentTaxYear.toString, Annual.toString, Voluntary.toString))
      mockRepository(optInContext, Some(currentTaxYear.toString))

      when(optOutConnector.signUp(any(), any())(any()))
        .thenReturn(Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure()))

      whenReady(service.makeOptInCall()) { result =>
        val currentTaxYearOptIn: CurrentSignUpTaxYear = CurrentSignUpTaxYear(Annual, currentTaxYear)

        eventually {
          verifyExtendedAudit(
            OptInAuditModel(
              SignUpProposition(
                currentTaxYearOptIn,
                NextSignUpTaxYear(Voluntary, nextTaxYear, currentTaxYearOptIn)
              ),
              currentTaxYear,
              ITSAStatusUpdateResponseFailure.defaultFailure()
            )
          )
        }

        result.isInstanceOf[ITSAStatusUpdateResponseFailure] shouldBe true
      }
    }

    "fail where missing intent tax-year case" in {

      mockRepository(selectedOptInYear = None)

      when(optOutConnector.makeITSAStatusUpdate(any(), any(), any())(any()))
        .thenReturn(Future.successful(ITSAStatusUpdateResponseFailure.defaultFailure()))

      val result = service.makeOptInCall().futureValue

      result.isInstanceOf[ITSAStatusUpdateResponseFailure] shouldBe true
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

      when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney))).thenReturn(Future.successful(Some(sessionData)))

      val result = service.initialiseOptInContextData()

      result.futureValue shouldBe true
      verify(mockITSAStatusService, times(0)).getStatusTillAvailableFutureYears(any())(any(), any(), any())
      verify(repository, times(0)).set(any())
    }

    "fetch ITSA statuses and store OptInContextData in session when missing" in {
      val sessionData = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = Opt(SignUpJourney).toString,
        signUpSessionData = Some(SignUpSessionData(signUpContextData = None, selectedSignUpYear = None))
      )

      when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney))).thenReturn(Future.successful(Some(sessionData)))
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

      when(repository.set(expectedUpdatedSession)).thenReturn(Future.successful(true))

      val result = service.initialiseOptInContextData()

      result.futureValue shouldBe true
      verify(mockDateService, times(1)).getCurrentTaxYear
      verify(mockITSAStatusService, times(1)).getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any)
      verify(repository, times(1)).set(expectedUpdatedSession)
    }

    "initialise Sign-up session and store context data when optInSessionData is missing" in {
      val sessionId = hc.sessionId.get.value

      val sessionWithoutOptInData = UIJourneySessionData(
        sessionId = sessionId,
        journeyType = Opt(SignUpJourney).toString,
        signUpSessionData = None
      )

      val sessionAfterSetup = UIJourneySessionData(
        sessionId = sessionId,
        journeyType = Opt(SignUpJourney).toString,
        signUpSessionData = Some(SignUpSessionData(None, None, Some(false)))
      )

      when(repository.get(sessionId, Opt(SignUpJourney)))
        .thenReturn(Future.successful(Some(sessionWithoutOptInData)))
        .thenReturn(Future.successful(Some(sessionAfterSetup)))

      when(mockDateService.getCurrentTaxYear).thenReturn(currentTaxYear)

      when(mockITSAStatusService.getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any))
        .thenReturn(Future.successful(
          Map(currentTaxYear -> statusDetailWith(Annual), nextTaxYear -> statusDetailWith(Voluntary))
        ))

      val result = service.initialiseOptInContextData()
      result.futureValue shouldBe true

      verify(repository, times(2)).get(sessionId, Opt(SignUpJourney))
      verify(mockITSAStatusService, times(1))
        .getStatusTillAvailableFutureYears(ArgumentMatchers.eq(currentTaxYear.previousYear))(any, any, any)

      val savedJourneySessionArguments = ArgumentCaptor.forClass(classOf[UIJourneySessionData])
      verify(repository, times(2)).set(savedJourneySessionArguments.capture())

      val savedSessions = savedJourneySessionArguments.getAllValues
      val firstSavedSession = savedSessions.get(0)
      val secondSavedSession = savedSessions.get(1)

      firstSavedSession.signUpSessionData shouldBe defined
      firstSavedSession.signUpSessionData.flatMap(_.signUpContextData) shouldBe empty

      secondSavedSession.signUpSessionData.flatMap(_.signUpContextData) shouldBe defined
      val savedContext = secondSavedSession.signUpSessionData.flatMap(_.signUpContextData).value

      savedContext.currentTaxYear shouldBe currentTaxYear.toString
      savedContext.currentYearITSAStatus shouldBe Annual.toString
      savedContext.nextYearITSAStatus shouldBe Voluntary.toString
    }
  }

  def executionContext()(implicit executionContext: ExecutionContext): ExecutionContext = executionContext

  def mockRepository(optInContextData: Option[SignUpContextData] = None, selectedOptInYear: Option[String] = None): Unit = {

    val sessionData = UIJourneySessionData(hc.sessionId.get.value, Opt(SignUpJourney).toString,
      signUpSessionData = Some(SignUpSessionData(optInContextData, selectedOptInYear)))

    when(repository.get(hc.sessionId.get.value, Opt(SignUpJourney))).thenReturn(Future.successful(Some(sessionData)))
  }
}
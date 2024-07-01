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
import connectors.optout.OptOutUpdateRequestModel.{ErrorItem, OptOutUpdateResponseFailure, OptOutUpdateResponseSuccess, optOutUpdateReason}
import mocks.services._
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, NoStatus, Voluntary}
import models.itsaStatus.{ITSAStatus, StatusDetail}
import models.optout._
import org.mockito.ArgumentMatchers.{any, same}
import org.mockito.Mockito._
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, Succeeded}
import play.mvc.Http.Status.{BAD_REQUEST, NO_CONTENT}
import repositories.UIJourneySessionDataRepository
import services.NextUpdatesService
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.optout.OptOutService.QuarterlyUpdatesCountForTaxYearModel
import services.optout.OptOutServiceSpec.TaxYearAndCountOfSubmissionsForIt
import services.optout.OptOutTestSupport.{buildOneYearOptOutPropositionForCurrentYear, buildOneYearOptOutPropositionForNextYear, buildOneYearOptOutPropositionForPreviousYear}
import testConstants.ITSAStatusTestConstants.yearToStatus
import testUtils.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.OptOutJourney

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/*
* Keys:
*
* U: Unknown
* V: Voluntary
* M: Mandated
* A: Annual
*
* PY: Previous Year
* CY: Current Year
* NY: Next Year
*
* */

object OptOutServiceSpec {
  case class TaxYearAndCountOfSubmissionsForIt(taxYear: TaxYear, submissions: Int)
}

class OptOutServiceSpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with MockITSAStatusUpdateConnector {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(5, Millis))

  val optOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val nextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  val repository: UIJourneySessionDataRepository = mock(classOf[UIJourneySessionDataRepository])

  implicit val user: MtdItUser[_] = mock(classOf[MtdItUser[_]])
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  val taxYear: TaxYear = TaxYear.forYearEnd(2021)
  val previousTaxYear: TaxYear = taxYear.previousYear
  val crystallised: Boolean = true

  val sessionIdValue = "123"
  val error = new RuntimeException("Some Error")

  val service: OptOutService = new OptOutService(optOutConnector, mockITSAStatusService,
    mockCalculationListService, nextUpdatesService, mockDateService, repository)

  before {
    reset(optOutConnector, mockITSAStatusService, mockCalculationListService, mockDateService, user, hc, repository)
  }

  val noOptOutOptionAvailable: Option[Nothing] = None

  val apiError: String = "some api error"

  "OptOutService.resetSavedIntent" should {
    "reset intent year" in {

      val forYearEnd = 2024

      when(hc.sessionId).thenReturn(Some(SessionId("123")))
      when(repository.set(any())).thenReturn(Future.successful(true))

      val data = UIJourneySessionData(
        sessionId = hc.sessionId.get.value,
        journeyType = OptOutJourney.Name,
        optOutSessionData = Some(OptOutSessionData(selectedOptOutYear = Some(TaxYear.forYearEnd(forYearEnd).toString)))
      )
      when(repository.get(any(), any())).thenReturn(Future.successful(Some(data)))

      def reconfigureMock(): Future[Unit] = {
        Future {
          Mockito.reset(repository)

          when(repository.set(any())).thenReturn(Future.successful(true))

          val data = UIJourneySessionData(
            sessionId = hc.sessionId.get.value,
            journeyType = OptOutJourney.Name,
            optOutSessionData = Some(OptOutSessionData(selectedOptOutYear = None))
          )
          when(repository.get(any(), any())).thenReturn(Future.successful(Some(data)))
        }
      }

      val f = for {
        isSaved <- service.saveIntent(TaxYear.forYearEnd(forYearEnd))
        savedIntent <- service.fetchSavedIntent()
        _ <- reconfigureMock()
        isReset <- service.resetSavedIntent()
        noneIntent <- service.fetchSavedIntent()
      } yield {
        isSaved shouldBe true
        savedIntent.isDefined shouldBe true
        isReset shouldBe true
        noneIntent.isDefined shouldBe false
      }

      f.futureValue shouldBe Succeeded
    }
  }

  "OptOutService.getSubmissionCountForTaxYear" when {
    "three years offered for opt-out; end-year 2023, 2024, 2025" when {
      "tax-payer made previous submissions for end-year 2023, 2024" should {
        "return count of submissions for each year" in {

          val optOutProposition = OptOutTestSupport.buildThreeYearOptOutProposition()

          val offeredTaxYearsAndCountsTestSetup = Seq(
            TaxYearAndCountOfSubmissionsForIt(optOutProposition.availableTaxYearsForOptOut.head, 1),
            TaxYearAndCountOfSubmissionsForIt(optOutProposition.availableTaxYearsForOptOut(1), 1),
            TaxYearAndCountOfSubmissionsForIt(optOutProposition.availableTaxYearsForOptOut.last, 0)
          )

          offeredTaxYearsAndCountsTestSetup map { year =>
            when(nextUpdatesService.getQuarterlyUpdatesCounts(same(year.taxYear))(any(), any()))
              .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(year.taxYear, year.submissions)))
          }

          val result = service.getQuarterlyUpdatesCountForOfferedYears(optOutProposition)

          val expectedResult = QuarterlyUpdatesCountForTaxYearModel(Seq(
            QuarterlyUpdatesCountForTaxYear(TaxYear.forYearEnd(2023), 2),
            QuarterlyUpdatesCountForTaxYear(TaxYear.forYearEnd(2024), 1),
          ))

          result.futureValue shouldBe expectedResult
        }
      }
    }

    "three years offered for opt-out; end-year 2023, 2025" when {
      "tax-payer made previous submissions for end-year 2023" should {
        "return count of submissions for each year" in {

          val optOutProposition = OptOutTestSupport.buildTwoYearOptOutPropositionOfferingPYAndNY()

          val offeredTaxYearsAndCountsTestSetup = Seq(
            TaxYearAndCountOfSubmissionsForIt(optOutProposition.availableTaxYearsForOptOut.head, 1),
            TaxYearAndCountOfSubmissionsForIt(optOutProposition.availableTaxYearsForOptOut.last, 0)
          )

          offeredTaxYearsAndCountsTestSetup map { year =>
            when(nextUpdatesService.getQuarterlyUpdatesCounts(same(year.taxYear))(any(), any()))
              .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(year.taxYear, year.submissions)))
          }

          val result = service.getQuarterlyUpdatesCountForOfferedYears(optOutProposition)

          val expectedResult = QuarterlyUpdatesCountForTaxYearModel(Seq(
            QuarterlyUpdatesCountForTaxYear(TaxYear.forYearEnd(2023), 1)
          ))

          result.futureValue shouldBe expectedResult
        }
      }
    }
  }

  "OptOutService.makeOptOutUpdateRequestForYear" when {
    "make opt-out update request for previous tax-year" should {

      "opt-out update request made for correct year" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val optOutTaxYear: TaxYear = TaxYear.forYearEnd(currentYear).previousYear
        val correlationId = "123"

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(optOutTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))

        val proposition = buildOneYearOptOutPropositionForPreviousYear(currentYear)
        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val intent = optOutTaxYear
        val sessionData: Option[OptOutSessionData] = Some(OptOutSessionData(Some(intent.toString)))
        val journeyData: UIJourneySessionData = UIJourneySessionData(sessionIdValue, OptOutJourney.Name, optOutSessionData = sessionData)
        when(repository.get(any[String], any[String])).thenReturn(Future.successful(Option(journeyData)))
        when(repository.set(any())).thenReturn(Future.successful(true))

        val result = service.makeOptOutUpdateRequest(proposition)
        result.futureValue shouldBe OptOutUpdateResponseSuccess(correlationId, NO_CONTENT)
      }
    }

    "make opt-out update request for current tax-year" should {

      "opt-out update request made for correct year" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val optOutTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val correlationId = "123"

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(optOutTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))
        val proposition = buildOneYearOptOutPropositionForCurrentYear(currentYear)

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val intent = optOutTaxYear
        val sessionData: Option[OptOutSessionData] = Some(OptOutSessionData(Some(intent.toString)))
        val journeyData: UIJourneySessionData = UIJourneySessionData(sessionIdValue, OptOutJourney.Name, optOutSessionData = sessionData)
        when(repository.get(any[String], any[String])).thenReturn(Future.successful(Option(journeyData)))
        when(repository.set(any())).thenReturn(Future.successful(true))

        val result = service.makeOptOutUpdateRequest(proposition)
        result.futureValue shouldBe OptOutUpdateResponseSuccess(correlationId, NO_CONTENT)
      }
    }

    "make opt-out update request for next tax-year" should {

      "opt-out update request made for correct year" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val optOutTaxYear: TaxYear = TaxYear.forYearEnd(currentYear).nextYear
        val correlationId = "123"

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(optOutTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))
        val proposition = buildOneYearOptOutPropositionForNextYear(currentYear)

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val intent = optOutTaxYear
        val sessionData: Option[OptOutSessionData] = Some(OptOutSessionData(Some(intent.toString)))
        val journeyData: UIJourneySessionData = UIJourneySessionData(sessionIdValue, OptOutJourney.Name, optOutSessionData = sessionData)
        when(repository.get(any[String], any[String])).thenReturn(Future.successful(Option(journeyData)))
        when(repository.set(any())).thenReturn(Future.successful(true))

        service.makeOptOutUpdateRequest(proposition)
      }
    }

    "make opt-out update request for tax-year 2023-2024 and can opt-out of this year" should {

      "successful update request was made" in {

        val correlationId = "123"
        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(currentTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))
        val proposition = OptOutTestSupport.buildOneYearOptOutPropositionForCurrentYear()


        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val intent = currentTaxYear
        val sessionData: Option[OptOutSessionData] = Some(OptOutSessionData(Some(intent.toString)))
        val journeyData: UIJourneySessionData = UIJourneySessionData(sessionIdValue, OptOutJourney.Name, optOutSessionData = sessionData)
        when(repository.get(any[String], any[String])).thenReturn(Future.successful(Option(journeyData)))
        when(repository.set(any())).thenReturn(Future.successful(true))

        val result = service.makeOptOutUpdateRequest(proposition)

        result.futureValue shouldBe OptOutUpdateResponseSuccess(correlationId, NO_CONTENT)
      }
    }

    "make opt-out update request for tax-year 2023-2024 and can not opt-out of this year" should {

      "return failure response for made update request" in {

        val correlationId = "123"
        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val errorItems = List(ErrorItem("INVALID_TAXABLE_ENTITY_ID",
          "Submission has not passed validation. Invalid parameter taxableEntityId."))

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(currentTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseFailure(correlationId, BAD_REQUEST, errorItems)
        ))
        val proposition = OptOutTestSupport.buildOneYearOptOutPropositionForCurrentYear()

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val intent = currentTaxYear
        val sessionData: Option[OptOutSessionData] = Some(OptOutSessionData(Some(intent.toString)))
        val journeyData: UIJourneySessionData = UIJourneySessionData(sessionIdValue, OptOutJourney.Name, optOutSessionData = sessionData)
        when(repository.get(any[String], any[String])).thenReturn(Future.successful(Option(journeyData)))
        when(repository.set(any())).thenReturn(Future.successful(true))

        val result = service.makeOptOutUpdateRequest(proposition)

        result.futureValue shouldBe OptOutUpdateResponseFailure(correlationId, BAD_REQUEST, errorItems)
      }
    }

  }

  "OptOutService.nextUpdatesPageOneYearOptOutViewModel" when {

    s"PY is $Voluntary, CY is $NoStatus, NY is $NoStatus and PY is NOT finalised" should {

      "offer PY as OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
        when(mockDateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.NoStatus, "")
        )
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

        val response = service.nextUpdatesPageOptOutViewModel()

        response.futureValue shouldBe Some(OptOutOneYearViewModel(TaxYear.forYearEnd(2023), None))

      }
    }

    s"PY is $Voluntary, CY is $NoStatus NY is $NoStatus and PY is finalised" should {

      "offer No OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
        when(mockDateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.NoStatus, "")
        )
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(true))

        val response = service.nextUpdatesPageOptOutViewModel()

        response.futureValue shouldBe noOptOutOptionAvailable
      }
    }

    s"PY is $NoStatus, CY is $Voluntary, NY is $Mandated" should {

      "offer CY OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
        when(mockDateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Mandated, "")
        )
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

        val response = service.nextUpdatesPageOptOutViewModel()

        response.futureValue shouldBe Some(OptOutOneYearViewModel(TaxYear.forYearEnd(2024), Some(OneYearOptOutFollowedByMandated)))
      }
    }

    s"PY is $NoStatus, CY is $NoStatus, NY is $Voluntary" should {

      "offer NY OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
        when(mockDateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Voluntary, "")
        )
        when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

        val response = service.nextUpdatesPageOptOutViewModel()

        response.futureValue shouldBe Some(OptOutOneYearViewModel(TaxYear.forYearEnd(2025), Some(NextYearOptOut)))
      }
    }

    "Single Year OptOut" when {
      s"PY : PY is $Voluntary, CY is $Mandated, NY is $Mandated and PY is NOT crystallised" should {
        s"offer PY OptOut Option with a warning as following year (CY) is $Mandated " in {

          val currentYear = 2024
          val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
          when(mockDateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

          val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
            TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
            TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.Mandated, ""),
            TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Mandated, "")
          )
          when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

          when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

          val response = service.nextUpdatesPageOptOutViewModel()

          val model = response.futureValue.get

          model match {
            case m: OptOutOneYearViewModel =>
              m.oneYearOptOutTaxYear shouldBe previousYear
              m.showWarning shouldBe true
            case _ => fail("model should be OptOutOneYearViewModel")
          }

        }
        s"CY : PY is $Mandated, CY is $Voluntary, NY is $Mandated " should {
          s"offer CY OptOut Option with a warning as following year (NY) is $Mandated " in {

            val currentYear = 2024
            val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
            when(mockDateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

            val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
              TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Mandated, ""),
              TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.Voluntary, ""),
              TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Mandated, "")
            )
            when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

            when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

            val response = service.nextUpdatesPageOptOutViewModel()

            val model = response.futureValue.get
            model match {
              case m: OptOutOneYearViewModel =>
                m.oneYearOptOutTaxYear shouldBe TaxYear.forYearEnd(currentYear)
                m.showWarning shouldBe true
              case _ => fail("model should be OptOutOneYearViewModel")
            }

          }
        }
      }


      "getStatusTillAvailableFutureYears api call fail" should {

        "return default response" in {

          val currentYear = 2024
          val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
          when(mockDateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

          when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.failed(new RuntimeException("some api error")))

          when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

          val response = service.nextUpdatesPageOptOutViewModel()

          response.failed.futureValue.getMessage shouldBe apiError
        }
      }

      "isTaxYearCrystallised api call fail" should {

        "return default response" in {

          val currentYear = 2024
          val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
          when(mockDateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

          val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
            TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
            TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
            TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Voluntary, "")
          )
          when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

          when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.failed(new RuntimeException("some api error")))

          val response = service.nextUpdatesPageOptOutViewModel()

          response.failed.futureValue.getMessage shouldBe apiError
        }
      }
    }

    "OptOutService.getNextUpdatesQuarterlyReportingContentChecks" when {
      "ITSA Status from CY-1 till future years and Calculation State for CY-1 is available" should {
        "return NextUpdatesQuarterlyReportingContentCheck" in {
          setupMockIsTaxYearCrystallisedCall(previousTaxYear.endYear)(Future.successful(Some(crystallised)))
          setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.successful(yearToStatus))
          setupMockGetCurrentTaxYearEnd(taxYear.endYear)

          val expected = NextUpdatesQuarterlyReportingContentChecks(
            currentYearItsaStatus = true,
            previousYearItsaStatus = false,
            previousYearCrystallisedStatus = Some(crystallised))

          service.getNextUpdatesQuarterlyReportingContentChecks.futureValue shouldBe expected
        }
      }

      "Calculation State for CY-1 is unavailable" should {
        "return NextUpdatesQuarterlyReportingContentCheck" in {
          setupMockIsTaxYearCrystallisedCall(previousTaxYear.endYear)(Future.failed(error))
          setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.successful(yearToStatus))
          setupMockGetCurrentTaxYearEnd(taxYear.endYear)

          service.getNextUpdatesQuarterlyReportingContentChecks.failed.map { ex =>
            ex shouldBe a[RuntimeException]
            ex.getMessage shouldBe error.getMessage
          }
        }
      }

      "ITSA Status from CY-1 till future years is unavailable" should {
        "return NextUpdatesQuarterlyReportingContentCheck" in {
          setupMockIsTaxYearCrystallisedCall(previousTaxYear.endYear)(Future.successful(Some(crystallised)))
          setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.failed(error))
          setupMockGetCurrentTaxYearEnd(taxYear.endYear)

          service.getNextUpdatesQuarterlyReportingContentChecks.failed.map { ex =>
            ex shouldBe a[RuntimeException]
            ex.getMessage shouldBe error.getMessage
          }
        }
      }
    }
  }

  "OptOutService.optOutCheckPointPageViewModel for single year case" when {
    val CY = TaxYear.forYearEnd(2024)
    val PY = CY.previousYear
    val NY = CY.nextYear
    val previousOptOutTaxYear = PreviousOptOutTaxYear(Voluntary, PY, crystallised = false)
    val currentOptOutTaxYear = CurrentOptOutTaxYear(Voluntary, CY)
    val nextOptOutTaxYear = NextOptOutTaxYear(Voluntary, NY, CurrentOptOutTaxYear(Mandated, CY))

    def testOptOutCheckPointPageViewModel(statusPY: ITSAStatus, statusCY: ITSAStatus, statusNY: ITSAStatus, crystallisedPY: Boolean)
                                         (optOutTaxYear: OptOutTaxYear, state: OptOutState): Unit = {

      def getTaxYearText(taxYear: TaxYear): String = {
        if (taxYear == CY) "CY" else if (taxYear == PY) "PY" else if (taxYear == NY) "NY" else ""
      }

      s"PY is $statusPY, CY is $statusCY, NY is $statusNY and PY is ${if (!crystallisedPY) "NOT "}finalised" should {
        s"offer ${getTaxYearText(optOutTaxYear.taxYear)} with state $state" in {

          val previousYear: TaxYear = PY
          when(mockDateService.getCurrentTaxYear).thenReturn(CY)

          val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
            PY -> StatusDetail("", statusPY, ""),
            CY -> StatusDetail("", statusCY, ""),
            NY -> StatusDetail("", statusNY, "")
          )
          when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

          when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(crystallisedPY))

          when(nextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(optOutTaxYear.taxYear))(any(), any()))
            .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(optOutTaxYear.taxYear, 0)))

          when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
          val intent = optOutTaxYear
          val sessionData: Option[OptOutSessionData] = Some(OptOutSessionData(Some(intent.toString)))
          val journeyData: UIJourneySessionData = UIJourneySessionData(sessionIdValue, OptOutJourney.Name, optOutSessionData = sessionData)
          when(repository.get(any[String], any[String])).thenReturn(Future.successful(Option(journeyData)))

          val response = service.optOutCheckPointPageViewModel()

          response.futureValue shouldBe Some(OneYearOptOutCheckpointViewModel(optOutTaxYear.taxYear, Some(state)))

        }
      }

    }

    val testCases = List(
      ((Voluntary, Annual, Mandated, false), (previousOptOutTaxYear, OneYearOptOutFollowedByAnnual)),
      ((Mandated, Voluntary, Annual, false), (currentOptOutTaxYear, OneYearOptOutFollowedByAnnual)),
      ((Mandated, Mandated, Voluntary, false), (nextOptOutTaxYear, NextYearOptOut))
    )
    testCases.foreach {
      case (input, output) =>
        val test = testOptOutCheckPointPageViewModel _
        test.tupled(input).tupled(output)
    }

  }

  "OptOutService.optOutCheckPointPageViewModel for multi year case" when {
    val CY = TaxYear.forYearEnd(2024)
    val PY = CY.previousYear
    val NY = CY.nextYear

    def testOptOutCheckPointPageViewModel(statusPY: ITSAStatus, statusCY: ITSAStatus, statusNY: ITSAStatus, crystallisedPY: Boolean)
                                         (intent: TaxYear, state: OptOutState): Unit = {

      def getTaxYearText(taxYear: TaxYear): String = {
        if (taxYear == CY) "CY" else if (taxYear == PY) "PY" else if (taxYear == NY) "NY" else ""
      }

      s"PY is $statusPY, CY is $statusCY, NY is $statusNY and PY is ${if (!crystallisedPY) "NOT "}finalised" should {
        s"offer ${getTaxYearText(intent)}" in {

          val previousYear: TaxYear = PY
          when(mockDateService.getCurrentTaxYear).thenReturn(CY)

          val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
            PY -> StatusDetail("", statusPY, ""),
            CY -> StatusDetail("", statusCY, ""),
            NY -> StatusDetail("", statusNY, "")
          )
          when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

          when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(crystallisedPY))

          when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
          val sessionData: Option[OptOutSessionData] = Some(OptOutSessionData(Some(intent.toString)))
          val journeyData: UIJourneySessionData = UIJourneySessionData(sessionIdValue, OptOutJourney.Name, optOutSessionData = sessionData)
          when(repository.get(any[String], any[String])).thenReturn(Future.successful(Option(journeyData)))

          val response = service.optOutCheckPointPageViewModel()

          response.futureValue shouldBe Some(MultiYearOptOutCheckpointViewModel(intent))

        }
      }

    }

    val testCases = List(
      ((Voluntary, Voluntary, Voluntary, false), (PY, MultiYearOptOutDefault)),
      ((Voluntary, Voluntary, Voluntary, false), (CY, MultiYearOptOutDefault)),
      ((Voluntary, Voluntary, Voluntary, false), (NY, MultiYearOptOutDefault))
    )
    testCases.foreach {
      case (input, output) =>
        val test = testOptOutCheckPointPageViewModel _
        test.tupled(input).tupled(output)
    }

  }


  "OptOutService.optOutConfirmedPageViewModel" when {
    val CY = TaxYear.forYearEnd(2024)
    val PY = CY.previousYear
    val NY = CY.nextYear
    val previousOptOutTaxYear = PreviousOptOutTaxYear(Voluntary, PY, crystallised = false)
    val currentOptOutTaxYear = CurrentOptOutTaxYear(Voluntary, CY)

    val testCases = List(
      ((Voluntary, Mandated, Mandated, false), Some(ConfirmedOptOutViewModel(previousOptOutTaxYear.taxYear, Some(OneYearOptOutFollowedByMandated)))),
      ((Mandated, Voluntary, Mandated, false), Some(ConfirmedOptOutViewModel(currentOptOutTaxYear.taxYear, Some(OneYearOptOutFollowedByMandated))))
    )
    testCases.foreach {
      case (input, output) =>
        val test = testOptOutConfirmedPageViewModel _
        test.tupled(input)(output)
    }

    def testOptOutConfirmedPageViewModel(statusPY: ITSAStatus, statusCY: ITSAStatus, statusNY: ITSAStatus, crystallisedPY: Boolean)
                                        (viewModel: Option[ConfirmedOptOutViewModel]): Unit = {

      s"PY is $statusPY, CY is $statusCY, NY is $statusNY and PY is ${if (!crystallisedPY) "NOT "}finalised" should {
        s"return  $viewModel" in {

          val previousYear: TaxYear = PY
          when(mockDateService.getCurrentTaxYear).thenReturn(CY)

          val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
            PY -> StatusDetail("", statusPY, ""),
            CY -> StatusDetail("", statusCY, ""),
            NY -> StatusDetail("", statusNY, "")
          )
          when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))
          when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(crystallisedPY))

          when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
          val optOutSessionData = OptOutSessionData(Some(viewModel.get.optOutTaxYear.toString))
          val sessionData = Some(UIJourneySessionData(hc.sessionId.get.value, OptOutJourney.Name, optOutSessionData = Some(optOutSessionData)))
          when(repository.get(any(), any())).thenReturn(Future.successful(sessionData))

          val response = service.optOutConfirmedPageViewModel()

          response.futureValue shouldBe viewModel

        }
      }

    }

  }
}

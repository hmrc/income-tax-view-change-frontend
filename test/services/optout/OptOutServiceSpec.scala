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
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import models.itsaStatus.StatusDetail
import models.optout._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, same}
import org.mockito.Mockito._
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfter, OneInstancePerTest}
import play.mvc.Http.Status.NO_CONTENT
import repositories.OptOutSessionDataRepository
import services.NextUpdatesService
import services.NextUpdatesService.QuarterlyUpdatesCountForTaxYear
import services.optout.OptOutProposition.createOptOutProposition
import services.optout.OptOutService.QuarterlyUpdatesCountForTaxYearModel
import services.optout.OptOutServiceSpec.TaxYearAndCountOfSubmissionsForIt
import services.optout.OptOutTestSupport._
import testConstants.ITSAStatusTestConstants.yearToStatus
import testUtils.UnitSpec
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

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
  with OneInstancePerTest
  with MockITSAStatusUpdateConnector {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(5, Millis))

  val optOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val nextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  val repository: OptOutSessionDataRepository = mock(classOf[OptOutSessionDataRepository])

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

  val service: OptOutService = new OptOutService(optOutConnector, mockITSAStatusService,
    mockCalculationListService, nextUpdatesService, mockDateService, repository)

  val noOptOutOptionAvailable: Option[Nothing] = None

  val apiError: String = "some api error"

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
            QuarterlyUpdatesCountForTaxYear(TaxYear.forYearEnd(2025), 0),
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
            QuarterlyUpdatesCountForTaxYear(TaxYear.forYearEnd(2023), 1),
            QuarterlyUpdatesCountForTaxYear(TaxYear.forYearEnd(2025), 0)
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

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(optOutTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess()
        ))

        val proposition = buildOneYearOptOutPropositionForPreviousYear(currentYear)
        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val intent = optOutTaxYear

        val result = service.makeOptOutUpdateRequest(proposition, Future.successful(Some(intent)))
        result.futureValue shouldBe OptOutUpdateResponseSuccess(NO_CONTENT)
      }
    }

    "make opt-out update request for current tax-year" should {

      "opt-out update request made for correct year" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val optOutTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(optOutTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess()
        ))
        val proposition = buildOneYearOptOutPropositionForCurrentYear(currentYear)

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val intent = optOutTaxYear

        val result = service.makeOptOutUpdateRequest(proposition, Future.successful(Some(intent)))
        result.futureValue shouldBe OptOutUpdateResponseSuccess(NO_CONTENT)
      }
    }

    "make opt-out update request for next tax-year" should {

      "opt-out update request made for correct year" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val optOutTaxYear: TaxYear = TaxYear.forYearEnd(currentYear).nextYear

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(optOutTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess()
        ))
        val proposition = buildOneYearOptOutPropositionForNextYear(currentYear)

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val intent = optOutTaxYear

        service.makeOptOutUpdateRequest(proposition, Future.successful(Some(intent)))
      }
    }

    "make opt-out update request for tax-year 2023-2024 and can opt-out of this year" should {

      "successful update request was made" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(currentTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess()
        ))
        val proposition = OptOutTestSupport.buildOneYearOptOutPropositionForCurrentYear()


        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val intent = currentTaxYear

        val result = service.makeOptOutUpdateRequest(proposition, Future.successful(Some(intent)))

        result.futureValue shouldBe OptOutUpdateResponseSuccess(NO_CONTENT)
      }
    }

    "make opt-out update request for tax-year 2023-2024 and can not opt-out of this year" should {

      "return failure response for made update request" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val errorItems = List(ErrorItem("INVALID_TAXABLE_ENTITY_ID",
          "Submission has not passed validation. Invalid parameter taxableEntityId."))

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(currentTaxYear, taxableEntityId, optOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseFailure(errorItems)
        ))
        val proposition = OptOutTestSupport.buildOneYearOptOutPropositionForCurrentYear()

        when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
        val intent = currentTaxYear

        val result = service.makeOptOutUpdateRequest(proposition, Future.successful(Some(intent)))

        result.futureValue shouldBe OptOutUpdateResponseFailure(errorItems)
      }
    }

  }

  "OptOutService.nextUpdatesPageOneYearOptOutViewModel" when {

    "PY is Voluntary, CY is NoStatus, NY is NoStatus and PY is NOT finalised" should {

      "offer PY as OptOut Option" in {

        stubOptOut(
          currentTaxYear = taxYear2023_2024,
          previousYearCrystallisedStatus = false,
          previousYearStatus = Voluntary,
          currentYearStatus = NoStatus,
          nextYearStatus = NoStatus)

        val response = service.nextUpdatesPageOptOutViewModels()

        response.futureValue._2 shouldBe Some(OptOutOneYearViewModel(taxYear2022_2023, None))

      }
    }

    "PY is Voluntary, CY is NoStatus NY is NoStatus and PY is finalised" should {

      "offer No OptOut Option" in {

        stubOptOut(
          currentTaxYear = taxYear2023_2024,
          previousYearCrystallisedStatus = true,
          previousYearStatus = Voluntary,
          currentYearStatus = NoStatus,
          nextYearStatus = NoStatus)

        val response = service.nextUpdatesPageOptOutViewModels()

        response.futureValue._2 shouldBe noOptOutOptionAvailable
      }
    }

    "PY is NoStatus, CY is Voluntary, NY is Mandated" should {

      "offer CY OptOut Option" in {

        stubOptOut(
          currentTaxYear = taxYear2023_2024,
          previousYearCrystallisedStatus = false,
          previousYearStatus = NoStatus,
          currentYearStatus = Voluntary,
          nextYearStatus = Mandated)

        val response = service.nextUpdatesPageOptOutViewModels()

        response.futureValue._2 shouldBe Some(OptOutOneYearViewModel(taxYear2023_2024, Some(OneYearOptOutFollowedByMandated)))
      }
    }

    "PY is NoStatus, CY is NoStatus, NY is Voluntary" should {

      "offer NY OptOut Option" in {

        stubOptOut(
          currentTaxYear = taxYear2023_2024,
          previousYearCrystallisedStatus = false,
          previousYearStatus = NoStatus,
          currentYearStatus = NoStatus,
          nextYearStatus = Voluntary)

        val response = service.nextUpdatesPageOptOutViewModels()

        response.futureValue._2 shouldBe Some(OptOutOneYearViewModel(taxYear2024_2025, Some(NextYearOptOut)))
      }
    }

    "Single Year OptOut" when {
      "PY : PY is Voluntary, CY is Mandated, NY is Mandated and PY is NOT crystallised" should {
        "offer PY OptOut Option with a warning as following year (CY) is Mandated " in {

          stubOptOut(
            currentTaxYear = taxYear2023_2024,
            previousYearCrystallisedStatus = false,
            previousYearStatus = Voluntary,
            currentYearStatus = Mandated,
            nextYearStatus = Mandated)

          val response = service.nextUpdatesPageOptOutViewModels()

          val model = response.futureValue._2.get

          model match {
            case m: OptOutOneYearViewModel =>
              m.oneYearOptOutTaxYear shouldBe taxYear2022_2023
              m.showWarning shouldBe true
            case _ => fail("model should be OptOutOneYearViewModel")
          }

        }
        "CY : PY is Mandated, CY is Voluntary, NY is Mandated " should {
          "offer CY OptOut Option with a warning as following year (NY) is Mandated " in {

            stubOptOut(
              currentTaxYear = taxYear2023_2024,
              previousYearCrystallisedStatus = false,
              previousYearStatus = Mandated,
              currentYearStatus = Voluntary,
              nextYearStatus = Mandated)

            val response = service.nextUpdatesPageOptOutViewModels()

            val model = response.futureValue._2.get
            model match {
              case m: OptOutOneYearViewModel =>
                m.oneYearOptOutTaxYear shouldBe taxYear2023_2024
                m.showWarning shouldBe true
              case _ => fail("model should be OptOutOneYearViewModel")
            }

          }
        }
      }


      "getStatusTillAvailableFutureYears api call fail" should {

        "return default response" in {

          val (previousYear, currentYear, nextYear) = taxYears(currentYear = taxYear2023_2024)

          stubCurrentTaxYear(currentYear)

          when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.failed(new RuntimeException("some api error")))

          stubCrystallisedStatus(previousYear, false)

          val response = service.nextUpdatesPageOptOutViewModels()

          response.failed.futureValue.getMessage shouldBe apiError
        }
      }

      "isTaxYearCrystallised api call fail" should {

        "return default response" in {

          val (previousYear, currentYear, nextYear) = taxYears(currentYear = taxYear2023_2024)

          stubCurrentTaxYear(currentYear)

          stubItsaStatuses(
            previousYear, NoStatus,
            currentYear, NoStatus,
            nextYear, Voluntary)

          when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.failed(new RuntimeException("some api error")))

          val response = service.nextUpdatesPageOptOutViewModels()

          response.failed.futureValue.getMessage shouldBe apiError
        }
      }
    }

    "OptOutService.nextUpdatesPageOptOutViewModels" when {
      "ITSA Status from CY-1 till future years and Calculation State for CY-1 is available" should {
        "return NextUpdatesQuarterlyReportingContentCheck" in {
          // Because we're saving the status as well as building view models...
          allowWriteOfOptOutDataToMongoToSucceed()

          setupMockIsTaxYearCrystallisedCall(previousTaxYear)(Future.successful(crystallised))
          setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.successful(yearToStatus))
          setupMockGetCurrentTaxYearEnd(taxYear)

          val expected = NextUpdatesQuarterlyReportingContentChecks(
            currentYearItsaStatus = true,
            previousYearItsaStatus = false,
            previousYearCrystallisedStatus = crystallised)

          service.nextUpdatesPageOptOutViewModels().futureValue._1 shouldBe expected
        }
      }

      "Calculation State for CY-1 is unavailable" should {
        "return NextUpdatesQuarterlyReportingContentCheck" in {
          setupMockIsTaxYearCrystallisedCall(previousTaxYear)(Future.failed(error))
          setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.successful(yearToStatus))
          setupMockGetCurrentTaxYearEnd(taxYear)

          service.nextUpdatesPageOptOutViewModels().failed.map { ex =>
            ex shouldBe a[RuntimeException]
            ex.getMessage shouldBe error.getMessage
          }
        }
      }

      "ITSA Status from CY-1 till future years is unavailable" should {
        "return NextUpdatesQuarterlyReportingContentCheck" in {
          setupMockIsTaxYearCrystallisedCall(previousTaxYear)(Future.successful(crystallised))
          setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.failed(error))
          setupMockGetCurrentTaxYearEnd(taxYear)

          service.nextUpdatesPageOptOutViewModels().failed.map { ex =>
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

          stubCurrentTaxYear(CY)

          when(nextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(optOutTaxYear.taxYear))(any(), any()))
            .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(optOutTaxYear.taxYear, 0)))

          when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
          when(repository.recallOptOutInitialState()).thenReturn(
            Future.successful(Some(
              createOptOutProposition(CY, crystallisedPY, statusPY, statusCY, statusNY))))
          when(repository.fetchSavedIntent()).thenReturn(Future.successful(Some(optOutTaxYear.taxYear)))

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

          stubCurrentTaxYear(CY)

          when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
          when(repository.recallOptOutInitialState()).thenReturn(Future.successful(Some(
              createOptOutProposition(CY, crystallisedPY, statusPY, statusCY, statusNY))))
          when(repository.fetchSavedIntent()).thenReturn(Future.successful(Some(intent)))

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

          stubCurrentTaxYear(CY)

          when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
          when(repository.recallOptOutInitialState()).thenReturn(Future.successful(Some(
            createOptOutProposition(CY, crystallisedPY, statusPY, statusCY, statusNY))))

          val response = service.optOutConfirmedPageViewModel()

          response.futureValue shouldBe viewModel

        }
      }

    }

  }

  private def stubOptOut(currentTaxYear: TaxYear,
                         previousYearCrystallisedStatus: Boolean,
                         previousYearStatus: Value,
                         currentYearStatus: Value,
                         nextYearStatus: Value): Unit = {

    val (previousYear, currentYear, nextYear) = taxYears(currentTaxYear)

    stubCurrentTaxYear(currentYear)

    stubItsaStatuses(
      previousYear, previousYearStatus,
      currentYear, currentYearStatus,
      nextYear, nextYearStatus)

    stubCrystallisedStatus(previousYear, previousYearCrystallisedStatus)

    allowWriteOfOptOutDataToMongoToSucceed()
  }

  private def taxYears(currentYear: TaxYear): (TaxYear, TaxYear, TaxYear) = {
    val previousYear = currentYear.previousYear
    val nextTaxYear = currentTaxYear.nextYear
    (previousYear, currentYear, nextTaxYear)
  }

  private def stubCurrentTaxYear(currentYear: TaxYear): Unit = {
    when(mockDateService.getCurrentTaxYear).thenReturn(currentYear)
  }

  private def stubItsaStatuses(previousYear: TaxYear, previousYearStatus: Value,
                               currentYear: TaxYear, currentYearStatus: Value,
                               nextYear: TaxYear, nextYearStatus: Value): Unit = {
    val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
      previousYear -> StatusDetail("", previousYearStatus, ""),
      currentYear -> StatusDetail("", currentYearStatus, ""),
      nextYear -> StatusDetail("", nextYearStatus, "")
    )
    when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))
  }

  private def stubCrystallisedStatus(taxYear: TaxYear, crystallisedStatus: Boolean): Unit = {
    when(mockCalculationListService.isTaxYearCrystallised(taxYear)).thenReturn(Future.successful(crystallisedStatus))
  }

  private def allowWriteOfOptOutDataToMongoToSucceed(): Unit = {
    when(hc.sessionId).thenReturn(Some(SessionId(sessionIdValue)))
    when(repository.initialiseOptOutJourney(any())(any())).thenReturn(Future.successful(true))
  }

}

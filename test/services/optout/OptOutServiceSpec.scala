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
import connectors.itsastatus.ITSAStatusUpdateConnector
import connectors.itsastatus.ITSAStatusUpdateConnectorModel.{ErrorItem, ITSAStatusUpdateResponseFailure, ITSAStatusUpdateResponseSuccess}
import mocks.services._
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import models.itsaStatus.{StatusDetail, StatusReason}
import models.optout._
import models.optout.newJourney.OptOutTaxYearQuestionViewModel
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
import services.optout.OptOutTestSupport._
import services.reportingfreq.ReportingFrequency.QuarterlyUpdatesCountForTaxYearModel
import testConstants.ITSAStatusTestConstants.yearToStatus
import testUtils.TestSupport

import scala.concurrent.Future

case class TaxYearAndCountOfSubmissionsForIt(taxYear: TaxYear, submissions: Int)

class OptOutServiceSpec
  extends TestSupport
    with BeforeAndAfter
    with MockITSAStatusService
    with MockCalculationListService
    with MockDateService
    with OneInstancePerTest {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(10, Seconds), interval = Span(5, Millis))

  val mockITSAStatusUpdateConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val mockNextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])
  val mockRepository: OptOutSessionDataRepository = mock(classOf[OptOutSessionDataRepository])

  val mockAuditingService: AuditingService = mock(classOf[AuditingService])

  val taxYear2022_2023: TaxYear = TaxYear.forYearEnd(2023)
  val taxYear2023_2024: TaxYear = taxYear2022_2023.nextYear
  val taxYear2024_2025: TaxYear = taxYear2023_2024.nextYear
  val testNino = "AB123456C"
  val taxYear: TaxYear = TaxYear.forYearEnd(2021)
  val previousTaxYear: TaxYear = taxYear.previousYear
  val crystallised: Boolean = true

  val sessionIdValue = "123"
  val error = new RuntimeException("Some Error")

  val service: OptOutService =
    new OptOutService(
      itsaStatusUpdateConnector = mockITSAStatusUpdateConnector,
      itsaStatusService = mockITSAStatusService,
      calculationListService = mockCalculationListService,
      nextUpdatesService = mockNextUpdatesService,
      dateService = mockDateService,
      optOutRepository = mockRepository,
      auditingService = mockAuditingService
    )

  val apiError: String = "some api error"

  private def stubOptOut(currentTaxYear: TaxYear,
                         previousYearCrystallisedStatus: Boolean,
                         previousYearStatus: Value,
                         currentYearStatus: Value,
                         nextYearStatus: Value,
                         nino: String): Unit = {

    val (previousYear, currentYear, nextYear) = taxYears(currentTaxYear)

    stubCurrentTaxYear(currentYear)

    stubItsaStatuses(
      previousYear, previousYearStatus,
      currentYear, currentYearStatus,
      nextYear, nextYearStatus, nino)

    stubCrystallisedStatus(previousYear, previousYearCrystallisedStatus)

    allowWriteOfOptOutDataToMongoToSucceed()
  }

  private def taxYears(currentYear: TaxYear): (TaxYear, TaxYear, TaxYear) = {
    val previousYear = currentYear.previousYear
    val nextTaxYear = currentYear.nextYear
    (previousYear, currentYear, nextTaxYear)
  }

  private def stubCurrentTaxYear(currentYear: TaxYear): Unit = {
    when(mockDateService.getCurrentTaxYear).thenReturn(currentYear)
  }

  private def stubItsaStatuses(previousYear: TaxYear, previousYearStatus: Value,
                               currentYear: TaxYear, currentYearStatus: Value,
                               nextYear: TaxYear, nextYearStatus: Value, nino: String): Unit = {
    val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
      previousYear -> StatusDetail("", previousYearStatus, StatusReason.Rollover),
      currentYear -> StatusDetail("", currentYearStatus, StatusReason.Rollover),
      nextYear -> StatusDetail("", nextYearStatus, StatusReason.Rollover)
    )
    when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))
  }

  private def stubCrystallisedStatus(taxYear: TaxYear, crystallisedStatus: Boolean): Unit = {
    when(mockCalculationListService.isTaxYearCrystallised(taxYear)).thenReturn(Future.successful(crystallisedStatus))
  }

  private def allowWriteOfOptOutDataToMongoToSucceed(): Unit = {
    when(mockRepository.initialiseOptOutJourney(any())(any())).thenReturn(Future.successful(true))
  }

  ".getSubmissionCountForTaxYear" when {
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
            when(mockNextUpdatesService.getQuarterlyUpdatesCounts(same(year.taxYear))(any(), any()))
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
            when(mockNextUpdatesService.getQuarterlyUpdatesCounts(same(year.taxYear))(any(), any()))
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

  ".makeOptOutUpdateRequestForYear" when {

    "make opt-out update request for previous tax-year" should {

      "opt-out update request made for correct year" in {

        val currentYear = 2024
        val optOutTaxYear: TaxYear = TaxYear.forYearEnd(currentYear).previousYear

        when(mockITSAStatusUpdateConnector.optOut(any(), any())(any()))
          .thenReturn(Future(ITSAStatusUpdateResponseSuccess()))

        val proposition = buildOneYearOptOutPropositionForPreviousYear(currentYear)
        val intent = optOutTaxYear

        val result = service.makeOptOutUpdateRequest(proposition, intent)
        result.futureValue shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)
      }
    }

    "make opt-out update request for current tax-year" should {

      "opt-out update request made for correct year" in {

        val currentYear = 2024
        val optOutTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)

        when(mockITSAStatusUpdateConnector.optOut(any(), any())(any()))
          .thenReturn(Future(ITSAStatusUpdateResponseSuccess()))

        val proposition = buildOneYearOptOutPropositionForCurrentYear(currentYear)

        val result = service.makeOptOutUpdateRequest(proposition, optOutTaxYear)
        result.futureValue shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)
      }
    }

    "make opt-out update request for next tax-year" should {

      "opt-out update request made for correct year" in {

        val currentYear = 2024
        val optOutTaxYear: TaxYear = TaxYear.forYearEnd(currentYear).nextYear

        val proposition = buildOneYearOptOutPropositionForNextYear(currentYear)

        when(mockITSAStatusUpdateConnector.makeITSAStatusUpdate(any(), any(), any())(any()))
          .thenReturn(Future(ITSAStatusUpdateResponseSuccess()))

        when(mockITSAStatusUpdateConnector.optOut(any(), any())(any()))
          .thenReturn(Future(ITSAStatusUpdateResponseSuccess()))

        val intent = optOutTaxYear

        service.makeOptOutUpdateRequest(proposition, intent)
      }
    }

    "make opt-out update request for tax-year 2023-2024 and can opt-out of this year" should {

      "successful update request was made" in {

        val currentTaxYear: TaxYear = TaxYear(2023, 2024)

        when(mockITSAStatusUpdateConnector.optOut(any(), any())(any()))
          .thenReturn(Future(ITSAStatusUpdateResponseSuccess()))

        val proposition = OptOutTestSupport.buildOneYearOptOutPropositionForCurrentYear()

        val intent = currentTaxYear

        val result = service.makeOptOutUpdateRequest(proposition, intent)

        result.futureValue shouldBe ITSAStatusUpdateResponseSuccess(NO_CONTENT)
      }
    }

    "make opt-out update request for tax-year 2023-2024 and can not opt-out of this year" should {

      "return failure response for made update request" in {

        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val errorItems = List(ErrorItem("INVALID_TAXABLE_ENTITY_ID",
          "Submission has not passed validation. Invalid parameter taxableEntityId."))

        when(mockITSAStatusUpdateConnector.optOut(any(), any())(any()))
          .thenReturn(Future(ITSAStatusUpdateResponseFailure(errorItems)))

        val proposition = OptOutTestSupport.buildOneYearOptOutPropositionForCurrentYear()

        val intent = currentTaxYear

        val result = service.makeOptOutUpdateRequest(proposition, intent)

        result.futureValue shouldBe ITSAStatusUpdateResponseFailure(errorItems)
      }
    }

  }

  ".nextUpdatesPageChecksAndProposition" when {
      "fetching the opt out proposition for CY-1, CY and CY+1" should {
          "parse the downstream data correctly and return the ITSA Status and Crystallisation Status for each tax year" in {

            stubOptOut(
              currentTaxYear = taxYear2023_2024,
              previousYearCrystallisedStatus = false,
              previousYearStatus = Annual,
              currentYearStatus = Voluntary,
              nextYearStatus = Mandated,
              nino = testNino)

            val response = service.nextUpdatesPageChecksAndProposition()

            response.futureValue._1 shouldBe NextUpdatesQuarterlyReportingContentChecks(
              currentYearItsaStatus = true,
              previousYearItsaStatus = false,
              previousYearCrystallisedStatus = false
            )

            response.futureValue._2 shouldBe Some(
              OptOutOneYearViewModel(
                oneYearOptOutTaxYear = taxYear2023_2024,
                state = Some(OneYearOptOutFollowedByMandated)
              )
            )

            val model = response.futureValue._3
            model match {
              case m: OptOutProposition =>
                m.previousTaxYear shouldBe PreviousOptOutTaxYear(Annual, taxYear2023_2024.previousYear, crystallised = false)
                m.currentTaxYear shouldBe CurrentOptOutTaxYear(Voluntary, taxYear2023_2024)
                m.nextTaxYear shouldBe NextOptOutTaxYear(Mandated, taxYear2023_2024.nextYear, CurrentOptOutTaxYear(Voluntary, taxYear2023_2024))
              case _ => fail("model should be OptOutProposition")
            }
          }
      }

      "getStatusTillAvailableFutureYears api call fail" should {

        "return default response" in {

          val (previousYear, currentYear, _) = taxYears(currentYear = taxYear2023_2024)

          stubCurrentTaxYear(currentYear)

          when(mockITSAStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.failed(new RuntimeException("some api error")))

          stubCrystallisedStatus(previousYear, crystallisedStatus = false)

          val response = service.nextUpdatesPageChecksAndProposition()

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
            nextYear, Voluntary,
           testNino)

          when(mockCalculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.failed(new RuntimeException("some api error")))

          val response = service.nextUpdatesPageChecksAndProposition()

          response.failed.futureValue.getMessage shouldBe apiError
        }

    }

    ".nextUpdatesPageOptOutViewModels" when {
      "ITSA Status from CY-1 till future years and Calculation State for CY-1 is available" should {
        "return NextUpdatesQuarterlyReportingContentCheck" in {
          // Because we're saving the status as well as building view models...
          allowWriteOfOptOutDataToMongoToSucceed()

          setupMockIsTaxYearCrystallisedCall(previousTaxYear)(Future.successful(crystallised))
          setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.successful(yearToStatus))
          setupMockGetCurrentTaxYear(taxYear)

          val expected = NextUpdatesQuarterlyReportingContentChecks(
            currentYearItsaStatus = true,
            previousYearItsaStatus = false,
            previousYearCrystallisedStatus = crystallised)

          service.nextUpdatesPageChecksAndProposition().futureValue._1 shouldBe expected
        }
      }

      "Calculation State for CY-1 is unavailable" should {
        "return NextUpdatesQuarterlyReportingContentCheck" in {
          setupMockIsTaxYearCrystallisedCall(previousTaxYear)(Future.failed(error))
          setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.successful(yearToStatus))
          setupMockGetCurrentTaxYear(taxYear)

          service.nextUpdatesPageChecksAndProposition().failed.map { ex =>

            ex shouldBe a[RuntimeException]
            ex.getMessage shouldBe error.getMessage
          }
        }
      }

      "ITSA Status from CY-1 till future years is unavailable" should {
        "return NextUpdatesQuarterlyReportingContentCheck" in {
          setupMockIsTaxYearCrystallisedCall(previousTaxYear)(Future.successful(crystallised))
          setupMockGetStatusTillAvailableFutureYears(previousTaxYear)(Future.failed(error))
          setupMockGetCurrentTaxYear(taxYear)

          service.nextUpdatesPageChecksAndProposition().failed.map { ex =>
            ex shouldBe a[RuntimeException]
            ex.getMessage shouldBe error.getMessage
          }
        }
      }
    }
  }

  ".optOutCheckPointPageViewModel for single year case" when {
    val CY = TaxYear.forYearEnd(2024)
    val PY = CY.previousYear
    val NY = CY.nextYear
    val previousOptOutTaxYear = PreviousOptOutTaxYear(Voluntary, PY, crystallised = false)
    val currentOptOutTaxYear = CurrentOptOutTaxYear(Voluntary, CY)
    val nextOptOutTaxYear = NextOptOutTaxYear(Voluntary, NY, CurrentOptOutTaxYear(Mandated, CY))

    def testOptOutCheckPointPageViewModel(statusPY: ITSAStatus,
                                          statusCY: ITSAStatus,
                                          statusNY: ITSAStatus,
                                          crystallisedPY: Boolean
                                         )(optOutTaxYear: OptOutTaxYear, state: OptOutState): Unit = {

      def getTaxYearText(taxYear: TaxYear): String = {
        if (taxYear == CY) "CY" else if (taxYear == PY) "PY" else if (taxYear == NY) "NY" else ""
      }

      s"PY is $statusPY, CY is $statusCY, NY is $statusNY and PY is ${if (!crystallisedPY) "NOT "}finalised" should {
        s"offer ${getTaxYearText(optOutTaxYear.taxYear)} with state $state" in {

          stubCurrentTaxYear(CY)

          when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(optOutTaxYear.taxYear))(any(), any()))
            .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(optOutTaxYear.taxYear, 0)))

          when(mockRepository.recallOptOutPropositionWithIntent()).thenReturn(
            Future.successful(Some(
              createOptOutProposition(CY, crystallisedPY, statusPY, statusCY, statusNY), Some(optOutTaxYear.taxYear))))

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

  ".optOutCheckPointPageViewModel for multi year case" when {
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

          when(mockRepository.recallOptOutPropositionWithIntent()).thenReturn(Future.successful(Some(
            createOptOutProposition(CY, crystallisedPY, statusPY, statusCY, statusNY), Some(intent)
          )))

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

  ".optOutConfirmedPageViewModel" when {
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

          when(mockRepository.recallOptOutPropositionWithIntent()).thenReturn(Future.successful(Some(
            createOptOutProposition(CY, crystallisedPY, statusPY, statusCY, statusNY), None)))

          val response = service.optOutConfirmedPageViewModel()

          response.futureValue shouldBe viewModel

        }
      }

    }
  }

  ".getTaxYearForOptOutCancelled" when {

    "single year opt out scenario" when {

      "scenario: CY-1 == Mandated, CY == Annual, CY+1 == Voluntary" should {

        "return the correct tax year" in {

          val taxYear = TaxYear(2025, 2026)

          when(mockDateService.getCurrentTaxYear).thenReturn(taxYear)

          when(mockCalculationListService.isTaxYearCrystallised(any())(any(), any()))
            .thenReturn(Future.successful(false))

          stubOptOut(
            currentTaxYear = taxYear,
            previousYearCrystallisedStatus = false,
            previousYearStatus = Mandated,
            currentYearStatus = Annual,
            nextYearStatus = Voluntary,
            nino = testNino
          )

          when(mockRepository.fetchSavedIntent()).thenReturn(Future.successful(None))

          val result: Future[Option[TaxYear]] = service.getTaxYearForOptOutCancelled()

          val expected = Some(TaxYear(2026, 2027))

          result.futureValue shouldBe expected
        }
      }
    }

    "multi year opt out scenario, and the user has chosen a tax year" should {

      "return the correct tax year" in {

        val taxYear = TaxYear(2025, 2026)

        when(mockDateService.getCurrentTaxYear).thenReturn(taxYear)

        when(mockCalculationListService.isTaxYearCrystallised(any())(any(), any()))
          .thenReturn(Future.successful(false))

        stubOptOut(
          currentTaxYear = taxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = NoStatus,
          currentYearStatus = Voluntary,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        when(mockRepository.fetchSavedIntent()).thenReturn(Future.successful(Some(taxYear)))

        val result: Future[Option[TaxYear]] = service.getTaxYearForOptOutCancelled()

        val expected = Some(taxYear)

        result.futureValue shouldBe expected
      }
    }
  }

  "isOptOutTaxYearValid" should {
    "return an OptOutTaxQuestionViewModel" when {
      "the current tax year is submitted that has a state of MultiYearOptOutDefault" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2025"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Voluntary,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear))(any(), any()))
          .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear, 0)))

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)), Some(MultiYearOptOutDefault), 0, Voluntary, Voluntary))
      }
      "the next tax year is submitted that has a state of MultiYearOptOutDefault" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2026"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Voluntary,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026))), Some(MultiYearOptOutDefault), 0, Voluntary, Voluntary))
      }
      "the current tax year is submitted that has a state of OneYearOptOutFollowedByMandated" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2025"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = NoStatus,
          currentYearStatus = Voluntary,
          nextYearStatus = Mandated,
          nino = testNino
        )

        when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear))(any(), any()))
          .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear, 0)))

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)), Some(OneYearOptOutFollowedByMandated), 0, Voluntary, Mandated))
      }

      "the current tax year is submitted that has a state of OneYearOptOutFollowedByMandated with submitted quarterly updates" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2025"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = NoStatus,
          currentYearStatus = Voluntary,
          nextYearStatus = Mandated,
          nino = testNino
        )

        when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear))(any(), any()))
          .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear, 1)))

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)), Some(OneYearOptOutFollowedByMandated), 1, Voluntary, Mandated))
      }

      "the current tax year is submitted that has a state of OneYearOptOutFollowedByAnnual" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2025"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = NoStatus,
          currentYearStatus = Voluntary,
          nextYearStatus = Annual,
          nino = testNino
        )

        when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear))(any(), any()))
          .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear, 0)))

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)), Some(OneYearOptOutFollowedByAnnual), 0, Voluntary, Annual))
      }

      "the current tax year is submitted that has a state of OneYearOptOutFollowedByAnnual with submitted quarterly updates" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2025"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = NoStatus,
          currentYearStatus = Voluntary,
          nextYearStatus = Annual,
          nino = testNino
        )

        when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear))(any(), any()))
          .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear, 1)))

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(CurrentOptOutTaxYear(Voluntary, TaxYear(2025, 2026)), Some(OneYearOptOutFollowedByAnnual), 1, Voluntary, Annual))
      }

      "the next tax year is submitted that has a state of NextYearOptOut and CY is Annual" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2026"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = NoStatus,
          currentYearStatus = Annual,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear.nextYear))(any(), any()))
          .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear.nextYear, 0)))

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Annual, TaxYear(2025, 2026))), Some(NextYearOptOut), 0, Annual, Voluntary))
      }
      "the next tax year is submitted that has a state of NextYearOptOut and CY is Mandated" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2026"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = NoStatus,
          currentYearStatus = Mandated,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear.nextYear))(any(), any()))
          .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear.nextYear, 0)))

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(Mandated, TaxYear(2025, 2026))), Some(NextYearOptOut), 0, Mandated, Voluntary))
      }

      "the next tax year is submitted that has a state of NextYearOptOut and CY is not Mandated or Annual" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2026"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = DigitallyExempt,
          currentYearStatus = DigitallyExempt,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear.nextYear))(any(), any()))
          .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear.nextYear, 0)))

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(NextOptOutTaxYear(Voluntary, TaxYear(2026, 2027), CurrentOptOutTaxYear(DigitallyExempt, TaxYear(2025, 2026))), Some(NextYearOptOut), 0, DigitallyExempt, Voluntary))
      }

      "the previous tax year is submitted that has a state of MultiYearDefault" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2024"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Voluntary,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear.previousYear))(any(), any()))
          .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear.previousYear, 0)))

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), crystallised = false), Some(MultiYearOptOutDefault), 0, Voluntary, Voluntary))
      }

      "the previous tax year is submitted that has a state of OneYearOptOutFollowedByAnnual" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2024"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Annual,
          nextYearStatus = Annual,
          nino = testNino
        )

        when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear.previousYear))(any(), any()))
          .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear.previousYear, 0)))

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), crystallised = false), Some(OneYearOptOutFollowedByAnnual), 0, Annual, Annual))
      }
      "the previous tax year is submitted that has a state of OneYearOptOutFollowedByMandated" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2024"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Mandated,
          nextYearStatus = Mandated,
          nino = testNino
        )

        when(mockNextUpdatesService.getQuarterlyUpdatesCounts(ArgumentMatchers.eq(currentTaxYear.previousYear))(any(), any()))
          .thenReturn(Future.successful(QuarterlyUpdatesCountForTaxYear(currentTaxYear.previousYear, 0)))

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe Some(OptOutTaxYearQuestionViewModel(PreviousOptOutTaxYear(Voluntary, TaxYear(2024, 2025), crystallised = false), Some(OneYearOptOutFollowedByMandated), 0, Mandated, Mandated))
      }
    }
    "return None" when {
      "no tax year is submitted" in {
        val currentTaxYear = TaxYear(2025, 2026)

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Voluntary,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        val result = service.isOptOutTaxYearValid(None)

        result.futureValue shouldBe None
      }

      "an invalid tax year is submitted" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "Invalid"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Voluntary,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe None
      }
      "the previous tax year is submitted and it is crystallised" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2024"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = true,
          previousYearStatus = Voluntary,
          currentYearStatus = Voluntary,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe None
      }
      "the previous tax year is submitted and it is mandated" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2024"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = Mandated,
          currentYearStatus = Voluntary,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe None
      }
      "the query tax year matches the current mandated tax year" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2025"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Mandated,
          nextYearStatus = Voluntary,
          nino = testNino
        )

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe None
      }
      "the query tax year matches the next mandated tax year" in {
        val currentTaxYear = TaxYear(2025, 2026)
        val queryTaxYear = "2026"

        stubOptOut(
          currentTaxYear = currentTaxYear,
          previousYearCrystallisedStatus = false,
          previousYearStatus = Voluntary,
          currentYearStatus = Voluntary,
          nextYearStatus = Mandated,
          nino = testNino
        )

        val result = service.isOptOutTaxYearValid(Some(queryTaxYear))

        result.futureValue shouldBe None
      }
    }
  }
}
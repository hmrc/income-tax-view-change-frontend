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
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockITSAStatusUpdateConnector}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus.{Annual, ITSAStatus, Mandated, NoStatus, Voluntary}
import models.itsaStatus.{ITSAStatus, StatusDetail}
import models.optout.{NextUpdatesQuarterlyReportingContentChecks, OptOutOneYearCheckpointViewModel, OptOutOneYearViewModel}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.time.{Millis, Seconds, Span}
import play.mvc.Http.Status.{BAD_REQUEST, NO_CONTENT}
import services.{CalculationListService, DateServiceInterface, ITSAStatusService}
import testConstants.ITSAStatusTestConstants.yearToStatus
import testUtils.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

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
class OptOutServiceSpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with MockITSAStatusUpdateConnector {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  val optOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val itsaStatusService: ITSAStatusService = mockITSAStatusService
  val calculationListService: CalculationListService = mockCalculationListService
  val dateService: DateServiceInterface = mockDateService

  implicit val user: MtdItUser[_] = mock(classOf[MtdItUser[_]])
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  val taxYear: TaxYear = TaxYear.forYearEnd(2021)
  val previousTaxYear: TaxYear = taxYear.previousYear
  val crystallised: Boolean = true

  val error = new RuntimeException("Some Error")

  val service = new OptOutService(optOutConnector, itsaStatusService, calculationListService, dateService)

  before {
    reset(optOutConnector, itsaStatusService, calculationListService, dateService, user, hc)
  }

  val noOptOutOptionAvailable = None

  "OptOutService.makeOptOutUpdateRequestForYear" when {

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
        val proposition = OptOutTestSupport.buildOneYearOptOutDataForCurrentYear()
        val intent = proposition.availableOptOutYears.head
        val result = service.makeOptOutUpdateRequest(proposition, intent)

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
        val proposition = OptOutTestSupport.buildOneYearOptOutDataForCurrentYear()
        val intent = proposition.availableOptOutYears.head
        val result = service.makeOptOutUpdateRequest(proposition, intent)

        result.futureValue shouldBe OptOutUpdateResponseFailure(correlationId, BAD_REQUEST, errorItems)
      }
    }
  }

  "OptOutService.nextUpdatesPageOneYearOptOutViewModel" when {

    s"PY is $Voluntary, CY is $NoStatus, NY is $NoStatus and PY is NOT finalised" should {

      "offer PY as OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        when(calculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

        val response = service.nextUpdatesPageOneYearOptOutViewModel()

        response.futureValue shouldBe Some(OptOutOneYearViewModel(TaxYear.forYearEnd(2023), showWarning = false))

      }
    }

    s"PY is $Voluntary, CY is $NoStatus NY is $NoStatus and PY is finalised" should {

      "offer No OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        when(calculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(true))

        val response = service.nextUpdatesPageOneYearOptOutViewModel()

        response.futureValue shouldBe noOptOutOptionAvailable
      }
    }

    s"PY is $NoStatus, CY is $Voluntary, NY is $Mandated" should {

      "offer CY OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Mandated, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        when(calculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

        val response = service.nextUpdatesPageOneYearOptOutViewModel()

        response.futureValue shouldBe Some(OptOutOneYearViewModel(TaxYear.forYearEnd(2024), showWarning = true))
      }
    }

    s"PY is $NoStatus, CY is $NoStatus, NY is $Voluntary" should {

      "offer NY OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        when(calculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

        val response = service.nextUpdatesPageOneYearOptOutViewModel()

        response.futureValue shouldBe Some(OptOutOneYearViewModel(TaxYear.forYearEnd(2025), showWarning = false))
      }
    }

    "Single Year OptOut" when {
      s"PY : PY is $Voluntary, CY is $Mandated, NY is $Mandated and PY is NOT crystallised" should {
        s"offer PY OptOut Option with a warning as following year (CY) is $Mandated " in {

          val currentYear = 2024
          val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
          when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

          val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
            TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
            TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.Mandated, ""),
            TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Mandated, ""),
          )
          when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

          when(calculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

          val response = service.nextUpdatesPageOneYearOptOutViewModel()

          val model = response.futureValue.get
          model.oneYearOptOutTaxYear shouldBe previousYear
          model.showWarning shouldBe true
        }
        s"CY : PY is $Mandated, CY is $Voluntary, NY is $Mandated " should {
          s"offer CY OptOut Option with a warning as following year (NY) is $Mandated " in {

            val currentYear = 2024
            val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
            when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

            val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
              TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Mandated, ""),
              TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.Voluntary, ""),
              TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Mandated, ""),
            )
            when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

            when(calculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

            val response = service.nextUpdatesPageOneYearOptOutViewModel()

            val model = response.futureValue.get
            model.oneYearOptOutTaxYear shouldBe TaxYear.forYearEnd(currentYear)
            model.showWarning shouldBe true
          }
        }
      }


      "getStatusTillAvailableFutureYears api call fail" should {

        "return default response" in {

          val currentYear = 2024
          val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
          when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

          when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.failed(new RuntimeException("some api error")))

          when(calculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(false))

          val response = service.nextUpdatesPageOneYearOptOutViewModel()

          response.futureValue shouldBe noOptOutOptionAvailable
        }
      }

      "isTaxYearCrystallised api call fail" should {

        "return default response" in {

          val currentYear = 2024
          val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
          when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

          val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
            TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
            TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
            TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          )
          when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

          when(calculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.failed(new RuntimeException("some api error")))

          val response = service.nextUpdatesPageOneYearOptOutViewModel()

          response.futureValue shouldBe noOptOutOptionAvailable
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
  "OptOutService.optOutCheckPointPageViewModel" when {
    val CY = TaxYear.forYearEnd(2024)
    val PY = CY.previousYear
    val NY = CY.nextYear

    def testOptOutCheckPointPageViewModel(statusPY: ITSAStatus, statusCY: ITSAStatus, statusNY: ITSAStatus, crystallisedPY: Boolean)
                                         (taxYear: TaxYear, showWarning: Boolean): Unit = {

      def getTaxYearText(taxYear: TaxYear): String = {
        if (taxYear == CY) "CY" else if (taxYear == PY) "PY" else if (taxYear == NY) "NY" else ""
      }

      s"PY is $statusPY, CY is $statusCY, NY is $statusNY and PY is ${if (!crystallisedPY) "NOT "}finalised" should {
        s"offer ${getTaxYearText(taxYear)} ${if (showWarning) "with  warning"}" in {

          val previousYear: TaxYear = PY
          when(dateService.getCurrentTaxYear).thenReturn(CY)

          val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
            PY -> StatusDetail("", statusPY, ""),
            CY -> StatusDetail("", statusCY, ""),
            NY -> StatusDetail("", statusNY, ""),
          )
          when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

          when(calculationListService.isTaxYearCrystallised(previousYear)).thenReturn(Future.successful(crystallisedPY))

          val response = service.optOutCheckPointPageViewModel()

          response.futureValue shouldBe Some(OptOutOneYearCheckpointViewModel(taxYear, showWarning))

        }
      }

    }

    val testCases = List(
      ((Mandated, Voluntary, Annual, false), (CY, true)),
      ((Mandated, Mandated, Voluntary, false), (NY, true)),
      ((Voluntary, Annual, Mandated, false), (PY, true))
    )
    testCases.foreach {
      case (input, output) =>
        val test = testOptOutCheckPointPageViewModel _
        test.tupled(input).tupled(output)
    }

  }
}

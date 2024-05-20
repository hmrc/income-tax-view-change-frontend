package services.optout

import auth.MtdItUser
import connectors.ITSAStatusUpdateConnector
import mocks.services.{MockCalculationListService, MockDateService, MockITSAStatusService, MockITSAStatusUpdateConnector}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.{ITSAStatus, StatusDetail}
import models.optOut.OptOutUpdateRequestModel.{OptOutUpdateResponseFailure, OptOutUpdateResponseSuccess, itsaOptOutUpdateReason}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, times, verify, when}
import org.scalatest.BeforeAndAfter
import play.mvc.Http.Status.NO_CONTENT
import services.{CalculationListService, DateServiceInterface, ITSAStatusService}
import testUtils.UnitSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OptOutServiceMakeOptOutUpdateRequestSpec extends UnitSpec
  with BeforeAndAfter
  with MockITSAStatusService
  with MockCalculationListService
  with MockDateService
  with MockITSAStatusUpdateConnector {

  val optOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])
  val itsaStatusService: ITSAStatusService = mockITSAStatusService
  val calculationListService: CalculationListService = mockCalculationListService
  val dateService: DateServiceInterface = mockDateService

  val service = new OptOutService(optOutConnector, itsaStatusService, calculationListService, dateService)

  implicit val user: MtdItUser[_] = mock(classOf[MtdItUser[_]])
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  before {
    reset(optOutConnector, itsaStatusService, calculationListService, dateService, user, hc)
  }

  "OptOutService.makeOptOutUpdateRequest" when {

    "make opt-out update request for one year, PY is offered" should {

      "successful update request was made" in {

        val correlationId = "123"
        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear

        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))
        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(currentTaxYear.previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))
        when(calculationListService.isTaxYearCrystallised(currentTaxYear.previousYear)).thenReturn(Future.successful(false))

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(previousTaxYear, taxableEntityId, itsaOptOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))

        val intent = currentTaxYear.previousYear
        val result = service.makeOptOutUpdateRequest(Some(intent))

        result.futureValue shouldBe OptOutUpdateResponseSuccess(correlationId, NO_CONTENT)
      }
    }

    "make opt-out update request for multi year, PY, CY, NY is offered" should {

      "successful update request was made" in {

        val correlationId = "123"
        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear
        val nextTaxYear: TaxYear = currentTaxYear.nextYear

        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))
        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(currentTaxYear.previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))
        when(calculationListService.isTaxYearCrystallised(currentTaxYear.previousYear)).thenReturn(Future.successful(false))

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(previousTaxYear, taxableEntityId, itsaOptOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))
        when(optOutConnector.requestOptOutForTaxYear(currentTaxYear, taxableEntityId, itsaOptOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))
        when(optOutConnector.requestOptOutForTaxYear(nextTaxYear, taxableEntityId, itsaOptOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))

        val intent = currentTaxYear.previousYear
        val result = service.makeOptOutUpdateRequest(Some(intent))

        verify(optOutConnector, times(3)).requestOptOutForTaxYear(any(), any(), any())(any())

        result.futureValue shouldBe OptOutUpdateResponseSuccess(correlationId, NO_CONTENT)
      }
    }

    "make opt-out update request for multi year, PY, CY, NY is offered and one fails" should {

      "successful update request was made" in {

        val correlationId = "123"
        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val previousTaxYear: TaxYear = currentTaxYear.previousYear
        val nextTaxYear: TaxYear = currentTaxYear.nextYear

        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))
        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(currentTaxYear.previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))
        when(calculationListService.isTaxYearCrystallised(currentTaxYear.previousYear)).thenReturn(Future.successful(false))

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(previousTaxYear, taxableEntityId, itsaOptOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))
        when(optOutConnector.requestOptOutForTaxYear(currentTaxYear, taxableEntityId, itsaOptOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseFailure.defaultFailure()
        ))
        when(optOutConnector.requestOptOutForTaxYear(nextTaxYear, taxableEntityId, itsaOptOutUpdateReason)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))

        val intent = currentTaxYear.previousYear
        val result = service.makeOptOutUpdateRequest(Some(intent))

        result.futureValue shouldBe OptOutUpdateResponseFailure.defaultFailure()
      }
    }


  }
}

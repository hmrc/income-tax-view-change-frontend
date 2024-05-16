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
import connectors.OptOutConnector
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.{ITSAStatus, StatusDetail}
import models.optOut.OptOutUpdateRequestModel.{ErrorItem, OptOutUpdateResponseFailure, OptOutUpdateResponseSuccess}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpecLike
import play.mvc.Http.Status.{BAD_REQUEST, NO_CONTENT}
import services.optout.OptOutTestSupport.{buildMultiYearOptOutData, buildOneYearOptOutDataForCurrentYear, buildOneYearOptOutDataForNextYear, buildOneYearOptOutDataForPreviousYear}
import services.{CalculationListService, DateServiceInterface, ITSAStatusService}
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
class OptOutServiceTest extends AnyWordSpecLike with Matchers with BeforeAndAfter with ScalaFutures {

  implicit val defaultPatience: PatienceConfig =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  val optOutConnector: OptOutConnector = mock(classOf[OptOutConnector])
  val itsaStatusService: ITSAStatusService = mock(classOf[ITSAStatusService])
  val calculationListService: CalculationListService = mock(classOf[CalculationListService])
  val dateService: DateServiceInterface = mock(classOf[DateServiceInterface])

  implicit val user: MtdItUser[_] = mock(classOf[MtdItUser[_]])
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  val service = new OptOutService(optOutConnector, itsaStatusService, calculationListService, dateService)

  before {
    reset(optOutConnector, itsaStatusService, calculationListService, dateService, user, hc)
  }

  val noOptOutOptionAvailable = None

  "OptOutService.makeOptOutUpdateRequestForYear" when {

    "make opt-out update request for previous tax-year" should {

      "opt-out update request made for correct year" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val optOutTaxYear: TaxYear = TaxYear.forYearEnd(currentYear).previousYear
        val correlationId = "123"

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(optOutTaxYear, taxableEntityId)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))
        service.makeOptOutUpdateRequestFor(buildOneYearOptOutDataForPreviousYear(currentYear))
        verify(optOutConnector, times(1)).requestOptOutForTaxYear(TaxYear.forYearEnd(currentYear).previousYear, taxableEntityId)
      }
    }

    "make opt-out update request for current tax-year" should {

      "opt-out update request made for correct year" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val optOutTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)
        val correlationId = "123"

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(optOutTaxYear, taxableEntityId)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))
        service.makeOptOutUpdateRequestFor(buildOneYearOptOutDataForCurrentYear(currentYear))
        verify(optOutConnector, times(1)).requestOptOutForTaxYear(TaxYear.forYearEnd(currentYear), taxableEntityId)
      }
    }

    "make opt-out update request for next tax-year" should {

      "opt-out update request made for correct year" in {

        val taxableEntityId = "456"
        val currentYear = 2024
        val optOutTaxYear: TaxYear = TaxYear.forYearEnd(currentYear).nextYear
        val correlationId = "123"

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(optOutTaxYear, taxableEntityId)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))
        service.makeOptOutUpdateRequestFor(buildOneYearOptOutDataForNextYear(currentYear))
        verify(optOutConnector, times(1)).requestOptOutForTaxYear(TaxYear.forYearEnd(currentYear).nextYear, taxableEntityId)
      }
    }
  }

  "OptOutService.makeOptOutUpdateRequestForYear" when {

    "make opt-out update request for tax-year 2023-2024 and can opt-out of this year" should {

      "successful update request was made" in {

        val correlationId = "123"
        val taxableEntityId = "456"
        val currentYear = 2024
        val currentTaxYear: TaxYear = TaxYear.forYearEnd(currentYear)

        when(user.nino).thenReturn(taxableEntityId)
        when(optOutConnector.requestOptOutForTaxYear(currentTaxYear, taxableEntityId)).thenReturn(Future.successful(
          OptOutUpdateResponseSuccess(correlationId)
        ))

        val result = service.makeOptOutUpdateRequestFor(buildOneYearOptOutDataForCurrentYear(currentYear))

        result.futureValue shouldBe List(OptOutUpdateResponseSuccess(correlationId, NO_CONTENT))
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
        when(optOutConnector.requestOptOutForTaxYear(currentTaxYear, taxableEntityId)).thenReturn(Future.successful(
          OptOutUpdateResponseFailure(correlationId, BAD_REQUEST, errorItems)
        ))

        val result = service.makeOptOutUpdateRequestFor(buildOneYearOptOutDataForCurrentYear(currentYear))

        result.futureValue shouldBe List(OptOutUpdateResponseFailure(correlationId, BAD_REQUEST, errorItems))
      }
    }
  }

  "OptOutService.displayOptOutMessage" when {

    "PY is V, CY is U, NY is U and PY is NOT finalised" should {

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

        val model = response.futureValue.get
        assert(model.oneYearOptOutTaxYear.startYear == 2022)
        assert(model.oneYearOptOutTaxYear.endYear == 2023)
      }
    }

    "PY is V, CY is U, NY is U and PY is finalised" should {

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

        val actualOptOutOption = response.futureValue
        assert(actualOptOutOption == noOptOutOptionAvailable)

      }
    }

    "PY is U, CY is V, NY is M" should {

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

        val model = response.futureValue.get
        assert(model.oneYearOptOutTaxYear.startYear == 2023)
        assert(model.oneYearOptOutTaxYear.endYear == 2024)
      }
    }

    "PY is U, CY is U, NY is V" should {

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

        val model = response.futureValue.get
        assert(model.oneYearOptOutTaxYear.startYear == 2024)
        assert(model.oneYearOptOutTaxYear.endYear == 2025)
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

        val actualOptOutOption = response.futureValue
        assert(actualOptOutOption == noOptOutOptionAvailable)
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

        val actualOptOutOption = response.futureValue
        assert(actualOptOutOption == noOptOutOptionAvailable)
      }
    }
  }
}

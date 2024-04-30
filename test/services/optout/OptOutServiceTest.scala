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
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.{ITSAStatus, StatusDetail}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import services.{CalculationListService, DateServiceInterface, ITSAStatusService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

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
class OptOutServiceTest extends AnyWordSpecLike with Matchers with BeforeAndAfter {

  val itsaStatusService: ITSAStatusService = mock(classOf[ITSAStatusService])
  val calculationListService: CalculationListService = mock(classOf[CalculationListService])
  val dateService: DateServiceInterface = mock(classOf[DateServiceInterface])

  implicit val user: MtdItUser[_] = mock(classOf[MtdItUser[_]])
  implicit val hc: HeaderCarrier = mock(classOf[HeaderCarrier])

  val service = new OptOutService(itsaStatusService, calculationListService, dateService)

  before {
    reset(itsaStatusService, calculationListService, dateService, user, hc)
  }

  "OptOutService.displayOptOutMessage" when {

    "PY is V, CY is U, NY is U and PY is NOT finalised" should {

      "offer PY as OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear(currentYear - 1)
        when(dateService.getCurrentTaxYearEnd).thenReturn(currentYear)

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear(currentYear + 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        val finalised: Option[Boolean] = Some(false)
        when(calculationListService.isTaxYearCrystallised(previousYear.endYear)).thenReturn(Future.successful(finalised))


        val response = service.displayOptOutMessage()

        Await.result(response, 10.seconds)

        response.value match {
          case Some(t) => t match {
            case Success(r) =>
              assert(r.canOptOut, "canOptOut should be true")
              assert(r.firstYear.startYear == 2022)
              assert(r.firstYear.endYear == 2023)
            case Failure(e) => fail(s"future should have succeeded, but failed with error: ${e.getMessage}")
          }
          case _ =>
        }
      }
    }

    "PY is V, CY is U, NY is U and PY is finalised" should {

      "offer No OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear(currentYear - 1)
        when(dateService.getCurrentTaxYearEnd).thenReturn(currentYear)

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear(currentYear + 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        val finalised: Option[Boolean] = Some(true)
        when(calculationListService.isTaxYearCrystallised(previousYear.endYear)).thenReturn(Future.successful(finalised))

        val response = service.displayOptOutMessage()

        Await.result(response, 10.seconds)

        response.value match {
          case Some(t) => t match {
            case Success(r) =>
              assert(!r.canOptOut, "canOptOut should be true")
              assert(r.taxYears.isEmpty)
            case Failure(e) => fail(s"future should have succeeded, but failed with error: ${e.getMessage}")
          }
          case _ =>
        }
      }
    }

    "PY is U, CY is V, NY is U" should {

      "offer CY OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear(currentYear - 1)
        when(dateService.getCurrentTaxYearEnd).thenReturn(currentYear)

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear(currentYear - 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear(currentYear) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear(currentYear + 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        val finalised: Option[Boolean] = Some(false)
        when(calculationListService.isTaxYearCrystallised(previousYear.endYear)).thenReturn(Future.successful(finalised))

        val response = service.displayOptOutMessage()

        Await.result(response, 10.seconds)

        response.value match {
          case Some(t) => t match {
            case Success(r) =>
              assert(r.canOptOut, "canOptOut should be true")
              assert(r.firstYear.startYear == 2023)
              assert(r.firstYear.endYear == 2024)
            case Failure(e) => fail(s"future should have succeeded, but failed with error: ${e.getMessage}")
          }
          case _ =>
        }
      }
    }

    "PY is U, CY is U, NY is V" should {

      "offer NY OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear(currentYear - 1)
        when(dateService.getCurrentTaxYearEnd).thenReturn(currentYear)

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear(currentYear - 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear(currentYear + 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        val finalised: Option[Boolean] = Some(false)
        when(calculationListService.isTaxYearCrystallised(previousYear.endYear)).thenReturn(Future.successful(finalised))

        val response = service.displayOptOutMessage()

        Await.result(response, 10.seconds)

        response.value match {
          case Some(t) => t match {
            case Success(r) =>
              assert(r.canOptOut, "canOptOut should be true")
              assert(r.firstYear.startYear == 2024)
              assert(r.firstYear.endYear == 2025)
            case Failure(e) => fail(s"future should have succeeded, but failed with error: ${e.getMessage}")
          }
          case _ =>
        }
      }
    }

  }
}

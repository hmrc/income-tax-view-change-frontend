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
        val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        val finalised: Option[Boolean] = Some(false)
        when(calculationListService.isTaxYearCrystallised(previousYear.endYear)).thenReturn(Future.successful(finalised))

        val response = service.displayOptOutMessage()

        Await.result(response, 10.seconds)

        response.value match {
          case Some(t) => t match {
            case Success(r) => r match {
              case Some(oneYearViewModel) =>
                assert(oneYearViewModel.oneYearOptOutTaxYear.startYear == 2022)
                assert(oneYearViewModel.oneYearOptOutTaxYear.endYear == 2023)
              case None => fail("cant opt out")
            }
            case Failure(e) => fail(s"future should have succeeded, but failed with error: ${e.getMessage}")
          }
          case _ =>
        }
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

        val finalised: Option[Boolean] = Some(true)
        when(calculationListService.isTaxYearCrystallised(previousYear.endYear)).thenReturn(Future.successful(finalised))

        val response = service.displayOptOutMessage()

        Await.result(response, 10.seconds)

        response.value match {
          case Some(t) => t match {
            case Success(r) => r match {
              case None => //no opt out expected
              case Some(oneYearViewModel) =>
                fail("One Year Opt Out offered when illegal")
              }
            case Failure(e) => fail(s"future should have succeeded, but failed with error: ${e.getMessage}")
          }
          case _ =>
        }
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

        val finalised: Option[Boolean] = Some(false)
        when(calculationListService.isTaxYearCrystallised(previousYear.endYear)).thenReturn(Future.successful(finalised))

        val response = service.displayOptOutMessage()

        Await.result(response, 10.seconds)

        response.value match {
          case Some(t) => t match {
            case Success(r) => r match {
              case Some(oneYearViewModel) =>
                assert(oneYearViewModel.oneYearOptOutTaxYear.startYear == 2023)
                assert(oneYearViewModel.oneYearOptOutTaxYear.endYear == 2024)

              case None => fail("No opt out offered")
            }

            case Failure(e) => fail(s"future should have succeeded, but failed with error: ${e.getMessage}")
          }
          case _ =>
        }
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

        val finalised: Option[Boolean] = Some(false)
        when(calculationListService.isTaxYearCrystallised(previousYear.endYear)).thenReturn(Future.successful(finalised))

        val response = service.displayOptOutMessage()

        Await.result(response, 10.seconds)

        response.value match {
          case Some(t) => t match {
            case Success(r) => r match {
              case Some(oneYearViewModel) =>
                assert(oneYearViewModel.oneYearOptOutTaxYear.startYear == 2024)
                assert(oneYearViewModel.oneYearOptOutTaxYear.endYear == 2025)
              case None => fail("No opt out offered")
            }
            case Failure(e) => fail(s"future should have succeeded, but failed with error: ${e.getMessage}")
          }
          case _ =>
        }
      }
    }

    "getStatusTillAvailableFutureYears api call fail" should {

      "return default response" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.failed(new RuntimeException("some api error")))

        val finalised: Option[Boolean] = Some(false)
        when(calculationListService.isTaxYearCrystallised(previousYear.endYear)).thenReturn(Future.successful(finalised))

        val response = service.displayOptOutMessage()

        Await.result(response, 10.seconds)

        response.value match {
          case Some(t) => t match {
            case Success(r) => r match {
              case None => //No opt out expected
              case Some(oneYearViewModel) =>
                fail("Opt Out Offered with API failure no allowed")
            }
            case Failure(e) => fail(s"future should have succeeded, but failed with error: ${e.getMessage}")
          }
          case _ =>
        }
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

        when(calculationListService.isTaxYearCrystallised(previousYear.endYear)).thenReturn(Future.failed(new RuntimeException("some api error")))

        val response = service.displayOptOutMessage()

        Await.result(response, 10.seconds)

        response.value match {
          case Some(t) => t match {
            case Success(r) => r match {
              case None => //No opt out expected
              case Some(oneYearViewModel) =>
                fail("Opt out offered when isCrystallised API failed not allowed")
            }
            case Failure(e) => fail(s"future should have succeeded, but failed with error: ${e.getMessage}")
          }
          case _ =>
        }
      }
    }

    "PY is V, CY is U, NY is U and finalised is missing from api call response" should {

      "assume finalised as false and offer PY as OptOut Option" in {

        val currentYear = 2024
        val previousYear: TaxYear = TaxYear.forYearEnd(currentYear - 1)
        when(dateService.getCurrentTaxYear).thenReturn(TaxYear.forYearEnd(currentYear))

        val taxYearStatusDetailMap: Map[TaxYear, StatusDetail] = Map(
          TaxYear.forYearEnd(currentYear - 1) -> StatusDetail("", ITSAStatus.Voluntary, ""),
          TaxYear.forYearEnd(currentYear) -> StatusDetail("", ITSAStatus.NoStatus, ""),
          TaxYear.forYearEnd(currentYear + 1) -> StatusDetail("", ITSAStatus.NoStatus, ""),
        )
        when(itsaStatusService.getStatusTillAvailableFutureYears(previousYear)).thenReturn(Future.successful(taxYearStatusDetailMap))

        val finalised: Option[Boolean] = None
        when(calculationListService.isTaxYearCrystallised(previousYear.endYear)).thenReturn(Future.successful(finalised))

        val response = service.displayOptOutMessage()

        Await.result(response, 10.seconds)

        response.value match {
          case Some(t) => t match {
            case Success(r) => r match {
              case Some(oneYearViewModel) =>
                assert(oneYearViewModel.oneYearOptOutTaxYear.startYear == 2022)
                assert(oneYearViewModel.oneYearOptOutTaxYear.endYear == 2023)
              case None => fail("no oo")
            }
            case Failure(e) => fail(s"future should have succeeded, but failed with error: ${e.getMessage}")
          }
          case _ =>
        }
      }
    }

  }
}

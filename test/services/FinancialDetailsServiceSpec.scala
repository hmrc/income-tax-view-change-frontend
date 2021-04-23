/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import java.time.LocalDate

import assets.BaseTestConstants._
import assets.BusinessDetailsTestConstants.getCurrentTaxYearEnd
import assets.FinancialDetailsTestConstants._
import auth.MtdItUser
import config.featureswitch.{API5, FeatureSwitching}
import controllers.Assets.INTERNAL_SERVER_ERROR
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.core.AccountingPeriodModel
import models.financialDetails._
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status
import play.api.test.FakeRequest
import testUtils.TestSupport
import uk.gov.hmrc.http.InternalServerException

import scala.concurrent.Future


class FinancialDetailsServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector with FeatureSwitching {

  override def beforeEach(): Unit = {
    disable(API5)
    super.beforeEach()
  }

  private val april = 4
  private val fifth = 5
  private val sixth = 6

  private def getTaxEndYear(date: LocalDate): Int = {
    if (date.isBefore(LocalDate.of(date.getYear, april, sixth))) date.getYear
    else date.getYear + 1
  }

  private def getFinancialDetailSuccess(taxYear: Int,
                                        charges: List[Charge] = List(fullChargeModel)): FinancialDetailsModel = {
    FinancialDetailsModel(charges)
  }

  private def mtdUser(numYears: Int): MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(
      testMtditid,
      None,
      businesses = (1 to numYears).toList.map { count =>
        BusinessDetailsModel(
          incomeSourceId = s"income-id-$count",
          accountingPeriod = AccountingPeriodModel(
            start = LocalDate.of(getTaxEndYear(LocalDate.now.minusYears(count)), april, sixth),
            end = LocalDate.of(getTaxEndYear(LocalDate.now.minusYears(count - 1)), april, fifth)
          ),
          None, None, None, None, None, None, None, None,
          firstAccountingPeriodEndDate = Some(LocalDate.of(getTaxEndYear(LocalDate.now.minusYears(count - 1)), april, fifth))
        )
      },
      property = None
    ),
    Some("testUtr"),
    Some("testCredId"),
    Some("Individual"),
    None
  )(FakeRequest())

  object TestFinancialDetailsService extends FinancialDetailsService(mockIncomeTaxViewChangeConnector)

  val testUserWithRecentYears: MtdItUser[_] = MtdItUser(testMtditid, testNino, None, IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(
      BusinessDetailsModel(
        "testId",
        AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
        None, None, None, None, None, None, None, None,
        Some(getCurrentTaxYearEnd.minusYears(1))
      )
    ),
    property = None
  ), None, None, None, None)(FakeRequest())

  "getFinancialDetails" when {
    "a successful financial details response is returned from the connector" should {
      "return a valid FinancialDetails model" in {
        setupMockGetFinancialDetails(testTaxYear, testNino)(financialDetailsModel(testTaxYear))
        await(TestFinancialDetailsService.getFinancialDetails(testTaxYear, testNino)) shouldBe financialDetailsModel(testTaxYear)
      }
    }
    "a error model is returned from the connector" should {
      "return a FinancialDetailsError model" in {
        setupMockGetFinancialDetails(testTaxYear, testNino)(testFinancialDetailsErrorModel)
        await(TestFinancialDetailsService.getFinancialDetails(testTaxYear, testNino)) shouldBe testFinancialDetailsErrorModel
      }
    }
  }

  "getChargeDueDates" when {
    "a financial detail returned from the connector returns a non 404 error model" should {
      "return an InternalServerException" in {
        val financialDetails: FinancialDetailsModel = FinancialDetailsModel(List(
          Charge("testYear", "testTransactionId", None, None, None, None, Some(100.00), None, None, None, Some(Seq(
            SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.toString), None, None),
            SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(1).toString), None, None)
          ))),
          Charge("testYear", "testTransactionId", None, None, None, None, Some(100.00), None, None, None, Some(Seq(
            SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(2).toString), None, None),
            SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(3).toString), None, None)
          )))
        ))

        setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear, testNino)(
          FinancialDetailsErrorModel(INTERNAL_SERVER_ERROR, "internal server error")
        )
        setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear - 1, testNino)(
          financialDetails
        )

        val result: Future[Option[Either[(LocalDate, Boolean), Int]]] = {
          TestFinancialDetailsService.getChargeDueDates(implicitly, testUserWithRecentYears)
        }

        intercept[InternalServerException](await(result))
          .message shouldBe "[FinancialDetailsService][getChargeDueDates] - Failed to retrieve successful financial details"
      }
    }
    "financial details are returned successfully" should {
      "return a single overdue date" when {
        "there is only one overdue date" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(List(
            Charge("testYear1", "testTransactionId", None, None, None, None, Some(100.00), None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.minusDays(1).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.toString), None, None)
            ))),
            Charge("testYear1", "testTransactionId", None, None, None, None, Some(200.00), None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(1).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(2).toString), None, None)
            )))
          ))

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(List(
            Charge("testYear2", "testTransactionId", None, None, None, None, Some(100.00), None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(3).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(4).toString), None, None)
            ))),
            Charge("testYear2", "testTransactionId", None, None, None, None, None, None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(5).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(6).toString), None, None)
            )))
          ))

          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear, testNino)(
            financialDetailsCurrentYear
          )
          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear - 1, testNino)(
            financialDetailsLastYear
          )

          val result: Future[Option[Either[(LocalDate, Boolean), Int]]] = {
            TestFinancialDetailsService.getChargeDueDates(implicitly, testUserWithRecentYears)
          }

          await(result) shouldBe Some(Left(LocalDate.now.minusDays(1) -> true))
        }
      }
      "return a single non-overdue date" when {
        "there are no overdue dates, but there are dates upcoming" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(List(
            Charge("testYear1", "testTransactionId", None, None, None, None, Some(100.00), None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(7).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.toString), None, None)
            ))),
            Charge("testYear1", "testTransactionId", None, None, None, None, Some(100.00), None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(1).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(2).toString), None, None)
            )))
          ))

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(List(
            Charge("testYear2", "testTransactionId", None, None, None, None, None, None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(3).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(4).toString), None, None)
            ))),
            Charge("testYear2", "testTransactionId", None, None, None, None, Some(100.00), None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(5).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(6).toString), None, None)
            )))
          ))

          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear, testNino)(
            financialDetailsCurrentYear
          )
          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear - 1, testNino)(
            financialDetailsLastYear
          )

          val result: Future[Option[Either[(LocalDate, Boolean), Int]]] = {
            TestFinancialDetailsService.getChargeDueDates(implicitly, testUserWithRecentYears)
          }

          await(result) shouldBe Some(Left(LocalDate.now -> false))
        }
      }
      "return the count of overdue dates" when {
        "there are more than one overdue dates" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(List(
            Charge("testYear1", "testTransactionId", None, None, None, None, Some(100.00), None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.minusDays(1).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.toString), None, None)
            ))),
            Charge("testYear1", "testTransactionId", None, None, None, None, Some(100.00), None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(1).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(2).toString), None, None)
            )))
          ))

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(List(
            Charge("testYear2", "testTransactionId", None, None, None, None, Some(100.00), None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(3).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(4).toString), None, None)
            ))),
            Charge("testYear2", "testTransactionId", None, None, None, None, Some(100.00), None, None, None, Some(Seq(
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.plusDays(5).toString), None, None),
              SubItem(None, None, None, None, None, None, None, Some(LocalDate.now.minusDays(2).toString), None, None)
            )))
          ))

          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear, testNino)(
            financialDetailsCurrentYear
          )
          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear - 1, testNino)(
            financialDetailsLastYear
          )

          val result: Future[Option[Either[(LocalDate, Boolean), Int]]] = {
            TestFinancialDetailsService.getChargeDueDates(implicitly, testUserWithRecentYears)
          }

          await(result) shouldBe Some(Right(2))
        }
      }
      "return none" when {
        "there are no upcoming or overdue dates" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(List())

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(List())

          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear, testNino)(
            financialDetailsCurrentYear
          )
          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear - 1, testNino)(
            financialDetailsLastYear
          )
          val result: Future[Option[Either[(LocalDate, Boolean), Int]]] = {
            TestFinancialDetailsService.getChargeDueDates(implicitly, testUserWithRecentYears)
          }

          await(result) shouldBe None
        }
      }
    }
  }

  "getAllFinancialDetails" when {
    "API5 is enabled" should {
      "return a set of successful financial details" when {
        "a successful response is returned for a single year" in {
          enable(API5)
          val financialDetail = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now), financialDetail)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetail)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "successful responses are returned for multiple years" in {
          enable(API5)
          val financialDetailLastYear = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now.minusYears(1)))
          val financialDetail = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialDetailLastYear),
            (getTaxEndYear(LocalDate.now), financialDetail)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetail)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "a successful response and a not found response are returned" in {
          enable(API5)
          val financialDetailLastYear = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now.minusYears(1)))
          val financialDetailNotFound = FinancialDetailsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialDetailLastYear)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailNotFound)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "only not found response is returned" in {
          enable(API5)
          val financialDetailNotFound = FinancialDetailsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List.empty

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailNotFound)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
      "return a set of financial transactions with error transactions" when {
        "an error response is returned for a single year" in {
          enable(API5)
          val financialDetailsError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now), financialDetailsError)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailsError)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "an error response is returned for multiple years" in {
          enable(API5)
          val financialDetailsError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialDetailsError),
            (getTaxEndYear(LocalDate.now), financialDetailsError)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(financialDetailsError)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailsError)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "an error response is returned along with a successful response" in {
          enable(API5)
          val financialDetailsErrorLastYear = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
          val financialDetails = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialDetailsErrorLastYear),
            (getTaxEndYear(LocalDate.now), financialDetails)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(financialDetailsErrorLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetails)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
    }
    "API5 is disabled" should {
      "return a set of successful financial transactions" when {
        "a successful response is returned for a single year" in {
          val financialDetail = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now), financialDetail)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetail)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "successful responses are returned for multiple years" in {
          val financialDetailLastYear = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now.minusYears(1)))
          val financialDetail = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialDetailLastYear),
            (getTaxEndYear(LocalDate.now), financialDetail)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetail)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "a successful response and a not found response are returned" in {
          val financialDetailLastYear = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now.minusYears(1)))
          val financialDetailNotFound = FinancialDetailsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialDetailLastYear)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailNotFound)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "only not found response is returned" in {
          val financialDetailNotFound = FinancialDetailsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List.empty

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailNotFound)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
      "return a set of financial transactions with error transactions" when {
        "an error response is returned for a single year" in {
          val financialDetailsError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now), financialDetailsError)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailsError)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "an error response is returned for multiple years" in {
          val financialDetailsError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialDetailsError),
            (getTaxEndYear(LocalDate.now), financialDetailsError)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(financialDetailsError)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailsError)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "an error response is returned along with a successful response" in {
          val financialDetailsErrorLastYear = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
          val financialDetails = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialDetailsErrorLastYear),
            (getTaxEndYear(LocalDate.now), financialDetails)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(financialDetailsErrorLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetails)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
    }
  }

  "getAllUnpaidFinancialDetails" when {
    "API5 is enabled" should {
      "return financial transactions with only the unpaid transactions" when {
        "only unpaid transactions exist" in {
          enable(API5)

          val financialDetailLastYear = getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(100.00)),
              fullChargeModel.copy(outstandingAmount = Some(200.00))
            )
          )
          val financialDetail = getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(300.00)),
              fullChargeModel.copy(outstandingAmount = Some(400.00))
            )
          )
          val expectedResult: List[FinancialDetailsResponseModel] = List(
            financialDetailLastYear,
            financialDetail
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetail)

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "a mix of unpaid, paid and non charge transactions exist" in {
          enable(API5)

          val expectedResult: List[FinancialDetailsResponseModel] = List(
            getFinancialDetailSuccess(
              taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
              charges = List(
                fullChargeModel.copy(outstandingAmount = Some(100.00), originalAmount = Some(100.00)),
              )
            ),
            getFinancialDetailSuccess(
              taxYear = getTaxEndYear(LocalDate.now),
              charges = List(
                fullChargeModel.copy(outstandingAmount = Some(300.00), originalAmount = Some(300.00)),
              )
            )
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = None),
              fullChargeModel.copy(outstandingAmount = Some(100.00), originalAmount = Some(100.00)),
              fullChargeModel.copy(outstandingAmount = Some(-200.00), totalAmount = Some(-200.00), originalAmount = Some(-200.00))
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(300.00), originalAmount = Some(300.00)),
              fullChargeModel.copy(outstandingAmount = Some(-400.00), totalAmount = Some(-400.00), originalAmount = Some(-400.00)),
              fullChargeModel.copy(outstandingAmount = None)
            )
          ))

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "no unpaid transactions exist" in {
          enable(API5)

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(0)),
              fullChargeModel.copy(outstandingAmount = Some(0))
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(0)),
              fullChargeModel.copy(outstandingAmount = Some(0))
            )
          ))

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe List.empty[FinancialDetailsResponseModel]
        }
        "errored financial transactions exist" in {
          enable(API5)

          val financialDetailError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
          val expectedResult: List[FinancialDetailsResponseModel] = List(
            getFinancialDetailSuccess(
              taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
              charges = List(
                fullChargeModel.copy(outstandingAmount = Some(100.00))
              )
            ),
            financialDetailError
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(100.00)),
              fullChargeModel.copy(outstandingAmount = None)
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailError)

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
    }
    "API5 is disabled" should {
      "return financial transactions with only the unpaid transactions" when {
        "only unpaid transactions exist" in {
          val financialDetailLastYear = getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(100.00)),
              fullChargeModel.copy(outstandingAmount = Some(200.00))
            )
          )
          val financialDetail = getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(300.00)),
              fullChargeModel.copy(outstandingAmount = Some(400.00))
            )
          )
          val expectedResult: List[FinancialDetailsResponseModel] = List(
            financialDetailLastYear,
            financialDetail
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetail)

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "a mix of unpaid, paid and non charge transactions exist" in {
          val expectedResult: List[FinancialDetailsResponseModel] = List(
            getFinancialDetailSuccess(
              taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
              charges = List(
                fullChargeModel.copy(outstandingAmount = Some(100.00), originalAmount = Some(100.00)),
              )
            ),
            getFinancialDetailSuccess(
              taxYear = getTaxEndYear(LocalDate.now),
              charges = List(
                fullChargeModel.copy(outstandingAmount = Some(300.00), originalAmount = Some(300.00)),
              )
            )
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = None),
              fullChargeModel.copy(outstandingAmount = Some(100.00), originalAmount = Some(100.00)),
              fullChargeModel.copy(outstandingAmount = Some(-200.00), totalAmount = Some(-200.00), originalAmount = Some(-200.00))
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(300.00), originalAmount = Some(300.00)),
              fullChargeModel.copy(outstandingAmount = Some(-400.00), totalAmount = Some(-400.00), originalAmount = Some(-400.00)),
              fullChargeModel.copy(outstandingAmount = None)
            )
          ))

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "no unpaid transactions exist" in {
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(0)),
              fullChargeModel.copy(outstandingAmount = Some(0))
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(0)),
              fullChargeModel.copy(outstandingAmount = Some(0))
            )
          ))

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe List.empty[FinancialDetailsResponseModel]
        }
        "errored financial transactions exist" in {
          val financialDetailError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
          val expectedResult: List[FinancialDetailsResponseModel] = List(
            getFinancialDetailSuccess(
              taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
              charges = List(
                fullChargeModel.copy(outstandingAmount = Some(100.00))
              )
            ),
            financialDetailError
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(100.00)),
              fullChargeModel.copy(outstandingAmount = None)
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailError)

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
    }
  }

}

/*
 * Copyright 2023 HM Revenue & Customs
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

import auth.MtdItUser
import authV2.AuthActionsTestData.defaultMTDITUser
import config.featureswitch.FeatureSwitching
import enums.ChargeType.NIC4_WALES
import enums.CodingOutType._
import mocks.connectors.MockFinancialDetailsConnector
import models.core.AccountingPeriodModel
import models.financialDetails._
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status
import testConstants.BaseTestConstants._
import testConstants.BusinessDetailsTestConstants.{address, getCurrentTaxYearEnd, testIncomeSource}
import testConstants.FinancialDetailsTestConstants.{documentDetailModel, _}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import java.time.LocalDate

class FinancialDetailsServiceSpec extends TestSupport with MockFinancialDetailsConnector with FeatureSwitching {

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  private val april = 4
  private val fifth = 5
  private val sixth = 6

  private def getTaxEndYear(date: LocalDate): Int = {
    if (date.isBefore(LocalDate.of(date.getYear, april, sixth))) date.getYear
    else date.getYear + 1
  }

  private def getFinancialDetailSuccess(documentDetails: List[DocumentDetail] = List(fullDocumentDetailModel),
                                        financialDetails: List[FinancialDetail] = List(fullFinancialDetailModel)) = {
    FinancialDetailsModel(balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = documentDetails, financialDetails = financialDetails)
  }

  private def getMultiYearFinancialDetailSuccess(documentDetails: List[DocumentDetail] = List(fullDocumentDetailModel, fullDocumentDetailModel),
                                                  financialDetails: List[FinancialDetail] = List(fullFinancialDetailModel, fullFinancialDetailModel)) = {
    FinancialDetailsModel(balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
      documentDetails = documentDetails, financialDetails = financialDetails)
  }

  private def mtdUser(numYears: Int): MtdItUser[_] = defaultMTDITUser(Some(Individual),
    IncomeSourceDetailsModel(
      testNino,
      testMtditid,
      Some(getTaxEndYear(fixedDate.minusYears(numYears - 1)).toString),
      businesses = (1 to numYears).toList.map { count =>
        BusinessDetailsModel(
          incomeSourceId = s"income-id-$count",
          incomeSource = Some(testIncomeSource),
          accountingPeriod = Some(AccountingPeriodModel(
            start = LocalDate.of(getTaxEndYear(fixedDate.minusYears(count)), april, sixth),
            end = LocalDate.of(getTaxEndYear(fixedDate.minusYears(count - 1)), april, fifth)
          )),
          None,
          firstAccountingPeriodEndDate = Some(LocalDate.of(getTaxEndYear(fixedDate.minusYears(count - 1)), april, fifth)),
          tradingStartDate = None,
          cessation = None,
          address = Some(address),
          cashOrAccruals = true
        )
      },
      properties = Nil
    ))


  object TestFinancialDetailsService extends FinancialDetailsService(mockFinancialDetailsConnector, dateService)

  val testUserWithRecentYears: MtdItUser[_] = defaultMTDITUser(Some(Individual), IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = Some(getCurrentTaxYearEnd.minusYears(1).getYear.toString),
    businesses = List(
      BusinessDetailsModel(
        "testId",
        incomeSource = Some(testIncomeSource),
        Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
        None,
        Some(getCurrentTaxYearEnd.minusYears(1)),
        tradingStartDate = None,
        cessation = None,
        address = Some(address),
        cashOrAccruals = true
      )
    ),
    properties = Nil
  ))

  "getFinancialDetails" when {
    "a successful financial details response is returned from the connector" should {
      "return a valid FinancialDetails model" in {
        setupMockGetFinancialDetails(testTaxYear, testNino)(financialDetailsModel(testTaxYear))
        TestFinancialDetailsService.getFinancialDetails(testTaxYear, testNino).futureValue shouldBe financialDetailsModel(testTaxYear)
      }
    }
    "a error model is returned from the connector" should {
      "return a FinancialDetailsError model" in {
        setupMockGetFinancialDetails(testTaxYear, testNino)(testFinancialDetailsErrorModel)
        TestFinancialDetailsService.getFinancialDetails(testTaxYear, testNino).futureValue shouldBe testFinancialDetailsErrorModel
      }
    }
  }

  "getFinancialDetailsV2" when {
    "a successful financial details response is returned from the connector" should {
      "return a valid FinancialDetails model" in {
        setupMockGetFinancialDetailsByTaxYearRange(fixedTaxYearRange, testNino)(financialDetailsModel(fixedTaxYear.endYear))
        TestFinancialDetailsService.getFinancialDetailsV2(fixedTaxYearRange, testNino).futureValue shouldBe financialDetailsModel(fixedTaxYear.endYear)
      }
    }
    "a error model is returned from the connector" should {
      "return a FinancialDetailsError model" in {
        setupMockGetFinancialDetailsByTaxYearRange(fixedTaxYearRange, testNino)(testFinancialDetailsErrorModel)
        TestFinancialDetailsService.getFinancialDetailsV2(fixedTaxYearRange, testNino).futureValue shouldBe testFinancialDetailsErrorModel
      }
    }
  }

  "getChargeDueDates" when {
    "financial details are returned successfully" should {
      "return a single overdue date" when {
        "there is only one overdue date" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", Some("ITSA- POA 1"), Some("documentText"), 100.00, 0, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.minusDays(1)), documentDueDate = Some(fixedDate.minusDays(1))),
              DocumentDetail(2018, "testTransactionId2", Some("ITSA - POA 2"), Some("documentText"), 200.00, 0, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(1)), documentDueDate = Some(fixedDate.plusDays(1)))
            ),
            financialDetails = List(
              FinancialDetail("2018", Some("SA Payment on Account 1"), Some("4920"), Some("testTransactionId1"), Some(fixedDate), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.minusDays(1)))))),
              FinancialDetail("2018", Some("SA Payment on Account 2"), Some("4930"), Some("testTransactionId2"), Some(fixedDate), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.plusDays(1))))))
            )
          )

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", None, None, 100.00, 0, LocalDate.of(2018, 3, 29)),
              DocumentDetail(2018, "testTransactionId2", None, None, 0, 0, LocalDate.of(2018, 3, 29))
            ),
            financialDetails = List(
              FinancialDetail("2018", None, None, Some("testTransactionId1"), None, None, None, None, None, None, None, None, None, Some(Seq(SubItem(Some(fixedDate.plusDays(3)))))),
              FinancialDetail("2018", None, None, Some("testTransactionId2"), None, None, None, None, None, None, None, None, None, Some(Seq(SubItem(Some(fixedDate.plusDays(5))))))
            )
          )

          val result: Option[Either[(LocalDate, Boolean), Int]] = {
            TestFinancialDetailsService.getChargeDueDates(List(financialDetailsCurrentYear, financialDetailsLastYear))
          }

          result shouldBe Some(Left(fixedDate.minusDays(1) -> true))
        }
      }
      "return a single non-overdue date" when {
        "there are no overdue dates, but there are dates upcoming" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", None, None, 100.00, 0, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(7))),
              DocumentDetail(2018, "testTransactionId2", None, None, 100.00, 0, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(1)))
            ),
            financialDetails = List(
              FinancialDetail("2018", None, None, Some("testTransactionId1"), None, None, None, None, None, None, None, None, None, Some(Seq(SubItem(Some(fixedDate.plusDays(7)))))),
              FinancialDetail("2018", None, None, Some("testTransactionId2"), None, None, None, None, None, None, None, None, None, Some(Seq(SubItem(Some(fixedDate.plusDays(1))))))
            )
          )

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", None, None, 0, 0, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(3)), documentDueDate = Some(fixedDate.plusDays(3))),
              DocumentDetail(2018, "testTransactionId2", Some("ITSA- POA 1"), Some("documentText"), 100.00, 0, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(5)), documentDueDate = Some(fixedDate.plusDays(5)))
            ),
            financialDetails = List(
              FinancialDetail("2018", Some("SA Payment on Account 1"), Some("4920"), Some("testTransactionId1"), Some(fixedDate), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.plusDays(3)))))),
              FinancialDetail("2018", Some("SA Payment on Account 2"), Some("4930"), Some("testTransactionId2"), Some(fixedDate), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.plusDays(5))))))
            )
          )

          val result: Option[Either[(LocalDate, Boolean), Int]] = {
            TestFinancialDetailsService.getChargeDueDates(List(financialDetailsCurrentYear, financialDetailsLastYear))
          }

          result shouldBe Some(Left(fixedDate.plusDays(5) -> false))
        }
      }
      "return the count of overdue dates" when {
        "there are more than one overdue dates" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", Some("ITSA- POA 1"), Some("documentText"), 100.00, 0.00, LocalDate.of(2018, 3, 29), documentDueDate = Some(fixedDate.minusDays(1))),
              DocumentDetail(2018, "testTransactionId2", Some("ITSA - POA 2"), Some("documentText"), 100.00, 0.00, LocalDate.of(2018, 3, 29), documentDueDate = Some(fixedDate.plusDays(1)))
            ),
            financialDetails = List(
              FinancialDetail("2018", Some("SA Payment on Account 1"), Some("4920"), Some("testTransactionId1"), Some(fixedDate), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.minusDays(1)))))),
              FinancialDetail("2018", Some("SA Payment on Account 2"), Some("4930"), Some("testTransactionId2"), Some(fixedDate), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.plusDays(1))))))
            )
          )

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", Some("ITSA- POA 1"), Some("documentText"), 100.00, 0.00, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(1)), documentDueDate = Some(fixedDate.plusDays(1))),
              DocumentDetail(2018, "testTransactionId2", Some("ITSA - POA 2"), Some("documentText"), 100.00, 0.00, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.minusDays(1)), documentDueDate = Some(fixedDate.minusDays(1)))
            ),
            financialDetails = List(
              FinancialDetail("2018", Some("SA Payment on Account 1"), Some("4920"), Some("testTransactionId1"), Some(fixedDate), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.plusDays(3)))))),
              FinancialDetail("2018", Some("SA Payment on Account 2"), Some("4930"), Some("testTransactionId2"), Some(fixedDate), Some("ABCD1234"), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.minusDays(2))))))
            )
          )

          val result: Option[Either[(LocalDate, Boolean), Int]] = {
            TestFinancialDetailsService.getChargeDueDates(List(financialDetailsCurrentYear, financialDetailsLastYear))
          }

          result shouldBe Some(Right(2))
        }
      }
      "return none" when {
        "there are no upcoming or overdue dates" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None), List(), List())

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None), List(), List())

          val result: Option[Either[(LocalDate, Boolean), Int]] = {
            TestFinancialDetailsService.getChargeDueDates(List(financialDetailsCurrentYear, financialDetailsLastYear))
          }

          result shouldBe None
        }
      }
    }
  }


  "getAllFinancialDetails" when {
    "return a set of successful financial details" when {
      "a successful response is returned for a single year" in {
        val financialDetail = getFinancialDetailSuccess()
        val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
          (getTaxEndYear(fixedDate), financialDetail)
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetail)
        val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)
        result.futureValue shouldBe expectedResult
      }

      "successful responses are returned for multiple years" in {
        val financialDetailLastYear = getFinancialDetailSuccess()
        val financialDetail = getFinancialDetailSuccess()
        val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
          (fixedDate.getYear, financialDetailLastYear),
          (fixedDate.getYear + 1, financialDetail)
        )

        setupMockGetFinancialDetails(fixedDate.getYear, testNino)(financialDetailLastYear)
        setupMockGetFinancialDetails(fixedDate.getYear + 1, testNino)(financialDetail)

        val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "a successful response and a not found response are returned" in {
        val financialDetailLastYear = getFinancialDetailSuccess()
        val financialDetailNotFound = FinancialDetailsErrorModel(Status.NOT_FOUND, "not found")
        val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
          (getTaxEndYear(fixedDate.minusYears(1)), financialDetailLastYear)
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(financialDetailLastYear)
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetailNotFound)

        val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "only not found response is returned" in {
        val financialDetailNotFound = FinancialDetailsErrorModel(Status.NOT_FOUND, "not found")
        val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List.empty

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetailNotFound)

        val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
    }
    "return a set of financial transactions with error transactions" when {
      "an error response is returned for a single year" in {
        val financialDetailsError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
        val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
          (getTaxEndYear(fixedDate), financialDetailsError)
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetailsError)

        val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "an error response is returned for multiple years" in {
        val financialDetailsError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
        val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
          (getTaxEndYear(fixedDate.minusYears(1)), financialDetailsError),
          (getTaxEndYear(fixedDate), financialDetailsError)
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(financialDetailsError)
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetailsError)

        val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "an error response is returned along with a successful response" in {
        val financialDetailsErrorLastYear = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
        val financialDetails = getFinancialDetailSuccess()
        val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
          (getTaxEndYear(fixedDate.minusYears(1)), financialDetailsErrorLastYear),
          (getTaxEndYear(fixedDate), financialDetails)
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(financialDetailsErrorLastYear)
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetails)

        val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
    }
  }

  "getAllFinancialDetailsV2" when {
    "return a set of successful financial details" when {
      "a successful response is returned for a single year" in {
        val financialDetail = getFinancialDetailSuccess()
        val expectedResult: Option[FinancialDetailsResponseModel] = Some(financialDetail)

        setupMockGetFinancialDetailsByTaxYearRange(fixedTaxYearRange, testNino)(financialDetail)
        val result = TestFinancialDetailsService.getAllFinancialDetailsV2(mtdUser(1), headerCarrier, ec)
        result.futureValue shouldBe expectedResult
      }

      "successful response is returned for multiple years" in {
        val financialDetail = getMultiYearFinancialDetailSuccess()
        val expectedResult: Option[FinancialDetailsResponseModel] = Some(financialDetail)

        setupMockGetFinancialDetailsByTaxYearRange(multiYearRange, testNino)(financialDetail)

        val result = TestFinancialDetailsService.getAllFinancialDetailsV2(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "not found response is returned" in {
        val financialDetailNotFound = FinancialDetailsErrorModel(Status.NOT_FOUND, "not found")
        val expectedResult: Option[FinancialDetailsResponseModel] = None

        setupMockGetFinancialDetailsByTaxYearRange(fixedTaxYearRange, testNino)(financialDetailNotFound)

        val result = TestFinancialDetailsService.getAllFinancialDetailsV2(mtdUser(1), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
    }
    "return a set of error transactions" when {
      "an error response is returned" in {
        val financialDetailsError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
        val expectedResult: Option[FinancialDetailsResponseModel] = Some(financialDetailsError)

        setupMockGetFinancialDetailsByTaxYearRange(fixedTaxYearRange, testNino)(financialDetailsError)

        val result = TestFinancialDetailsService.getAllFinancialDetailsV2(mtdUser(1), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
    }
  }

  "getAllUnpaidFinancialDetails" when {
    "return financial transactions with only the unpaid transactions" when {
      "only unpaid transactions exist" in {

        val financialDetailLastYear = getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 100.00, interestOutstandingAmount = Some(0)),
          fullDocumentDetailModel.copy(outstandingAmount = 200.00, interestOutstandingAmount = Some(0))
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel,
        ))
        val financialDetail = getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 300.00),
          fullDocumentDetailModel.copy(outstandingAmount = 400.00)
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel
        ))
        val expectedResult: List[FinancialDetailsResponseModel] = List(
          financialDetailLastYear,
          financialDetail
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(financialDetailLastYear)
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetail)

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails()(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "a mix of unpaid, paid and non charge transactions exist" in {

        val expectedResult: List[FinancialDetailsResponseModel] = List(
          getFinancialDetailSuccess(documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(50.0)),
            fullDocumentDetailModel.copy(outstandingAmount = 100.00, originalAmount = 100.00),
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(0.00), interestOutstandingAmount = Some(100.00))
          ), financialDetails = List(
            fullFinancialDetailModel
          )),
          getFinancialDetailSuccess(documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = 300.00, originalAmount = 300.00),
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(25.0))
          ), financialDetails = List(
            fullFinancialDetailModel
          ))
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(50.0)),
          fullDocumentDetailModel.copy(outstandingAmount = 100.00, originalAmount = 100.00),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(0.00), interestOutstandingAmount = Some(100.00)),
          fullDocumentDetailModel.copy(outstandingAmount = 0, originalAmount = -200.00, latePaymentInterestAmount = None, interestOutstandingAmount = None)
        ), financialDetails = List(
          fullFinancialDetailModel
        )))
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 300.00, originalAmount = 300.00),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, originalAmount = -400.00, latePaymentInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(25.0))
        ), financialDetails = List(
          fullFinancialDetailModel
        )))

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails()(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "no unpaid transactions exist" in {

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 0, latePaymentInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0, latePaymentInterestAmount = None, interestOutstandingAmount = None)
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel
        )))
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 0, latePaymentInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0, latePaymentInterestAmount = None, interestOutstandingAmount = None)
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel
        )))

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails()(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe List.empty[FinancialDetailsResponseModel]
      }
      "errored financial transactions exist" in {

        val financialDetailError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
        val expectedResult: List[FinancialDetailsResponseModel] = List(
          getFinancialDetailSuccess(documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = 100.00)
          ), financialDetails = List(
            fullFinancialDetailModel
          )),
          financialDetailError
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 100.00),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(0.00), interestOutstandingAmount = Some(0.00))
        ), financialDetails = List(
          fullFinancialDetailModel
        )))
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetailError)

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails()(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
    }
    "return unpaid transactions and coding out document details" when {
      "coding out is enabled and coding out data exists" in {
        val financialDetailCodingOut = getFinancialDetailSuccess(documentDetails = List(
          documentDetailModel(transactionId = "transid1", outstandingAmount = 200.00).copy(
            interestOutstandingAmount = Some(0), documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_CLASS2_NICS)),
          documentDetailModel(taxYear = 2021, transactionId = "transid2", outstandingAmount = 0).copy(
            interestOutstandingAmount = Some(0), documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_ACCEPTED)),
          documentDetailModel(transactionId = "transid3", outstandingAmount = 0).copy(
            interestOutstandingAmount = Some(0), documentDescription = Some("TRM Amend Charge"), documentText = Some(CODING_OUT_CANCELLED)),
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel,
        ))
        val financialDetail = getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 300.00),
          fullDocumentDetailModel.copy(outstandingAmount = 400.00)
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel
        ))

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(financialDetailCodingOut)
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetail)

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails()(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe List(
          financialDetailCodingOut,
          financialDetail
        )
      }
    }
  }

  "getAllUnpaidFinancialDetailsV2" when {
    "return financial transactions with only the unpaid transactions" when {
      "only unpaid transactions exist" in {

        val financialDetail = getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 100.00, interestOutstandingAmount = Some(0)),
          fullDocumentDetailModel.copy(outstandingAmount = 200.00, interestOutstandingAmount = Some(0)),
          fullDocumentDetailModel.copy(outstandingAmount = 300.00),
          fullDocumentDetailModel.copy(outstandingAmount = 400.00)
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel,
          fullFinancialDetailModel,
          fullFinancialDetailModel
        ))

        val expectedResult: Option[FinancialDetailsResponseModel] = Some(
          financialDetail
        )

        setupMockGetFinancialDetails(fixedTaxYear, fixedTaxYear, testNino)(financialDetail)
        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetailsV2()(mtdUser(1), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "a mix of unpaid, paid and non charge transactions exist" in {

        val expectedResult: Option[FinancialDetailsResponseModel] = Some(
          getFinancialDetailSuccess(documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(50.0)),
            fullDocumentDetailModel.copy(outstandingAmount = 100.00, originalAmount = 100.00),
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(0.00), interestOutstandingAmount = Some(100.00)),
            fullDocumentDetailModel.copy(outstandingAmount = 300.00, originalAmount = 300.00),
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(25.0))
          ), financialDetails = List(
            fullFinancialDetailModel,
            fullFinancialDetailModel
          ))
        )

        setupMockGetFinancialDetails(fixedTaxYear.previousYear, fixedTaxYear, testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(50.0)),
          fullDocumentDetailModel.copy(outstandingAmount = 100.00, originalAmount = 100.00),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(0.00), interestOutstandingAmount = Some(100.00)),
          fullDocumentDetailModel.copy(outstandingAmount = 0, originalAmount = -200.00, latePaymentInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 300.00, originalAmount = 300.00),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, originalAmount = -400.00, latePaymentInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, latePaymentInterestAmount = Some(25.0))
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel
        )))

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetailsV2()(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "no unpaid transactions exist" in {

        setupMockGetFinancialDetails(fixedTaxYear.previousYear, fixedTaxYear, testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 0, latePaymentInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0, latePaymentInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0, latePaymentInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0, latePaymentInterestAmount = None, interestOutstandingAmount = None)
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel,
          fullFinancialDetailModel,
          fullFinancialDetailModel
        )))

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetailsV2()(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe None
      }
    }
    "return unpaid transactions and coding out document details" when {
      "coding out is enabled and coding out data exists" in {
        val financialDetailCodingOut = getFinancialDetailSuccess(documentDetails = List(
          documentDetailModel(transactionId = "transid1", outstandingAmount = 200.00).copy(
            interestOutstandingAmount = Some(0), documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_CLASS2_NICS)),
          documentDetailModel(taxYear = 2021, transactionId = "transid2", outstandingAmount = 0).copy(
            interestOutstandingAmount = Some(0), documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_ACCEPTED)),
          documentDetailModel(transactionId = "transid3", outstandingAmount = 0).copy(
            interestOutstandingAmount = Some(0), documentDescription = Some("TRM Amend Charge"), documentText = Some(CODING_OUT_CANCELLED)),
          fullDocumentDetailModel.copy(outstandingAmount = 300.00),
          fullDocumentDetailModel.copy(outstandingAmount = 400.00)
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel,
          fullFinancialDetailModel,
          fullFinancialDetailModel
        ))

        setupMockGetFinancialDetails(fixedTaxYear.previousYear, fixedTaxYear, testNino)(financialDetailCodingOut)

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetailsV2()(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe Some(
          financialDetailCodingOut
        )
      }
    }
  }
}

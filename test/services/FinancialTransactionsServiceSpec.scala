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

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime}

import assets.BaseTestConstants._
import assets.FinancialTransactionsTestConstants._
import auth.MtdItUser
import config.featureswitch.{API5, FeatureSwitching}
import mocks.connectors.MockFinancialTransactionsConnector
import models.core.AccountingPeriodModel
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel, FinancialTransactionsResponseModel, TransactionModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import testUtils.TestSupport


class FinancialTransactionsServiceSpec extends TestSupport with MockFinancialTransactionsConnector with FeatureSwitching {

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

  private def getFinancialTransactionSuccess(taxYear: Int,
                                             financialTransactions: Seq[TransactionModel] = Seq(fullTransactionModel)): FinancialTransactionsModel = {
    FinancialTransactionsModel(
      idType = Some("MTDBSA"),
      idNumber = Some("XQIT00000000001"),
      regimeType = Some("ITSA"),
      processingDate = ZonedDateTime.parse(s"$taxYear-07-06T12:34:56.789Z", DateTimeFormatter.ISO_ZONED_DATE_TIME),
      financialTransactions = Some(financialTransactions)
    )
  }

  private def mtdUser(numYears: Int): MtdItUser[AnyContent] = MtdItUser(
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
    Some("individual")
  )(FakeRequest())

  object TestFinancialTransactionsService extends FinancialTransactionsService(mockFinancialTransactionsConnector)

  "getFinancialTransactions" when {
    "a successful financial transaction response is returned from the connector" should {
      "return a valid FinancialTransactions model" in {
        mockGetFinancialTransactions(testNino, testTaxYear)(financialTransactionsModel())
        await(TestFinancialTransactionsService.getFinancialTransactions(testNino, testTaxYear)) shouldBe financialTransactionsModel()
      }
    }
    "a error model is returned from the connector" should {
      "return a FinancialTransactionsError model" in {
        mockGetFinancialTransactions(testNino, testTaxYear)(financialTransactionsErrorModel)
        await(TestFinancialTransactionsService.getFinancialTransactions(testNino, testTaxYear)) shouldBe financialTransactionsErrorModel
      }
    }
  }

  "getAllFinancialTransactions" when {
    "API5 is enabled" should {
      "return a set of successful financial transactions" when {
        "a successful response is returned for a single year" in {
          enable(API5)
          val financialTransaction = getFinancialTransactionSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now), financialTransaction)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransaction)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "successful responses are returned for multiple years" in {
          enable(API5)
          val financialTransactionLastYear = getFinancialTransactionSuccess(getTaxEndYear(LocalDate.now.minusYears(1)))
          val financialTransaction = getFinancialTransactionSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialTransactionLastYear),
            (getTaxEndYear(LocalDate.now), financialTransaction)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(financialTransactionLastYear)
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransaction)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "a successful response and a not found response are returned" in {
          enable(API5)
          val financialTransactionLastYear = getFinancialTransactionSuccess(getTaxEndYear(LocalDate.now.minusYears(1)))
          val financialTransactionNotFound = FinancialTransactionsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialTransactionLastYear)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(financialTransactionLastYear)
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactionNotFound)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "only not found response is returned" in {
          enable(API5)
          val financialTransactionNotFound = FinancialTransactionsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List.empty

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactionNotFound)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
      "return a set of financial transactions with error transactions" when {
        "an error response is returned for a single year" in {
          enable(API5)
          val financialTransactionsError = FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now), financialTransactionsError)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactionsError)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "an error response is returned for multiple years" in {
          enable(API5)
          val financialTransactionsError = FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialTransactionsError),
            (getTaxEndYear(LocalDate.now), financialTransactionsError)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(financialTransactionsError)
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactionsError)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "an error response is returned along with a successful response" in {
          enable(API5)
          val financialTransactionsErrorLastYear = FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
          val financialTransactions = getFinancialTransactionSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialTransactionsErrorLastYear),
            (getTaxEndYear(LocalDate.now), financialTransactions)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(financialTransactionsErrorLastYear)
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactions)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
    }
    "API5 is disabled" should {
      "return a set of successful financial transactions" when {
        "a successful response is returned for a single year" in {
          val financialTransaction = getFinancialTransactionSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now), financialTransaction)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransaction)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "successful responses are returned for multiple years" in {
          val financialTransactionLastYear = getFinancialTransactionSuccess(getTaxEndYear(LocalDate.now.minusYears(1)))
          val financialTransaction = getFinancialTransactionSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialTransactionLastYear),
            (getTaxEndYear(LocalDate.now), financialTransaction)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(financialTransactionLastYear)
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransaction)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "a successful response and a not found response are returned" in {
          val financialTransactionLastYear = getFinancialTransactionSuccess(getTaxEndYear(LocalDate.now.minusYears(1)))
          val financialTransactionNotFound = FinancialTransactionsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialTransactionLastYear)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(financialTransactionLastYear)
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactionNotFound)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "only not found response is returned" in {
          val financialTransactionNotFound = FinancialTransactionsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List.empty

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactionNotFound)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
      "return a set of financial transactions with error transactions" when {
        "an error response is returned for a single year" in {
          val financialTransactionsError = FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now), financialTransactionsError)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactionsError)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "an error response is returned for multiple years" in {
          val financialTransactionsError = FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialTransactionsError),
            (getTaxEndYear(LocalDate.now), financialTransactionsError)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(financialTransactionsError)
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactionsError)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "an error response is returned along with a successful response" in {
          val financialTransactionsErrorLastYear = FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
          val financialTransactions = getFinancialTransactionSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialTransactionsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialTransactionsErrorLastYear),
            (getTaxEndYear(LocalDate.now), financialTransactions)
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(financialTransactionsErrorLastYear)
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactions)

          val result = TestFinancialTransactionsService.getAllFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
    }
  }

  "getAllUnpaidFinancialTransactions" when {
    "API5 is enabled" should {
      "return financial transactions with only the unpaid transactions" when {
        "only unpaid transactions exist" in {
          enable(API5)

          val financialTransactionLastYear = getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = Some(100.00)),
              fullTransactionModel.copy(outstandingAmount = Some(200.00))
            )
          )
          val financialTransaction = getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = Some(300.00)),
              fullTransactionModel.copy(outstandingAmount = Some(400.00))
            )
          )
          val expectedResult: List[FinancialTransactionsResponseModel] = List(
            financialTransactionLastYear,
            financialTransaction
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(financialTransactionLastYear)
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransaction)

          val result = TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "a mix of unpaid, paid and non charge transactions exist" in {
          enable(API5)

          val expectedResult: List[FinancialTransactionsResponseModel] = List(
            getFinancialTransactionSuccess(
              taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
              financialTransactions = Seq(
                fullTransactionModel.copy(outstandingAmount = Some(100.00)),
              )
            ),
            getFinancialTransactionSuccess(
              taxYear = getTaxEndYear(LocalDate.now),
              financialTransactions = Seq(
                fullTransactionModel.copy(outstandingAmount = Some(300.00)),
              )
            )
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = None),
              fullTransactionModel.copy(outstandingAmount = Some(100.00)),
              fullTransactionModel.copy(outstandingAmount = Some(-200.00), originalAmount = Some(-200.00))
            )
          ))
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = Some(300.00)),
              fullTransactionModel.copy(outstandingAmount = Some(-400.00), originalAmount = Some(-400.00)),
              fullTransactionModel.copy(outstandingAmount = None)
            )
          ))

          val result = TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "no unpaid transactions exist" in {
          enable(API5)

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = None),
              fullTransactionModel.copy(outstandingAmount = None)
            )
          ))
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = None),
              fullTransactionModel.copy(outstandingAmount = None)
            )
          ))

          val result = TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe List.empty[FinancialTransactionsResponseModel]
        }
        "errored financial transactions exist" in {
          enable(API5)

          val financialTransactionError = FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
          val expectedResult: List[FinancialTransactionsResponseModel] = List(
            getFinancialTransactionSuccess(
              taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
              financialTransactions = Seq(
                fullTransactionModel.copy(outstandingAmount = Some(100.00))
              )
            ),
            financialTransactionError
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = Some(100.00)),
              fullTransactionModel.copy(outstandingAmount = None)
            )
          ))
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactionError)

          val result = TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
    }
    "API5 is disabled" should {
      "return financial transactions with only the unpaid transactions" when {
        "only unpaid transactions exist" in {
          val financialTransactionLastYear = getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = Some(100.00)),
              fullTransactionModel.copy(outstandingAmount = Some(200.00))
            )
          )
          val financialTransaction = getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = Some(300.00)),
              fullTransactionModel.copy(outstandingAmount = Some(400.00))
            )
          )
          val expectedResult: List[FinancialTransactionsResponseModel] = List(
            financialTransactionLastYear,
            financialTransaction
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(financialTransactionLastYear)
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransaction)

          val result = TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "a mix of unpaid, paid and non charge transactions exist" in {
          val expectedResult: List[FinancialTransactionsResponseModel] = List(
            getFinancialTransactionSuccess(
              taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
              financialTransactions = Seq(
                fullTransactionModel.copy(outstandingAmount = Some(100.00)),
              )
            ),
            getFinancialTransactionSuccess(
              taxYear = getTaxEndYear(LocalDate.now),
              financialTransactions = Seq(
                fullTransactionModel.copy(outstandingAmount = Some(300.00)),
              )
            )
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = None),
              fullTransactionModel.copy(outstandingAmount = Some(100.00)),
              fullTransactionModel.copy(outstandingAmount = Some(-200.00), originalAmount = Some(-200.00))
            )
          ))
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = Some(300.00)),
              fullTransactionModel.copy(outstandingAmount = Some(-400.00), originalAmount = Some(-400.00)),
              fullTransactionModel.copy(outstandingAmount = None)
            )
          ))

          val result = TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "no unpaid transactions exist" in {
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = None),
              fullTransactionModel.copy(outstandingAmount = None)
            )
          ))
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = None),
              fullTransactionModel.copy(outstandingAmount = None)
            )
          ))

          val result = TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe List.empty[FinancialTransactionsResponseModel]
        }
        "errored financial transactions exist" in {
          val financialTransactionError = FinancialTransactionsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
          val expectedResult: List[FinancialTransactionsResponseModel] = List(
            getFinancialTransactionSuccess(
              taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
              financialTransactions = Seq(
                fullTransactionModel.copy(outstandingAmount = Some(100.00))
              )
            ),
            financialTransactionError
          )

          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialTransactionSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            financialTransactions = Seq(
              fullTransactionModel.copy(outstandingAmount = Some(100.00)),
              fullTransactionModel.copy(outstandingAmount = None)
            )
          ))
          mockGetFinancialTransactions(testMtditid, getTaxEndYear(LocalDate.now))(financialTransactionError)

          val result = TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
    }
  }

}

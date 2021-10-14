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

import assets.BaseTestConstants._
import assets.BusinessDetailsTestConstants.getCurrentTaxYearEnd
import assets.ChargeHistoryTestConstants.{testChargeHistoryErrorModel, testValidChargeHistoryModel}
import assets.FinancialDetailsTestConstants._
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.core.AccountingPeriodModel
import models.financialDetails._
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.http.Status
import play.api.test.FakeRequest
import play.api.test.Helpers._
import testUtils.TestSupport
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.LocalDate
import scala.concurrent.Future

class FinancialDetailsServiceSpec extends TestSupport with MockIncomeTaxViewChangeConnector with FeatureSwitching {

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

  private def getFinancialDetailSuccess(taxYear: Int,
                                        documentDetails: List[DocumentDetail] = List(fullDocumentDetailModel),
                                        financialDetails: List[FinancialDetail] = List(fullFinancialDetailModel)): FinancialDetailsModel = {
    FinancialDetailsModel(balanceDetails = BalanceDetails(1.00, 2.00, 3.00), documentDetails = documentDetails, financialDetails = financialDetails)
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

  "getChargeDueDates" when {
    "a financial detail returned from the connector returns a non 404 error model" should {
      "return an InternalServerException" in {
        val financialDetails: FinancialDetailsModel = FinancialDetailsModel(
          balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
          documentDetails = List(
            DocumentDetail("testYear", "testTransactionId", None, Some(100.00), None, LocalDate.of(2018, 3, 29)),
            DocumentDetail("testYear2", "testTransactionId", None, Some(100.00), None, LocalDate.of(2018, 3, 29))
          ),
          financialDetails = List(
            FinancialDetail("testYear", None,None,None,None,None,None,None,None,None,None,Some(Seq(SubItem(Some(LocalDate.now.toString))))),
            FinancialDetail("testYear2", None, None,None,None,None,None,None,None,None,None,Some(Seq(SubItem(Some(LocalDate.now.plusDays(2).toString))
            )))
          )
        )

        setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear, testNino)(
          FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
        )
        setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear - 1, testNino)(
          financialDetails
        )

        val result: Future[Option[Either[(LocalDate, Boolean), Int]]] = {
          TestFinancialDetailsService.getChargeDueDates(implicitly, testUserWithRecentYears)
        }

        result.failed.futureValue shouldBe an[InternalServerException]
        result.failed.futureValue.getMessage shouldBe "[FinancialDetailsService][getChargeDueDates] - Failed to retrieve successful financial details"
      }
    }
    "financial details are returned successfully" should {
      "return a single overdue date" when {
        "there is only one overdue date" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
            documentDetails = List(
              DocumentDetail("testYear1", "testTransactionId1", Some("ITSA- POA 1"), Some(100.00), None, LocalDate.of(2018, 3, 29)),
              DocumentDetail("testYear1", "testTransactionId2", Some("ITSA - POA 2"), Some(200.00), None, LocalDate.of(2018, 3, 29))
            ),
            financialDetails = List(
              FinancialDetail("testYear1", Some("SA Payment on Account 1"),Some("testTransactionId1") ,Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"),Some(100),Some(Seq(SubItem(Some(LocalDate.now.minusDays(1).toString))))),
              FinancialDetail("testYear1", Some("SA Payment on Account 2"), Some("testTransactionId2") ,Some("transactionDate"), Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"),Some(100),Some(Seq(SubItem(Some(LocalDate.now.plusDays(1).toString)))))
            )
          )

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
            documentDetails = List(
              DocumentDetail("testYear2", "testTransactionId1", None, Some(100.00), None, LocalDate.of(2018, 3, 29)),
              DocumentDetail("testYear2", "testTransactionId2", None, None, None, LocalDate.of(2018, 3, 29))
            ),
            financialDetails = List(
              FinancialDetail("testYear2", None,None,None,None,None,None,None,None,None,None,Some(Seq(SubItem(Some(LocalDate.now.plusDays(3).toString))))),
              FinancialDetail("testYear2", None,None,None,None,None,None,None,None,None,None,Some(Seq(SubItem(Some(LocalDate.now.plusDays(5).toString)))))
            )
          )

          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear, testNino)(
            financialDetailsCurrentYear
          )
          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear - 1, testNino)(
            financialDetailsLastYear
          )

          val result: Future[Option[Either[(LocalDate, Boolean), Int]]] = {
            TestFinancialDetailsService.getChargeDueDates(implicitly, testUserWithRecentYears)
          }

          result.futureValue shouldBe Some(Left(LocalDate.now.minusDays(1) -> true))
        }
      }
      "return a single non-overdue date" when {
        "there are no overdue dates, but there are dates upcoming" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
            documentDetails = List(
              DocumentDetail("testYear1", "testTransactionId1", None, Some(100.00), None, LocalDate.of(2018, 3, 29)),
              DocumentDetail("testYear1", "testTransactionId2", None, Some(100.00), None, LocalDate.of(2018, 3, 29))
            ),
            financialDetails = List(
              FinancialDetail("testYear1", None,None, None,None,None,None,None,None,None,None,Some(Seq(SubItem(Some(LocalDate.now.plusDays(7).toString))))),
              FinancialDetail("testYear1", None,None, None,None,None,None,None,None,None,None,Some(Seq(SubItem(Some(LocalDate.now.plusDays(1).toString)))))
            )
          )

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
            documentDetails = List(
              DocumentDetail("testYear2", "testTransactionId1", None, None, None, LocalDate.of(2018, 3, 29)),
              DocumentDetail("testYear2", "testTransactionId2", Some("ITSA- POA 1"), Some(100.00), None, LocalDate.of(2018, 3, 29))
            ),
            financialDetails = List(
              FinancialDetail("testYear2", Some("SA Payment on Account 1"), Some("testTransactionId1") , Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(Some(LocalDate.now.plusDays(3).toString))))),
              FinancialDetail("testYear2", Some("SA Payment on Account 2"), Some("testTransactionId2")  ,Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(Some(LocalDate.now.plusDays(5).toString)))))
            )
          )

          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear, testNino)(
            financialDetailsCurrentYear
          )
          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear - 1, testNino)(
            financialDetailsLastYear
          )

          val result: Future[Option[Either[(LocalDate, Boolean), Int]]] = {
            TestFinancialDetailsService.getChargeDueDates(implicitly, testUserWithRecentYears)
          }

          result.futureValue shouldBe Some(Left(LocalDate.now.plusDays(3) -> false))
        }
      }
      "return the count of overdue dates" when {
        "there are more than one overdue dates" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
            documentDetails = List(
              DocumentDetail("testYear1", "testTransactionId1", Some("ITSA- POA 1"), Some(100.00), Some(0.00), LocalDate.of(2018, 3, 29)),
              DocumentDetail("testYear1", "testTransactionId2", Some("ITSA - POA 2"), Some(100.00), Some(0.00), LocalDate.of(2018, 3, 29))
            ),
            financialDetails = List(
              FinancialDetail("testYear1", Some("SA Payment on Account 1"), Some("testTransactionId1"), Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(Some(LocalDate.now.minusDays(1).toString))))),
              FinancialDetail("testYear1", Some("SA Payment on Account 2"), Some("testTransactionId2"), Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100), Some(Seq(SubItem(Some(LocalDate.now.plusDays(1).toString)))))
            )
          )

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00),
            documentDetails = List(
              DocumentDetail("testYear2", "testTransactionId1", Some("ITSA- POA 1"), Some(100.00), Some(0.00), LocalDate.of(2018, 3, 29)),
              DocumentDetail("testYear2", "testTransactionId2", Some("ITSA - POA 2"), Some(100.00), Some(0.00), LocalDate.of(2018, 3, 29))
            ),
            financialDetails = List(
              FinancialDetail("testYear2", Some("SA Payment on Account 1"), Some("testTransactionId1"),Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100),Some(Seq(SubItem(Some(LocalDate.now.plusDays(3).toString))))),
              FinancialDetail("testYear2", Some("SA Payment on Account 2"), Some("testTransactionId2"),Some("transactionDate"),Some("type"),Some(100),Some(100),Some(100),Some(100),Some("NIC4 Wales"), Some(100),Some(Seq(SubItem(Some(LocalDate.now.minusDays(2).toString)))))
            )
          )

          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear, testNino)(
            financialDetailsCurrentYear
          )
          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear - 1, testNino)(
            financialDetailsLastYear
          )

          val result: Future[Option[Either[(LocalDate, Boolean), Int]]] = {
            TestFinancialDetailsService.getChargeDueDates(implicitly, testUserWithRecentYears)
          }

          result.futureValue shouldBe Some(Right(2))
        }
      }
      "return none" when {
        "there are no upcoming or overdue dates" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(BalanceDetails(1.00, 2.00, 3.00),List(), List())

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(BalanceDetails(1.00, 2.00, 3.00),List(), List())

          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear, testNino)(
            financialDetailsCurrentYear
          )
          setupMockGetFinancialDetails(getCurrentTaxYearEnd.getYear - 1, testNino)(
            financialDetailsLastYear
          )
          val result: Future[Option[Either[(LocalDate, Boolean), Int]]] = {
            TestFinancialDetailsService.getChargeDueDates(implicitly, testUserWithRecentYears)
          }

          result.futureValue shouldBe None
        }
      }
    }
  }

  "getChargeHistoryDetails" when {

    "the connector returns a successful ChargesHistoryModel" should {
      "return the chargeHistoryDetails from connector response" in {
        val docNumber = "chargeId"
        val hc = implicitly[HeaderCarrier]

        when(mockIncomeTaxViewChangeConnector.getChargeHistory(any(), any())(any()))
          .thenReturn(Future.successful(testValidChargeHistoryModel))

        val result = TestFinancialDetailsService.getChargeHistoryDetails(testMtditid, docNumber)(hc)

        result.futureValue shouldBe testValidChargeHistoryModel.chargeHistoryDetails
        verify(mockIncomeTaxViewChangeConnector).getChargeHistory(testMtditid, docNumber)(hc)
      }
    }

    "the connector returns an erroneous ChargesHistoryErrorModel" should {
      "generate a failure with InternalServerException" in {
        when(mockIncomeTaxViewChangeConnector.getChargeHistory(any(), any())(any()))
          .thenReturn(Future.successful(testChargeHistoryErrorModel))

        val result = TestFinancialDetailsService.getChargeHistoryDetails(testMtditid, "chargeId")(implicitly)

        result.failed.futureValue shouldBe an[InternalServerException]
        result.failed.futureValue.getMessage shouldBe "[FinancialDetailsService][getChargeHistoryDetails] - Failed to retrieve successful charge history"
      }
    }

    "the connector call fails" should {
      "propagate a failure from the connector" in {
        val emulatedConnectorFailure = Future.failed(new RuntimeException)
        when(mockIncomeTaxViewChangeConnector.getChargeHistory(any(), any())(any()))
          .thenReturn(emulatedConnectorFailure)

        val result = TestFinancialDetailsService.getChargeHistoryDetails(testMtditid, "chargeId")(implicitly)

        result shouldBe emulatedConnectorFailure
      }
    }
  }

  "getAllFinancialDetails" when {
      "return a set of successful financial details" when {
        "a successful response is returned for a single year" in {
          val financialDetail = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now))
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now), financialDetail)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetail)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)

          result.futureValue shouldBe expectedResult
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

          result.futureValue shouldBe expectedResult
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

          result.futureValue shouldBe expectedResult
        }
        "only not found response is returned" in {
          val financialDetailNotFound = FinancialDetailsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List.empty

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailNotFound)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)

          result.futureValue shouldBe expectedResult
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

          result.futureValue shouldBe expectedResult
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

          result.futureValue shouldBe expectedResult
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

          result.futureValue shouldBe expectedResult
        }
      }
  }

  "getAllUnpaidFinancialDetails" when {
      "return financial transactions with only the unpaid transactions" when {
        "only unpaid transactions exist" in {

          val financialDetailLastYear = getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            documentDetails = List(
              fullDocumentDetailModel.copy(outstandingAmount = Some(100.00)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(200.00))
            ),
            financialDetails = List(
              fullFinancialDetailModel,
              fullFinancialDetailModel,
            )
          )
          val financialDetail = getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            documentDetails = List(
              fullDocumentDetailModel.copy(outstandingAmount = Some(300.00)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(400.00))
            ),
            financialDetails = List(
              fullFinancialDetailModel,
              fullFinancialDetailModel
            )
          )
          val expectedResult: List[FinancialDetailsResponseModel] = List(
            financialDetailLastYear,
            financialDetail
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetail)

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          result.futureValue shouldBe expectedResult
        }
        "a mix of unpaid, paid and non charge transactions exist" in {

          val expectedResult: List[FinancialDetailsResponseModel] = List(
            getFinancialDetailSuccess(
              taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
              documentDetails = List(
                fullDocumentDetailModel.copy(outstandingAmount = Some(100.00), originalAmount = Some(100.00)),
              ),
              financialDetails = List(
                fullFinancialDetailModel
              )
            ),
            getFinancialDetailSuccess(
              taxYear = getTaxEndYear(LocalDate.now),
              documentDetails = List(
                fullDocumentDetailModel.copy(outstandingAmount = Some(300.00), originalAmount = Some(300.00)),
              ),
              financialDetails = List(
                fullFinancialDetailModel
              )
            )
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            documentDetails = List(
              fullDocumentDetailModel.copy(outstandingAmount = Some(0.00)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(100.00), originalAmount = Some(100.00)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(0), originalAmount = Some(-200.00))
            ),
            financialDetails = List(
              fullFinancialDetailModel
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            documentDetails = List(
              fullDocumentDetailModel.copy(outstandingAmount = Some(300.00), originalAmount = Some(300.00)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(0.00), originalAmount = Some(-400.00)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(0.00))
            ),
            financialDetails = List(
              fullFinancialDetailModel
            )
          ))

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          result.futureValue shouldBe expectedResult
        }
        "no unpaid transactions exist" in {

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            documentDetails = List(
              fullDocumentDetailModel.copy(outstandingAmount = Some(0)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(0))
            ),
            financialDetails = List(
              fullFinancialDetailModel,
              fullFinancialDetailModel
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            documentDetails = List(
              fullDocumentDetailModel.copy(outstandingAmount = Some(0)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(0))
            ),
            financialDetails = List(
              fullFinancialDetailModel,
              fullFinancialDetailModel
            )
          ))

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          result.futureValue shouldBe List.empty[FinancialDetailsResponseModel]
        }
        "errored financial transactions exist" in {

          val financialDetailError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
          val expectedResult: List[FinancialDetailsResponseModel] = List(
            getFinancialDetailSuccess(
              taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
              documentDetails = List(
                fullDocumentDetailModel.copy(outstandingAmount = Some(100.00))
              ),
              financialDetails = List(
                fullFinancialDetailModel
              )
            ),
            financialDetailError
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)), testNino)(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            documentDetails = List(
              fullDocumentDetailModel.copy(outstandingAmount = Some(100.00)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(0.00))
            ),
            financialDetails = List(
              fullFinancialDetailModel
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now), testNino)(financialDetailError)

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          result.futureValue shouldBe expectedResult
        }
      }
  }

}

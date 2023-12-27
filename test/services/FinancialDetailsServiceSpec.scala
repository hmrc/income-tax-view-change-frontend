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
import config.featureswitch.{CodingOut, FeatureSwitching, TimeMachineAddYear}
import enums.ChargeType.NIC4_WALES
import enums.CodingOutType._
import mocks.connectors.MockFinancialDetailsConnector
import models.core.AccountingPeriodModel
import models.financialDetails._
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when}
import play.api.http.Status
import play.api.test.FakeRequest
import testConstants.BaseTestConstants._
import testConstants.BusinessDetailsTestConstants.{address, getCurrentTaxYearEnd}
import testConstants.ChargeHistoryTestConstants.{testChargeHistoryErrorModel, testValidChargeHistoryModel}
import testConstants.FinancialDetailsTestConstants.{documentDetailModel, _}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}

import java.time.LocalDate
import scala.concurrent.Future

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

  private def getFinancialDetailSuccess(taxYear: Int,
                                        documentDetails: List[DocumentDetail] = List(fullDocumentDetailModel),
                                        financialDetails: List[FinancialDetail] = List(fullFinancialDetailModel)): FinancialDetailsModel = {
    FinancialDetailsModel(balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None), documentDetails = documentDetails, financialDetails = financialDetails)
  }

  private def mtdUser(numYears: Int): MtdItUser[_] = MtdItUser(
    testMtditid,
    testNino,
    Some(testRetrievedUserName),
    IncomeSourceDetailsModel(
      testNino,
      testMtditid,
      Some(getTaxEndYear(fixedDate.minusYears(numYears - 1)).toString),
      businesses = (1 to numYears).toList.map { count =>
        BusinessDetailsModel(
          incomeSourceId = s"income-id-$count",
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
    ),
    btaNavPartial = None,
    Some("testUtr"),
    Some("testCredId"),
    Some(Individual),
    None
  )(FakeRequest())


  object TestFinancialDetailsService extends FinancialDetailsService(mockFinancialDetailsConnector, dateService)

  val testUserWithRecentYears: MtdItUser[_] = MtdItUser(testMtditid, testNino, None, IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = Some(getCurrentTaxYearEnd.minusYears(1).getYear.toString),
    businesses = List(
      BusinessDetailsModel(
        "testId",
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
  ), btaNavPartial = None, None, None, None, None)(FakeRequest())

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
    "financial details are returned successfully" should {
      "return a single overdue date" when {
        "there is only one overdue date" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", Some("ITSA- POA 1"), Some("documentText"), Some(100.00), None, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.minusDays(1)), documentDueDate = Some(fixedDate.minusDays(1))),
              DocumentDetail(2018, "testTransactionId2", Some("ITSA - POA 2"), Some("documentText"), Some(200.00), None, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(1)), documentDueDate = Some(fixedDate.plusDays(1)))
            ),
            financialDetails = List(
              FinancialDetail("2018", Some("SA Payment on Account 1"), Some("testTransactionId1"), Some(fixedDate), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.minusDays(1)))))),
              FinancialDetail("2018", Some("SA Payment on Account 2"), Some("testTransactionId2"), Some(fixedDate), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.plusDays(1))))))
            )
          )

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", None, None, Some(100.00), None, LocalDate.of(2018, 3, 29)),
              DocumentDetail(2018, "testTransactionId2", None, None, None, None, LocalDate.of(2018, 3, 29))
            ),
            financialDetails = List(
              FinancialDetail("2018", None, Some("testTransactionId1"), None, None, None, None, None, None, None, None, Some(Seq(SubItem(Some(fixedDate.plusDays(3)))))),
              FinancialDetail("2018", None, Some("testTransactionId2"), None, None, None, None, None, None, None, None, Some(Seq(SubItem(Some(fixedDate.plusDays(5))))))
            )
          )

          val result: Option[Either[(LocalDate, Boolean), Int]] = {
            TestFinancialDetailsService.getChargeDueDates(List(financialDetailsCurrentYear, financialDetailsLastYear))(isEnabled(TimeMachineAddYear))
          }

          result shouldBe Some(Left(fixedDate.minusDays(1) -> true))
        }
      }
      "return a single non-overdue date" when {
        "there are no overdue dates, but there are dates upcoming" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", None, None, Some(100.00), None, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(7))),
              DocumentDetail(2018, "testTransactionId2", None, None, Some(100.00), None, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(1)))
            ),
            financialDetails = List(
              FinancialDetail("2018", None, Some("testTransactionId1"), None, None, None, None, None, None, None, None, Some(Seq(SubItem(Some(fixedDate.plusDays(7)))))),
              FinancialDetail("2018", None, Some("testTransactionId2"), None, None, None, None, None, None, None, None, Some(Seq(SubItem(Some(fixedDate.plusDays(1))))))
            )
          )

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", None, None, None, None, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(3)), documentDueDate = Some(fixedDate.plusDays(3))),
              DocumentDetail(2018, "testTransactionId2", Some("ITSA- POA 1"), Some("documentText"), Some(100.00), None, LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(5)), documentDueDate = Some(fixedDate.plusDays(5)))
            ),
            financialDetails = List(
              FinancialDetail("2018", Some("SA Payment on Account 1"), Some("testTransactionId1"), Some(fixedDate), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.plusDays(3)))))),
              FinancialDetail("2018", Some("SA Payment on Account 2"), Some("testTransactionId2"), Some(fixedDate), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.plusDays(5))))))
            )
          )

          val result: Option[Either[(LocalDate, Boolean), Int]] = {
            TestFinancialDetailsService.getChargeDueDates(List(financialDetailsCurrentYear, financialDetailsLastYear))(isEnabled(TimeMachineAddYear))
          }

          result shouldBe Some(Left(fixedDate.plusDays(5) -> false))
        }
      }
      "return the count of overdue dates" when {
        "there are more than one overdue dates" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", Some("ITSA- POA 1"), Some("documentText"), Some(100.00), Some(0.00), LocalDate.of(2018, 3, 29), documentDueDate = Some(fixedDate.minusDays(1))),
              DocumentDetail(2018, "testTransactionId2", Some("ITSA - POA 2"), Some("documentText"), Some(100.00), Some(0.00), LocalDate.of(2018, 3, 29), documentDueDate = Some(fixedDate.plusDays(1)))
            ),
            financialDetails = List(
              FinancialDetail("2018", Some("SA Payment on Account 1"), Some("testTransactionId1"), Some(fixedDate), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.minusDays(1)))))),
              FinancialDetail("2018", Some("SA Payment on Account 2"), Some("testTransactionId2"), Some(fixedDate), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.plusDays(1))))))
            )
          )

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(
            balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None),
            documentDetails = List(
              DocumentDetail(2018, "testTransactionId1", Some("ITSA- POA 1"), Some("documentText"), Some(100.00), Some(0.00), LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.plusDays(1)), documentDueDate = Some(fixedDate.plusDays(1))),
              DocumentDetail(2018, "testTransactionId2", Some("ITSA - POA 2"), Some("documentText"), Some(100.00), Some(0.00), LocalDate.of(2018, 3, 29), effectiveDateOfPayment = Some(fixedDate.minusDays(1)), documentDueDate = Some(fixedDate.minusDays(1)))
            ),
            financialDetails = List(
              FinancialDetail("2018", Some("SA Payment on Account 1"), Some("testTransactionId1"), Some(fixedDate), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.plusDays(3)))))),
              FinancialDetail("2018", Some("SA Payment on Account 2"), Some("testTransactionId2"), Some(fixedDate), Some("type"), Some(100), Some(100), Some(100), Some(100), Some(NIC4_WALES), Some(100), Some(Seq(SubItem(Some(fixedDate.minusDays(2))))))
            )
          )

          val result: Option[Either[(LocalDate, Boolean), Int]] = {
            TestFinancialDetailsService.getChargeDueDates(List(financialDetailsCurrentYear, financialDetailsLastYear))(isEnabled(TimeMachineAddYear))
          }

          result shouldBe Some(Right(2))
        }
      }
      "return none" when {
        "there are no upcoming or overdue dates" in {
          val financialDetailsCurrentYear: FinancialDetailsModel = FinancialDetailsModel(BalanceDetails(1.00, 2.00, 3.00, None, None, None, None), List(), List())

          val financialDetailsLastYear: FinancialDetailsModel = FinancialDetailsModel(BalanceDetails(1.00, 2.00, 3.00, None, None, None, None), List(), List())

          val result: Option[Either[(LocalDate, Boolean), Int]] = {
            TestFinancialDetailsService.getChargeDueDates(List(financialDetailsCurrentYear, financialDetailsLastYear))(isEnabled(TimeMachineAddYear))
          }

          result shouldBe None
        }
      }
    }
  }

  "getChargeHistoryDetails" when {

    "the connector returns a successful ChargesHistoryModel" should {
      "return the chargeHistoryDetails from connector response" in {
        val docNumber = "chargeId"
        val hc = implicitly[HeaderCarrier]

        when(mockFinancialDetailsConnector.getChargeHistory(any(), any())(any()))
          .thenReturn(Future.successful(testValidChargeHistoryModel))

        val result = TestFinancialDetailsService.getChargeHistoryDetails(testMtditid, docNumber)(hc)

        result.futureValue shouldBe testValidChargeHistoryModel.chargeHistoryDetails
        verify(mockFinancialDetailsConnector).getChargeHistory(testMtditid, docNumber)(hc)
      }
    }

    "the connector returns an erroneous ChargesHistoryErrorModel" should {
      "generate a failure with InternalServerException" in {
        when(mockFinancialDetailsConnector.getChargeHistory(any(), any())(any()))
          .thenReturn(Future.successful(testChargeHistoryErrorModel))

        val result = TestFinancialDetailsService.getChargeHistoryDetails(testMtditid, "chargeId")(implicitly)

        result.failed.futureValue shouldBe an[InternalServerException]
        result.failed.futureValue.getMessage shouldBe "[FinancialDetailsService][getChargeHistoryDetails] - Failed to retrieve successful charge history"
      }
    }

    "the connector call fails" should {
      "propagate a failure from the connector" in {
        val emulatedConnectorFailure = Future.failed(new RuntimeException)
        when(mockFinancialDetailsConnector.getChargeHistory(any(), any())(any()))
          .thenReturn(emulatedConnectorFailure)

        val result = TestFinancialDetailsService.getChargeHistoryDetails(testMtditid, "chargeId")(implicitly)

        result shouldBe emulatedConnectorFailure
      }
    }
  }


  "getAllFinancialDetails" when {
    "return a set of successful financial details" when {
      "a successful response is returned for a single year" in {
        val financialDetail = getFinancialDetailSuccess(getTaxEndYear(fixedDate))
        val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
          (getTaxEndYear(fixedDate), financialDetail)
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetail)
        val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)
        result.futureValue shouldBe expectedResult
      }

      "successful responses are returned for multiple years" in {
        val financialDetailLastYear = getFinancialDetailSuccess(fixedDate.getYear)
        val financialDetail = getFinancialDetailSuccess(fixedDate.getYear + 1)
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
        val financialDetailLastYear = getFinancialDetailSuccess(getTaxEndYear(fixedDate.minusYears(1)))
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
        val financialDetails = getFinancialDetailSuccess(getTaxEndYear(fixedDate))
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

  "getAllUnpaidFinancialDetails" when {
    "return financial transactions with only the unpaid transactions" when {
      "only unpaid transactions exist" in {

        val financialDetailLastYear = getFinancialDetailSuccess(
          taxYear = getTaxEndYear(fixedDate.minusYears(1)),
          documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = Some(100.00), interestOutstandingAmount = Some(0)),
            fullDocumentDetailModel.copy(outstandingAmount = Some(200.00), interestOutstandingAmount = Some(0))
          ),
          financialDetails = List(
            fullFinancialDetailModel,
            fullFinancialDetailModel,
          )
        )
        val financialDetail = getFinancialDetailSuccess(
          taxYear = getTaxEndYear(fixedDate),
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

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(financialDetailLastYear)
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetail)

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(isEnabled(CodingOut))(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "a mix of unpaid, paid and non charge transactions exist" in {

        val expectedResult: List[FinancialDetailsResponseModel] = List(
          getFinancialDetailSuccess(
            taxYear = getTaxEndYear(fixedDate.minusYears(1)),
            documentDetails = List(
              fullDocumentDetailModel.copy(outstandingAmount = Some(0.00), latePaymentInterestAmount = Some(50.0)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(100.00), originalAmount = Some(100.00)),
            ),
            financialDetails = List(
              fullFinancialDetailModel
            )
          ),
          getFinancialDetailSuccess(
            taxYear = getTaxEndYear(fixedDate),
            documentDetails = List(
              fullDocumentDetailModel.copy(outstandingAmount = Some(300.00), originalAmount = Some(300.00)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(0.00), latePaymentInterestAmount = Some(25.0))
            ),
            financialDetails = List(
              fullFinancialDetailModel
            )
          )
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(getFinancialDetailSuccess(
          taxYear = getTaxEndYear(fixedDate.minusYears(1)),
          documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = Some(0.00), latePaymentInterestAmount = Some(50.0)),
            fullDocumentDetailModel.copy(outstandingAmount = Some(100.00), originalAmount = Some(100.00)),
            fullDocumentDetailModel.copy(outstandingAmount = Some(0), originalAmount = Some(-200.00), latePaymentInterestAmount = None)
          ),
          financialDetails = List(
            fullFinancialDetailModel
          )
        ))
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(getFinancialDetailSuccess(
          taxYear = getTaxEndYear(fixedDate),
          documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = Some(300.00), originalAmount = Some(300.00)),
            fullDocumentDetailModel.copy(outstandingAmount = Some(0.00), originalAmount = Some(-400.00), latePaymentInterestAmount = None),
            fullDocumentDetailModel.copy(outstandingAmount = Some(0.00), latePaymentInterestAmount = Some(25.0))
          ),
          financialDetails = List(
            fullFinancialDetailModel
          )
        ))

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(isEnabled(CodingOut))(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "no unpaid transactions exist" in {

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(getFinancialDetailSuccess(
          taxYear = getTaxEndYear(fixedDate.minusYears(1)),
          documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = Some(0), latePaymentInterestAmount = None),
            fullDocumentDetailModel.copy(outstandingAmount = Some(0), latePaymentInterestAmount = None)
          ),
          financialDetails = List(
            fullFinancialDetailModel,
            fullFinancialDetailModel
          )
        ))
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(getFinancialDetailSuccess(
          taxYear = getTaxEndYear(fixedDate),
          documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = Some(0), latePaymentInterestAmount = None),
            fullDocumentDetailModel.copy(outstandingAmount = Some(0), latePaymentInterestAmount = None)
          ),
          financialDetails = List(
            fullFinancialDetailModel,
            fullFinancialDetailModel
          )
        ))

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(isEnabled(CodingOut))(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe List.empty[FinancialDetailsResponseModel]
      }
      "errored financial transactions exist" in {

        val financialDetailError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal server error")
        val expectedResult: List[FinancialDetailsResponseModel] = List(
          getFinancialDetailSuccess(
            taxYear = getTaxEndYear(fixedDate.minusYears(1)),
            documentDetails = List(
              fullDocumentDetailModel.copy(outstandingAmount = Some(100.00))
            ),
            financialDetails = List(
              fullFinancialDetailModel
            )
          ),
          financialDetailError
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(getFinancialDetailSuccess(
          taxYear = getTaxEndYear(fixedDate.minusYears(1)),
          documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = Some(100.00)),
            fullDocumentDetailModel.copy(outstandingAmount = Some(0.00), latePaymentInterestAmount = Some(0.00), interestOutstandingAmount = Some(0.00))
          ),
          financialDetails = List(
            fullFinancialDetailModel
          )
        ))
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetailError)

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(isEnabled(CodingOut))(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
    }
    "return unpaid transactions and coding out document details" when {
      "coding out is enabled and coding out data exists" in {
        enable(CodingOut)
        val financialDetailCodingOut = getFinancialDetailSuccess(
          taxYear = getTaxEndYear(fixedDate.minusYears(1)),
          documentDetails = List(
            documentDetailModel(transactionId = "transid1", outstandingAmount = Some(200.00)).copy(
              interestOutstandingAmount = Some(0), documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_CLASS2_NICS)),
            documentDetailModel(taxYear = 2021, transactionId = "transid2", outstandingAmount = Some(0)).copy(
              interestOutstandingAmount = Some(0), documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_ACCEPTED)),
            documentDetailModel(transactionId = "transid3", outstandingAmount = Some(0)).copy(
              interestOutstandingAmount = Some(0), documentDescription = Some("TRM Amend Charge"), documentText = Some(CODING_OUT_CANCELLED)),
          ),
          financialDetails = List(
            fullFinancialDetailModel,
            fullFinancialDetailModel,
          )
        )
        val financialDetail = getFinancialDetailSuccess(
          taxYear = getTaxEndYear(fixedDate),
          documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = Some(300.00)),
            fullDocumentDetailModel.copy(outstandingAmount = Some(400.00))
          ),
          financialDetails = List(
            fullFinancialDetailModel,
            fullFinancialDetailModel
          )
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(financialDetailCodingOut)
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetail)

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(isEnabled(CodingOut))(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe List(
          financialDetailCodingOut,
          financialDetail
        )
      }
    }

    "return unpaid transactions without coding out document details" should {
      "coding out is disabled" when {
        "class 2 nics exists" in {
          disable(CodingOut)
          val ddNics = documentDetailModel(
            transactionId = "transid1", outstandingAmount = Some(200.00), latePaymentInterestAmount = None).copy(interestOutstandingAmount = Some(0), documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_CLASS2_NICS))
          val ddCodedOut = documentDetailModel(
            taxYear = getTaxEndYear(fixedDate.minusYears(1)), transactionId = "transid2", outstandingAmount = Some(2500.00),
            latePaymentInterestAmount = None).copy(interestOutstandingAmount = Some(0), documentDescription = Some("TRM Amend Charge"),
            documentText = Some(CODING_OUT_ACCEPTED))
          val ddCancelledCodedOut = documentDetailModel(
            transactionId = "transid3", outstandingAmount = Some(2500.00), latePaymentInterestAmount = None).copy(
            interestOutstandingAmount = Some(0), documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_CANCELLED))
          val financialDetailCodingOut = getFinancialDetailSuccess(
            taxYear = getTaxEndYear(fixedDate.minusYears(1)),
            documentDetails = List(ddNics, ddCodedOut, ddCancelledCodedOut),
            financialDetails = List(
              fullFinancialDetailModel,
              fullFinancialDetailModel,
              fullFinancialDetailModel
            )
          )
          val financialDetail = getFinancialDetailSuccess(
            taxYear = getTaxEndYear(fixedDate),
            documentDetails = List(
              fullDocumentDetailModel.copy(outstandingAmount = Some(300.00)),
              fullDocumentDetailModel.copy(outstandingAmount = Some(400.00))
            ),
            financialDetails = List(
              fullFinancialDetailModel,
              fullFinancialDetailModel
            )
          )

          setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(financialDetailCodingOut)
          setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetail)

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(isEnabled(CodingOut))(mtdUser(2), headerCarrier, ec)

          result.futureValue shouldBe List(
            financialDetail
          )
        }
      }
    }
  }

}

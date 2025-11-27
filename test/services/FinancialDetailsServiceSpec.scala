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
    FinancialDetailsModel(balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
      documentDetails = documentDetails, financialDetails = financialDetails)
  }

  private def getMultiYearFinancialDetailSuccess(documentDetails: List[DocumentDetail] = List(fullDocumentDetailModel, fullDocumentDetailModel),
                                                  financialDetails: List[FinancialDetail] = List(fullFinancialDetailModel, fullFinancialDetailModel)) = {
    FinancialDetailsModel(balanceDetails = BalanceDetails(1.00, 2.00, 3.00, None, None, None, None, None, None, None),
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
          contextualTaxYear = None,
          cessation = None,
          address = Some(address)
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
        contextualTaxYear = None,
        cessation = None,
        address = Some(address)
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
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(50.0)),
            fullDocumentDetailModel.copy(outstandingAmount = 100.00, originalAmount = 100.00),
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(0.00), interestOutstandingAmount = Some(100.00))
          ), financialDetails = List(
            fullFinancialDetailModel
          )),
          getFinancialDetailSuccess(documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = 300.00, originalAmount = 300.00),
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(25.0))
          ), financialDetails = List(
            fullFinancialDetailModel
          ))
        )

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(50.0)),
          fullDocumentDetailModel.copy(outstandingAmount = 100.00, originalAmount = 100.00),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(0.00), interestOutstandingAmount = Some(100.00)),
          fullDocumentDetailModel.copy(outstandingAmount = 0, originalAmount = -200.00, accruingInterestAmount = None, interestOutstandingAmount = None)
        ), financialDetails = List(
          fullFinancialDetailModel
        )))
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 300.00, originalAmount = 300.00),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, originalAmount = -400.00, accruingInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(25.0))
        ), financialDetails = List(
          fullFinancialDetailModel
        )))

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails()(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "no unpaid transactions exist" in {

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 0, accruingInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0, accruingInterestAmount = None, interestOutstandingAmount = None)
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel
        )))
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 0, accruingInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0, accruingInterestAmount = None, interestOutstandingAmount = None)
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
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(0.00), interestOutstandingAmount = Some(0.00))
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
          documentDetailModel(transactionId = "transid4", outstandingAmount = 0).copy(
            interestOutstandingAmount = Some(0), documentDescription = Some("TRM New Charge"), documentText = Some(CODING_OUT_CLASS2_NICS))
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel,
        ))
        val financialDetail = getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 300.00),
          fullDocumentDetailModel.copy(outstandingAmount = 400.00),
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel,
          fullFinancialDetailModel
        ))

        setupMockGetFinancialDetails(getTaxEndYear(fixedDate.minusYears(1)), testNino)(financialDetailCodingOut)
        setupMockGetFinancialDetails(getTaxEndYear(fixedDate), testNino)(financialDetail)

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails()(mtdUser(2), headerCarrier, ec)

        val expectedFinancialDetailCodingOut = getFinancialDetailSuccess(documentDetails = List(
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

        result.futureValue shouldBe List(
          expectedFinancialDetailCodingOut,
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

        setupMockGetFinancialDetailsByTaxYearRange(fixedTaxYearRange, testNino)(financialDetail)
        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetailsV2()(mtdUser(1), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "a mix of unpaid, paid and non charge transactions exist" in {

        val expectedResult: Option[FinancialDetailsResponseModel] = Some(
          getFinancialDetailSuccess(documentDetails = List(
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(50.0)),
            fullDocumentDetailModel.copy(outstandingAmount = 100.00, originalAmount = 100.00),
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(0.00), interestOutstandingAmount = Some(100.00)),
            fullDocumentDetailModel.copy(outstandingAmount = 300.00, originalAmount = 300.00),
            fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(25.0))
          ), financialDetails = List(
            fullFinancialDetailModel,
            fullFinancialDetailModel
          ))
        )

        setupMockGetFinancialDetailsByTaxYearRange(multiYearRange, testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(50.0)),
          fullDocumentDetailModel.copy(outstandingAmount = 100.00, originalAmount = 100.00),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(0.00), interestOutstandingAmount = Some(100.00)),
          fullDocumentDetailModel.copy(outstandingAmount = 0, originalAmount = -200.00, accruingInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 300.00, originalAmount = 300.00),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, originalAmount = -400.00, accruingInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0.00, accruingInterestAmount = Some(25.0))
        ), financialDetails = List(
          fullFinancialDetailModel,
          fullFinancialDetailModel
        )))

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetailsV2()(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe expectedResult
      }
      "no unpaid transactions exist" in {

        setupMockGetFinancialDetailsByTaxYearRange(multiYearRange, testNino)(getFinancialDetailSuccess(documentDetails = List(
          fullDocumentDetailModel.copy(outstandingAmount = 0, accruingInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0, accruingInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0, accruingInterestAmount = None, interestOutstandingAmount = None),
          fullDocumentDetailModel.copy(outstandingAmount = 0, accruingInterestAmount = None, interestOutstandingAmount = None)
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

        setupMockGetFinancialDetailsByTaxYearRange(multiYearRange, testNino)(financialDetailCodingOut)

        val result = TestFinancialDetailsService.getAllUnpaidFinancialDetailsV2()(mtdUser(2), headerCarrier, ec)

        result.futureValue shouldBe Some(
          financialDetailCodingOut
        )
      }
    }
  }
}

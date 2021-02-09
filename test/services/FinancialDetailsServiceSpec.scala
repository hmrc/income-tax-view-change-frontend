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
import assets.FinancialDetailsTestConstants._
import auth.MtdItUser
import config.featureswitch.{API5, FeatureSwitching, NewFinancialDetailsApi}
import mocks.connectors.MockIncomeTaxViewChangeConnector
import models.core.AccountingPeriodModel
import models.financialDetails.{Charge, FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.http.Status
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import testUtils.TestSupport

import java.time.LocalDate


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

  object TestFinancialDetailsService extends FinancialDetailsService(mockIncomeTaxViewChangeConnector)

  "getFinancialDetails" when {
    "a successful financial details response is returned from the connector" should {
      "return a valid FinancialDetails model" in {
        setupMockGetFinancialDetails(testTaxYear)(financialDetailsModel(testTaxYear))
        await(TestFinancialDetailsService.getFinancialDetails(testTaxYear)) shouldBe financialDetailsModel(testTaxYear)
      }
    }
    "a error model is returned from the connector" should {
      "return a FinancialDetailsError model" in {
        setupMockGetFinancialDetails(testTaxYear)(testFinancialDetailsErrorModel)
        await(TestFinancialDetailsService.getFinancialDetails(testTaxYear)) shouldBe testFinancialDetailsErrorModel
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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetail)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetail)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetailNotFound)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "only not found response is returned" in {
          enable(API5)
          val financialDetailNotFound = FinancialDetailsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List.empty

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetailNotFound)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetailsError)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(financialDetailsError)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetailsError)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(financialDetailsErrorLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetails)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetail)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetail)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "a successful response and a not found response are returned" in {
          val financialDetailLastYear = getFinancialDetailSuccess(getTaxEndYear(LocalDate.now.minusYears(1)))
          val financialDetailNotFound = FinancialDetailsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialDetailLastYear)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetailNotFound)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "only not found response is returned" in {
          val financialDetailNotFound = FinancialDetailsErrorModel(Status.NOT_FOUND, "not found")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List.empty

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetailNotFound)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetailsError)

          val result = TestFinancialDetailsService.getAllFinancialDetails(mtdUser(1), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
        "an error response is returned for multiple years" in {
          val financialDetailsError = FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "internal service error")
          val expectedResult: List[(Int, FinancialDetailsResponseModel)] = List(
            (getTaxEndYear(LocalDate.now.minusYears(1)), financialDetailsError),
            (getTaxEndYear(LocalDate.now), financialDetailsError)
          )

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(financialDetailsError)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetailsError)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(financialDetailsErrorLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetails)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetail)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = None),
              fullChargeModel.copy(outstandingAmount = Some(100.00), originalAmount = Some(100.00)),
              fullChargeModel.copy(outstandingAmount = Some(-200.00), totalAmount = Some(-200.00), originalAmount = Some(-200.00))
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(getFinancialDetailSuccess(
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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = None),
              fullChargeModel.copy(outstandingAmount = None)
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            charges = List(
              fullChargeModel.copy(outstandingAmount = None),
              fullChargeModel.copy(outstandingAmount = None)
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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(100.00)),
              fullChargeModel.copy(outstandingAmount = None)
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetailError)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(financialDetailLastYear)
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetail)

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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = None),
              fullChargeModel.copy(outstandingAmount = Some(100.00), originalAmount = Some(100.00)),
              fullChargeModel.copy(outstandingAmount = Some(-200.00), totalAmount = Some(-200.00), originalAmount = Some(-200.00))
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(getFinancialDetailSuccess(
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
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = None),
              fullChargeModel.copy(outstandingAmount = None)
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now),
            charges = List(
              fullChargeModel.copy(outstandingAmount = None),
              fullChargeModel.copy(outstandingAmount = None)
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

          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now.minusYears(1)))(getFinancialDetailSuccess(
            taxYear = getTaxEndYear(LocalDate.now.minusYears(1)),
            charges = List(
              fullChargeModel.copy(outstandingAmount = Some(100.00)),
              fullChargeModel.copy(outstandingAmount = None)
            )
          ))
          setupMockGetFinancialDetails(getTaxEndYear(LocalDate.now))(financialDetailError)

          val result = TestFinancialDetailsService.getAllUnpaidFinancialDetails(mtdUser(2), headerCarrier, ec)

          await(result) shouldBe expectedResult
        }
      }
    }
  }

}

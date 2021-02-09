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

package controllers

import assets.BaseTestConstants._
import assets.BusinessDetailsTestConstants._
import assets.EstimatesTestConstants._
import assets.FinancialDetailsTestConstants._
import assets.FinancialTransactionsTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.{BaseTestConstants, MessagesLookUp}
import audit.AuditingService
import config.featureswitch.{API5, FeatureSwitching, NewFinancialDetailsApi, Payment}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockFinancialDetailsService, MockFinancialTransactionsService}
import models.calculation.CalculationResponseModelWithYear
import models.incomeSourceDetails.IncomeSourceDetailsModel
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import services.CalculationService
import testUtils.TestSupport

import scala.concurrent.Future

class TaxYearsControllerSpec extends MockCalculationService
  with MockFinancialTransactionsService with MockFinancialDetailsService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter with FeatureSwitching {

  val calculationService: CalculationService = mock[CalculationService]

  object TestTaxYearsController extends TaxYearsController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    calculationService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AuditingService],
    mockFinancialTransactionsService,
    mockFinancialDetailsService
  )

  lazy val CalcMessages = new MessagesLookUp.Calculation(testYear)

  "The TestYearsController.viewTaxYears action" when {
    "NewFinancialDetailsApi FS is disabled" when {
      "called with an authenticated HMRC-MTD-IT user and with API5 is enabled" which {
        "successfully retrieves Business only income from the Income Sources predicate" when {
          "the getAllFinancialTransactions brings back an instance of FinancialTransactionsErrorModel" should {
            lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
            lazy val document = result.toHtmlDocument
            lazy val messages = MessagesLookUp.TaxYears

            "return an ISE (500)" in {
              enable(API5)
              when(calculationService.getAllLatestCalculations(any(), any())(any()))
                .thenReturn(Future.successful(lastTaxCalcWithYearList))
              setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(testMtditid, List(business1, business2018), None))
              mockGetAllFinancialTransactions(List(
                2019 -> financialTransactionsModel("2020-5-30", Some(1000)),
                2020 -> financialTransactionsErrorModel
              ))
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }

          "the getAllFinancialTransactions brings back an instance of FinancialTransactionsModel" should {
            lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
            lazy val document = result.toHtmlDocument
            lazy val messages = MessagesLookUp.TaxYears

            "return status OK (200)" in {
              disable(NewFinancialDetailsApi)
              enable(API5)
              when(calculationService.getAllLatestCalculations(any(), any())(any()))
                .thenReturn(Future.successful(lastTaxCalcWithYearList))
              setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(testMtditid,List(business1, business2018), None))
              mockGetAllFinancialTransactions(List(
                2019 -> financialTransactionsModel("2020-5-30", Some(1000)),
                2020 -> financialTransactionsModel("2020-3-6", Some(0))
              ))
              status(result) shouldBe Status.OK
            }
            "return HTML" in {
              disable(NewFinancialDetailsApi)
              enable(API5)
              contentType(result) shouldBe Some("text/html")
              charset(result) shouldBe Some("utf-8")
            }
            "render the Tax Years sub-page" in {
              disable(NewFinancialDetailsApi)
              enable(API5)
              document.title shouldBe messages.title
            }
          }
        }

        "successfully retrieves income sources, but the list returned from the service has a not found error model" should {
          lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

          "return status OK (200)" in {
            enable(API5)
            setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
            when(calculationService.getAllLatestCalculations(any(), any())(any()))
              .thenReturn(Future.successful(lastTaxCalcWithYearListOneNotFound))

            mockGetAllLatestCalcSuccessOneNotFound()
            mockGetAllFinancialTransactions(List(
              2019 -> financialTransactionsModel("2020-3-6", Some(1000)),
              2020 -> financialTransactionsModel("2020-3-6", Some(0))
            ))

            status(result) shouldBe Status.OK
          }
        }


        "successfully retrieves income sources, but the list returned from the service has an internal server error model" should {

          lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

          "return an ISE (500)" in {
            enable(API5)
            setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
            when(calculationService.getAllLatestCalculations(any(), any())(any()))
              .thenReturn(Future.successful(lastTaxCalcWithYearListWithError))
            mockGetAllFinancialTransactions(List(
              2019 -> financialTransactionsModel("2020-3-6", Some(1000)),
              2020 -> financialTransactionsModel("2020-3-6", Some(0))
            ))

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }
    }

    "NewFinancialDetailsApi FS is enabled" when {
      "called with an authenticated HMRC-MTD-IT user and with API5 is enabled" which {
        "successfully retrieves Business only income from the Income Sources predicate" when {
          "the getAllFinancialDetails brings back an instance of FinancialTransactionsErrorModel" should {

            lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
            lazy val document = result.toHtmlDocument
            lazy val messages = MessagesLookUp.TaxYears

            "return an ISE (500)" in {
              enable(NewFinancialDetailsApi)
              enable(API5)
              setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(testMtditid, List(business1, business2018), None))
              when(calculationService.getAllLatestCalculations(any(), any())(any()))
                .thenReturn(Future.successful(lastTaxCalcWithYearList))
              mockGetAllFinancialDetails(List(
                2019 -> financialDetailsModel(2021, Some(1000)),
                2020 -> testFinancialDetailsErrorModel
              ))

              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }

          "the getAllFinancialDetails brings back an instance of FinancialTransactionsModel" should {

            lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
            lazy val document = result.toHtmlDocument
            lazy val messages = MessagesLookUp.TaxYears

            "return status OK (200)" in {
              enable(NewFinancialDetailsApi)
              enable(API5)
              setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(testMtditid, List(business1, business2018), None))
              when(calculationService.getAllLatestCalculations(any(), any())(any()))
                .thenReturn(Future.successful(lastTaxCalcWithYearList))
              mockGetAllFinancialDetails(List(
                2019 -> financialDetailsModel(2021, Some(1000)),
                2020 -> financialDetailsModel(2020, Some(0))
              ))
              status(result) shouldBe Status.OK
            }
            "return HTML" in {
              enable(NewFinancialDetailsApi)
              enable(API5)
              contentType(result) shouldBe Some("text/html")
              charset(result) shouldBe Some("utf-8")
            }
            "render the Tax Years sub-page" in {
              enable(NewFinancialDetailsApi)
              enable(API5)
              document.title shouldBe messages.title
            }
          }
        }

        "successfully retrieves income sources, but the list returned from the service has a not found error model" should {
          lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

          "return status OK (200)" in {
            enable(NewFinancialDetailsApi)
            enable(API5)
            setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
            when(calculationService.getAllLatestCalculations(any(), any())(any()))
              .thenReturn(Future.successful(lastTaxCalcWithYearListOneNotFound))
            mockGetAllFinancialDetails(List(
              2019 -> financialDetailsModel(2020, Some(1000)),
              2020 -> financialDetailsModel(2020, Some(0))
            ))

            status(result) shouldBe Status.OK
          }
        }


        "successfully retrieves income sources, but the list returned from the service has an internal server error model" should {

          lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

          "return an ISE (500)" in {
            enable(NewFinancialDetailsApi)
            enable(API5)
            setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
            when(calculationService.getAllLatestCalculations(any(), any())(any()))
              .thenReturn(Future.successful(lastTaxCalcWithYearListWithError))
            mockGetAllFinancialDetails(List(
              2019 -> financialDetailsModel(2020, Some(1000)),
              2020 -> financialDetailsModel(2020, Some(0))
            ))

            status(result) shouldBe Status.INTERNAL_SERVER_ERROR
          }
        }
      }
      disable(NewFinancialDetailsApi)
    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        enable(API5)
        setupMockAuthorisationException()
        val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }


  "called with an authenticated HMRC-MTD-IT user and with API5 and NewFinancialDetailsApi FS are disabled" which {
    "successfully retrieves Business only income from the Income Sources predicate" when {
      "the getAllFinancialTransactions brings back an instance of FinancialTransactionsErrorModel" should {

        lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument
        lazy val messages = MessagesLookUp.TaxYears

        "return an ISE (500)" in {
          disable(NewFinancialDetailsApi)
          disable(API5)
          setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(testMtditid, List(business1, business2018), None))
          mockGetAllLatestCalcSuccess()
          mockGetAllFinancialTransactions(List(
            2019 -> financialTransactionsModel("2020-5-30", Some(1000)),
            2020 -> financialTransactionsErrorModel
          ))
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      "the getAllFinancialTransactions brings back an instance of FinancialTransactionsModel" should {

        lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument
        lazy val messages = MessagesLookUp.TaxYears

        "return status OK (200)" in {
          disable(NewFinancialDetailsApi)
          disable(API5)
          setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(testMtditid, List(business1, business2018), None))
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenReturn(Future.successful(lastTaxCalcWithYearList))
          mockGetAllFinancialTransactions(List(
            2019 -> financialTransactionsModel("2020-5-30", Some(1000)),
            2020 -> financialTransactionsModel("2020-3-6", Some(0))
          ))
          status(result) shouldBe Status.OK
        }
        "return HTML" in {
          disable(NewFinancialDetailsApi)
          disable(API5)
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }
        "render the Tax Years sub-page" in {
          disable(NewFinancialDetailsApi)
          disable(API5)
          document.title shouldBe messages.title
        }
      }
    }

    "successfully retrieves income sources, but the list returned from the service has a not found error model" should {
      lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

      "return status OK (200)" in {
        disable(NewFinancialDetailsApi)
        disable(API5)
        setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
        when(calculationService.getAllLatestCalculations(any(), any())(any()))
          .thenReturn(Future.successful(lastTaxCalcWithYearListOneNotFound))
        mockGetAllFinancialTransactions(List(
          2019 -> financialTransactionsModel("2020-3-6", Some(1000)),
          2020 -> financialTransactionsModel("2020-3-6", Some(0))
        ))

        status(result) shouldBe Status.OK
      }
    }


    "successfully retrieves income sources, but the list returned from the service has an internal server error model" should {

      lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

      "return an ISE (500)" in {
        disable(NewFinancialDetailsApi)
        disable(API5)
        setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
        when(calculationService.getAllLatestCalculations(any(), any())(any()))
          .thenReturn(Future.successful(lastTaxCalcWithYearListWithError))
        mockGetAllFinancialTransactions(List(
          2019 -> financialTransactionsModel("2020-3-6", Some(1000)),
          2020 -> financialTransactionsModel("2020-3-6", Some(0))
        ))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }


  "Called with an Unauthenticated User with API5 and NewFinancialDetailsApi FS are disabled" should {

    "return redirect SEE_OTHER (303)" in {
      disable(NewFinancialDetailsApi)
      disable(API5)
      setupMockAuthorisationException()
      val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

  "called with an authenticated HMRC-MTD-IT user and with API5 FS is disable and NewFinancialDetailsApi FS is enabled" which {
    "successfully retrieves Business only income from the Income Sources predicate" when {
      "the getAllFinancialDetails brings back an instance of FinancialDetailsErrorModel" should {

        lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument
        lazy val messages = MessagesLookUp.TaxYears

        "return an ISE (500)" in {
          enable(NewFinancialDetailsApi)
          disable(API5)
          setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(testMtditid, List(business1, business2018), None))
          mockGetAllLatestCalcSuccess()
          mockGetAllFinancialDetails(List(
            2019 -> financialDetailsModel(2021, Some(1000)),
            2020 -> testFinancialDetailsErrorModel
          ))

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }

      "the getAllFinancialDetails brings back an instance of FinancialDetailsModel" should {

        lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument
        lazy val messages = MessagesLookUp.TaxYears

        "return status OK (200)" in {
          enable(NewFinancialDetailsApi)
          disable(API5)
          setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(testMtditid, List(business1, business2018), None))
          when(calculationService.getAllLatestCalculations(any(), any())(any()))
            .thenReturn(Future.successful(lastTaxCalcWithYearList))
          mockGetAllFinancialDetails(List(
            2019 -> financialDetailsModel(2021, Some(1000)),
            2020 -> financialDetailsModel(2020, Some(0))
          ))

          status(result) shouldBe Status.OK
        }
        "return HTML" in {
          enable(NewFinancialDetailsApi)
          disable(API5)
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }
        "render the Tax Years sub-page" in {
          enable(NewFinancialDetailsApi)
          disable(API5)
          document.title shouldBe messages.title
        }
      }
    }

    "successfully retrieves income sources, but the list returned from the service has a not found error model" should {
      lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

      "return status OK (200)" in {
        enable(NewFinancialDetailsApi)
        disable(API5)
        setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
        when(calculationService.getAllLatestCalculations(any(), any())(any()))
          .thenReturn(Future.successful(lastTaxCalcWithYearListOneNotFound))
        mockGetAllFinancialDetails(List(
          2019 -> financialDetailsModel(2020, Some(1000)),
          2020 -> financialDetailsModel(2020, Some(0))
        ))

        status(result) shouldBe Status.OK
      }
    }


    "successfully retrieves income sources, but the list returned from the service has an internal server error model" should {

      lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

      "return an ISE (500)" in {
        enable(NewFinancialDetailsApi)
        disable(API5)
        setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
        mockGetAllLatestCrystallisedCalcWithError()
        mockGetAllFinancialTransactions(List(
          2019 -> financialTransactionsModel("2020-3-6", Some(1000)),
          2020 -> financialTransactionsModel("2020-3-6", Some(0))
        ))

        status(result) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "Called with an Unauthenticated User with API5 FS is disable and NewFinancialDetailsApi FS is enabled" should {

    "return redirect SEE_OTHER (303)" in {
      enable(NewFinancialDetailsApi)
      disable(API5)
      setupMockAuthorisationException()
      val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
      status(result) shouldBe Status.SEE_OTHER
    }
  }

}

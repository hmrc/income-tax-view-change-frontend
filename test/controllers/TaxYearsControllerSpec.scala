/*
 * Copyright 2020 HM Revenue & Customs
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
import assets.FinancialTransactionsTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import assets.MessagesLookUp
import audit.AuditingService
import config.featureswitch.{API5, FeatureSwitching}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import implicits.ImplicitDateFormatter
import javax.inject.Inject
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate}
import mocks.services.{MockCalculationService, MockFinancialTransactionsService}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers._
import testUtils.TestSupport
import uk.gov.hmrc.play.language.LanguageUtils

class TaxYearsControllerSpec @Inject() (val languageUtils: LanguageUtils) extends TestSupport with MockCalculationService with MockFinancialTransactionsService
  with MockAuthenticationPredicate with MockIncomeSourceDetailsPredicate with ImplicitDateFormatter with FeatureSwitching {

  object TestTaxYearsController extends TaxYearsController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesControllerComponents],
    ec,
    app.injector.instanceOf[SessionTimeoutPredicate],
    MockAuthenticationPredicate,
    app.injector.instanceOf[NinoPredicate],
    MockIncomeSourceDetailsPredicate,
    mockCalculationService,
    app.injector.instanceOf[ItvcHeaderCarrierForPartialsConverter],
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AuditingService],
    mockFinancialTransactionsService
  )

  lazy val CalcMessages = new MessagesLookUp.Calculation(testYear)

  "The TestYearsController.viewTaxYears action" when {

    "called with an authenticated HMRC-MTD-IT user and with API5 is enabled" which {

      "successfully retrieves Business only income from the Income Sources predicate" when {

        "the getAllFinancialTransactions brings back an instance of FinancialTransactionsErrorModel" should {

          lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument
          lazy val messages = MessagesLookUp.TaxYears

          "return an ISE (500)" in {
            enable(API5)
            setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(List(business1, business2018), None))
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
            enable(API5)
            setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(List(business1, business2018), None))
            mockGetAllLatestCalcSuccess()
            mockGetAllFinancialTransactions(List(
              2019 -> financialTransactionsModel("2020-5-30", Some(1000)),
              2020 -> financialTransactionsModel("2020-3-6", Some(0))
            ))
            status(result) shouldBe Status.OK
          }
          "return HTML" in {
            enable(API5)
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }
          "render the Tax Years sub-page" in {
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
          mockGetAllLatestCrystallisedCalcWithError()
          mockGetAllFinancialTransactions(List(
            2019 -> financialTransactionsModel("2020-3-6", Some(1000)),
            2020 -> financialTransactionsModel("2020-3-6", Some(0))
          ))

          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
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


    "called with an authenticated HMRC-MTD-IT user and with API5 is disabled" which {

      "successfully retrieves Business only income from the Income Sources predicate" when {

        "the getAllFinancialTransactions brings back an instance of FinancialTransactionsErrorModel" should {

          lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
          lazy val document = result.toHtmlDocument
          lazy val messages = MessagesLookUp.TaxYears

          "return an ISE (500)" in {
            disable(API5)
            setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(List(business1, business2018), None))
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
            disable(API5)
            setupMockGetIncomeSourceDetails(testMtdUserNino)(IncomeSourceDetailsModel(List(business1, business2018), None))
            mockGetAllLatestCalcSuccess()
            mockGetAllFinancialTransactions(List(
              2019 -> financialTransactionsModel("2020-5-30", Some(1000)),
              2020 -> financialTransactionsModel("2020-3-6", Some(0))
            ))
            status(result) shouldBe Status.OK
          }
          "return HTML" in {
            disable(API5)
            contentType(result) shouldBe Some("text/html")
            charset(result) shouldBe Some("utf-8")
          }
          "render the Tax Years sub-page" in {
            disable(API5)
            document.title shouldBe messages.title
          }
        }
      }

      "successfully retrieves income sources, but the list returned from the service has a not found error model" should {
        lazy val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)

        "return status OK (200)" in {
          disable(API5)
          setupMockGetIncomeSourceDetails(testMtdUserNino)(businessIncome2018and2019)
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

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        disable(API5)
        setupMockAuthorisationException()
        val result = TestTaxYearsController.viewTaxYears(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }


}

/*
 * Copyright 2017 HM Revenue & Customs
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

import assets.Messages.{EstimatedTaxLiability => messages}
import assets.TestConstants.BusinessDetails._
import assets.TestConstants.Estimates._
import assets.TestConstants.IncomeSourceDetails
import assets.TestConstants.PropertyDetails._
import assets.TestConstants._
import config.FrontendAppConfig
import mocks.controllers.predicates.MockAsyncActionPredicate
import mocks.services.MockFinancialDataService
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.Helpers.{contentType, _}
import utils.TestSupport

class FinancialDataControllerSpec extends TestSupport with MockFinancialDataService with MockAsyncActionPredicate {

  object TestFinancialDataController extends FinancialDataController()(
    app.injector.instanceOf[FrontendAppConfig],
    app.injector.instanceOf[MessagesApi],
    MockAsyncActionPredicate,
    mockFinancialDataService
  )

  "The FinancialDataController.getFinancialData(year) action" when {

    "Called with an Authenticated HMRC-MTD-IT User" which {

      "that successfully retrieves Business only income from the Income Sources predicate" +
        "and financial data from the FinancialData Service" should {

        lazy val result = TestFinancialDataController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockFinancialDataSuccess()
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.business2018IncomeSourceSuccess)
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the EstimatedTaxLiability page" in {
          document.title() shouldBe messages.title
        }
      }

      "receives Property only income from the Income Sources predicate" +
        "and financial data from the FinancialData Service" should {

        lazy val result = TestFinancialDataController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockPropertyIncomeSource()
          mockFinancialDataSuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the EstimatedTaxLiability page" in {
          document.title() shouldBe messages.title
        }
      }

      "receives Business Income source from the Income Sources predicate and an error from the FinancialData Service" should {

        lazy val result = TestFinancialDataController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          setupMockGetIncomeSourceDetails(testNino)(IncomeSourceDetails.business2018IncomeSourceSuccess)
          mockFinancialDataNoBreakdown()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the EstimatedTaxLiability page" in {
          document.title() shouldBe messages.title
        }
      }

      "receives Business Income from the Income Sources predicate and an error from the FinancialData Service" should {

        lazy val result = TestFinancialDataController.getFinancialData(testYear)(fakeRequestWithActiveSession)

        "return Internal Server Error (500)" in {
          mockFinancialDataError()
          mockSingleBusinessIncomeSource()
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }
      }

      "receives Business Income from the Income Sources predicate and No Data Found from the FinancialData Service" should {

        lazy val result = TestFinancialDataController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return a 404" in {
          mockFinancialDataNotFound()
          mockSingleBusinessIncomeSource()
          status(result) shouldBe Status.NOT_FOUND
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the EstimatedTaxLiabilityNoData page" in {
          document.title() shouldBe messages.title
        }
      }

    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        mockPropertyIncomeSource()
        val result = TestFinancialDataController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }


  "The FinancialDataController.redirectToEarliestEstimatedTaxLiability() action" when {

    "Called with an Authenticated HMRC-MTD-IT User" which {

      "successfully retrieves Business only income from the Income Sources predicate" should {

        lazy val result = TestFinancialDataController.redirectToEarliestEstimatedTaxLiability(fakeRequestWithActiveSession)

        "return Status SEE_OTHER (303) (redirect)" in {
          mockSingleBusinessIncomeSource()
          status(result) shouldBe Status.SEE_OTHER
        }

        s"redirect to ${
          controllers.routes.FinancialDataController
            .getFinancialData(businessIncomeModel.accountingPeriod.determineTaxYear)
        }" in {
          redirectLocation(result) shouldBe Some(controllers.routes.FinancialDataController
            .getFinancialData(businessIncomeModel.accountingPeriod.determineTaxYear).url)
        }
      }

      "successfully retrieves Property only income from the Income Sources predicate" should {

        lazy val result = TestFinancialDataController.redirectToEarliestEstimatedTaxLiability(fakeRequestWithActiveSession)

        "return Status SEE_OTHER (303) (redirect)" in {
          mockPropertyIncomeSource()
          status(result) shouldBe Status.SEE_OTHER
        }

        s"redirect to ${
          controllers.routes.FinancialDataController
            .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear)
        }" in {
          redirectLocation(result) shouldBe Some(controllers.routes.FinancialDataController
            .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear).url)
        }
      }

      "successfully retrieves both Property and Business income from the Income Sources predicate" when {

        "the Business Accounting Period is aligned to the Tax Year and the Property Income" should {

          lazy val result = TestFinancialDataController.redirectToEarliestEstimatedTaxLiability(fakeRequestWithActiveSession)

          "return Status SEE_OTHER (303) (redirect)" in {
            mockBothIncomeSourcesBusinessAligned()
            status(result) shouldBe Status.SEE_OTHER
          }

          s"redirect to ${
            controllers.routes.FinancialDataController
              .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear)
          }" in {
            redirectLocation(result) shouldBe Some(controllers.routes.FinancialDataController
              .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear).url)
          }

        }

        "the Property Income Accounting Period is prior to the Business Income Accounting Period" should {

          lazy val result = TestFinancialDataController.redirectToEarliestEstimatedTaxLiability(fakeRequestWithActiveSession)

          "return Status SEE_OTHER (303) (redirect)" in {
            mockBothIncomeSources()
            status(result) shouldBe Status.SEE_OTHER
          }

          s"redirect to ${
            controllers.routes.FinancialDataController
              .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear)
          }" in {
            redirectLocation(result) shouldBe Some(controllers.routes.FinancialDataController
              .getFinancialData(propertySuccessModel.accountingPeriod.determineTaxYear).url)
          }

        }
      }

      "retrieves no Income Sources via the Income Sources predicate" should {

        lazy val result = TestFinancialDataController.redirectToEarliestEstimatedTaxLiability(fakeRequestWithActiveSession)

        "return Status ISE (500)" in {
          mockNoIncomeSources()
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }
      }
    }

    "Called with an Unauthenticated User" should {

      "return redirect SEE_OTHER (303)" in {
        setupMockAuthorisationException()
        val result = TestFinancialDataController.getFinancialData(testYear)(fakeRequestWithActiveSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }

}

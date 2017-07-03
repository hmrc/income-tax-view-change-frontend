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
import assets.TestConstants._
import auth.MockAuthenticationPredicate
import config.FrontendAppConfig
import mocks.services.{MockBusinessDetailsService, MockFinancialDataService}
import play.api.http.Status
import play.api.i18n.MessagesApi
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentType, _}
import utils.TestSupport

class EstimatedTaxLiabilityControllerSpec extends TestSupport with MockAuthenticationPredicate
  with MockBusinessDetailsService with MockFinancialDataService {

  // Last Calculation Service mocks
  def mockLastCalculationSuccess(): Unit = setupMockLastTaxCalculationResult(testNino, testYear)(lastTaxCalcSuccess)
  def mockLastCalculationError(): Unit = setupMockLastTaxCalculationResult(testNino, testYear)(lastTaxCalcError)

  // Business Details Service mocks
  def mockBusinessDetailsSuccess(): Unit = setupMockBusinessDetailsResult(testNino)(businessesSuccessModel)
  def mockBusinessDetailsEmpty(): Unit = setupMockBusinessDetailsResult(testNino)(businessSuccessEmptyModel)
  def mockBusinessDetailsError(): Unit = setupMockBusinessDetailsResult(testNino)(businessErrorModel)


  "The EstimatedTaxLiabilityController.home action" when {

    "Called with an Authenticated HMRC-MTD-IT User" which {

      object TestEstimatedLiabilityController extends EstimatedTaxLiabilityController()(
        fakeApplication.injector.instanceOf[FrontendAppConfig],
        fakeApplication.injector.instanceOf[MessagesApi],
        MockAuthenticated,
        mockFinancialDataService,
        mockBusinessDetailsService
      )

      "successfully retrieves a Business from the Business Details service" +
        "and an Estimated Tax Liability amount from the EstimatedTaxLiability Service" should {

        lazy val result = TestEstimatedLiabilityController.getEstimatedTaxLiability()(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockLastCalculationSuccess()
          mockBusinessDetailsSuccess()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          mockLastCalculationSuccess()
          mockBusinessDetailsSuccess()
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the EstimatedTaxLiability page" in {
          mockLastCalculationSuccess()
          mockBusinessDetailsSuccess()
          document.title() shouldBe messages.title
        }
      }

      "receives no businesses from the BusinessDetails Service" +
        "and an Estimated Tax Liability amount from the EstimatedTaxLiability Service" should {

        lazy val result = TestEstimatedLiabilityController.getEstimatedTaxLiability()(fakeRequestWithActiveSession)
        lazy val document = result.toHtmlDocument

        "return Status OK (200)" in {
          mockLastCalculationSuccess()
          mockBusinessDetailsEmpty()
          status(result) shouldBe Status.OK
        }

        "return HTML" in {
          mockLastCalculationSuccess()
          mockBusinessDetailsEmpty()
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }

        "render the EstimatedTaxLiability page" in {
          mockLastCalculationSuccess()
          mockBusinessDetailsEmpty()
          document.title() shouldBe messages.title
        }
      }

      "receives no businesses from the BusinessDetails Service" +
        "and  an error from the Last Calculation Service" should {

        lazy val result = TestEstimatedLiabilityController.getEstimatedTaxLiability()(fakeRequestWithActiveSession)

        "return Internal Server Error (500)" in {
          mockBusinessDetailsEmpty()
          mockLastCalculationError()
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }
      }


      "receives an error from the BusinessDetails Service" should {

        lazy val result = TestEstimatedLiabilityController.getEstimatedTaxLiability()(fakeRequestWithActiveSession)

        "return Internal Server Error (500)" in {
          mockBusinessDetailsError()
          status(result) shouldBe Status.INTERNAL_SERVER_ERROR
        }

        "return HTML" in {
          contentType(result) shouldBe Some("text/html")
          charset(result) shouldBe Some("utf-8")
        }
      }


    }

    "Called with an Unauthenticated User" should {

      object TestEstimatedLiabilityController extends EstimatedTaxLiabilityController()(
        fakeApplication.injector.instanceOf[FrontendAppConfig],
        fakeApplication.injector.instanceOf[MessagesApi],
        MockUnauthorised,
        mockFinancialDataService,
        mockBusinessDetailsService
      )

      "return redirect SEE_OTHER (303)" in {
        val result = TestEstimatedLiabilityController.getEstimatedTaxLiability()(fakeRequestNoSession)
        status(result) shouldBe Status.SEE_OTHER
      }
    }
  }
}

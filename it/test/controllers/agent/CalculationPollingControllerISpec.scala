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

package controllers.agent

import controllers.agent.sessionUtils.SessionKeys._
import forms.utils.SessionKeys
import helpers.ComponentSpecBase
import helpers.servicemocks._
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import models.liabilitycalculation.LiabilityCalculationError
import play.api.http.HeaderNames
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.{address, b2TradingStart}
import testConstants.NewCalcBreakdownItTestConstants._
import testConstants.PropertyDetailsIntegrationTestConstants.{propertyIncomeType, propertyTradingStartDate}

import java.time.LocalDate

class CalculationPollingControllerISpec extends ComponentSpecBase {
  val (taxYear, month, dayOfMonth) = (2018, 5, 6)
  val (hour, minute) = (12, 0)
  lazy val fixedDate : LocalDate = LocalDate.of(2023, 12, 15)

  val urlFinalCalcFalse: String = s"http://localhost:$port" + controllers.routes.CalculationPollingController
    .calculationPollerAgent(taxYear, isFinalCalc = false).url

  val urlFinalCalcTrue: String = s"http://localhost:$port" + controllers.routes.CalculationPollingController
    .calculationPollerAgent(taxYear, isFinalCalc = true).url

  unauthorisedTest(s"/calculation/$testYear/submitted")

  def calculationStub(): Unit = {
    IncomeTaxCalculationStub.stubGetCalculationResponseByCalcId(testNino, testTaxYear, "idOne")(
      status = OK,
      body = liabilityCalculationModelSuccessful
    )
  }

  def failedCalculationStub(): Unit = {
    IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino,"idOne", taxYear)(
      status = NOT_FOUND,
      body = LiabilityCalculationError(NOT_FOUND, "not found")
    )
  }

  lazy val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
      Some("Test Trading Name"),
      Some(LocalDate.of(2018,1,1)),
      Some(b2TradingStart),
      None,
      address = Some(address),
      cashOrAccruals = false
    )
    ),
    properties = List(
      PropertyDetailsModel(
        "testId2",
        Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
        Some(LocalDate.of(2018,1,1)),
        propertyIncomeType,
        propertyTradingStartDate,
        None,
        cashOrAccruals = false
      )
    )
  )

  val clientDetailsWithConfirmation: Map[String, String] = Map(
    clientFirstName -> "Test",
    clientLastName -> "User",
    clientUTR -> "1234567890",
    clientNino -> testNino,
    clientMTDID -> testMtditid,
    confirmedClient -> "true",
    SessionKeys.calculationId -> "idOne"
  )

  val clientDetailsWithConfirmationNoCalcId: Map[String, String] = Map(
    clientFirstName -> "Test",
    clientLastName -> "User",
    clientUTR -> "1234567890",
    clientNino -> testNino,
    clientMTDID -> testMtditid,
    confirmedClient -> "true"
  )

  lazy val playSessionCookie: String = bakeSessionCookie(clientDetailsWithConfirmation)
  lazy val playSessionCookieNoCalcId: String = bakeSessionCookie(clientDetailsWithConfirmationNoCalcId)

  s"calling GET ${controllers.routes.CalculationPollingController.calculationPollerAgent(testYearInt, isFinalCalc = false).url}" when {

    "the user is authorised with an active enrolment" should {

      "redirect the user to the tax year summary page" which {
        lazy val result = {
          stubAuthorisedAgentUser(authorised = true)
          calculationStub()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            status = OK,
            response = incomeSourceDetailsSuccess
          )

          ws.url(urlFinalCalcFalse)
            .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie)
            .withFollowRedirects(false)
            .get()
        }.futureValue

        "has the status of SEE_OTHER (303)" in {
          result.status shouldBe SEE_OTHER
        }

        s"redirect to '${controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(testTaxYear).url}''" in {
          result.header("Location").head shouldBe controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(testTaxYear).url
        }

      }

      "redirect to internal server error" when {

        "a non 200 status is returned from Calc service" which {
          lazy val result = {
            stubAuthorisedAgentUser(authorised = true)
            failedCalculationStub()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetailsSuccess
            )

            ws.url(urlFinalCalcFalse)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookieNoCalcId)
              .withFollowRedirects(false)
              .get()
          }.futureValue

          "has a result of 500" in {
            result.status shouldBe INTERNAL_SERVER_ERROR
          }
        }

        "the calc ID is not in session" which {
          lazy val result = {
            stubAuthorisedAgentUser(authorised = true)
            calculationStub()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetailsSuccess
            )

            ws.url(urlFinalCalcFalse)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookieNoCalcId)
              .withFollowRedirects(false)
              .get()
          }.futureValue

          "has a status of 500" in {
            result.status shouldBe INTERNAL_SERVER_ERROR
          }

        }

        "calculation service returns non-retryable response back" in {
          lazy val res = {
            stubAuthorisedAgentUser(authorised = true)
            IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino, "idTwo")(INTERNAL_SERVER_ERROR,
              LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error"))
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetailsSuccess
            )

            ws.url(urlFinalCalcFalse)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie)
              .withFollowRedirects(false)
              .get()
          }.futureValue

          res.status shouldBe INTERNAL_SERVER_ERROR
        }

      }

    }

  }

  s"calling GET ${controllers.routes.CalculationPollingController.calculationPollerAgent(testYearInt, isFinalCalc = true).url}" when {

    "the user is authorised with an active enrolment" should {

      "redirect the user to the tax year summary page" which {
        lazy val result = {
          stubAuthorisedAgentUser(authorised = true)
          calculationStub()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
            status = OK,
            response = incomeSourceDetailsSuccess
          )

          ws.url(urlFinalCalcTrue)
            .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie)
            .withFollowRedirects(false)
            .get()
        }.futureValue

        "has the status of SEE_OTHER (303)" in {
          result.status shouldBe SEE_OTHER
        }

        s"redirect to '${controllers.routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(testTaxYear).url}''" in {
          result.header("Location").head shouldBe controllers.routes.FinalTaxCalculationController.showAgent(testTaxYear).url
        }

      }

      "redirect to internal server error" when {

        "a non 200 status is returned from Calc service" which {
          lazy val result = {
            stubAuthorisedAgentUser(authorised = true)
            failedCalculationStub()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetailsSuccess
            )

            ws.url(urlFinalCalcTrue)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookieNoCalcId)
              .withFollowRedirects(false)
              .get()
          }.futureValue

          "has a result of 500" in {
            result.status shouldBe INTERNAL_SERVER_ERROR
          }
        }

        "the calc ID is not in session" which {
          lazy val result = {
            stubAuthorisedAgentUser(authorised = true)
            calculationStub()
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetailsSuccess
            )


            ws.url(urlFinalCalcTrue)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookieNoCalcId)
              .withFollowRedirects(false)
              .get()
          }.futureValue

          "has a status of 500" in {
            result.status shouldBe INTERNAL_SERVER_ERROR
          }

        }

        "calculation service returns non-retryable response back" in {
          lazy val res = {
            stubAuthorisedAgentUser(authorised = true)
            IncomeTaxCalculationStub.stubGetCalculationErrorResponse(testNino, "idTwo")(INTERNAL_SERVER_ERROR,
              LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error"))
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetailsSuccess
            )


            ws.url(urlFinalCalcTrue)
              .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie)
              .withFollowRedirects(false)
              .get()
          }.futureValue

          res.status shouldBe INTERNAL_SERVER_ERROR
        }

      }

    }

  }


}

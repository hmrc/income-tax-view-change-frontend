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

import testConstants.BaseIntegrationTestConstants._
import testConstants.CalcDataIntegrationTestConstants._
import controllers.agent.utils.SessionKeys._
import forms.utils.SessionKeys
import helpers.ComponentSpecBase
import helpers.servicemocks._
import models.calculation.{CalculationItem, ListCalculationItems}
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, PropertyDetailsModel}
import play.api.http.HeaderNames
import play.api.http.Status._
import repositories.MongoLockRepositoryImpl

import java.time.{LocalDate, LocalDateTime}
import scala.concurrent.ExecutionContext

class CalculationPollingControllerISpec extends ComponentSpecBase {

  val mongoDbConnection: MongoLockRepositoryImpl = app.injector.instanceOf[MongoLockRepositoryImpl]
  implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]

  val (taxYear, month, dayOfMonth) = (2018, 5, 6)
  val (hour, minute) = (12, 0)
  
  val urlFinalCalcFalse: String = s"http://localhost:$port" + controllers.agent.routes.CalculationPollingController
    .calculationPoller(taxYear, isFinalCalc = false).url

  val urlFinalCalcTrue: String = s"http://localhost:$port" + controllers.agent.routes.CalculationPollingController
    .calculationPoller(taxYear, isFinalCalc = true).url

  unauthorisedTest(s"/calculation/$testYear/submitted")

  def calculationStub(taxYearString: String = "2017-18"): Unit = {
    IndividualCalculationStub.stubGetCalculationList(testNino, taxYearString)(
      status = OK,
      body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.of(LocalDate.now().getYear, month, dayOfMonth, hour, minute))))
    )
    IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
      status = OK,
      body = estimatedCalculationFullJson
    )
  }

  def failedCalculationStub(taxYearString: String = "2017-18"): Unit = {
    IndividualCalculationStub.stubGetCalculationList(testNino, taxYearString)(
      status = OK,
      body = ListCalculationItems(Seq(CalculationItem("idOne", LocalDateTime.of(LocalDate.now().getYear, month, dayOfMonth, hour, minute))))
    )
    IndividualCalculationStub.stubGetCalculation(testNino, "idOne")(
      status = NOT_FOUND,
      body = estimatedCalculationFullJson
    )
  }

  lazy val incomeSourceDetailsSuccess: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
      Some("Test Trading Name"), None, None, None, None, None, None, None,
      Some(LocalDate.of(2018, 1, 1))
    )),
    property = Some(
      PropertyDetailsModel(
        "testId2",
        AccountingPeriodModel(LocalDate.now, LocalDate.now.plusYears(1)),
        None, None, None, None,
        Some(LocalDate.of(2018, 1, 1))
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
  
  s"calling GET ${controllers.agent.routes.CalculationPollingController.calculationPoller(testYearInt, isFinalCalc = false).url}" when {
    
    "the user is authorised with an active enrolment" should {
      
      "redirect the user to the tax year overview page" which {
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
        
        s"redirect to '${controllers.agent.routes.TaxYearOverviewController.show(testTaxYear).url}''" in {
          result.header("Location").head shouldBe controllers.agent.routes.TaxYearOverviewController.show(testTaxYear).url
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
            IndividualCalculationStub.stubGetCalculationError(testNino, "idTwo")
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

  s"calling GET ${controllers.agent.routes.CalculationPollingController.calculationPoller(testYearInt, isFinalCalc = true).url}" when {

    "the user is authorised with an active enrolment" should {

      "redirect the user to the tax year overview page" which {
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

        s"redirect to '${controllers.agent.routes.TaxYearOverviewController.show(testTaxYear).url}''" in {
          result.header("Location").head shouldBe controllers.agent.routes.FinalTaxCalculationController.show(testTaxYear).url
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
            IndividualCalculationStub.stubGetCalculationError(testNino, "idTwo")
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

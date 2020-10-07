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

package services

import assets.BaseTestConstants._
import assets.FinancialTransactionsTestConstants._
import assets.IncomeSourceDetailsTestConstants._
import auth.MtdItUser
import config.featureswitch.{API5, FeatureSwitching}
import implicits.ImplicitDateFormatter
import javax.inject.Inject
import mocks.connectors.MockFinancialTransactionsConnector
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel}
import play.api.data.Forms.list
import play.api.http.Status
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import testUtils.TestSupport
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.ExecutionContext


class FinancialTransactionsServiceSpec @Inject() (val languageUtils: LanguageUtils)
  extends TestSupport with MockFinancialTransactionsConnector with ImplicitDateFormatter with FeatureSwitching{

  object TestFinancialTransactionsService extends FinancialTransactionsService(mockFinancialTransactionsConnector)

  "The FinancialTransactionsService.getFinancialTransactions method" when {

    "a successful financial transaction response is returned from the connector" should {

      "return a valid FinancialTransactions model" in {
        setupFinancialTransactionsResponse(testNino, testTaxYear)(financialTransactionsModel())
        await(TestFinancialTransactionsService.getFinancialTransactions(testNino, testTaxYear)) shouldBe financialTransactionsModel()
      }

    }

    "a error model is returned from the connector" should {

      "return a FinancialTransactionsError model" in {
        setupFinancialTransactionsResponse(testNino, testTaxYear)(financialTransactionsErrorModel)
        await(TestFinancialTransactionsService.getFinancialTransactions(testNino, testTaxYear)) shouldBe financialTransactionsErrorModel
      }

    }

  }

    "The FinancialTransactionsService.getAllFinancialTransactions method with API5 enabled" when {

      " a successful financial transaction response is returned from the connector" should {
        val hc = new HeaderCarrier()

        def testFinancialTransactionWithYear(taxYear: Int) = (taxYear, financialTransactionsModel(s"$taxYear-04-05"))

        "return a valid set of transactions for a single tax year" in {
          enable(API5)
          val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
            testMtditid,
            testNino,
            Some(testRetrievedUserName),
            propertyIncomeOnly
          )(FakeRequest())

          setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsModel())
          val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

          result shouldBe List(testFinancialTransactionWithYear(2019))
        }

        "return a valid set of transactions for a multiple tax years" in {
          enable(API5)
          val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
            testMtditid,
            testNino,
            Some(testRetrievedUserName),
            businessIncome2018and2019
          )(FakeRequest())
          setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsModel())
          setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2020)(financialTransactionsModel("2019-04-05"))

          val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc, ec: ExecutionContext))

          result shouldBe List(testFinancialTransactionWithYear(2019), testFinancialTransactionWithYear(2019))
        }

        "return no financial transactions" in {
          enable(API5)
          val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
            testMtditid,
            testNino,
            Some(testRetrievedUserName),
            noIncomeDetails
          )(FakeRequest())

          setupFinancialTransactionsResponse(testNino, testTaxYear)(FinancialTransactionsModel(
            None,
            None,
            None,
            "2017-07-06T12:34:56.789Z".toZonedDateTime,
            None))

          val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc, ec: ExecutionContext))

          result shouldBe List.empty
        }

      }


      "a un-successful financial transaction response is returned from the connector" should {
        val hc = new HeaderCarrier()


        def testFinancialTransactionWithYear(taxYear: Int) = (taxYear, financialTransactionsModel(s"$taxYear-04-05"))

        "return a financial transaction with a valid transaction retrieved and a bad Request response" in {
          enable(API5)
          val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
            testMtditid,
            testNino,
            Some(testRetrievedUserName),
            businessIncome2018and2019
          )(FakeRequest())

          setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsModel())
          setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2020)(FinancialTransactionsErrorModel(
            Status.BAD_REQUEST,
            "Bad Request Recieved"
          ))

          val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

          result shouldBe List(testFinancialTransactionWithYear(2019))
        }

        "pass the errored tax year with a valid transaction retrieved and a internal server error response" in {
          enable(API5)
          val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
            testMtditid,
            testNino,
            Some(testRetrievedUserName),
            businessIncome2018and2019
          )(FakeRequest())

          setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsModel())
          setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2020)(financialTransactionsErrorModel)

          val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

          result shouldBe List(testFinancialTransactionWithYear(2019), (2020, financialTransactionsErrorModel))
        }

        "return an internal error with a financial transaction with a server error response" in {
          enable(API5)
          setupFinancialTransactionsResponse(testNino, testTaxYear)(financialTransactionsModel())
          val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
            testMtditid,
            testNino,
            Some(testRetrievedUserName),
            propertyIncomeOnly
          )(FakeRequest())

          setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsErrorModel)

          val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

          result shouldBe List((2018, financialTransactionsErrorModel))
        }
      }
    }


  "The FinancialTransactionsService.getAllFinancialTransactions method with API5 disabled" when {

    " a successful financial transaction response is returned from the connector" should {
      val hc = new HeaderCarrier()

      def testFinancialTransactionWithYear(taxYear: Int) = (taxYear, financialTransactionsModel(s"$taxYear-04-05"))


      "return a valid set of transactions for a single tax year" in {
        disable(API5)
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testRetrievedUserName),
          propertyIncomeOnly
        )(FakeRequest())

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsModel())
        val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

        result shouldBe List(testFinancialTransactionWithYear(2019))
      }

      "return a valid set of transactions for a multiple tax years" in {
        disable(API5)
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testRetrievedUserName),
          businessIncome2018and2019
        )(FakeRequest())
        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsModel())
        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2020)(financialTransactionsModel("2019-04-05"))

        val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc, ec: ExecutionContext))

        result shouldBe List(testFinancialTransactionWithYear(2019), testFinancialTransactionWithYear(2019))
      }

      "return no financial transactions" in {
        disable(API5)
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testRetrievedUserName),
          noIncomeDetails
        )(FakeRequest())

        setupFinancialTransactionsResponse(testNino, testTaxYear)(FinancialTransactionsModel(
          None,
          None,
          None,
          "2017-07-06T12:34:56.789Z".toZonedDateTime,
          None))

        val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc, ec: ExecutionContext))

        result shouldBe List.empty
      }

    }


    "a un-successful financial transaction response is returned from the connector" should {
      val hc = new HeaderCarrier()


      def testFinancialTransactionWithYear(taxYear: Int) = (taxYear, financialTransactionsModel(s"$taxYear-04-05"))

      "return a financial transaction with a valid transaction retrieved and a bad Request response" in {
        disable(API5)
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testRetrievedUserName),
          businessIncome2018and2019
        )(FakeRequest())

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsModel())
        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2020)(FinancialTransactionsErrorModel(
          Status.BAD_REQUEST,
          "Bad Request Recieved"
        ))

        val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

        result shouldBe List(testFinancialTransactionWithYear(2019))
      }

      "pass the errored tax year with a valid transaction retrieved and a internal server error response" in {
        disable(API5)
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testRetrievedUserName),
          businessIncome2018and2019
        )(FakeRequest())

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsModel())
        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2020)(financialTransactionsErrorModel)

        val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

        result shouldBe List(testFinancialTransactionWithYear(2019), (2020, financialTransactionsErrorModel))
      }

      "return an internal error with a financial transaction with a server error response" in {
        disable(API5)
        setupFinancialTransactionsResponse(testNino, testTaxYear)(financialTransactionsModel())
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testRetrievedUserName),
          propertyIncomeOnly
        )(FakeRequest())

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsErrorModel)

        val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

        result shouldBe List((2018, financialTransactionsErrorModel))
      }
    }
  }

}

/*
 * Copyright 2019 HM Revenue & Customs
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
import mocks.connectors.MockFinancialTransactionsConnector
import models.financialTransactions.{FinancialTransactionsErrorModel, FinancialTransactionsModel}
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.http.Status
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import testUtils.TestSupport
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext


class FinancialTransactionsServiceSpec extends TestSupport with MockFinancialTransactionsConnector{

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

  "The FinancialTransactionsService.getAllFinancialTransactions method" when {


    "a successful financial transaction response is returned from the connector" should {
      val hc = new HeaderCarrier()



      def testFinancialTransactionWithYear(taxYear:Int) = (taxYear, financialTransactionsModel(s"$taxYear-04-05"))


      "return a valid set of transactions for a single tax year" in {
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testUserDetails),
          propertyIncomeOnly
        )(FakeRequest())

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2018)(financialTransactionsModel())
        val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

        result shouldBe List(testFinancialTransactionWithYear(2018))
      }

      "return a valid set of transactions for a multiple tax years" in {
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testUserDetails),
          businessIncome2018and2019
        )(FakeRequest())
        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2018)(financialTransactionsModel())
        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsModel("2019-04-05"))

        val result = await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc, ec: ExecutionContext))

         result shouldBe List(testFinancialTransactionWithYear(2018) , testFinancialTransactionWithYear(2019))
      }

      "return no financial transactions" in {
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testUserDetails),
          noIncomeDetails
        )(FakeRequest())

        setupFinancialTransactionsResponse(testNino, testTaxYear)(FinancialTransactionsModel(
          None,
          None,
          None,
          "2017-07-06T12:34:56.789Z".toZonedDateTime,
          None))

        val result =  await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc, ec: ExecutionContext))

        result shouldBe  List.empty
      }

    }


    "a un-successful financial transaction response is returned from the connector" should {
      val hc = new HeaderCarrier()



      def testFinancialTransactionWithYear(taxYear:Int) = (taxYear, financialTransactionsModel(s"$taxYear-04-05"))

      "return a financial transaction with a valid transaction retrieved and a bad Request response" in {
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testUserDetails),
          businessIncome2018and2019
        )(FakeRequest())

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2018)(financialTransactionsModel())
        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(FinancialTransactionsErrorModel(
          Status.BAD_REQUEST,
          "Bad Request Recieved"
        ))

        val result =  await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

        result shouldBe List(testFinancialTransactionWithYear(2018))
      }

      "pass the errored tax year with a valid transaction retrieved and a internal server error response" in {

        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
            testMtditid,
            testNino,
            Some(testUserDetails),
            businessIncome2018and2019
          )(FakeRequest())

          setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2018)(financialTransactionsModel())
          setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsErrorModel)

          val result =  await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

          result shouldBe List(testFinancialTransactionWithYear(2018), (2019, financialTransactionsErrorModel))
      }

      "return an internal error with a financial transaction with a server error response" in {
        setupFinancialTransactionsResponse(testNino, testTaxYear)(financialTransactionsModel())
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testUserDetails),
          propertyIncomeOnly
        )(FakeRequest())

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2018)(financialTransactionsErrorModel)

        val result =  await(TestFinancialTransactionsService.getAllFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

        result shouldBe List((2018, financialTransactionsErrorModel))
      }
    }

  }

  "The FinancialTransactionsService.getAllUnpaidFinancialTransactions method" when {
    "returning all the unpaid/errored transactions" should {
      val hc = new HeaderCarrier()

      "with a single unpaid transaction" in {
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testUserDetails),
          propertyIncomeOnly
        )(FakeRequest())

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2018)(financialTransactionsModel())

        val result =  await(TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

        result shouldBe List(financialTransactionsModel())
      }

      "with a single unpaid transaction and paid transaction" in {
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testUserDetails),
          propertyIncomeOnly
        )(FakeRequest())

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2018)(FinancialTransactionsModel(
          None,
          None,
          None,
          "2017-07-06T12:34:56.789Z".toZonedDateTime,
          Some(Seq(paidTransactionModel("2018-04-05")))
        ))

        val result =  await(TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

        result shouldBe List()
      }

      "with a single error response transaction" in {
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testUserDetails),
          propertyIncomeOnly
        )(FakeRequest())

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2018)(financialTransactionsErrorModel)

        val result =  await(TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

        result shouldBe List(financialTransactionsErrorModel)
      }

      "with a error response transaction, unpaid and paid transaction" in {
        val testMtdItUser: MtdItUser[AnyContent] = MtdItUser(
          testMtditid,
          testNino,
          Some(testUserDetails),
          businessIncome2018and2019AndProp
        )(FakeRequest())

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2017)(FinancialTransactionsModel(
          None,
          None,
          None,
          "2017-07-06T12:34:56.789Z".toZonedDateTime,
          Some(Seq(paidTransactionModel("2018-04-05")))
        ))

        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2018)(financialTransactionsModel())
        setupFinancialTransactionsResponse(testMtdItUser.mtditid, 2019)(financialTransactionsErrorModel)


        val result =  await(TestFinancialTransactionsService.getAllUnpaidFinancialTransactions(testMtdItUser, hc: HeaderCarrier, ec: ExecutionContext))

        result shouldBe List(financialTransactionsModel(),financialTransactionsErrorModel)
      }

    }
  }
}

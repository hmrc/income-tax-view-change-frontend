/*
 * Copyright 2024 HM Revenue & Customs
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

package connectors

import _root_.helpers.servicemocks.AuditStub
import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import com.github.tomakehurst.wiremock.client.WireMock
import models.createIncomeSource._
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.test.Injecting

class CreateIncomeSourceConnectorISpec extends AnyWordSpec with ComponentSpecBase with Injecting {

  lazy val connector: CreateIncomeSourceConnector = app.injector.instanceOf[CreateIncomeSourceConnector]

  val mtdId = "ID"

  override def beforeEach(): Unit = {
    WireMock.reset()
    AuditStub.stubAuditing()
  }

  val businessRequestBody: JsValue = Json.parse(
    """
      |{
      |  "businessDetails": [
      |    {
      |      "accountingPeriodStartDate": "01-02-2023",
      |      "accountingPeriodEndDate": "",
      |      "tradingName": "",
      |      "addressDetails": {
      |        "addressLine1": "tests test",
      |        "addressLine2": "",
      |        "countryCode": "UK",
      |        "postalCode": ""
      |      },
      |      "tradingStartDate": "",
      |      "cashOrAccrualsFlag": "CASH",
      |      "cessationDate": ""
      |    }
      |  ]
      |}
      |""".stripMargin
  )

  val foreignPropertyRequestBody: JsValue = Json.parse(
    """
      |{
      |   "foreignPropertyDetails" : {
      |       "tradingStartDate" : "01-02-2023",
      |       "cashOrAccrualsFlag" : "CASH",
      |       "startDate" : "01-02-2023"
      |   }
      |}
      |""".stripMargin
  )

  val ukPropertyRequestBody: JsValue = Json.parse(
    """
      |{
      |  "ukPropertyDetails": {
      |    "tradingStartDate": "01-02-2023",
      |    "cashOrAccrualsFlag": "CASH",
      |    "startDate": "01-02-2023"
      |  }
      |}
      |""".stripMargin
  )

  val createBusinessDetailsRequest: CreateBusinessIncomeSourceRequest = CreateBusinessIncomeSourceRequest(
    List(
      BusinessDetails(accountingPeriodStartDate = "01-02-2023",
        accountingPeriodEndDate = "",
        tradingName = "",
        addressDetails = AddressDetails(
          addressLine1 = "tests test",
          addressLine2 = Some(""),
          addressLine3 = None,
          addressLine4 = None,
          countryCode = Some("UK"),
          postalCode = Some("")
        ),
        typeOfBusiness = None,
        tradingStartDate = "",
        cashOrAccrualsFlag = Some("CASH"),
        cessationDate = Some(""),
        cessationReason = None
      )
    )
  )

  val createForeignPropertyRequest: CreateForeignPropertyIncomeSourceRequest = CreateForeignPropertyIncomeSourceRequest(
    PropertyDetails(
      "01-02-2023",
      Some("CASH"),
      "01-02-2023"
    )
  )

  val createUKPropertyRequest: CreateUKPropertyIncomeSourceRequest = CreateUKPropertyIncomeSourceRequest(
    PropertyDetails(
      "01-02-2023",
      Some("CASH"),
      "01-02-2023"
    )
  )

  val successfulApiResponse: String =
    """
      |[
      |   {
      |       "incomeSourceId": "ID"
      |   }
      |]
      |""".stripMargin

  "CreateIncomeSourceConnector" when {
    ".createBusiness()" when {
      "sending a request" should {
        "return a successful response" in {
          WiremockHelper.stubPostWithRequest(s"/income-tax-view-change/create-income-source/business/$mtdId", businessRequestBody, OK, successfulApiResponse)

          val result = connector.createBusiness(mtdId, createBusinessDetailsRequest).futureValue

          result shouldBe Right(List(CreateIncomeSourceResponse("ID")))
          WiremockHelper.verifyPost(s"/income-tax-view-change/create-income-source/business/$mtdId")
        }
        "return an error when the request returns a 200 but the json is invalid" in {
          WiremockHelper.stubPostWithRequest(s"/income-tax-view-change/create-income-source/business/$mtdId", businessRequestBody, OK, "{}")

          val result = connector.createBusiness(mtdId, createBusinessDetailsRequest).futureValue

          result shouldBe  Left(CreateIncomeSourceErrorResponse(200, "Not valid json: {}"))
          WiremockHelper.verifyPost(s"/income-tax-view-change/create-income-source/business/$mtdId")
        }

        "return an error when the request doesn't return a 200" in {
          WiremockHelper.stubPostWithRequest(s"/income-tax-view-change/create-income-source/business/$mtdId", businessRequestBody, INTERNAL_SERVER_ERROR, "{}")

          val result = connector.createBusiness(mtdId, createBusinessDetailsRequest).futureValue

          result shouldBe Left(CreateIncomeSourceErrorResponse(500, "Error creating incomeSource: {}"))
          WiremockHelper.verifyPost(s"/income-tax-view-change/create-income-source/business/$mtdId")
        }
      }
    }
    ".createForeignProperty()" when {
      "sending a request" should {
        "return a successful response" in {
          WiremockHelper.stubPostWithRequest(s"/income-tax-view-change/create-income-source/business/$mtdId", foreignPropertyRequestBody, OK, successfulApiResponse)

          val result = connector.createForeignProperty(mtdId, createForeignPropertyRequest).futureValue

          result shouldBe Right(List(CreateIncomeSourceResponse(mtdId)))
          WiremockHelper.verifyPost(s"/income-tax-view-change/create-income-source/business/$mtdId")
        }
        "return an error when the request returns a 200 but the json is invalid" in {
          WiremockHelper.stubPostWithRequest(s"/income-tax-view-change/create-income-source/business/$mtdId", foreignPropertyRequestBody, OK, "{}")

          val result = connector.createForeignProperty(mtdId, createForeignPropertyRequest).futureValue

          result shouldBe  Left(CreateIncomeSourceErrorResponse(200, "Not valid json: {}"))
          WiremockHelper.verifyPost(s"/income-tax-view-change/create-income-source/business/$mtdId")
        }

        "return an error when the request doesn't return a 200" in {
          WiremockHelper.stubPostWithRequest(s"/income-tax-view-change/create-income-source/business/$mtdId", foreignPropertyRequestBody, INTERNAL_SERVER_ERROR, "{}")

          val result = connector.createForeignProperty(mtdId, createForeignPropertyRequest).futureValue

          result shouldBe Left(CreateIncomeSourceErrorResponse(500, "Error creating incomeSource: {}"))
          WiremockHelper.verifyPost(s"/income-tax-view-change/create-income-source/business/$mtdId")
        }
      }
    }
    ".createUKProperty()" when {
      "sending a request" should {
        "return a successful response" in {
          WiremockHelper.stubPostWithRequest(s"/income-tax-view-change/create-income-source/business/$mtdId", ukPropertyRequestBody, OK, successfulApiResponse)

          val result = connector.createUKProperty(mtdId, createUKPropertyRequest).futureValue

          result shouldBe Right(List(CreateIncomeSourceResponse(mtdId)))
          WiremockHelper.verifyPost(s"/income-tax-view-change/create-income-source/business/$mtdId")
        }
        "return an error when the request returns a 200 but the json is invalid" in {
          WiremockHelper.stubPostWithRequest(s"/income-tax-view-change/create-income-source/business/$mtdId", ukPropertyRequestBody, OK, "{}")

          val result = connector.createUKProperty(mtdId, createUKPropertyRequest).futureValue

          result shouldBe  Left(CreateIncomeSourceErrorResponse(200, "Not valid json: {}"))
          WiremockHelper.verifyPost(s"/income-tax-view-change/create-income-source/business/$mtdId")
        }

        "return an error when the request doesn't return a 200" in {
          WiremockHelper.stubPostWithRequest(s"/income-tax-view-change/create-income-source/business/$mtdId", ukPropertyRequestBody, INTERNAL_SERVER_ERROR, "{}")

          val result = connector.createUKProperty(mtdId, createUKPropertyRequest).futureValue

          result shouldBe Left(CreateIncomeSourceErrorResponse(500, "Error creating incomeSource: {}"))
          WiremockHelper.verifyPost(s"/income-tax-view-change/create-income-source/business/$mtdId")
        }
      }
    }
  }

}


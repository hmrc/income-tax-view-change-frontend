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
  val createIncomeSourceUrl = "/income-tax-view-change/create-income-source/business"

  override def beforeEach(): Unit = {
    WireMock.reset()
    AuditStub.stubAuditing()
  }

  val businessRequestBody: JsValue = Json.parse(
    """{
      |  "mtdbsa" : "XAIT00001234567",
      |  "businessDetails" : [ {
      |    "accountingPeriodStartDate" : "01-02-2023",
      |    "accountingPeriodEndDate" : "",
      |    "tradingName" : "",
      |    "address" : {
      |      "addressLine1" : "tests test",
      |      "addressLine2" : "",
      |      "countryCode" : "UK",
      |      "postalCode" : ""
      |    },
      |    "tradingStartDate" : "",
      |    "cashOrAccrualsFlag" : "CASH",
      |    "cessationDate" : ""
      |  } ]
      |}
      |""".stripMargin
  )

  val foreignPropertyRequestBody: JsValue = Json.parse(
    """{
      |  "mtdbsa" : "XAIT00001234567",
      |  "foreignPropertyDetails" : {
      |    "tradingStartDate" : "01-02-2023",
      |    "cashOrAccrualsFlag" : "CASH",
      |    "startDate" : "01-02-2023"
      |  }
      |}
      |""".stripMargin
  )

  val ukPropertyRequestBody: JsValue = Json.parse(
    """{
      |  "mtdbsa" : "XAIT00001234567",
      |  "ukPropertyDetails" : {
      |    "tradingStartDate" : "01-02-2023",
      |    "cashOrAccrualsFlag" : "CASH",
      |    "startDate" : "01-02-2023"
      |  }
      |}
      |""".stripMargin
  )

  val createBusinessDetailsRequest: CreateBusinessIncomeSourceRequest = CreateBusinessIncomeSourceRequest(
    mtdbsa = "XAIT00001234567",
    businessDetails = List(
      BusinessDetails(accountingPeriodStartDate = "01-02-2023",
        accountingPeriodEndDate = "",
        tradingName = "",
        address = AddressDetails(
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
    mtdbsa = "XAIT00001234567",
    foreignPropertyDetails = PropertyDetails(
      "01-02-2023",
      Some("CASH"),
      "01-02-2023"
    )
  )

  val createUKPropertyRequest: CreateUKPropertyIncomeSourceRequest = CreateUKPropertyIncomeSourceRequest(
    mtdbsa = "XAIT00001234567",
    ukPropertyDetails = PropertyDetails(
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
          WiremockHelper.stubPostWithRequest(createIncomeSourceUrl, businessRequestBody, OK, successfulApiResponse)

          val result = connector.createBusiness(createBusinessDetailsRequest).futureValue

          result shouldBe Right(List(CreateIncomeSourceResponse("ID")))
          WiremockHelper.verifyPost(createIncomeSourceUrl)
        }
        "return an error when the request returns a 200 but the json is invalid" in {
          WiremockHelper.stubPostWithRequest(createIncomeSourceUrl, businessRequestBody, OK, "{}")

          val result = connector.createBusiness(createBusinessDetailsRequest).futureValue

          result shouldBe  Left(CreateIncomeSourceErrorResponse(200, "Not valid json: {}"))
          WiremockHelper.verifyPost(createIncomeSourceUrl)
        }

        "return an error when the request doesn't return a 200" in {
          WiremockHelper.stubPostWithRequest(createIncomeSourceUrl, businessRequestBody, INTERNAL_SERVER_ERROR, "{}")

          val result = connector.createBusiness(createBusinessDetailsRequest).futureValue

          result shouldBe Left(CreateIncomeSourceErrorResponse(500, "Error creating incomeSource: {}"))
          WiremockHelper.verifyPost(createIncomeSourceUrl)
        }
      }
    }
    ".createForeignProperty()" when {
      "sending a request" should {
        "return a successful response" in {
          WiremockHelper.stubPostWithRequest(createIncomeSourceUrl, foreignPropertyRequestBody, OK, successfulApiResponse)
          val result = connector.createForeignProperty(createForeignPropertyRequest).futureValue

          result shouldBe Right(List(CreateIncomeSourceResponse(mtdId)))
          WiremockHelper.verifyPost(createIncomeSourceUrl)
        }
        "return an error when the request returns a 200 but the json is invalid" in {
          WiremockHelper.stubPostWithRequest(createIncomeSourceUrl, foreignPropertyRequestBody, OK, "{}")

          val result = connector.createForeignProperty(createForeignPropertyRequest).futureValue

          result shouldBe  Left(CreateIncomeSourceErrorResponse(200, "Not valid json: {}"))
          WiremockHelper.verifyPost(createIncomeSourceUrl)
        }

        "return an error when the request doesn't return a 200" in {
          WiremockHelper.stubPostWithRequest(createIncomeSourceUrl, foreignPropertyRequestBody, INTERNAL_SERVER_ERROR, "{}")

          val result = connector.createForeignProperty(createForeignPropertyRequest).futureValue

          result shouldBe Left(CreateIncomeSourceErrorResponse(500, "Error creating incomeSource: {}"))
          WiremockHelper.verifyPost(createIncomeSourceUrl)
        }
      }
    }
    ".createUKProperty()" when {
      "sending a request" should {
        "return a successful response" in {
          WiremockHelper.stubPostWithRequest(createIncomeSourceUrl, ukPropertyRequestBody, OK, successfulApiResponse)

          val result = connector.createUKProperty(createUKPropertyRequest).futureValue

          result shouldBe Right(List(CreateIncomeSourceResponse(mtdId)))
          WiremockHelper.verifyPost(createIncomeSourceUrl)
        }
        "return an error when the request returns a 200 but the json is invalid" in {
          WiremockHelper.stubPostWithRequest(createIncomeSourceUrl, ukPropertyRequestBody, OK, "{}")

          val result = connector.createUKProperty(createUKPropertyRequest).futureValue

          result shouldBe  Left(CreateIncomeSourceErrorResponse(200, "Not valid json: {}"))
          WiremockHelper.verifyPost(createIncomeSourceUrl)
        }

        "return an error when the request doesn't return a 200" in {
          WiremockHelper.stubPostWithRequest(createIncomeSourceUrl, ukPropertyRequestBody, INTERNAL_SERVER_ERROR, "{}")

          val result = connector.createUKProperty(createUKPropertyRequest).futureValue

          result shouldBe Left(CreateIncomeSourceErrorResponse(500, "Error creating incomeSource: {}"))
          WiremockHelper.verifyPost(createIncomeSourceUrl)
        }
      }
    }
  }
}

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

import _root_.helpers.{ComponentSpecBase, WiremockHelper}
import auth.authV2.models.AuthorisedAndEnrolledRequest
import enums.MTDIndividual
import models.core.{AccountingPeriodModel, AddressModel}
import models.incomeSourceDetails._
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.defaultAuthUserDetails

import java.time.LocalDate

class BusinessDetailsConnectorISpec extends AnyWordSpec with ComponentSpecBase {

  lazy val connector: BusinessDetailsConnector = app.injector.instanceOf[BusinessDetailsConnector]

  "BusinessDetailsConnector" when {

    ".getBusinessDetails()" when {

      "OK - 200" should {

        "return a successful response with the correct business details for the given nino" in {

          val nino = "AB123456A"
          val testMtditid = "XAITSA123456"

          val url = s"/income-tax-view-change/get-business-details/nino/$nino"

          val business =
            BusinessDetailsModel(
              incomeSourceId = "XA00001234",
              incomeSource = Some("Fruit Ltd"),
              accountingPeriod = Some(AccountingPeriodModel(LocalDate.of(2017, 6, 1), LocalDate.of(2018, 5, 30))),
              tradingName = Some("nextUpdates.business"),
              firstAccountingPeriodEndDate = Some(LocalDate.of(2018, 4, 5)),
              tradingStartDate = Some(LocalDate.of(2022, 1, 1)),
              contextualTaxYear = None,
              cessation = None,
              address = Some(AddressModel(Some("8 Test"), Some("New Court"), Some("New Town"), Some("New City"), Some("NE12 6CI"), Some("United Kingdom"))),
              latencyDetails = Some(LatencyDetails(LocalDate.of(2019, 1, 1), "2018", "A", "2019", "Q")),
              quarterTypeElection = None
            )

          val requestBody =
            Json.toJson(IncomeSourceDetailsModel(
              nino = nino,
              mtdbsa = testMtditid,
              yearOfMigration = Some("2017"),
              businesses = List(business),
              properties = Nil)
            ).toString()

          val expectedResponse: IncomeSourceDetailsResponse =
            IncomeSourceDetailsModel(
              nino = nino,
              mtdbsa = testMtditid,
              yearOfMigration = Some("2017"),
              businesses = List(business),
              properties = Nil
            )

          WiremockHelper.stubGet(url, OK, requestBody)

          val result: IncomeSourceDetailsResponse = connector.getBusinessDetails(nino).futureValue

          result shouldBe expectedResponse

          WiremockHelper.verifyGet(
            uri = s"/income-tax-view-change/get-business-details/nino/$nino"
          )
        }
      }

      "INTERNAL_SERVER_ERROR - 500" should {

        "return IncomeSourceDetailsError with some response body" in {

          val nino = "AB123456A"
          val url = s"/income-tax-view-change/get-business-details/nino/$nino"

          val responseBody =
            """{
              |"message": "fake value"
              |}""".stripMargin

          WiremockHelper.stubGet(url, INTERNAL_SERVER_ERROR, responseBody)

          val result: IncomeSourceDetailsResponse = connector.getBusinessDetails(nino).futureValue

          result shouldBe IncomeSourceDetailsError(status = INTERNAL_SERVER_ERROR, reason = responseBody)

          WiremockHelper.verifyGet(
            uri = s"/income-tax-view-change/get-business-details/nino/$nino"
          )
        }
      }
    }

    ".getIncomeSources()" when {

      "OK - 200" should {

        "return a successful response with the correct business details for the given nino" in {

          val nino = "AB123456A"
          val testMtditid = "XAITSA123456"

          val url = s"/income-tax-view-change/income-sources/$testMtditid"

          val business =
            BusinessDetailsModel(
              incomeSourceId = "XA00001234",
              incomeSource = Some("Fruit Ltd"),
              accountingPeriod = Some(AccountingPeriodModel(LocalDate.of(2017, 6, 1), LocalDate.of(2018, 5, 30))),
              tradingName = Some("nextUpdates.business"),
              firstAccountingPeriodEndDate = Some(LocalDate.of(2018, 4, 5)),
              tradingStartDate = Some(LocalDate.of(2022, 1, 1)),
              contextualTaxYear = None,
              cessation = None,
              address = Some(AddressModel(Some("8 Test"), Some("New Court"), Some("New Town"), Some("New City"), Some("NE12 6CI"), Some("United Kingdom"))),
              latencyDetails = Some(LatencyDetails(LocalDate.of(2019, 1, 1), "2018", "A", "2019", "Q")),
              quarterTypeElection = None
            )

          val requestBody =
            Json.toJson(IncomeSourceDetailsModel(
              nino = nino,
              mtdbsa = testMtditid,
              yearOfMigration = Some("2017"),
              businesses = List(business),
              properties = Nil)
            ).toString()

          val expectedResponse: IncomeSourceDetailsResponse =
            IncomeSourceDetailsModel(
              nino = nino,
              mtdbsa = testMtditid,
              yearOfMigration = Some("2017"),
              businesses = List(business),
              properties = Nil
            )

          WiremockHelper.stubGet(url, OK, requestBody)

          implicit val testAuthorisedAndEnrolled: AuthorisedAndEnrolledRequest[_] =
            AuthorisedAndEnrolledRequest(
              mtditId = testMtditid,
              mtdUserRole = MTDIndividual,
              authUserDetails = defaultAuthUserDetails(MTDIndividual),
              None
            )(FakeRequest())

          val result = connector.getIncomeSources()(hc, mtdItUser = testAuthorisedAndEnrolled).futureValue

          result shouldBe expectedResponse

          WiremockHelper.verifyGet(uri = url)
        }
      }

      "INTERNAL_SERVER_ERROR - 500" should {

        "return IncomeSourceDetailsError with some response body" in {
          val testMtditid = "XAITSA123456"

          val url = s"/income-tax-view-change/income-sources/$testMtditid"

          implicit val testAuthorisedAndEnrolled: AuthorisedAndEnrolledRequest[_] =
            AuthorisedAndEnrolledRequest(
              mtditId = testMtditid,
              mtdUserRole = MTDIndividual,
              authUserDetails = defaultAuthUserDetails(MTDIndividual),
              None
            )(FakeRequest())

          val responseBody =
            """{
              |"message": "fake value"
              |}""".stripMargin

          WiremockHelper.stubGet(url, INTERNAL_SERVER_ERROR, responseBody)

          val result = connector.getIncomeSources()(hc, mtdItUser = testAuthorisedAndEnrolled).futureValue

          result shouldBe IncomeSourceDetailsError(status = INTERNAL_SERVER_ERROR, reason = responseBody)

          WiremockHelper.verifyGet(uri = url)
        }
      }
    }
  }
}
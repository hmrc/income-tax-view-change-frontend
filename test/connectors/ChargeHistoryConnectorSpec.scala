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

import audit.mocks.MockAuditingService
import config.FrontendAppConfig
import mocks.MockHttpV2
import models.chargeHistory.ChargesHistoryResponse.ChargesHistoryResponse
import models.chargeHistory.{ChargeHistoryModel, ChargeHistoryResponseModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import play.api.Configuration
import play.mvc.Http.Status
import testConstants.BaseTestConstants.{chargeReference, testMtditid}
import testConstants.ChargeHistoryTestConstants.{testChargeHistoryErrorModelParsing, testValidChargeHistoryModel}
import testUtils.TestSupport
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.LocalDate
import scala.concurrent.Future

class ChargeHistoryConnectorSpec extends TestSupport with MockHttpV2 with MockAuditingService {


  trait Setup {
    val baseUrl = "http://localhost:9999"

    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val itvcProtectedService: String = "http://localhost:9999"
      }

    val connector = new ChargeHistoryConnector(mockHttpClientV2, getAppConfig())
  }


  "getChargeHistoryUrl" should {
    "return the correct url" in new Setup {
      connector.getChargeHistoryUrl(testMtditid, chargeReference) shouldBe s"$baseUrl/income-tax-view-change/charge-history/$testMtditid/chargeReference/$chargeReference"
    }
  }

  "getChargeHistory" should {

    val testChargeHistoryModelSuccess = ChargeHistoryModel(taxYear = "2017", documentId = "123456789", documentDate = LocalDate.of(2020, 1, 29), documentDescription = "Balancing Charge", totalAmount = 123456789012345.67, reversalDate = LocalDate.of(2020, 2, 24), reversalReason = "amended return", poaAdjustmentReason = Some("005"))

    val successResponseModel: ChargeHistoryResponseModel =
      ChargesHistoryModel(idType = "NINO", idValue = "AB123456C", regimeType = "ITSA", chargeHistoryDetails = Some(List(testChargeHistoryModelSuccess)))
    val successResponseBadJsonModel: ChargeHistoryResponseModel =
      ChargesHistoryErrorModel(code = Status.INTERNAL_SERVER_ERROR, message = "Json Validation Error. Parsing ChargeHistory Data Response")
    val badResponseModel = ChargesHistoryErrorModel(code = Status.BAD_REQUEST, message = "Error Message")

    val getChargeHistoryUrlTestUrl =
      s"http://localhost:9999/income-tax-view-change/charge-history/$testMtditid/chargeReference/$chargeReference"

    "return a ChargeHistory model when successful JSON is received" in new Setup {
      setupMockHttpV2Get[ChargesHistoryResponse](getChargeHistoryUrlTestUrl)(successResponseModel)

      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, Some(chargeReference))
      result.futureValue shouldBe testValidChargeHistoryModel

    }

    "return a ChargeHistory model in case of future failed scenario" in new Setup {
      setupMockFailedHttpV2Get[ChargesHistoryResponse](getChargeHistoryUrlTestUrl)

      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, Some(chargeReference))
      result.futureValue shouldBe ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
    }


    "return ChargeHistoryErrorResponse model in case of failure" in new Setup {
      setupMockHttpV2Get[ChargesHistoryResponse](getChargeHistoryUrlTestUrl)(badResponseModel)

      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, Some(chargeReference))
      result.futureValue shouldBe ChargesHistoryErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return ChargeHistoryErrorResponse model in case of bad/malformed JSON response" in new Setup {
      setupMockHttpV2Get[ChargesHistoryResponse](getChargeHistoryUrlTestUrl)(successResponseBadJsonModel)

      val result: Future[ChargeHistoryResponseModel] = connector.getChargeHistory(testMtditid, Some(chargeReference))
      result.futureValue shouldBe testChargeHistoryErrorModelParsing
    }

  }

}

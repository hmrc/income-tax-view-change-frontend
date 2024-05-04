/*
 * Copyright 2023 HM Revenue & Customs
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

/*
 * Copyright 2023 HM Revenue & Customs
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

import config.FrontendAppConfig
import mocks.MockHttp
import models.incomeSourceDetails.TaxYear
import models.optOut.OptOutApiCallResponse
import org.mockito.Mockito.{mock, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import testConstants.OptOutStatusUpdateTestConstants
import testConstants.OptOutStatusUpdateTestConstants._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class OptOutConnectorTest extends AnyWordSpecLike with Matchers with BeforeAndAfter with MockHttp {

  val http: HttpClient = mock(classOf[HttpClient])
  val appConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])
  implicit val headerCarrier: HeaderCarrier = mock(classOf[HeaderCarrier])
  val connector = new OptOutConnector(http, appConfig)

  val taxYear = TaxYear.forYearEnd(2024)
  val taxableEntityId: String = "AB123456A"
  when(appConfig.itvcProtectedService).thenReturn(s"http://localhost:9082")

//  trait Setup {
//
//    val baseUrl = "http://localhost:9082"
//    def getAppConfig(): FrontendAppConfig =
//      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
//        override lazy val itvcProtectedService: String = baseUrl
//      }
//
//    val connector = new OptOutConnector(http, getAppConfig())
//  }

  "updateCessationDate" should {

    setupMockHttpPutWithHeaderCarrier(connector.getUrl(taxableEntityId))(
      OptOutStatusUpdateTestConstants.request,
      OptOutStatusUpdateTestConstants.successHttpResponse
    )
    val result: Future[OptOutApiCallResponse] = connector.requestOptOutForTaxYear(taxYear, taxableEntityId)
    result.futureValue shouldBe expectedSuccessResponse

//    s"return INTERNAL_SERVER_ERROR UpdateIncomeSourceResponseError" when {
//      "invalid json response" in new Setup {
//        setupMockHttpPutWithHeaderCarrier(connector.getUpdateIncomeSourceUrl)(
//          UpdateIncomeSourceTestConstants.request,
//          UpdateIncomeSourceTestConstants.successInvalidJsonResponse)
//        val result: Future[UpdateIncomeSourceResponse] = connector.updateCessationDate(testNino, incomeSourceId, Some(LocalDate.parse(cessationDate)))
//        result.futureValue shouldBe badJsonResponse
//      }
//      "receiving a 500+ response" in new Setup {
//        setupMockHttpPutWithHeaderCarrier(connector.getUpdateIncomeSourceUrl)(
//          UpdateIncomeSourceTestConstants.request, HttpResponse(status = Status.INTERNAL_SERVER_ERROR,
//            json = Json.toJson(failureResponse), headers = Map.empty))
//        val result: Future[UpdateIncomeSourceResponse] = connector.updateCessationDate(testNino, incomeSourceId, Some(LocalDate.parse(cessationDate)))
//        result.futureValue shouldBe failureResponse
//      }
//    }

  }

//  "updateTaxYearSpecific" should {
//
//    s"return a valid UpdateIncomeSourceResponseModel" in new Setup {
//      setupMockHttpPutWithHeaderCarrier(connector.getUpdateIncomeSourceUrl)(
//        UpdateIncomeSourceTestConstants.requestTaxYearSpecific,
//        UpdateIncomeSourceTestConstants.successHttpResponse)
//      val result: Future[UpdateIncomeSourceResponse] = connector.updateIncomeSourceTaxYearSpecific(
//        testNino, incomeSourceId, taxYearSpecific)
//      result.futureValue shouldBe successResponse
//    }
//
//    s"return INTERNAL_SERVER_ERROR UpdateIncomeSourceResponseError" when {
//      "invalid json response" in new Setup {
//        setupMockHttpPutWithHeaderCarrier(connector.getUpdateIncomeSourceUrl)(
//          UpdateIncomeSourceTestConstants.requestTaxYearSpecific,
//          UpdateIncomeSourceTestConstants.successInvalidJsonResponse)
//        val result: Future[UpdateIncomeSourceResponse] = connector.updateIncomeSourceTaxYearSpecific(
//          testNino, incomeSourceId, taxYearSpecific)
//        result.futureValue shouldBe badJsonResponse
//      }
//      "receiving a 500+ response" in new Setup {
//        setupMockHttpPutWithHeaderCarrier(connector.getUpdateIncomeSourceUrl)(
//          UpdateIncomeSourceTestConstants.requestTaxYearSpecific,
//          HttpResponse(status = Status.INTERNAL_SERVER_ERROR,
//            json = Json.toJson(failureResponse), headers = Map.empty))
//        val result: Future[UpdateIncomeSourceResponse] = connector.updateIncomeSourceTaxYearSpecific(
//          testNino, incomeSourceId, taxYearSpecific)
//        result.futureValue shouldBe failureResponse
//      }
//    }
//  }
}

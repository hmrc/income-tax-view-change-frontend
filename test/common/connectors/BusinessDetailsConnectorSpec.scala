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

package common.connectors

import common.config.FrontendAppConfig
import common.models.auth.AuthorisedAndEnrolledRequest
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Configuration
import play.api.libs.json.Json
import play.mvc.Http.Status
import testConstants.BaseTestConstants.*
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncome
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class BusinessDetailsConnectorSpec extends BaseConnectorSpec {

  implicit val authorisedAndEnrolledRequest: AuthorisedAndEnrolledRequest[_] = testAuthorisedAndEnrolled

  trait Setup {
    val baseUrl = "http://localhost:9999"

    def getAppConfig: FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
<<<<<<< HEAD
        override lazy val itvcProtectedService: String = "http://localhost:9999"
=======
        override lazy val incomeTaxBusinessDetailsBaseUrl: String = "http://localhost:9999"
        
        override def incomeSourceOverrides(): Option[Seq[String]] = Some(incomeSourceOverride)
>>>>>>> 51a7c0a85 (Reapply "MIPR-2520")
      }

    val connector = new BusinessDetailsConnector(mockHttpClientV2, getAppConfig)
    val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "BusinessDetailsConnector" when {

    ".getBusinessDetailsUrl()" should {
      "return the correct url" in new Setup {
        connector.getBusinessDetailsUrl(testNino) shouldBe s"$baseUrl/income-tax-business-details/get-business-details/nino/$testNino"
      }
    }

<<<<<<< HEAD
=======
    ".getIncomeSourcesUrl()" should {
      "return the correct url" in new Setup {
        connector.getIncomeSourcesUrl(testMtditid) shouldBe s"$baseUrl/income-tax-business-details/income-sources/$testMtditid"
      }
    }

    ".getNinoLookupUrl()" should {
      "return the correct url" in new Setup {
        connector.getNinoLookupUrl(testMtditid) shouldBe s"$baseUrl/income-tax-business-details/nino-lookup/$testMtditid"      }
    }

>>>>>>> 51a7c0a85 (Reapply "MIPR-2520")
    ".getBusinessDetails()" should {

      val successResponse = HttpResponse(status = Status.OK, json = Json.toJson(singleBusinessIncome), headers = Map.empty)
      val successResponseBadJson = HttpResponse(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)
      val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

      "return an IncomeSourceDetailsModel when successful JSON is received" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponse))

        val result: Future[IncomeSourceDetailsResponse] = connector.getBusinessDetails(testNino)
        result.futureValue shouldBe singleBusinessIncome
      }

      "return IncomeSourceDetailsError in case of bad/malformed JSON response" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponseBadJson))

        val result: Future[IncomeSourceDetailsResponse] = connector.getBusinessDetails(testNino)
        result.futureValue shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
      }

      "return IncomeSourceDetailsError model in case of failure" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(badResponse))

        val result: Future[IncomeSourceDetailsResponse] = connector.getBusinessDetails(testNino)
        result.futureValue shouldBe IncomeSourceDetailsError(Status.BAD_REQUEST, "Error Message")
      }

      "return IncomeSourceDetailsError model in case of future failed scenario" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.failed(new Exception("unknown error")))

        val result: Future[IncomeSourceDetailsResponse] = connector.getBusinessDetails(testNino)
        result.futureValue shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error, unknown error")
      }
    }
  }
}

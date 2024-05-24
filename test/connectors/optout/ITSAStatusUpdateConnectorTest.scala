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

package connectors.optout

import config.FrontendAppConfig
import connectors.optout.ITSAStatusUpdateConnector.CorrelationIdHeader
import connectors.optout.OptOutUpdateRequestModel._
import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import play.mvc.Http.Status.{BAD_REQUEST, NO_CONTENT}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import ITSAStatusUpdateConnector._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ITSAStatusUpdateConnectorTest extends AnyWordSpecLike with Matchers with BeforeAndAfter with ScalaFutures {

  val httpClient: HttpClient = mock(classOf[HttpClient])
  val appConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])
  implicit val headerCarrier: HeaderCarrier = mock(classOf[HeaderCarrier])
  val connector = new ITSAStatusUpdateConnector(httpClient, appConfig)

  val taxYear = TaxYear.forYearEnd(2024)
  val taxableEntityId: String = "AB123456A"

  before {
    reset(httpClient, appConfig, headerCarrier)
  }

  "For OptOutConnector.requestOptOutForTaxYear " when {

    "happy case" should {

      "return successful response" in {

        when(appConfig.itvcProtectedService).thenReturn(s"http://localhost:9082")

        val apiRequest = OptOutUpdateRequest(toApiFormat(taxYear), optOutUpdateReason)
        val apiResponse = OptOutUpdateResponseSuccess("123", NO_CONTENT)
        val httpResponse = HttpResponse(NO_CONTENT, Json.toJson(apiResponse), Map(CorrelationIdHeader -> Seq("123")))

        setupHttpClientMock[OptOutUpdateRequest](connector.buildRequestUrlWith(taxableEntityId))(apiRequest, httpResponse)

        val result: Future[OptOutUpdateResponse] = connector.requestOptOutForTaxYear(taxYear, taxableEntityId, optOutUpdateReason)

        result.futureValue shouldBe OptOutUpdateResponseSuccess("123", NO_CONTENT)

      }
    }

    "unhappy case" should {

      "return failure response" in {

        when(appConfig.itvcProtectedService).thenReturn(s"http://localhost:9082")

        val errorItems = List(ErrorItem("INVALID_TAXABLE_ENTITY_ID",
          "Submission has not passed validation. Invalid parameter taxableEntityId."))
        val correlationId = "123"
        val apiRequest = OptOutUpdateRequest(toApiFormat(taxYear), optOutUpdateReason)
        val apiFailResponse = OptOutUpdateResponseFailure(correlationId, BAD_REQUEST, errorItems)
        val httpResponse = HttpResponse(BAD_REQUEST, Json.toJson(apiFailResponse), Map(CorrelationIdHeader -> Seq("123")))

        setupHttpClientMock[OptOutUpdateRequest](connector.buildRequestUrlWith(taxableEntityId))(apiRequest, httpResponse)

        val result: Future[OptOutUpdateResponse] = connector.requestOptOutForTaxYear(taxYear, taxableEntityId, optOutUpdateReason)

        result.futureValue shouldBe OptOutUpdateResponseFailure(correlationId, BAD_REQUEST, errorItems)

      }
    }

    "unhappy case, missing header" should {

      "return failure response" in {

        when(appConfig.itvcProtectedService).thenReturn(s"http://localhost:9082")

        val errorItems = List(ErrorItem("INVALID_TAXABLE_ENTITY_ID",
          "Submission has not passed validation. Invalid parameter taxableEntityId."))
        val correlationId = "123"
        val apiRequest = OptOutUpdateRequest(toApiFormat(taxYear), optOutUpdateReason)
        val apiFailResponse = OptOutUpdateResponseFailure(correlationId, BAD_REQUEST, errorItems)
        val httpResponse = HttpResponse(BAD_REQUEST, Json.toJson(apiFailResponse), Map.empty)

        setupHttpClientMock[OptOutUpdateRequest](connector.buildRequestUrlWith(taxableEntityId))(apiRequest, httpResponse)

        val result: Future[OptOutUpdateResponse] = connector.requestOptOutForTaxYear(taxYear, taxableEntityId, optOutUpdateReason)

        result.futureValue shouldBe OptOutUpdateResponseFailure("Unknown_CorrelationId", BAD_REQUEST, errorItems)

      }
    }

  }

  "For OptOutConnector.taxYearToFormat " when {
    "happy case" should {
      "match required format" in {
        toApiFormat(TaxYear.forYearEnd(2024)) shouldBe "2023-24"
      }
    }

    "unhappy case" should {
      "default format not match required format" in {
        toApiFormat(TaxYear.forYearEnd(2024)) should not be TaxYear.forYearEnd(2024).toString
      }
    }

  }

  def setupHttpClientMock[R](url: String, headers: Seq[(String, String)] = Seq())(body: R, response: HttpResponse): Unit = {
    when(httpClient.PUT[R, HttpResponse](ArgumentMatchers.eq(url), ArgumentMatchers.eq(body), ArgumentMatchers.eq(headers))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))
  }
}

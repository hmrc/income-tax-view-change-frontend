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
import connectors.OptOutConnector.CorrelationIdHeader
import models.incomeSourceDetails.TaxYear
import models.optOut.OptOutUpdateRequestModel._
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import play.api.libs.json.Json
import play.mvc.Http.Status.{BAD_REQUEST, NO_CONTENT}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

class OptOutConnectorTest extends AnyWordSpecLike with Matchers with BeforeAndAfter {

  val httpClient: HttpClient = mock(classOf[HttpClient])
  val appConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])
  implicit val headerCarrier: HeaderCarrier = mock(classOf[HeaderCarrier])
  val connector = new OptOutConnector(httpClient, appConfig)

  val taxYear = TaxYear.forYearEnd(2024)
  val taxableEntityId: String = "AB123456A"

  before {
    reset(httpClient, appConfig, headerCarrier)
  }

  "For OptOutConnector.requestOptOutForTaxYear " when {

    "happy case" should {

      "return successful response" in {

        when(appConfig.itvcProtectedService).thenReturn(s"http://localhost:9082")

        val apiRequest = OptOutUpdateRequest(taxYear.toString)
        val apiResponse = OptOutUpdateResponseSuccess("123", NO_CONTENT)
        val httpResponse = HttpResponse(NO_CONTENT, Json.toJson(apiResponse), Map(CorrelationIdHeader -> Seq("123")))

        setupHttpClientMock[OptOutUpdateRequest](connector.getUrl(taxableEntityId))(apiRequest, httpResponse)

        val result: Future[OptOutUpdateResponse] = connector.requestOptOutForTaxYear(taxYear, taxableEntityId)

        Await.result(result, 10.seconds)

        result.value.map {
          case Success(response) =>
            response shouldBe apiResponse
          case Failure(e) => fail(s"error: ${e.getMessage}")
        }
      }
    }

    "unhappy case" should {

      "return failure response" in {

        when(appConfig.itvcProtectedService).thenReturn(s"http://localhost:9082")

        val apiRequest = OptOutUpdateRequest(taxYear.toString)
        val apiFailResponse = OptOutUpdateResponseFailure(
          BAD_REQUEST,
          List(ErrorItem("INVALID_TAXABLE_ENTITY_ID",
          "Submission has not passed validation. Invalid parameter taxableEntityId."))
        )
        val httpResponse = HttpResponse(BAD_REQUEST, Json.toJson(apiFailResponse), Map(CorrelationIdHeader -> Seq("123")))

        setupHttpClientMock[OptOutUpdateRequest](connector.getUrl(taxableEntityId))(apiRequest, httpResponse)

        val result: Future[OptOutUpdateResponse] = connector.requestOptOutForTaxYear(taxYear, taxableEntityId)

        Await.result(result, 10.seconds)

        result.value.map {
          case Success(response) => response shouldBe apiFailResponse
          case Failure(e) => fail(s"error: ${e.getMessage}")
        }
      }
    }
  }

  def setupHttpClientMock[R](url: String, headers: Seq[(String, String)] = Seq())(body: R, response: HttpResponse): Unit = {
    when(httpClient.PUT[R, HttpResponse](ArgumentMatchers.eq(url), ArgumentMatchers.eq(body), ArgumentMatchers.eq(headers))
      (ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(response))
  }
}

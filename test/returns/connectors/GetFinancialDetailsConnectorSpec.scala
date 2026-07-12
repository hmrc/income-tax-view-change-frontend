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

package returns.connectors

import common.config.FrontendAppConfig
import common.connectors.BaseConnectorSpec
import common.models.audit.{AuditModel, ExtendedAuditModel}
import common.services.AuditingService
import common.testConstants.BaseTestConstants.*
import returns.models.*
import returns.testConstants.FinancialDetailsTestConstants.*
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, verify, when}
import org.mockito.{AdditionalMatchers, ArgumentMatchers}
import play.api.Configuration
import play.api.mvc.Request
import play.mvc.Http.Status
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}


class GetFinancialDetailsConnectorSpec extends BaseConnectorSpec {

  trait Setup {

    val baseUrl = "http://localhost:9999"

    def getAppConfig: FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
      }

    val connector = new GetFinancialDetailsConnector(mockHttpClientV2, getAppConfig)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditingService)
  }

  lazy val mockAuditingService: AuditingService = mock(classOf[AuditingService])

  def verifyAudit(model: AuditModel, path: Option[String] = None): Unit = {
    verify(mockAuditingService).audit(
      ArgumentMatchers.eq(model),
      AdditionalMatchers.or(ArgumentMatchers.eq(path), ArgumentMatchers.isNull)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]],
      ArgumentMatchers.any[ExecutionContext]
    )
  }

  def verifyExtendedAudit(model: ExtendedAuditModel, path: Option[String] = None): Unit =
    verify(mockAuditingService).extendedAudit(
      ArgumentMatchers.eq(model),
      AdditionalMatchers.or(ArgumentMatchers.eq(path), ArgumentMatchers.isNull)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]],
      ArgumentMatchers.any[ExecutionContext]
    )

  def verifyExtendedAuditSent(model: ExtendedAuditModel): Unit =
    verify(mockAuditingService).extendedAudit(
      ArgumentMatchers.eq(model),
      any()
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[Request[_]],
      ArgumentMatchers.any[ExecutionContext]
    )

  "FinancialDetailsConnector" when {

    ".getFinancialDetails() for a single tax year" should {

      val successResponse = HttpResponse(status = Status.OK, json = testValidFinancialDetailsModelJsonReads, headers = Map.empty)
      val successResponseBadJson = HttpResponse(status = Status.OK, json = testInvalidFinancialDetailsJson, headers = Map.empty)
      val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

      "return a FinancialDetails model when successful JSON is received" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponse))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
        result.futureValue shouldBe testValidFinancialDetailsModel
      }

      "return FinancialDetails model in case of bad/malformed JSON response" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(successResponseBadJson))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
        result.futureValue shouldBe testFinancialDetailsErrorModelParsing
      }

      "return FinancialDetailsErrorResponse model in case of failure" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future(badResponse))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
        result.futureValue shouldBe FinancialDetailsErrorModel(Status.BAD_REQUEST, "Error Message")
      }

      "return FinancialDetailsErrorModel model in case of future failed scenario" in new Setup {

        when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.withBody(any())(any(), any(), any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)

        when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
          .thenReturn(Future.failed(new Exception("unknown error")))

        val result: Future[FinancialDetailsResponseModel] = connector.getFinancialDetails(testYear2017, testNino)
        result.futureValue shouldBe FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, unknown error")
      }

    }
  }
}

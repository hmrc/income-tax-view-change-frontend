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

import audit.AuditingService
import audit.models._
import auth.authV2.models.AuthorisedAndEnrolledRequest
import config.FrontendAppConfig
import models.core.{NinoResponse, NinoResponseError}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsResponse}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, verify, when}
import org.mockito.{AdditionalMatchers, ArgumentMatchers}
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.Request
import play.mvc.Http.Status
import testConstants.BaseTestConstants._
import testConstants.NinoLookupTestConstants.{testNinoModel, testNinoModelJson}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.{singleBusinessAndPropertyMigrat2019, singleBusinessIncome}
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}

class BusinessDetailsConnectorSpec extends BaseConnectorSpec {

  lazy val mockAuditingService: AuditingService = mock(classOf[AuditingService])
  implicit val authorisedAndEnrolledRequest: AuthorisedAndEnrolledRequest[_] = testAuthorisedAndEnrolled

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

  trait Setup {
    val baseUrl = "http://localhost:9999"

    def getAppConfig(): FrontendAppConfig =
      new FrontendAppConfig(app.injector.instanceOf[ServicesConfig], app.injector.instanceOf[Configuration]) {
        override lazy val itvcProtectedService: String = "http://localhost:9999"

        override def incomeSourceOverrides(): Option[Seq[String]] = Some(incomeSourceOverride)
      }

    val connector = new BusinessDetailsConnector(mockHttpClientV2, mockAuditingService, getAppConfig())
    val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionId)))
  }

  val incomeSourceOverride =
    Seq(
      "uk-property-reporting-method", // UK Property Select reporting method
      "foreign-property-reporting-method", // Foreign Property Select reporting method
      "business-reporting-method", // Sole Trader Select reporting method
      "reporting-frequency" // Manage business reporting frequency
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditingService)
  }

  "BusinessDetailsConnector" when {

    ".getBusinessDetailsUrl()" should {
      "return the correct url" in new Setup {
        connector.getBusinessDetailsUrl(testNino) shouldBe s"$baseUrl/income-tax-view-change/get-business-details/nino/$testNino"
      }
    }

    ".getIncomeSourcesUrl()" should {
      "return the correct url" in new Setup {
        connector.getIncomeSourcesUrl(testMtditid) shouldBe s"$baseUrl/income-tax-view-change/income-sources/$testMtditid"
      }
    }

    ".getNinoLookupUrl()" should {
      "return the correct url" in new Setup {
        connector.getNinoLookupUrl(testMtditid) shouldBe s"$baseUrl/income-tax-view-change/nino-lookup/$testMtditid"
      }
    }

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

        ".getIncomeSources()" should {

          val successResponse = HttpResponse(status = Status.OK, json = Json.toJson(singleBusinessAndPropertyMigrat2019), headers = Map.empty)
          val successResponseBadJson = HttpResponse(status = Status.OK, json = Json.parse("{}"), headers = Map.empty)
          val badResponse = HttpResponse(status = Status.BAD_REQUEST, body = "Error Message")

          "return an IncomeSourceDetailsModel when successful JSON is received" in new Setup {

            when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.withBody(any())(any(), any(), any()))
              .thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.setHeader(any()))
              .thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
              .thenReturn(Future(successResponse))

            val result: Future[IncomeSourceDetailsResponse] = connector.getIncomeSources()
            result.futureValue shouldBe singleBusinessAndPropertyMigrat2019

            verifyExtendedAudit(IncomeSourceDetailsResponseAuditModel(testAuthorisedAndEnrolled, testNino, List(testSelfEmploymentId), List(testPropertyIncomeId), Some(testMigrationYear2019)))
          }

          "return IncomeSourceDetailsError in case of bad/malformed JSON response" in new Setup {

            when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.withBody(any())(any(), any(), any()))
              .thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.setHeader(any()))
              .thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
              .thenReturn(Future(successResponseBadJson))

            val result: Future[IncomeSourceDetailsResponse] = connector.getIncomeSources()
            result.futureValue shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")

          }

          "return IncomeSourceDetailsError model in case of failure" in new Setup {

            when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.withBody(any())(any(), any(), any()))
              .thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.setHeader(any()))
              .thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
              .thenReturn(Future(badResponse))

            val result: Future[IncomeSourceDetailsResponse] = connector.getIncomeSources()
            result.futureValue shouldBe IncomeSourceDetailsError(Status.BAD_REQUEST, "Error Message")

          }

          "return IncomeSourceDetailsError model in case of future failed scenario" in new Setup {

            when(mockHttpClientV2.get(any())(any())).thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.withBody(any())(any(), any(), any()))
              .thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.setHeader(any()))
              .thenReturn(mockRequestBuilder)

            when(mockRequestBuilder.execute(any[HttpReads[HttpResponse]], any()))
              .thenReturn(Future.failed(new Exception("unknown error")))

            val result: Future[IncomeSourceDetailsResponse] = connector.getIncomeSources()
            result.futureValue shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error, unknown error")

          }
        }

    ".modifyHeaderCarrier()" should {

      "add test header when path matches certain patterns" should {

        val scenarios =
          Table(
            ("scenarioName", "path", "expectedHeader"),
            ("Manage Business Journey", "/manage-your-businesses/reporting-frequency", Some("afterIncomeSourceCreated")),
            ("UK Property", "/income-sources/uk-property-reporting-method", Some("afterIncomeSourceCreated")),
            ("Foreign Property", "/income-sources/foreign-property-reporting-method", Some("afterIncomeSourceCreated")),
            ("Sole Trader", "/income-sources/business-reporting-method", Some("afterIncomeSourceCreated"))
          )

        forAll(scenarios) { (scenarioName: String, path: String, expectedHeader: Option[String]) =>
          s"add test header for $scenarioName with path: $path" in new Setup {
            implicit val appConfig: FrontendAppConfig = getAppConfig()
            val modifiedHeaderCarrier: HeaderCarrier = connector.modifyHeaderCarrier(path, hc)

            modifiedHeaderCarrier.extraHeaders.toMap.get("Gov-Test-Scenario") shouldBe expectedHeader
          }
        }
      }

      "not add test header when path does not match patterns" should {

        val noHeaderScenarios = Table(
          ("scenarioName", "path", "expectedHeader"),
          ("Other Path", "/some/other-path", None),
          ("Manage Business Other Path", "/manage-your-businesses/other-path", Some("")),
          ("Income Sources Other Path", "/income-sources/other-path", Some(""))
        )

        forAll(noHeaderScenarios) { (scenarioName: String, path: String, expectedHeader: Option[String]) =>
          s"not add test header for $scenarioName with path: $path" in new Setup {
            implicit val appConfig: FrontendAppConfig = getAppConfig()
            val modifiedHeaderCarrier: HeaderCarrier = connector.modifyHeaderCarrier(path, hc)

            modifiedHeaderCarrier.extraHeaders.toMap.get("Gov-Test-Scenario") shouldBe expectedHeader
          }
        }
      }
    }
  }
}

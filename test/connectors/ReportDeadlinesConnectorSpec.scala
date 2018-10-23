/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.BaseTestConstants._
import assets.ReportDeadlinesTestConstants._
import audit.mocks.MockAuditingService
import audit.models.{ReportDeadlinesRequestAuditModel, ReportDeadlinesResponseAuditModel}
import mocks.MockHttp
import models.reportDeadlines.{ReportDeadlinesErrorModel, ReportDeadlinesResponseModel}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testUtils.TestSupport
import uk.gov.hmrc.http.HttpResponse

import scala.concurrent.Future


class ReportDeadlinesConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  val successResponse = HttpResponse(Status.OK, responseJson = Some(obligationsDataFromJson))
  val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestReportDeadlinesDataConnector extends ReportDeadlinesConnector(mockHttpGet, frontendAppConfig, mockAuditingService)

  "ReportDeadlinesDataConnector.getReportDeadlines()" should {

    lazy val testUrl = TestReportDeadlinesDataConnector.getReportDeadlinesUrl(testSelfEmploymentId, testNino)
    def result: Future[ReportDeadlinesResponseModel] = TestReportDeadlinesDataConnector.getReportDeadlines(testSelfEmploymentId)

    "have the correct URL to ITVC backend" in {
      testUrl shouldBe s"${frontendAppConfig.itvcProtectedService}/income-tax-view-change/$testNino/income-source/$testSelfEmploymentId/report-deadlines"
    }

    "return a SuccessResponse with JSON in case of success" in {
      setupMockHttpGet(testUrl)(successResponse)
      await(result) shouldBe obligationsDataSuccessModel
      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId), Some(testReferrerUrl))
      verifyExtendedAudit(ReportDeadlinesResponseAuditModel(testMtditid, testNino, testSelfEmploymentId, obligationsDataSuccessModel.obligations))
    }

    "return ErrorResponse model in case of failure" in {
      setupMockHttpGet(testUrl)(badResponse)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.BAD_REQUEST, "Error Message")
      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId))
    }

    "return BusinessListError model when bad JSON is received" in {
      setupMockHttpGet(testUrl)(successResponseBadJson)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Report Deadlines Data Response")
      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId))
    }

    "return ReportDeadlinesErrorModel model in case of future failed scenario" in {
      setupMockFailedHttpGet(testUrl)(badResponse)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
      verifyAudit(ReportDeadlinesRequestAuditModel(testMtditid, testNino, testSelfEmploymentId))
    }
  }
}

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

import assets.TestConstants.ReportDeadlines._
import assets.TestConstants._
import mocks.MockHttp
import models.{ReportDeadlinesErrorModel, ReportDeadlinesResponseModel}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.HttpResponse
import utils.TestSupport

import scala.concurrent.Future


class BusinessEOPSDeadlinesConnectorSpec extends TestSupport with MockHttp {

  val successResponse = HttpResponse(Status.OK, Some(Json.toJson(obligationsDataSuccessModel)))
  val successResponseBadJson = HttpResponse(Status.OK, responseJson = Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestBusinessEOPSDeadlinesConnector extends BusinessEOPSDeadlinesConnector(mockHttpGet, frontendAppConfig)

  "BusinessEOPSDeadlinesConnector.getBusinessEOPSDeadlineUrl" should {

    lazy val testUrl = TestBusinessEOPSDeadlinesConnector.getBusinessEOPSDeadlineUrl(testNino, testSelfEmploymentId)
    def result: Future[ReportDeadlinesResponseModel] = TestBusinessEOPSDeadlinesConnector.getBusinessEOPSDeadline(testNino, testSelfEmploymentId)

    "have the correct URL to Self-Assessment-API" in {
      testUrl shouldBe s"${frontendAppConfig.saApiService}/ni/$testNino/self-employments/$testSelfEmploymentId/end-of-period-statements/obligations"
    }

    "return a SuccessResponse with JSON in case of sucess" in {
      setupMockHttpGet(testUrl)(successResponse)
      await(result) shouldBe obligationsDataSuccessModel
    }

    "return ErrorResponse model in case of failure" in {
      setupMockHttpGet(testUrl)(badResponse)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.BAD_REQUEST, "Error Message")
    }

    "return ReportDeadlinesErrorModel when bad JSON is received" in {
      setupMockHttpGet(testUrl)(successResponseBadJson)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing SE Business EOPS Deadlines Response")
    }

    "return ReportDeadlinesErrorModel model in case of future failed scenario" in {
      setupMockFailedHttpGet(testUrl)(badResponse)
      await(result) shouldBe ReportDeadlinesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected future failed error")
    }
  }
}

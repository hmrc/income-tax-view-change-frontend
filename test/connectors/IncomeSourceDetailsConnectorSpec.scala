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

import assets.TestConstants.NewIncomeSourceDetails._
import assets.TestConstants.{testMtditid, testNino, testSelfEmploymentId, testReferrerUrl}
import audit.mocks.MockAuditingService
import audit.models.{IncomeSourceDetailsRequestAuditModel, IncomeSourceDetailsResponseAuditModel}
import mocks.MockHttp
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsResponse}
import play.api.libs.json.Json
import play.mvc.Http.Status
import uk.gov.hmrc.http.HttpResponse
import utils.TestSupport

import scala.concurrent.Future

class IncomeSourceDetailsConnectorSpec extends TestSupport with MockHttp with MockAuditingService {

  val successResponse = HttpResponse(Status.OK, Some(Json.toJson(incomeSourceDetails)))
  val successResponseBadJson = HttpResponse(Status.OK, Some(Json.parse("{}")))
  val badResponse = HttpResponse(Status.BAD_REQUEST, responseString = Some("Error Message"))

  object TestIncomeSourceDetailsConnector extends IncomeSourceDetailsConnector(mockHttpGet, frontendAppConfig, mockAuditingService)

  "IncomeSourceDetailsConnector.getIncomeSources" should {

    lazy val testUrl = TestIncomeSourceDetailsConnector.getIncomeSourcesUrl(testMtditid)
    def result: Future[IncomeSourceDetailsResponse] = TestIncomeSourceDetailsConnector.getIncomeSources(testMtditid, testNino)

    "return an IncomeSourceDetailsModel when successful JSON is received" in {
      setupMockHttpGet(testUrl)(successResponse)
      await(result) shouldBe incomeSourceDetails
      verifyAudit(IncomeSourceDetailsRequestAuditModel(testMtditid, testNino), testReferrerUrl)
      verifyExtendedAudit(IncomeSourceDetailsResponseAuditModel(testMtditid, testNino, List(testSelfEmploymentId), None), testReferrerUrl)
    }

    "return IncomeSourceDetailsError in case of bad/malformed JSON response" in {
      setupMockHttpGet(testUrl)(successResponseBadJson)
      await(result) shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error Parsing Income Source Details response")
      verifyAudit(IncomeSourceDetailsRequestAuditModel(testMtditid, testNino), testReferrerUrl)
    }

    "return IncomeSourceDetailsError model in case of failure" in {
      setupMockHttpGet(testUrl)(badResponse)
      await(result) shouldBe IncomeSourceDetailsError(Status.BAD_REQUEST, "Error Message")
      verifyAudit(IncomeSourceDetailsRequestAuditModel(testMtditid, testNino), testReferrerUrl)
    }

    "return IncomeSourceDetailsError model in case of future failed scenario" in {
      setupMockFailedHttpGet(testUrl)(badResponse)
      await(result) shouldBe IncomeSourceDetailsError(Status.INTERNAL_SERVER_ERROR, "Unexpected future failed error")
      verifyAudit(IncomeSourceDetailsRequestAuditModel(testMtditid, testNino), testReferrerUrl)
    }
  }

}

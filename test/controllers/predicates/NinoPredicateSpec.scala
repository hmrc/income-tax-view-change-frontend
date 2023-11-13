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

package controllers.predicates

import testConstants.BaseTestConstants.{testErrorMessage, testErrorStatus, testMtditid, testNino, testRetrievedUserName}
import audit.AuditingService
import auth.{MtdItUserOptionNino, MtdItUserWithNino}
import config.ItvcErrorHandler
import mocks.services.MockNinoLookupService
import models.core.{NinoResponseError, NinoResponseSuccess}
import org.scalatest.EitherValues
import org.mockito.Mockito.mock
import play.api.http.Status
import play.api.test.Helpers._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Individual

import scala.concurrent.Future

class NinoPredicateSpec extends TestSupport with MockNinoLookupService with EitherValues {

  "The NinoPredicate" when {

    lazy val userNoNino = MtdItUserOptionNino(testMtditid, Some("nino"), Some(testRetrievedUserName), None,
      Some("testUtr"), Some("testCredId"), Some(Individual), None)(fakeRequestWithActiveSession)
    lazy val userNinoInSession = MtdItUserOptionNino(
      testMtditid,
      Some("nino"),
      Some(testRetrievedUserName),
      None,
      Some("testUtr"), Some("testCredId"), Some(Individual), None)(fakeRequestWithNino)
    lazy val userWithNino = MtdItUserOptionNino(testMtditid, Some(testNino), Some(testRetrievedUserName), None,
      Some("testUtr"), Some("testCredId"), Some(Individual), None)(fakeRequestWithActiveSession)
    lazy val successResponse = MtdItUserWithNino(testMtditid, testNino, Some(testRetrievedUserName),
      btaNavPartial = None, Some("testUtr"), Some("testCredId"), Some(Individual), None)

    lazy val ninoServiceSuccess = NinoResponseSuccess(testNino)
    lazy val ninoServiceError = NinoResponseError(testErrorStatus, testErrorMessage)

    object TestPredicate extends NinoPredicate(
      mockNinoLookupService,
      app.injector.instanceOf[ItvcErrorHandler],
      app.injector.instanceOf[AuditingService]
    )

    "called with a user with a NINO enrolment" should {
      "return the expected MtdItUserWithNino" in {
        val result = TestPredicate.refine(userWithNino)
        result.futureValue shouldBe Right(successResponse)
      }
    }

    "called with a NINO in Session" should {
      "return the expected MtdItUserWithNino" in {
        val result = TestPredicate.refine(userNinoInSession)
        result.futureValue shouldBe Right(successResponse)
      }
    }
    "there is no HMRC-NI enrolment and no NINO in session" should {

      "retrieve the NINO from the NINO lookup service and redirect" in {
        setupMockGetNino(testMtditid)(ninoServiceSuccess)
        val result = TestPredicate.refine(userNoNino)
        status(result.map(_.swap.toOption.value)) shouldBe Status.SEE_OTHER
      }
      "throw an ISE if no NINO can be retrieved from lookup service" in {
        setupMockGetNino(testMtditid)(ninoServiceError)
        val result = TestPredicate.refine(userNoNino)
        status(result.map(_.swap.toOption.value)) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}

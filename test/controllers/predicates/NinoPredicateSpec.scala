/*
 * Copyright 2017 HM Revenue & Customs
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

import assets.TestConstants._
import auth.{MtdItUserOptionNino, MtdItUserWithNino}
import config.ItvcErrorHandler
import mocks.services.MockNinoLookupService
import models.{Nino, NinoResponseError}
import org.mockito.ArgumentMatchers
import org.scalatest.EitherValues
import org.scalatest.mockito.MockitoSugar
import play.api.http.{HttpEntity, Status}
import play.api.mvc.{AnyContentAsEmpty, ResponseHeader, Result}
import play.mvc.Http.HeaderNames
import play.twirl.api.Html
import utils.TestSupport

import scala.concurrent.Future

class NinoPredicateSpec extends TestSupport with MockitoSugar with MockNinoLookupService with EitherValues{

  "The NinoPredicate" when {

    lazy val userNoNino = MtdItUserOptionNino(testMtditid, None, Some(testUserDetails))(fakeRequestWithActiveSession)
    lazy val userNinoInSession = MtdItUserOptionNino(testMtditid, None, Some(testUserDetails))(fakeRequestWithNino)
    lazy val userWithNino = MtdItUserOptionNino(testMtditid, Some(testNino), Some(testUserDetails))(fakeRequestWithActiveSession)
    lazy val successResponse = MtdItUserWithNino(testMtditid, testNino, Some(testUserDetails))

    lazy val ninoServiceSuccess = Nino(testNino)
    lazy val ninoServiceError   = NinoResponseError(testErrorStatus, testErrorMessage)

    object TestPredicate extends NinoPredicate(
      mockNinoLookupService,
      app.injector.instanceOf[ItvcErrorHandler]
    )

    "called with a user with a NINO enrolment" should {
      "return the expected MtdItUserWithNino" in {
        val result = TestPredicate.refine(userWithNino)
        await(result) shouldBe Right(successResponse)
      }
    }

    "called with a NINO in Session" should {
      "return the expected MtdItUserWithNino" in {
        val result = TestPredicate.refine(userNinoInSession)
        await(result) shouldBe Right(successResponse)
      }
    }
    "there is no HMRC-NI enrolment and no NINO in session" should {

      "retrieve the NINO from the NINO lookup service and redirect" in {
        setupMockGetNino(testMtditid)(ninoServiceSuccess)
        val result = TestPredicate.refine(userNoNino)
        status(await(result.left.get)) shouldBe Status.SEE_OTHER
      }
      "throw an ISE if no NINO can be retrieved from lookup service" in {
        setupMockGetNino(testMtditid)(ninoServiceError)
        val result = TestPredicate.refine(userNoNino)
        status(await(result.left.get)) shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
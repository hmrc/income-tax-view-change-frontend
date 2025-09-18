/*
 * Copyright 2025 HM Revenue & Customs
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

package services

import connectors.NrsConnector
import models.nrs.NrsSubmissionFailure.NrsErrorResponse
import models.nrs.NrsSuccessResponse
import org.mockito.Mockito._
import play.api.http.Status.NOT_FOUND
import testConstants.NrsUtils.nrsSubmission
import testUtils.TestSupport

import scala.concurrent.Future

class NrsServiceSpec extends TestSupport{

  val mockNrsConnector:NrsConnector = mock(classOf[NrsConnector])

  object TestNrsService extends NrsService(mockNrsConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNrsConnector)
  }

  "submit" should {
    "return Some(response) when connector returns Right" in {
      when(mockNrsConnector.submit(nrsSubmission)).thenReturn(Future.successful(Right(NrsSuccessResponse("submissionId"))))

      TestNrsService.submit(nrsSubmission).map { result =>
        result shouldBe Future.successful(Some(NrsSuccessResponse("submissionId")))
        verify(mockNrsConnector, times(1)).submit(nrsSubmission)
      }
    }

    "return None when connector returns Left" in {
      val error = NrsErrorResponse(NOT_FOUND)
      when(mockNrsConnector.submit(nrsSubmission)).thenReturn(Future.successful(Left(error)))

      TestNrsService.submit(nrsSubmission).map { result =>
        result shouldBe Future.successful(Some(NrsSuccessResponse("submissionId")))
        verify(mockNrsConnector, times(1)).submit(nrsSubmission)
      }
    }
  }
}

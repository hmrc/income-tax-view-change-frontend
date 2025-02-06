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

package mocks.services

import authV2.AuthActionsTestData.sessionGetSuccessResponse
import models.sessionData.SessionDataGetResponse.SessionDataNotFound
import models.sessionData.SessionDataPostResponse.SessionDataPostResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.SessionDataService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockSessionDataService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockSessionDataService: SessionDataService = mock(classOf[SessionDataService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionDataService)
  }

  def setupMockPostSessionData(response: SessionDataPostResponse): Unit = {
    when(mockSessionDataService.postSessionData(any())(any())).thenReturn(Future.successful(response))
  }

  def setupMockGetSessionDataSuccess(): Unit = {
    when(mockSessionDataService.getSessionData(any())(any(), any()))
      .thenReturn(Future.successful(Right(sessionGetSuccessResponse)))
  }

  def setupMockGetSessionDataNotFound(): Unit = {
    when(mockSessionDataService.getSessionData(any())(any(), any()))
      .thenReturn(Future.successful(Left(SessionDataNotFound("session not found"))))
  }

}

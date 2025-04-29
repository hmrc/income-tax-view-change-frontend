/*
 * Copyright 2024 HM Revenue & Customs
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

package mocks.connectors

import connectors.SessionDataConnector
import models.sessionData.SessionDataGetResponse.SessionGetResponse
import models.sessionData.SessionDataPostResponse.SessionDataPostResponse
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockSessionDataConnector extends UnitSpec with BeforeAndAfterEach {

  val mockSessionDataConnector: SessionDataConnector = mock(classOf[SessionDataConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionDataConnector)
  }

  def setupMockPostSessionData(response: SessionDataPostResponse): Unit = {
    when(mockSessionDataConnector.postSessionData(any())(any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockGetSessionData(response: SessionGetResponse): Unit = {
    when(mockSessionDataConnector.getSessionData()(any()))
      .thenReturn(Future.successful(response))
  }

}

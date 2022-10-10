/*
 * Copyright 2022 HM Revenue & Customs
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

package mocks.views.agent

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.mockito.Mockito.mock
import play.twirl.api.Html
import views.html.agent.confirmClient

trait MockConfirmClient extends BeforeAndAfterEach {
  self: Suite =>

  val confirmClient: confirmClient = mock(classOf[confirmClient])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(confirmClient)
  }

  def mockConfirmClient(response: Html): Unit = {
    when(confirmClient.apply(any(), any(), any(), any())(any(), any(), any()))
      .thenReturn(response)
  }

}

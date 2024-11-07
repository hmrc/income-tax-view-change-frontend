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

package mocks

import config.AgentItvcErrorHandler
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.mockito.Mockito.mock
import play.api.mvc.Results._
import play.twirl.api.HtmlFormat

trait MockItvcErrorHandler extends BeforeAndAfterEach {
  self: Suite =>

  lazy val mockItvcErrorHandler: AgentItvcErrorHandler = mock(classOf[AgentItvcErrorHandler])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockItvcErrorHandler)
  }

  def mockShowInternalServerError(): Unit = {
    when(mockItvcErrorHandler.showInternalServerError()(any()))
      .thenReturn(InternalServerError(HtmlFormat.empty))
  }

  def mockShowOkTechnicalDifficulties(): Unit = {
    when(mockItvcErrorHandler.showOkTechnicalDifficulties()(any()))
      .thenReturn(Ok(HtmlFormat.empty))
  }

  def mockNotFound(): Unit = {
    when(mockItvcErrorHandler.notFoundTemplate(any()))
      .thenReturn(HtmlFormat.empty)
  }

  def unauthorisedSupportingAgent(): Unit = {
    when(mockItvcErrorHandler.supportingAgentUnauthorised()(any()))
      .thenReturn(Unauthorized(HtmlFormat.empty))
  }

}

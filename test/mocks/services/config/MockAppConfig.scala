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

package mocks.services.config

import config.FrontendAppConfig
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import testUtils.UnitSpec

trait MockAppConfig extends UnitSpec with BeforeAndAfterEach {

  lazy val mockAppConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAppConfig)
  }

  def setupMockIsUserSessionApiEnabled(): Unit =
    when(mockAppConfig.isSessionDataStorageEnabled).thenReturn(true)

  def setupMockIsUserSessionApiDisabled(): Unit =
    when(mockAppConfig.isSessionDataStorageEnabled).thenReturn(false)

}

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

package mocks.services

import models.core.NinoResponse
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.mockito.Mockito.mock
import services.NinoLookupService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockNinoLookupService extends UnitSpec with BeforeAndAfterEach {

  val mockNinoLookupService: NinoLookupService = mock(classOf[NinoLookupService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNinoLookupService)
  }

  def setupMockGetNino(mtdRef: String)(response: NinoResponse): Unit =
    when(mockNinoLookupService
      .getNino(ArgumentMatchers.eq(mtdRef))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

}

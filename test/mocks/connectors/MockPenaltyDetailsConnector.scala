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

package mocks.connectors

import connectors.GetPenaltyDetailsConnector
import models.penalties.GetPenaltyDetails
import models.penalties.GetPenaltyDetailsParser.GetPenaltyDetailsResponse
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockPenaltyDetailsConnector extends UnitSpec with BeforeAndAfterEach {

  val mockGetPenaltyDetailsConnector: GetPenaltyDetailsConnector = mock(classOf[GetPenaltyDetailsConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGetPenaltyDetailsConnector)
  }

  def setupMockGetPenaltyDetailsConnector(mtdItId: String)(response: GetPenaltyDetailsResponse): Unit = {
    when(mockGetPenaltyDetailsConnector.getPenaltyDetails(ArgumentMatchers.eq(mtdItId))(any()))
      .thenReturn(Future.successful(response))
  }

}

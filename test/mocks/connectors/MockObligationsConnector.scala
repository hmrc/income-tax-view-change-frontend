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

package mocks.connectors

import connectors.ObligationsConnector
import models.obligations.ObligationsResponseModel
import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import testUtils.UnitSpec

import java.time.LocalDate
import scala.concurrent.Future

trait MockObligationsConnector extends UnitSpec with BeforeAndAfterEach {

  val mockObligationsConnector: ObligationsConnector = mock(classOf[ObligationsConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockObligationsConnector)
  }

  def setupMockNextUpdates(response: ObligationsResponseModel): Unit = {
    when(mockObligationsConnector.getNextUpdates()(any(), any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockFulfilledObligations(response: ObligationsResponseModel): Unit = {
    when(mockObligationsConnector.getFulfilledObligations()(any(), any()))
      .thenReturn(Future.successful(response))
  }

  def setupMockAllObligationsWithDates(from: LocalDate, to: LocalDate)(response: ObligationsResponseModel): Unit = {
    when(mockObligationsConnector.getAllObligations(
      fromDate = matches(from), toDate = matches(to))(any(), any()))
      .thenReturn(Future.successful(response))
  }
}

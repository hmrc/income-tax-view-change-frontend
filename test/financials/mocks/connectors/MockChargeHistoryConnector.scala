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

package financials.mocks.connectors

import common.testUtils.UnitSpec
import financials.connectors.ChargeHistoryConnector
import financials.models.chargeHistory.{ChargeHistoryResponseModel, ChargesHistoryErrorModel}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Future

trait MockChargeHistoryConnector extends UnitSpec with BeforeAndAfterEach {

  val mockChargeHistoryConnector: ChargeHistoryConnector = mock(classOf[ChargeHistoryConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockChargeHistoryConnector)
  }

  def setupGetChargeHistory(nino: String, chargeRef: Option[String])(response: ChargeHistoryResponseModel): Unit = {
    when(mockChargeHistoryConnector.getChargeHistory(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(chargeRef))(any()))
      .thenReturn(Future.successful(response))
  }

  def setupGetChargeHistoryError(nino: String, chargeRef: Option[String])(response: ChargesHistoryErrorModel): Unit = {
    when(mockChargeHistoryConnector.getChargeHistory(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(chargeRef))(any()))
      .thenReturn(Future.successful(response))
  }
}

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

import connectors.ITSAStatusConnector
import models.itsaStatus.{ITSAStatusResponse, ITSAStatusResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockITSAStatusConnector extends UnitSpec with BeforeAndAfterEach {

  val mockITSAStatusConnector: ITSAStatusConnector = mock(classOf[ITSAStatusConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockITSAStatusConnector)
  }

  def setupGetITSAStatusDetail(nino: String, taxYear: String, futureYears: Boolean, history: Boolean)(
    response: Either[ITSAStatusResponse, List[ITSAStatusResponseModel]]): Unit = {
    when(mockITSAStatusConnector.getITSAStatusDetail(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(taxYear),
      ArgumentMatchers.eq(futureYears), ArgumentMatchers.eq(history))(any()))
      .thenReturn(Future.successful(response))
  }

}

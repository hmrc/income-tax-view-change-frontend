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

package returns.mocks.connectors

import common.testUtils.UnitSpec
import returns.connectors.GetFinancialDetailsConnector
import returns.models.FinancialDetailsResponseModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Future

trait MockFinancialDetailsConnector extends UnitSpec with BeforeAndAfterEach {

  val mockFinancialDetailsConnector: GetFinancialDetailsConnector = mock(classOf[GetFinancialDetailsConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFinancialDetailsConnector)
  }

  def setupMockGetFinancialDetails(taxYear: Int, nino: String)(response: FinancialDetailsResponseModel): Unit = {
    when(mockFinancialDetailsConnector.getFinancialDetails(ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(nino))(any(),any()))
      .thenReturn(Future.successful(response))
  }
}

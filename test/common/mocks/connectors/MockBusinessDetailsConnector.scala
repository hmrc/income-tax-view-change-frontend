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

package common.mocks.connectors

import common.connectors.BusinessDetailsConnector
import common.models.incomeSourceDetails.IncomeSourceDetailsResponse
import common.testUtils.UnitSpec
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.Future

trait MockBusinessDetailsConnector extends UnitSpec with BeforeAndAfterEach {

  lazy val mockBusinessDetailsConnector: BusinessDetailsConnector = mock(classOf[BusinessDetailsConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockBusinessDetailsConnector)
  }
  
  def setupBusinessDetails(nino: String)(response: Future[IncomeSourceDetailsResponse]): Unit = {
    when(mockBusinessDetailsConnector.getBusinessDetails(ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
      .thenReturn(response)
  }
}

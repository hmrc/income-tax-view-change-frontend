/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.CalculationDataConnector
import models.CalculationDataResponseModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

trait MockCalculationDataConnector extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockCalculationDataConnector: CalculationDataConnector = mock[CalculationDataConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCalculationDataConnector)
  }

  def setupCalculationDataResponse(nino: String, taxCalculationId: String)(response: CalculationDataResponseModel): Unit =
    when(mockCalculationDataConnector
      .getCalculationData(
        ArgumentMatchers.eq(nino),
        ArgumentMatchers.eq(taxCalculationId))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))

}

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

import common.models.core.Nino
import common.testUtils.UnitSpec
import shared.connectors.CalculationListConnector
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfterEach
import shared.models.calculationList.CalculationListResponseModel

import scala.concurrent.Future

trait MockCalculationListConnector extends UnitSpec with BeforeAndAfterEach {

  val mockCalculationListConnector: CalculationListConnector = mock(classOf[CalculationListConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockCalculationListConnector)
  }

  def setupGetCalculationList(nino: String, taxYearEnd: String, mtditid: String)(response: CalculationListResponseModel): OngoingStubbing[Future[CalculationListResponseModel]] = {
    val normalisedTaxYear = taxYearEnd match {
      case s if s.matches("""\d{2}-\d{2}""") => s"20${s.takeRight(2)}"
      case other => other
    }
    when(mockCalculationListConnector.getCalculationList(
      Nino(ArgumentMatchers.eq(nino)),
      ArgumentMatchers.argThat((arg: String) => arg == taxYearEnd || arg == normalisedTaxYear),
      ArgumentMatchers.eq(mtditid)
    )(any()))
      .thenReturn(Future.successful(response))
  }
}

/*
 * Copyright 2021 HM Revenue & Customs
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

import connectors.IndividualCalculationsConnector
import models.calculation.CalculationResponseModel
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockIndividualCalculationsConnector extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockIndividualCalculationsConnector: IndividualCalculationsConnector = mock[IndividualCalculationsConnector]

  def mockGetLatestCalculationId(nino: String, taxYear: String)(response: Either[CalculationResponseModel, String]): Unit = {
    when(mockIndividualCalculationsConnector.getLatestCalculationId(
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(taxYear)
    )(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(response)
  }

  def mockGetCalculation(nino: String, id: String)(response: CalculationResponseModel): Unit = {
    when(mockIndividualCalculationsConnector.getCalculation(
      ArgumentMatchers.eq(nino),
      ArgumentMatchers.eq(id)
    )(ArgumentMatchers.any(), ArgumentMatchers.any())) thenReturn Future.successful(response)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIndividualCalculationsConnector)
  }

}

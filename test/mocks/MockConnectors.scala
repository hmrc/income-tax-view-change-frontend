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

package mocks

import connectors.{BusinessDetailsConnector, EstimatedTaxLiabilityConnector, ObligationDataConnector}
import models.{BusinessListResponseModel, ConnectorResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

sealed trait MockConnectors extends UnitSpec with MockitoSugar with BeforeAndAfterEach

trait MockEstimatedTaxLiabilityConnector extends MockConnectors {

    val mockEstimatedTaxLiabilityConnector: EstimatedTaxLiabilityConnector = mock[EstimatedTaxLiabilityConnector]

    override def beforeEach(): Unit = {
      super.beforeEach()
      reset(mockEstimatedTaxLiabilityConnector)
    }

    def setupMockFinancialDataResult(mtditid: String)(response: ConnectorResponseModel): Unit =
      when(mockEstimatedTaxLiabilityConnector.getEstimatedTaxLiability(ArgumentMatchers.eq(mtditid))(ArgumentMatchers.any()))
        .thenReturn(Future.successful(response))
}

trait MockObligationDataConnector extends MockConnectors {

  val mockObligationDataConnector: ObligationDataConnector = mock[ObligationDataConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockObligationDataConnector)
  }

  def setupMockObligation(nino: String, selfEmploymentId: String)(response: ConnectorResponseModel): Unit = {
    when(mockObligationDataConnector.getObligationData(ArgumentMatchers.eq(nino), ArgumentMatchers.eq(selfEmploymentId))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

}

trait MockBusinessDetilsConnector extends MockConnectors {

  val mockBusinessDetailsConnector: BusinessDetailsConnector = mock[BusinessDetailsConnector]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockBusinessDetailsConnector)
  }

  def setupMockBusinesslistResult(nino: String)(response: BusinessListResponseModel): Unit ={
    when(mockBusinessDetailsConnector.getBusinessList(ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

}

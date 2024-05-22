/*
 * Copyright 2024 HM Revenue & Customs
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

package mocks.services

import connectors.optout.ITSAStatusUpdateConnector
import models.incomeSourceDetails.TaxYear
import connectors.optout.OptOutUpdateRequestModel.{optOutUpdateReason, OptOutUpdateResponse}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockITSAStatusUpdateConnector extends UnitSpec with BeforeAndAfterEach {

  val mockOptOutConnector: ITSAStatusUpdateConnector = mock(classOf[ITSAStatusUpdateConnector])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOptOutConnector)
  }

  def setupMockRequestOptOutForTaxYear(taxYear: TaxYear, taxableEntityId: String)(out: OptOutUpdateResponse): Unit = {
    when(mockOptOutConnector.requestOptOutForTaxYear(ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(taxableEntityId), optOutUpdateReason)
      (any())).thenReturn(Future.successful(out))
  }

}
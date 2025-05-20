/*
 * Copyright 2025 HM Revenue & Customs
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

import enums.IncomeSourceJourney.{IncomeSourceType, JourneyState}
import enums.JourneyType.IncomeSourceJourneyType
import org.mockito.ArgumentMatchers.{any, matches}
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import play.api.mvc.{Result, Results}
import services.manageBusinesses.IncomeSourceRFService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockIncomeSourceRFService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockIncomeSourceRFService: IncomeSourceRFService = mock(classOf[IncomeSourceRFService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeSourceRFService)
  }

  def mockRedirectChecksForIncomeSourceRF(): Unit = {
    when(mockIncomeSourceRFService.redirectChecksForIncomeSourceRF(any(), any(), any(), any(), any(), any())(any())(any(), any()))
      .thenReturn(Future.successful(Results.Ok("Successful")))
  }

}

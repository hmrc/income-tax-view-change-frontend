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

import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, matches}
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import services.optIn.OptInService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockOptInService extends UnitSpec with BeforeAndAfterEach {

  val mockOptInService: OptInService = mock(classOf[OptInService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOptInService)
  }

  def mockSaveIntent(intent: TaxYear, isSuccessful: Boolean = true): Unit = {
    when(mockOptInService.saveIntent(ArgumentMatchers.eq(intent))(any(), any(), any())).thenReturn(Future.successful(isSuccessful))
  }

  def mockAvailableOptInTaxYear(choices: List[TaxYear]): Unit = {
    when(mockOptInService.availableOptInTaxYear()(any(), any(), any())).thenReturn(Future.successful(choices))
  }

  def mockFetchSavedChosenTaxYear(intent: TaxYear): Unit = {
    when(mockOptInService.fetchSavedChosenTaxYear()(any(), any(), any())).thenReturn(Future.successful(Some(intent)))
  }
}
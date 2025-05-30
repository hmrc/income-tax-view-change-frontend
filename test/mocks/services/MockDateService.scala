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
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import services.DateService
import testUtils.UnitSpec

trait MockDateService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockDateService: DateService = mock(classOf[DateService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockDateService)
  }

  def setupMockGetCurrentTaxYear(taxYear: TaxYear): Unit = {
    when(mockDateService.getCurrentTaxYear).thenReturn(taxYear)
  }

  def setupMockGetCurrentTaxYearEnd(taxYearEnd: Int): Unit = {
    when(mockDateService.getCurrentTaxYearEnd).thenReturn(taxYearEnd)
  }

  def setupMockGetCurrentTaxYearStart(startDate: java.time.LocalDate): Unit = {
    when(mockDateService.getCurrentTaxYearStart).thenReturn(startDate)
  }

}

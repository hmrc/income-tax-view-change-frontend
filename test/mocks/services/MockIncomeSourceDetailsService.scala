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

package mocks.services

import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsResponse}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import services.IncomeSourceDetailsService
import testConstants.BaseTestConstants.{testErrorMessage, testErrorStatus}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._

import scala.concurrent.Future


trait MockIncomeSourceDetailsService extends BeforeAndAfterEach {
  self: Suite =>

  lazy val mockIncomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeSourceDetailsService)
  }

  def setupMockGetIncomeSourceDetails(sources: IncomeSourceDetailsResponse): Unit = {
    when(
      mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
      .thenReturn(Future.successful(sources))
  }

  def mockSingleBusinessIncomeSource(userMigrated: Boolean = true): Unit = setupMockGetIncomeSourceDetails(
    if (userMigrated) {
      singleBusinessIncome
    } else {
      singleBusinessIncomeNotMigrated
    }
  )

  def mockSingleBusinessIncomeSourceNoLatency(): Unit =
    setupMockGetIncomeSourceDetails(singleBusinessIncomeNoLatency)

  def mockSingleBusinessIncomeSourceError(): Unit =
    setupMockGetIncomeSourceDetails(errorResponse)

  def ukPlusForeignPropertyWithSoleTraderIncomeSource(): Unit = setupMockGetIncomeSourceDetails(singleBusinessIncomeWithCurrentYear)

  def mockUkPlusForeignPlusSoleTraderNoLatency(): Unit = setupMockGetIncomeSourceDetails(ukPlusForeignPropertyAndSoleTraderNoLatency)

  def mockUkPlusForeignPlusSoleTraderWithLatency(): Unit = setupMockGetIncomeSourceDetails(ukPlusForeignPropertyAndSoleTraderWithLatency)

  def mockUkPlusForeignPlusSoleTraderWithLatencyExpired(): Unit = setupMockGetIncomeSourceDetails(ukPlusForeignPropertyAndSoleTraderWithLatencyExpired)

  def mockUkPlusForeignPlusSoleTraderWithLatencyAnnual(): Unit = setupMockGetIncomeSourceDetails(ukPlusForeignPropertyAndSoleTraderWithLatencyAnnual)

  def mockUkPlusForeignPlusSoleTrader2023WithLatencyAndUnknowns(): Unit = setupMockGetIncomeSourceDetails(ukPlusForeignPropertyAndSoleTrader2023WithUnknowns)

  def mockSingleBISWithCurrentYearAsMigrationYear(): Unit = setupMockGetIncomeSourceDetails(singleBusinessIncomeWithCurrentYear)

  def mockBusinessIncomeSource(): Unit = setupMockGetIncomeSourceDetails(businessIncome)

  def mockBusinessIncomeSourceWithAccruals(): Unit = setupMockGetIncomeSourceDetails(businessIncome2)

  def mockPropertyIncomeSource(): Unit = setupMockGetIncomeSourceDetails(propertyIncomeOnly)

  def mockUKPropertyIncomeSource(): Unit = setupMockGetIncomeSourceDetails(ukPropertyIncome)

  def mockUKPropertyIncomeSourceWithCeasedUkProperty(): Unit = setupMockGetIncomeSourceDetails(ukPropertyIncomeWithCeasedUkPropertyIncome)

  def mockTwoActiveUkPropertyIncomeSourcesErrorScenario(): Unit = setupMockGetIncomeSourceDetails(twoActiveUkPropertyIncomes)

  def mockForeignPropertyIncomeSource(): Unit = setupMockGetIncomeSourceDetails(foreignPropertyIncome)

  def mockTwoActiveForeignPropertyIncomeSourcesErrorScenario(): Unit = setupMockGetIncomeSourceDetails(twoActiveForeignPropertyIncomes)

  def mockForeignPropertyIncomeSourceWithCeasedForeignProperty(): Unit = setupMockGetIncomeSourceDetails(foreignPropertyIncomeWithCeasedForiegnPropertyIncome)

  def mockBothIncomeSources(): Unit = setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

  def mockUkPropertyWithSoleTraderBusiness(): Unit = setupMockGetIncomeSourceDetails(ukPropertyWithSoleTraderBusiness)

  def mockNoIncomeSources(): Unit = setupMockGetIncomeSourceDetails(noIncomeDetails)

  def mockBothIncomeSourcesBusinessAligned(): Unit = setupMockGetIncomeSourceDetails(businessAndPropertyAligned)

  def mockErrorIncomeSource(): Unit = setupMockGetIncomeSourceDetails(IncomeSourceDetailsError(testErrorStatus, testErrorMessage))

  def mockBothPropertyBothBusiness(): Unit = setupMockGetIncomeSourceDetails(ukPlusForeignPropertyAndSoleTraderPlusCeasedBusinessIncome)

  def mockSoleTraderWithStartDate2005(): Unit = setupMockGetIncomeSourceDetails(soleTraderWithStartDate2005)

  def mockBothPropertyBothBusinessWithLatency(): Unit = setupMockGetIncomeSourceDetails(ukPlusForeignPropertyAndSoleTraderWithLatency)

  def mockBusinessIncomeSourceWithLatency2023(): Unit = setupMockGetIncomeSourceDetails(singleBusinessIncome2023)

  def mockBusinessIncomeSourceWithLatency2023AndUnknownValues(): Unit = setupMockGetIncomeSourceDetails(singleBusinessIncome2023WithUnknowns)

  def mockBusinessIncomeSourceWithLatency2024(): Unit = setupMockGetIncomeSourceDetails(singleBusinessIncome2024)

  def mockUKPropertyIncomeSourceWithLatency2023(): Unit = setupMockGetIncomeSourceDetails(singleUKPropertyIncome2023)

  def mockForeignPropertyIncomeSourceWithLatency2023(): Unit = setupMockGetIncomeSourceDetails(singleForeignPropertyIncome2023)

  def mockUKPropertyIncomeSourceWithLatency2024(): Unit = setupMockGetIncomeSourceDetails(singleUKPropertyIncome2024)

  def mockForeignPropertyIncomeSourceWithLatency2024(): Unit = setupMockGetIncomeSourceDetails(singleForeignPropertyIncome2024)
}
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

import testConstants.BaseTestConstants.{testErrorMessage, testErrorStatus}
import testConstants.IncomeSourceDetailsTestConstants._
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.mockito.Mockito.mock
import services.IncomeSourceDetailsService

import scala.concurrent.Future


trait MockIncomeSourceDetailsService extends BeforeAndAfterEach {
  self: Suite =>

  val mockIncomeSourceDetailsService: IncomeSourceDetailsService = mock(classOf[IncomeSourceDetailsService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeSourceDetailsService)
  }

  def setupMockGetIncomeSourceDetails()(sources: IncomeSourceDetailsResponse): Unit = {
    when(
      mockIncomeSourceDetailsService.getIncomeSourceDetails(ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(sources))
  }

  def mockSingleBusinessIncomeSource(userMigrated : Boolean = true): Unit = setupMockGetIncomeSourceDetails()(
    if (userMigrated){
      singleBusinessIncome
    } else {
      singleBusinessIncomeNotMigrated
    }
  )

  def mockSingleBusinessIncomeSourceError(): Unit = setupMockGetIncomeSourceDetails()(
    errorResponse
  )

  def ukPlusForeignPropertyWithSoleTraderIncomeSource(): Unit = setupMockGetIncomeSourceDetails()(singleBusinessIncomeWithCurrentYear)

  def mockSingleBISWithCurrentYearAsMigrationYear(): Unit = setupMockGetIncomeSourceDetails()(singleBusinessIncomeWithCurrentYear)

  def mockBusinessIncomeSource(): Unit = setupMockGetIncomeSourceDetails()(businessIncome)

  def mockBusinessIncomeSourceWithAccruals(): Unit = setupMockGetIncomeSourceDetails()(businessIncome2)

  def mockBusinessIncomeSourceWithCashAndAccruals(): Unit = setupMockGetIncomeSourceDetails()(businessIncome3)

  def mockBusinessIncomeSourceMissingCashOrAccrualsField(): Unit = setupMockGetIncomeSourceDetails()(businessIncome4)

  def mockPropertyIncomeSource(): Unit = setupMockGetIncomeSourceDetails()(propertyIncomeOnly)

  def mockUKPropertyIncomeSource(): Unit = setupMockGetIncomeSourceDetails()(ukPropertyIncome)

  def mockForeignPropertyIncomeSource(): Unit = setupMockGetIncomeSourceDetails()(foreignPropertyIncome)

  def mockBothIncomeSources(): Unit = setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

  def mockUkPropertyWithSoleTraderBusiness(): Unit = setupMockGetIncomeSourceDetails()(ukPropertyWithSoleTraderBusiness)
  def mockNoIncomeSources(): Unit = setupMockGetIncomeSourceDetails()(noIncomeDetails)

  def mockBothIncomeSourcesBusinessAligned(): Unit = setupMockGetIncomeSourceDetails()(businessAndPropertyAligned)

  def mockErrorIncomeSource(): Unit = setupMockGetIncomeSourceDetails()(IncomeSourceDetailsError(testErrorStatus, testErrorMessage))

  def mockBothPropertyBothBusiness(): Unit = setupMockGetIncomeSourceDetails()(ukPlusForeignPropertyAndSoleTraderPlusCeasedBusinessIncome)
}
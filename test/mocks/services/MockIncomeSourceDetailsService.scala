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

package mocks.services

import assets.BaseTestConstants.{testErrorMessage, testErrorStatus}
import assets.IncomeSourceDetailsTestConstants._
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import services.IncomeSourceDetailsService

import scala.concurrent.Future


trait MockIncomeSourceDetailsService extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  val mockIncomeSourceDetailsService: IncomeSourceDetailsService = mock[IncomeSourceDetailsService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeSourceDetailsService)
  }

  def setupMockGetIncomeSourceDetails()(sources: IncomeSourceDetailsResponse): Unit = {
    when(
      mockIncomeSourceDetailsService.getIncomeSourceDetails()(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(sources))
  }

  def mockSingleBusinessIncomeSource(): Unit = setupMockGetIncomeSourceDetails()(singleBusinessIncome)

  def mockSingleBISWithCurrentYearAsMigrationYear(): Unit = setupMockGetIncomeSourceDetails()(singleBusinessIncomeWithCurrentYear)

  def mockPropertyIncomeSource(): Unit = setupMockGetIncomeSourceDetails()(propertyIncomeOnly)

  def mockBothIncomeSources(): Unit = setupMockGetIncomeSourceDetails()(businessesAndPropertyIncome)

  def mockNoIncomeSources(): Unit = setupMockGetIncomeSourceDetails()(noIncomeDetails)

  def mockBothIncomeSourcesBusinessAligned(): Unit = setupMockGetIncomeSourceDetails()(businessAndPropertyAligned)

  def mockErrorIncomeSource(): Unit = setupMockGetIncomeSourceDetails()(IncomeSourceDetailsError(testErrorStatus, testErrorMessage))

}
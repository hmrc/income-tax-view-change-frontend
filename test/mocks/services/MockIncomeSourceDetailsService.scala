/*
 * Copyright 2018 HM Revenue & Customs
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

import assets.TestConstants.{IncomeSources, testMtditid, testNino}
import models.core.ErrorModel
import models.incomeSourceDetails.BusinessDetailsModel
import models.incomeSourcesWithDeadlines.{IncomeSourcesWithDeadlinesError, IncomeSourcesWithDeadlinesResponse}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Suite}
import services.IncomeSourceDetailsService

import scala.concurrent.Future


trait MockIncomeSourceDetailsService extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  val mockIncomeSourceDetailsService: IncomeSourceDetailsService = mock[IncomeSourceDetailsService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockIncomeSourceDetailsService)
  }

  def setupMockGetIncomeSourceDetails(mtditid: String, nino: String)(sources: IncomeSourcesWithDeadlinesResponse): Unit = {
    when(
      mockIncomeSourceDetailsService.getIncomeSourceDetails(ArgumentMatchers.eq(mtditid), ArgumentMatchers.eq(nino))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(sources))
  }

  def setupMockGetBusinessDetails(mtditid: String, id: Int)(sources: Either[ErrorModel, Option[(BusinessDetailsModel, Int)]]): Unit = {
    when(mockIncomeSourceDetailsService.getBusinessDetails(ArgumentMatchers.eq(mtditid), ArgumentMatchers.eq(id))(ArgumentMatchers.any()))
      .thenReturn(Future.successful(sources))
  }

  def mockSingleBusinessIncomeSource(): Unit = setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSources.businessIncomeSourceSuccess)
  def mockPropertyIncomeSource(): Unit = setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSources.propertyIncomeSourceSuccess)
  def mockBothIncomeSources(): Unit = setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSources.bothIncomeSourceSuccessMisalignedTaxYear)
  def mockNoIncomeSources(): Unit = setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSources.noIncomeSourceSuccess)
  def mockBothIncomeSourcesBusinessAligned(): Unit =
    setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSources.bothIncomeSourcesSuccessBusinessAligned)
  def mockErrorIncomeSource(): Unit = setupMockGetIncomeSourceDetails(testMtditid, testNino)(IncomeSourcesWithDeadlinesError)

}
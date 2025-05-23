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

import models.financialDetails.{FinancialDetailsModel, FinancialDetailsResponseModel}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.FinancialDetailsService
import testConstants.BaseTestConstants.{testNino, testTaxYear}
import testConstants.FinancialDetailsTestConstants._
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockFinancialDetailsService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockFinancialDetailsService: FinancialDetailsService = mock(classOf[FinancialDetailsService])


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFinancialDetailsService)
  }

  def setupMockGetFinancialDetails(taxYear: Int)(response: FinancialDetailsResponseModel): Unit =
    when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(testNino))
    (any(), any())).thenReturn(Future.successful(response))

  def setupMockGetFinancialDetailsWithTaxYearAndNino(taxYear: Int, nino: String)(response: FinancialDetailsResponseModel): Unit = {
    when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(nino))
    (any(), any())).thenReturn(Future.successful(response))
  }

  def mockFinancialDetailsSuccess(financialDetailsModelResponse: FinancialDetailsModel = financialDetailsModel(), taxYear: Int = testTaxYear): Unit =
    setupMockGetFinancialDetails(taxYear)(financialDetailsModelResponse)

  def mockFinancialDetailsFailed(): Unit =
    setupMockGetFinancialDetails(testTaxYear)(testFinancialDetailsErrorModel)

  def mockFinancialDetailsNotFound(): Unit =
    setupMockGetFinancialDetails(testTaxYear)(testFinancialDetailsNotFoundErrorModel)

  def mockGetAllFinancialDetails(response: List[(Int, FinancialDetailsResponseModel)]): Unit = {
    when(mockFinancialDetailsService.getAllFinancialDetails(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockGetAllUnpaidFinancialDetails(response: FinancialDetailsModel = financialDetailsDueInMoreThan30Days()): Unit = {
    when(mockFinancialDetailsService.getAllUnpaidFinancialDetails()(any(), any(), any()))
      .thenReturn(Future.successful(List(response)))
  }
}

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

import java.time.LocalDate

import assets.BaseTestConstants.{testNino, testTaxYear}
import assets.FinancialDetailsTestConstants._
import models.financialDetails.FinancialDetailsResponseModel
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.FinancialDetailsService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

trait MockFinancialDetailsService extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockFinancialDetailsService: FinancialDetailsService = mock[FinancialDetailsService]


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockFinancialDetailsService)
  }

  def setupMockGetFinancialDetails(taxYear: Int)(response: FinancialDetailsResponseModel): Unit =
    when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(testNino))
    (ArgumentMatchers.any())).thenReturn(Future.successful(response))

  def setupMockGetFinancialDetails(taxYear: Int, nino: String)(response: FinancialDetailsResponseModel): Unit = {
    when(mockFinancialDetailsService.getFinancialDetails(ArgumentMatchers.eq(taxYear), ArgumentMatchers.eq(nino))
    (ArgumentMatchers.any())).thenReturn(Future.successful(response))
  }

  def mockFinancialDetailsSuccess(taxYear: LocalDate = LocalDate.of(2018, 4, 5)): Unit =
    setupMockGetFinancialDetails(testTaxYear)(financialDetailsModel(taxYear.getYear))

  def mockFinancialDetailsFailed(): Unit =
    setupMockGetFinancialDetails(testTaxYear)(testFinancialDetailsErrorModel)

  def mockFinancialDetailsNotFound(): Unit =
    setupMockGetFinancialDetails(testTaxYear)(testFinancialDetailsNotFoundErrorModel)

  def mockGetAllFinancialDetails(response: List[(Int, FinancialDetailsResponseModel)]): Unit = {
    when(mockFinancialDetailsService.getAllFinancialDetails(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockGetChargeDueDates(response: Future[Option[Either[(LocalDate, Boolean), Int]]]): Unit = {
    when(mockFinancialDetailsService.getChargeDueDates(any(), any()))
      .thenReturn(response)
  }
}

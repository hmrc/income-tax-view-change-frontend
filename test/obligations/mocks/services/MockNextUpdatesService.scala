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

package obligations.mocks.services

import common.implicits.ImplicitDateFormatter
import obligations.services.NextUpdatesService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import businessDetails.testConstants.IncomeSourcesWithDeadlinesTestConstants.*
import common.models.obligations.ObligationsResponseModel
import common.testUtils.UnitSpec

import scala.concurrent.Future


trait MockNextUpdatesService extends UnitSpec with BeforeAndAfterEach with ImplicitDateFormatter {

  lazy val mockNextUpdatesService: NextUpdatesService = mock(classOf[NextUpdatesService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockNextUpdatesService)
  }

  def setupMockNextUpdatesResult()(response: ObligationsResponseModel): Unit = {
    when(mockNextUpdatesService.getOpenObligations()(any(), any()))
      .thenReturn(Future.successful(response))
  }

  def mockSingleBusinessIncomeSourceWithDeadlines(): Unit = setupMockNextUpdatesResult()(singleBusinessIncomeWithDeadlines)

  def mockPropertyIncomeSourceWithDeadlines(): Unit = setupMockNextUpdatesResult()(propertyIncomeOnlyWithDeadlines)

  def mockNoIncomeSourcesWithDeadlines(): Unit = setupMockNextUpdatesResult()(noIncomeDetailsWithNoDeadlines)

  def mockBothIncomeSourcesBusinessAlignedWithDeadlines(): Unit = setupMockNextUpdatesResult()(businessAndPropertyAlignedWithDeadlines)

}

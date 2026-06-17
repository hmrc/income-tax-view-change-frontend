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

package businessDetails.mocks.services

import businessDetails.models.triggeredMigration.viewModels.CheckHmrcRecordsViewModel
import businessDetails.services.triggeredMigration.TriggeredMigrationService
import common.testUtils.UnitSpec
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach

trait MockTriggeredMigrationService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockTriggeredMigrationService: TriggeredMigrationService = mock(classOf[TriggeredMigrationService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockTriggeredMigrationService)
  }

  def mockGetCheckHmrcRecordsViewModel(out: CheckHmrcRecordsViewModel): Unit = {
    when(mockTriggeredMigrationService.getCheckHmrcRecordsViewModel(any(), any()))
      .thenReturn(out)
  }
}

/*
 * Copyright 2022 HM Revenue & Customs
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

import models.btaNavBar.ListLinks
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.INTERNAL_SERVER_ERROR
import services.BtaNavBarService
import testConstants.BaseTestConstants.testListLink

trait MockBtaNavBarService extends BeforeAndAfterEach with MockitoSugar {
  self: Suite =>

  val mockBtaNavBarService: BtaNavBarService = mock[BtaNavBarService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockBtaNavBarService)
  }

  def setupMockGetBtaNavBar()(sources: Seq[ListLinks]): Unit = {
    when(
      mockBtaNavBarService.partialList(ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(sources)
  }

  def mockErrorBtaNavBar(): Unit = setupMockGetBtaNavBar()(testListLink)

}

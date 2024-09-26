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

package mocks.repositories

import models.incomeSourceDetails.TaxYear
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, reset, when}
import org.scalatest.BeforeAndAfterEach
import repositories.OptOutSessionDataRepository
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockOptOutSessionDataRepository extends UnitSpec with BeforeAndAfterEach {

  val mockOptOutSessionDataRepository: OptOutSessionDataRepository = mock(classOf[OptOutSessionDataRepository])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockOptOutSessionDataRepository)
  }

  def mockSaveIntent(taxYear: TaxYear, out: Future[Boolean]): Unit = {
    when(mockOptOutSessionDataRepository.saveIntent(ArgumentMatchers.eq(taxYear))(any(), any())).thenReturn(out)
  }

  def mockFetchIntent(out: Future[Option[TaxYear]]): Unit = {
    when(mockOptOutSessionDataRepository.fetchSavedIntent()(any(), any())).thenReturn(out)
  }

}

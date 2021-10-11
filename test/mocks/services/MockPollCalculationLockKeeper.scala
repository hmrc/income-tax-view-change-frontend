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

import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import repositories.MongoLockRepository
import testUtils.UnitSpec
import uk.gov.hmrc.lock.LockRepository
import uk.gov.hmrc.mongo.MongoSpecSupport
import utils.PollCalculationLockKeeper

import scala.concurrent.Future

trait MockPollCalculationLockKeeper extends UnitSpec with MongoSpecSupport with MockitoSugar with BeforeAndAfterEach {

  val testCalcId: String = "1234567890"

  val mockLockRepository: LockRepository = mock[LockRepository]

  val mockMongoLockRepository: MongoLockRepository = new MongoLockRepository {
    override val repo: LockRepository = mockLockRepository
  }

  val mockPollCalculationLockKeeper = mock[PollCalculationLockKeeper]


  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockLockRepository)
    reset(mockPollCalculationLockKeeper)
  }

  def setupLockRepositoryLock(locked: Boolean): Unit = when(mockLockRepository.lock(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
    .thenReturn(Future.successful(locked))

  def mockLockRepositoryIsLockedTrue(): Unit = setupLockRepositoryLock(true)
  def mockLockRepositoryIsLockedFalse(): Unit = setupLockRepositoryLock(false)


}

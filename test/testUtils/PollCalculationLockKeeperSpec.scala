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

package testUtils

import mocks.services.MockPollCalculationLockKeeper
import org.joda.time.Duration
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{atLeastOnce, verify, when}
import uk.gov.hmrc.lock.LockRepository
import utils.PollCalculationLockKeeper

import scala.concurrent.Future

class PollCalculationLockKeeperSpec extends TestSupport with MockPollCalculationLockKeeper {

  class TestableLockKeeper extends PollCalculationLockKeeper {
    val repo: LockRepository = mockLockRepository
    val lockId = "lockId"
    val forceLockReleaseAfter = Duration.millis(1000L)
  }

  val lockKeeper = new TestableLockKeeper

  "lockKeeper is locked" should {
    "return true if a lock exists in the repo" in {
      when(mockLockRepository.isLocked(ArgumentMatchers.eq("lockId"), ArgumentMatchers.any())).thenReturn(Future.successful(true))
      val result = lockKeeper.isLocked
      result.futureValue shouldBe true
    }

    "return false if a lock does not exist in the repo" in {
      when(mockLockRepository.isLocked(ArgumentMatchers.eq("lockId"), ArgumentMatchers.any())).thenReturn(Future.successful(false))
      val result = lockKeeper.isLocked
      result.futureValue shouldBe false
    }
  }

  "lockKeeper lock" should {
    "be acquired if the lock does not exist" in {
      when(mockLockRepository.lock(ArgumentMatchers.eq("lockId"), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(false))

      val result = lockKeeper.tryLock()(ec)
      result.futureValue shouldBe false
    }

    "fail when the repo throws an exception" in {
      when(mockLockRepository.lock(ArgumentMatchers.eq("lockId"), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new RuntimeException("test exception")))
      when(mockLockRepository.releaseLock(ArgumentMatchers.eq("lockId"), ArgumentMatchers.any())).thenReturn(Future.successful(()))

      val result = lockKeeper.tryLock()(ec).failed.futureValue

      verify(mockLockRepository, atLeastOnce()).releaseLock(ArgumentMatchers.eq("lockId"), ArgumentMatchers.any())
      result.isInstanceOf[RuntimeException] shouldBe true
      result.getMessage shouldBe "test exception"
    }
  }
}

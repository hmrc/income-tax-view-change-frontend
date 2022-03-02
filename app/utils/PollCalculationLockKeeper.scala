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

package utils

import uk.gov.hmrc.lock.LockRepository

trait PollCalculationLockKeeper {

  import org.joda.time.Duration

  import java.util.UUID
  import scala.concurrent.{ExecutionContext, Future}

  def repo: LockRepository

  def lockId: String

  val forceLockReleaseAfter: Duration

  lazy val serverId: String = UUID.randomUUID().toString

  def tryLock()(implicit ec: ExecutionContext): Future[Boolean] = {
    repo.lock(lockId, serverId, forceLockReleaseAfter)
      .recoverWith { case ex => repo.releaseLock(lockId, serverId).flatMap(_ => Future.failed(ex)) }
  }

  def releaseLock(implicit ec: ExecutionContext): Future[Unit] = repo.releaseLock(lockId, serverId)

  def isLocked: Future[Boolean] = repo.isLocked(lockId, serverId)

}

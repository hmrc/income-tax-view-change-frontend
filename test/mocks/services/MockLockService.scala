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

import testUtils.TestSupport
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}

import scala.concurrent.duration.{Duration, FiniteDuration, MILLISECONDS}
import scala.concurrent.{ExecutionContext, Future}

trait MockLockService extends LockService with TestSupport {
  lazy val mockMongoLockRepository: MongoLockRepository = app.injector.instanceOf[MongoLockRepository]
  val lockRepository: MongoLockRepository = mockMongoLockRepository
  val lockId: String = "calc-poller"
  val ttl: FiniteDuration = Duration.create(100, MILLISECONDS)
}
class LockServiceDidNotAcquireMongoLock extends MockLockService {
  override def withLock[T](body: => Future[T])(implicit ec : ExecutionContext): Future[Option[T]] =
    Future.successful(None)
}
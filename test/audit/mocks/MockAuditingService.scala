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

package audit.mocks

import akka.dispatch.Dispatcher
import akka.stream.ActorAttributes.Dispatcher
import audit.AuditingService
import audit.models.{AuditModel, ExtendedAuditModel}
import org.mockito.Mockito._
import org.mockito.{AdditionalMatchers, ArgumentMatchers}
import org.scalatest.BeforeAndAfterEach
import testUtils.TestSupport
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait MockAuditingService extends TestSupport with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockAuditingService)
  }

  val mockAuditingService: AuditingService = mock[AuditingService]

  def verifyAudit(model: AuditModel, path: Option[String] = None): Unit = {
    verify(mockAuditingService).audit(
      ArgumentMatchers.eq(model),
      AdditionalMatchers.or(ArgumentMatchers.eq(path), ArgumentMatchers.isNull)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[ExecutionContext]
    )
  }

  def verifyExtendedAudit(model: ExtendedAuditModel, path: Option[String] = None): Unit =
    verify(mockAuditingService).extendedAudit(
      ArgumentMatchers.eq(model),
      AdditionalMatchers.or(ArgumentMatchers.eq(path), ArgumentMatchers.isNull)
    )(
      ArgumentMatchers.any[HeaderCarrier],
      ArgumentMatchers.any[ExecutionContext]
    )
}
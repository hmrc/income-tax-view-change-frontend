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

import org.mockito.ArgumentMatchers.{any, eq => matches}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import services.agent.ClientRelationshipService
import services.agent.ClientRelationshipService.{ClientDetails, ClientRelationshipFailure}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

trait MockClientRelationshipService extends UnitSpec with MockitoSugar with BeforeAndAfterEach {

  val mockClientRelationshipService: ClientRelationshipService = mock[ClientRelationshipService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockClientRelationshipService)
  }

  def mockCheckAgentClientRelationship(utr: String, arn: String)(response: Either[ClientRelationshipFailure, ClientDetails]): Unit = {
    when(mockClientRelationshipService.checkAgentClientRelationship(matches(utr), matches(arn), any())(any()))
      .thenReturn(Future.successful(response))
  }

}

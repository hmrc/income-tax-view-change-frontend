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

import config.FrontendAppConfig
import enums.JourneyType.Operation
import models.incomeSourceDetails.UIJourneySessionData
import org.bson.BsonValue
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import repositories.{SensitiveUIJourneySessionDataRepository, UIJourneySessionDataRepository}
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockUIJourneySessionDataRepository extends UnitSpec with BeforeAndAfterEach {

  val mockUIJourneySessionDataRepository: UIJourneySessionDataRepository = mock(classOf[UIJourneySessionDataRepository])

  val mockSensitiveUIJourneySessionDataRepository: SensitiveUIJourneySessionDataRepository = mock(classOf[SensitiveUIJourneySessionDataRepository])

  val mockFrontendAppConfig: FrontendAppConfig = mock(classOf[FrontendAppConfig])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUIJourneySessionDataRepository)
  }

  def mockRepositoryGet(response: Option[UIJourneySessionData]): Unit = {
    when(mockUIJourneySessionDataRepository.get(ArgumentMatchers.anyString(), ArgumentMatchers.anyString()))
      .thenReturn(Future.successful(response))
  }

  def mockRepositorySet(response: Boolean): Unit = {
    when(mockUIJourneySessionDataRepository.set(ArgumentMatchers.any()))
      .thenReturn(Future.successful(response))
  }

  def mockRepositoryUpdateData(): Unit = {
    when(mockUIJourneySessionDataRepository.updateData(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(new org.mongodb.scala.result.UpdateResult {
        override def wasAcknowledged(): Boolean = true

        override def getMatchedCount: Long = 4

        override def getModifiedCount: Long = 5

        override def getUpsertedId: BsonValue = null
      }))
  }

  def mockDeleteOne(): Unit = {
    when(mockUIJourneySessionDataRepository.deleteOne(any[UIJourneySessionData]())).thenReturn(Future.successful(true))
  }

  def mockDeleteSession(): Unit = {
    when(mockUIJourneySessionDataRepository.deleteJourneySession(anyString(), any[Operation]())).thenReturn(Future.successful(true))
  }
}

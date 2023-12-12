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

import enums.JourneyType.JourneyType
import models.incomeSourceDetails.UIJourneySessionData
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.SessionService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockSessionService extends UnitSpec with BeforeAndAfterEach {

  val mockSessionService: SessionService = mock(classOf[SessionService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionService)
  }


  def setupMockCreateSession(result: Boolean): Unit =
    when(
      mockSessionService.createSession(ArgumentMatchers.anyString())(ArgumentMatchers.any(), ArgumentMatchers.any()))
      .thenReturn(Future.successful(result))

  def setupMockGetSessionKeyMongo(result: Either[Throwable, Option[String]]): Unit = {
    when(
      mockSessionService.getMongoKey(ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockGetSessionKeyMongoTyped[A](result: Either[Throwable, Option[A]]): Unit = {
    when(
      mockSessionService.getMongoKeyTyped[A](ArgumentMatchers.anyString(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockGetSessionKeyMongoTyped[A](key: String, journeyType: JourneyType, result: Either[Throwable, Option[A]]): Unit = {
    when(
      mockSessionService.getMongoKeyTyped[A](ArgumentMatchers.eq(key), ArgumentMatchers.eq(journeyType))(ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockSetSessionKeyMongo(result: Either[Throwable, Boolean]): Unit = {
    when(
      mockSessionService.setMongoKey(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockGetMongo(result: Either[Throwable, Option[UIJourneySessionData]]): Unit = {
    when(
      mockSessionService.getMongo(ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockSetMongoData(result: Boolean): Unit = {
    when(
      mockSessionService.setMongoData(ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockSetSessionKeyMongo(expectedKey: String,
                                  expectedValue: String,
                                  expectedJourneyType: JourneyType,
                                  result: Either[Throwable, Boolean]): Unit = {
    when(mockSessionService.setMongoKey(
      ArgumentMatchers.eq(expectedKey),
      ArgumentMatchers.eq(expectedValue),
      ArgumentMatchers.eq(expectedJourneyType)
    )(any(), any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockDeleteSession(result: Boolean): Unit = {
    when(
      mockSessionService.deleteSession(ArgumentMatchers.any())(ArgumentMatchers.any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockSetSessionKeyMongo(key: String)(result: Either[Throwable, Boolean]): Unit = {
    when(
      mockSessionService.setMongoKey(ArgumentMatchers.eq(key), ArgumentMatchers.any(), ArgumentMatchers.any())
      (ArgumentMatchers.any(), ArgumentMatchers.any())
    ).thenReturn(Future.successful(result))
  }

  def verifyMockGetMongoKeyResponse(noOfCalls: Int) =
    verify(mockSessionService, times(noOfCalls)).getMongoKey(ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

  def verifyMockGetMongoKeyTypedResponse[A](noOfCalls: Int) =
    verify(mockSessionService, times(noOfCalls)).getMongoKeyTyped[A](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())

  def verifyMockSetMongoKeyResponse(noOfCalls: Int) =
    verify(mockSessionService, times(noOfCalls)).setMongoKey(ArgumentMatchers.anyString(), ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())
}

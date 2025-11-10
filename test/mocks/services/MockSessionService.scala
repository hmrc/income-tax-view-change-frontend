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

import enums.JourneyType.IncomeSourceJourneyType
import models.UIJourneySessionData
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import services.SessionService
import testUtils.UnitSpec

import scala.concurrent.Future

trait MockSessionService extends UnitSpec with BeforeAndAfterEach {

  lazy val mockSessionService: SessionService = mock(classOf[SessionService])

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockSessionService)
  }

  def setupMockCreateSession(result: Boolean): Unit =
    when(
      mockSessionService.createSession(any())(any()))
      .thenReturn(Future.successful(result))

  def setupMockGetSessionKeyMongo(result: Either[Throwable, Option[String]]): Unit = {
    when(
      mockSessionService.getMongoKey(anyString(), any())(any(), any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockGetSessionKeyMongoTyped[A](result: Either[Throwable, Option[A]]): Unit = {
    when(
      mockSessionService.getMongoKeyTyped[A](anyString(), any())(any(), any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockGetSessionKeyMongoTyped[A](key: String, journeyType: IncomeSourceJourneyType, result: Either[Throwable, Option[A]]): Unit = {
    when(
      mockSessionService.getMongoKeyTyped[A](ArgumentMatchers.eq(key), ArgumentMatchers.eq(journeyType))(any(), any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockSetSessionKeyMongo(result: Either[Throwable, Boolean]): Unit = {
    when(
      mockSessionService.setMongoKey(anyString(), any(), any())
      (any(), any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockSetMultipleMongoData(key: Map[String, String])(result: Either[Throwable, Boolean]): Unit = {
    when(
      mockSessionService.setMultipleMongoData(ArgumentMatchers.eq(key), any())
      (any(), any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockSetMultipleMongoData(result: Either[Throwable, Boolean]): Unit = {
    when(
      mockSessionService.setMultipleMongoData(any(), any())
      (any(), any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockGetMongo(result: Either[Throwable, Option[UIJourneySessionData]]): Unit = {
    when(
      mockSessionService.getMongo(any())(any(), any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockSetMongoData(result: Boolean): Unit = {
    when(
      mockSessionService.setMongoData(any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockSetSessionKeyMongo(expectedKey: String,
                                  expectedValue: String,
                                  expectedJourneyType: IncomeSourceJourneyType,
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
      mockSessionService.deleteSession(any())(any())
    ).thenReturn(Future.successful(result))
  }

  def setupMockClearSession(result: Boolean): Unit = {
    when(
      mockSessionService.clearSession(any())(any())).thenReturn(Future.successful(()))
  }

  def setupMockSetSessionKeyMongo(key: String)(result: Either[Throwable, Boolean]): Unit = {
    when(
      mockSessionService.setMongoKey(ArgumentMatchers.eq(key), any(), any())
      (any(), any())
    ).thenReturn(Future.successful(result))
  }

  def verifyMockGetMongoKeyResponse(noOfCalls: Int) =
    verify(mockSessionService, times(noOfCalls)).getMongoKey(any(), any())(any(), any())

  def verifyMockGetMongoKeyTypedResponse[A](noOfCalls: Int) =
    verify(mockSessionService, times(noOfCalls)).getMongoKeyTyped[A](any(), any())(any(), any())

  def verifyMockSetMongoKeyResponse(noOfCalls: Int) =
    verify(mockSessionService, times(noOfCalls)).setMongoKey(anyString(), any(), any())(any(), any())

  def verifyMockSetMultipleMongoDataResponse(noOfCalls: Int) =
    verify(mockSessionService, times(noOfCalls)).setMultipleMongoData(any(), any())(any(), any())
}
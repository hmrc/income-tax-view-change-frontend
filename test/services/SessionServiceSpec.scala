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

package services

import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, Cease, IncomeSourceJourneyType, Manage}
import mocks.repositories.MockUIJourneySessionDataRepository
import models.UIJourneySessionData
import models.incomeSourceDetails.AddIncomeSourceData
import org.mockito.Mockito.when
import testUtils.TestSupport

import java.time.LocalDate
import scala.concurrent.Future

class SessionServiceSpec extends TestSupport with MockUIJourneySessionDataRepository {

  object TestSessionService extends SessionService(
    mockUIJourneySessionDataRepository,
    mockSensitiveUIJourneySessionDataRepository,
    mockFrontendAppConfig
  )

  "sessionService " when {
    "mongo" when {
      "createSession method" should {
        "successfully create a session, returning a Future Boolean" in {
          mockRepositorySet(response = true)

          val result = TestSessionService.createSession(IncomeSourceJourneyType(Add, SelfEmployment)).futureValue

          result shouldBe true
        }
      }

      "getMongo method " should {
        "return the correct session value for given key" in {
          val sessionData = UIJourneySessionData("session-123456", "ADD-SE")
          mockRepositoryGet(Some(sessionData))
          TestSessionService.getMongo(IncomeSourceJourneyType(Add, SelfEmployment))(headerCarrier, ec).futureValue shouldBe Right(Some(sessionData))
        }

        "return the correct session value for given key if encryption is enabled" in {
          val sessionData = UIJourneySessionData("session-123456", "ADD-SE")
          when(mockFrontendAppConfig.encryptionIsEnabled).thenReturn(true)
          mockRepositoryGet(Some(sessionData), isSensitive = true)
          TestSessionService.getMongo(IncomeSourceJourneyType(Add, SelfEmployment))(headerCarrier, ec).futureValue shouldBe Right(Some(sessionData))
        }

      }

      "getMongoKey method " should {
        "return the correct session value for given key" in {
          val sessionData = UIJourneySessionData("session-123456", "ADD-SE", Some(AddIncomeSourceData(Some("my business"))))
          mockRepositoryGet(Some(sessionData))
          TestSessionService.getMongoKey("businessName", IncomeSourceJourneyType(Add, SelfEmployment))(headerCarrier, ec).futureValue shouldBe Right(Some("my business"))
        }
      }

      "getMongoKeyTyped method with type" should {
        "get string value" in {
          val sessionData = UIJourneySessionData("session-123456", "ADD-SE", Some(AddIncomeSourceData(Some("my business"))))
          mockRepositoryGet(Some(sessionData))
          TestSessionService.getMongoKeyTyped[String]("businessName", IncomeSourceJourneyType(Add, SelfEmployment))(headerCarrier, ec)
            .futureValue shouldBe Right(Some("my business"))
        }
        "get LocalDate value" in {
          val sessionData = UIJourneySessionData("session-123456", "ADD-SE", Some(AddIncomeSourceData(
            Some("my business"), Some("plumbing"), Some(LocalDate.of(2023, 5, 23)))))
          mockRepositoryGet(Some(sessionData))
          TestSessionService.getMongoKeyTyped[LocalDate]("dateStarted", IncomeSourceJourneyType(Add, SelfEmployment))(headerCarrier, ec)
            .futureValue shouldBe Right(Some(LocalDate.parse("2023-05-23")))
        }
      }

      "setMongoData method" should {
        "return true when data is set" in {
          mockRepositorySet(response = true)
          val result: Boolean = TestSessionService.setMongoData(UIJourneySessionData("session-1", "ADD-SE"))
            .futureValue
          result shouldBe true
        }
        "return true when data is set and encryption is enabled" in {
          mockRepositorySet(response = true, isSensitive = true)
          when(mockFrontendAppConfig.encryptionIsEnabled).thenReturn(true)
          val result: Boolean = TestSessionService.setMongoData(UIJourneySessionData("session-1", "ADD-SE"))
            .futureValue
          result shouldBe true
        }
        "return a future error" in {
          mockRepositorySet(response = true, withFailureResult = true)
          val result= TestSessionService.setMongoData(UIJourneySessionData("session-1", "ADD-SE"))
          result.failed.futureValue.leftSideValue.getMessage shouldBe "Error while set data"
        }
      }

      "setMultipleMongoData method" should {
        "return true when journey type is Add" in {
          updateMultipleData()
          val result: Either[Throwable, Boolean] = TestSessionService.setMultipleMongoData(Map("key" -> "value"),
            IncomeSourceJourneyType(Add, SelfEmployment))(headerCarrier, ec).futureValue
          result shouldBe Right(true)
        }
        "return true when journey type is Manage" in {
          updateMultipleData()
          val result: Either[Throwable, Boolean] = TestSessionService.setMultipleMongoData(Map("key" -> "value"),
            IncomeSourceJourneyType(Manage, SelfEmployment))(headerCarrier, ec).futureValue
          result shouldBe Right(true)
        }
        "return true when journey type is Cease" in {
          updateMultipleData()
          val result: Either[Throwable, Boolean] = TestSessionService.setMultipleMongoData(Map("key" -> "value"),
            IncomeSourceJourneyType(Cease, SelfEmployment))(headerCarrier, ec).futureValue
          result shouldBe Right(true)
        }
        "return a future error" in {
          updateMultipleData(false)
          val result = TestSessionService.setMultipleMongoData(Map("key" -> "value"),
            IncomeSourceJourneyType(Add, SelfEmployment))(headerCarrier, ec)
          result.failed.futureValue.leftSideValue.getMessage shouldBe "Error returned from mongoDb"
        }
      }

      "setMongoKey method" should {
        "return true when journey type is Add" in {
          mockRepositoryUpdateData()
          val result: Either[Throwable, Boolean] = TestSessionService.setMongoKey("key", "value",
            IncomeSourceJourneyType(Add, SelfEmployment))(headerCarrier, ec).futureValue
          result shouldBe Right(true)
        }
        "return true when journey type is Manage" in {
          mockRepositoryUpdateData()
          val result: Either[Throwable, Boolean] = TestSessionService.setMongoKey("key", "value",
            IncomeSourceJourneyType(Manage, SelfEmployment))(headerCarrier, ec).futureValue
          result shouldBe Right(true)
        }
        "return true when journey type is Cease" in {
          mockRepositoryUpdateData()
          val result: Either[Throwable, Boolean] = TestSessionService.setMongoKey("key", "value",
            IncomeSourceJourneyType(Cease, SelfEmployment))(headerCarrier, ec).futureValue
          result shouldBe Right(true)
        }
      }

      "deleteMongoData method" should {
        "return a future boolean value" in {
          mockDeleteOne()
          val result: Boolean = TestSessionService.deleteMongoData(IncomeSourceJourneyType(Add, SelfEmployment))(headerCarrier).futureValue
          result shouldBe true
        }
        "return a future boolean value if encryption is enabled" in {
          when(mockFrontendAppConfig.encryptionIsEnabled).thenReturn(true)
          mockDeleteOne(isSensitive = true)
          val result: Boolean = TestSessionService.deleteMongoData(IncomeSourceJourneyType(Add, SelfEmployment))(headerCarrier).futureValue
          result shouldBe true
        }
      }

      "deleteSession method" should {
        "return a future boolean value" in {
          mockDeleteSession()
          val result: Boolean = TestSessionService.deleteSession(Add)(headerCarrier).futureValue
          result shouldBe true
        }
        "return a future boolean value if encryption is enabled" in {
          when(mockFrontendAppConfig.encryptionIsEnabled).thenReturn(true)
          mockDeleteSession(isSensitive = true)
          val result: Boolean = TestSessionService.deleteSession(Add)(headerCarrier).futureValue
          result shouldBe true
        }
      }

      "clearSession method" should {
        "return a future unit value" in {
          mockClearSession(headerCarrier.sessionId.get.value)(Future.successful(true))
          val result: Unit = TestSessionService.clearSession(headerCarrier.sessionId.get.value).futureValue
          result shouldBe()
        }
        "return a failed future value" in {
          mockClearSession(headerCarrier.sessionId.get.value)(Future.successful(false))
          val result: Future[Unit] = TestSessionService.clearSession(headerCarrier.sessionId.get.value)

          whenReady(result.failed) { exception =>
            exception.getMessage shouldBe "failed to clear session"
          }
        }
      }
    }
  }
}

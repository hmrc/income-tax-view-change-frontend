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
import enums.JourneyType.{Add, JourneyType}
import mocks.repositories.MockUIJourneySessionDataRepository
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import testUtils.TestSupport

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

class SessionServiceSpec extends TestSupport with MockUIJourneySessionDataRepository {

  object TestSessionService extends SessionService(
    mockUIJourneySessionDataRepository,
    mockSensitiveUIJourneySessionDataRepository,
    mockFrontendAppConfig
  )

  private val testKeyOne = "businessName"
  private val testValueOne = "Big Bid Niss"
  private val testKeyTwo = "dateStarted"
  private val testValueTwo = "2023-05-23"

  "sessionService " when {
    "mongo" when {
      "getMongo method " should {
        "return the correct session value for given key" in {
          val sessionData = UIJourneySessionData("session-123456", "ADD-SE")
          mockRepositoryGet(Some(sessionData))
          TestSessionService.getMongo("ADD-SE")(headerCarrier, ec).futureValue shouldBe Right(Some(sessionData))
        }
      }
      "getMongoKey method " should {
        "return the correct session value for given key" in {
          val sessionData = UIJourneySessionData("session-123456", "ADD-SE", Some(AddIncomeSourceData(Some("my business"))))
          mockRepositoryGet(Some(sessionData))
          TestSessionService.getMongoKey("businessName", JourneyType(Add, SelfEmployment))(headerCarrier, ec).futureValue shouldBe Right(Some("my business"))
        }
      }

      "getMongoKeyTyped method with type" should {
        "get string value" in {
          val sessionData = UIJourneySessionData("session-123456", "ADD-SE", Some(AddIncomeSourceData(Some("my business"))))
          mockRepositoryGet(Some(sessionData))
          TestSessionService.getMongoKeyTyped[String]("businessName", JourneyType(Add, SelfEmployment))(headerCarrier, ec)
            .futureValue shouldBe Right(Some("my business"))
        }
        "get LocalDate value" in {
          val sessionData = UIJourneySessionData("session-123456", "ADD-SE", Some(AddIncomeSourceData(
            Some("my business"), Some("plumbing"), Some(LocalDate.of(2023, 5, 23)))))
          mockRepositoryGet(Some(sessionData))
          TestSessionService.getMongoKeyTyped[LocalDate]("dateStarted", JourneyType(Add, SelfEmployment))(headerCarrier, ec)
            .futureValue shouldBe Right(Some(LocalDate.parse("2023-05-23")))
        }
      }

      "setMongoData method" should {
        "return a future boolean value" in {
          mockRepositorySet(true)
          val result: Boolean = TestSessionService.setMongoData(UIJourneySessionData("session-1", "ADD-SE"))(headerCarrier, ec)
            .futureValue
          result shouldBe true
        }
      }
      "setMongoKey method" should {
        "return a future boolean value" in {
          mockRepositoryUpdateData()
          val result: Either[Throwable, Boolean] = TestSessionService.setMongoKey("key", "value",
            JourneyType(Add, SelfEmployment))(headerCarrier, ec).futureValue
          result shouldBe Right(true)
        }
      }

      "setMultipleMongoData method" should {
        "return a future boolean value" in {
          mockRepositoryUpdateData()
          val testMap: Map[String, String] = Map("key1" -> "value1", "key2" -> "value2")
          val result: Either[Throwable, Boolean] = TestSessionService.setMultipleMongoData(testMap,
            JourneyType(Add, SelfEmployment))(headerCarrier, ec).futureValue
          result shouldBe Right(true)
        }
        "return a future left exception" in {
          mockRepositoryUpdateDataFailure()
          val testMap: Map[String, String] = Map("key1" -> "value1", "key2" -> "value2")
          val result: Either[Throwable, Boolean] = TestSessionService.setMultipleMongoData(testMap,
            JourneyType(Add, SelfEmployment))(headerCarrier, ec).futureValue
          result.toString shouldBe Left(new Exception("Mongo Save data operation was not acknowledged")).toString
        }

        "return a future left exception when the second key-value fails" in {

          mockRepositoryUpdateDataSuccess(s"addIncomeSourceData.$testKeyOne", testValueOne)
          mockRepositoryUpdateDataFailure(s"addIncomeSourceData.$testKeyTwo", testValueTwo)

          val testMap: Map[String, String] =
            Map(
              testKeyOne -> testValueOne,
              testKeyTwo -> testValueTwo
            )

          val result: Either[Throwable, Boolean] =
            TestSessionService
              .setMultipleMongoData(testMap, JourneyType(Add, SelfEmployment))
              .futureValue

          result.isLeft shouldBe true
          result.left.toOption.map(_.getMessage) should contain("Mongo Save data operation was not acknowledged")
        }
      }

      "deleteMongoData method" should {
        "return a future boolean value" in {
          mockDeleteOne()
          val result: Boolean = TestSessionService.deleteMongoData(JourneyType(Add, SelfEmployment))(headerCarrier).futureValue
          result shouldBe true
        }
      }

      "deleteSession method" should {
        "return a future boolean value" in {
          mockDeleteSession()
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

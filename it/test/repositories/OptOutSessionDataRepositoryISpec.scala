/*
 * Copyright 2024 HM Revenue & Customs
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

package repositories

import cats.data.OptionT
import helpers.ComponentSpecBase
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.optout.OptOutSessionData
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.ScalaFutures
import play.api.http.HttpEntity.Strict
import play.api.mvc.Results
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import utils.OptOutJourney

import scala.concurrent.{ExecutionContext, Future}


class OptOutSessionDataRepositoryISpec extends ComponentSpecBase with ScalaFutures {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  private val repository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    await(repository.collection.deleteMany(BsonDocument()).toFuture())
  }

  "UIJourneySessionDataRepository.set" should {
    s"save opt-out session-data" should {
      s"fetch saved opt-out session-data" in {

        val currentYear = 2024
        val sessionId = "123"
        val expectedOptOutSessionData = OptOutSessionData(intent = Some(TaxYear.forYearEnd(currentYear).toString))
        val expectedSessionData = UIJourneySessionData(sessionId = sessionId,
          journeyType = OptOutJourney.Name,
          optOutSessionData = Some(expectedOptOutSessionData))

        repository.set(expectedSessionData)

        val savedData = repository.get(sessionId, OptOutJourney.Name)

        val result = for {
          fetchedOptOutSessionData <- OptionT(savedData)
        } yield fetchedOptOutSessionData

        result.value.futureValue.get.sessionId shouldBe expectedSessionData.sessionId
        result.value.futureValue.get.journeyType shouldBe expectedSessionData.journeyType
        result.value.futureValue.get.optOutSessionData shouldBe expectedSessionData.optOutSessionData
      }
    }
  }


  "OptOutJourney.withSessionData" should {
    s"create opt-out session-data if missing" in {

      class OptOutJourneyTarget(override val sessionService: SessionService)(implicit val ec: ExecutionContext) extends OptOutJourney
      val target = new OptOutJourneyTarget(sessionService)

      val result = target.withSessionData(
        data => Future.successful(Results.Ok(s"Yes! I have data: ${data.sessionId}, ${data.optOutSessionData.getOrElse("None")}")),
        th => Future.successful(Results.BadRequest(s"Oh No! got error: ${th.getMessage}"))
      )

      val resultAsText = result.futureValue.body match {
        case Strict(data, _) => data.map(_.toChar).mkString
        case _ => "No"
      }

      resultAsText shouldBe "Yes! I have data: xsession-12345, None"
    }
  }

  "OptOutJourney.withSessionData" should {
    s"use saved opt-out session-data if present" in {

      class OptOutJourneyTarget(override val sessionService: SessionService)(implicit val ec: ExecutionContext) extends OptOutJourney
      val target = new OptOutJourneyTarget(sessionService)

      val currentYear = 2025
      val sessionId = "xsession-12345"
      val expectedOptOutSessionData = OptOutSessionData(intent = Some(TaxYear.forYearEnd(currentYear).toString))
      val expectedSessionData = UIJourneySessionData(sessionId = sessionId,
        journeyType = OptOutJourney.Name,
        optOutSessionData = Some(expectedOptOutSessionData))

      repository.set(expectedSessionData)

      val result = target.withSessionData(
        data => Future.successful(Results.Ok(s"Yes! I have data: ${data.sessionId}, ${data.optOutSessionData.get.intent.get}")),
        th => Future.successful(Results.BadRequest(s"Oh No! got error: ${th.getMessage}"))
      )

      val resultAsText = result.futureValue.body match {
        case Strict(data, _) => data.map(_.toChar).mkString
        case _ => "No"
      }

      resultAsText shouldBe "Yes! I have data: xsession-12345, 2024-2025"
    }
  }

}

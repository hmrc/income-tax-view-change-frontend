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

import helpers.ComponentSpecBase
import models.claimToAdjustPoa.{MainIncomeLower, PoaAmendmentData, PoaSessionData}
import org.mongodb.scala.bson.BsonDocument
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import org.mongodb.scala.{SingleObservableFuture, ObservableFuture}

class PoaAmendmentDataRepositoryISpec extends ComponentSpecBase{

  private val repository = app.injector.instanceOf[PoaAmendmentDataRepository]

  override def beforeEach(): Unit = {
    await(repository.collection.deleteMany(BsonDocument()).toFuture())
  }

  val ammendmentData: PoaAmendmentData = PoaAmendmentData(Some(MainIncomeLower), None)

  val sessionData: PoaSessionData = PoaSessionData("session-123456", Some(ammendmentData))

  "PoA Amendment repository" should {
    "set some data" in {
      val acknowledged = await(repository.set(sessionData))
      acknowledged shouldBe true
      await(repository.get("session-123456")).get.poaAmendmentData shouldBe Some(ammendmentData)
    }
    "get some data" in {
      await(repository.set(sessionData))
      val data = await(repository.get("session-123456"))
      data.get.poaAmendmentData shouldBe Some(ammendmentData)
    }
  }

}

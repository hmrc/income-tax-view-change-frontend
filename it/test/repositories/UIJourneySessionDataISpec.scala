/*
 * Copyright 2017 HM Revenue & Customs
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
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import org.mongodb.scala.bson.BsonDocument
import play.api.test.Helpers.{await, defaultAwaitTimeout}

class UIJourneySessionDataISpec extends ComponentSpecBase {
  private val repository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    await(repository.collection.deleteMany(BsonDocument()).toFuture())
  }

  "UI Journey Session Data" should {
    "set some data" in {
      val acknowledged = await(repository.set(UIJourneySessionData("session-12345", "ADD-UKPROP", Some(AddIncomeSourceData(Some("business1"))))))
      acknowledged shouldBe true
    }
    "get some data" in {
      await(repository.set(UIJourneySessionData("session-12345", "ADD-UKPROP", Some(AddIncomeSourceData(Some("business1"))))))
      val sessionData = await(repository.get("session-12345", "ADD-UKPROP")).get
      sessionData.addIncomeSourceData.get shouldBe AddIncomeSourceData(Some("business1"), None, None)
    }
    "updateDate should set a data field correctly" in {
      await(repository.set(UIJourneySessionData("session-12345", "ADD-UKPROP", Some(AddIncomeSourceData(Some("business1"))))))

      val updateResult = await(repository.updateData(UIJourneySessionData("session-12345", "ADD-UKPROP"), "addIncomeSourceData.businessName", "business2"))
      updateResult.wasAcknowledged() shouldBe true
      val sessionData = await(repository.get("session-12345", "ADD-UKPROP")).get
      sessionData.addIncomeSourceData.get shouldBe AddIncomeSourceData(Some("business2"), None, None)
    }
    "deleteOne should remove a sessionData item" in {
      await(repository.set(UIJourneySessionData("session-12345", "ADD-UKPROP", Some(AddIncomeSourceData(Some("business1"))))))
      val result = await(repository.deleteOne(UIJourneySessionData("session-12345", "ADD-UKPROP")))
      result shouldBe true
      val sessionData = await(repository.get("session-12345", "ADD-UKPROP"))
      sessionData shouldBe None
    }
  }
}

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
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus._
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.optout.OptOutProposition.createOptOutProposition


class OptOutSessionDataRepositoryISpec extends ComponentSpecBase with ScalaFutures {

  private val journeyRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  private val repository = app.injector.instanceOf[OptOutSessionDataRepository]

  private val taxYearEnd = 2024
  private val taxYear2023_2024 = TaxYear.forYearEnd(taxYearEnd)
  private val taxYear2024_2025 = taxYear2023_2024.nextYear

  override def beforeEach(): Unit = {
    await(journeyRepository.collection.deleteMany(BsonDocument()).toFuture())
  }

  "OptOutSessionDataRepository" should {
    s"stores the OptOutProposition at initialisation for recall later in the journey" in {

      val optOutProposition =
        createOptOutProposition(
          currentYear = taxYear2023_2024,
          previousYearCrystallised = true,
          previousYearItsaStatus = Voluntary,
          currentYearItsaStatus = Mandated,
          nextYearItsaStatus = Annual
        )

      await(repository.initialiseOptOutJourney(optOutProposition))

      repository.recallOptOutPropositionWithIntent().futureValue.get shouldBe (optOutProposition, None)
    }

    s"Removes the customer intent at journey initialisation" in {

      await(repository.initialiseOptOutJourney(someOptOutProposition))
      await(repository.saveIntent(taxYear2024_2025))

      await(repository.initialiseOptOutJourney(someOptOutProposition))

      repository.fetchSavedIntent().futureValue shouldBe None
    }

    s"save and recall the user choice" in {

      await(repository.initialiseOptOutJourney(someOptOutProposition))

      await(repository.saveIntent(taxYear2024_2025))

      repository.fetchSavedIntent().futureValue.get shouldBe taxYear2024_2025
    }

    s"not overwrite the OptOutProposition when saving the user choice" in {

      val optOutProposition =
        createOptOutProposition(
          currentYear = taxYear2023_2024,
          previousYearCrystallised = true,
          previousYearItsaStatus = Voluntary,
          currentYearItsaStatus = Mandated,
          nextYearItsaStatus = Annual
        )

      await(repository.initialiseOptOutJourney(optOutProposition))
      await(repository.saveIntent(taxYear2024_2025))

      repository.recallOptOutPropositionWithIntent().futureValue.get shouldBe (optOutProposition, Some(taxYear2024_2025))
    }
  }

  private def someOptOutProposition = {
    createOptOutProposition(
      currentYear = taxYear2023_2024,
      previousYearCrystallised = true,
      previousYearItsaStatus = Voluntary,
      currentYearItsaStatus = Voluntary,
      nextYearItsaStatus = Voluntary
    )
  }

}

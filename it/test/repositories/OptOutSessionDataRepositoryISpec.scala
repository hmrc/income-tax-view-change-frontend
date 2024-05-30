package repositories

import cats.data.OptionT
import helpers.ComponentSpecBase
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.optout.OptOutSessionData
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers.{await, defaultAwaitTimeout}


class OptOutSessionDataRepositoryISpec extends ComponentSpecBase with ScalaFutures {

  private val repository = app.injector.instanceOf[UIJourneySessionDataRepository]

  override def beforeEach(): Unit = {
    await(repository.collection.deleteMany(BsonDocument()).toFuture())
  }

  "UIJourneySessionDataRepository.set" should {
    s"save opt-out session-data" should {
      s"fetch saved opt-out session-data" in {

        val expectedOptOutSessionData = OptOutSessionData(intent = Some(TaxYear.forYearEnd(2024).toString))
        val expectedSessionData = UIJourneySessionData(sessionId = "123", "OPTOUT", optOutSessionData = Some(expectedOptOutSessionData))

        repository.set(expectedSessionData)

        val savedData = repository.get("123", "OPTOUT")

        val result = for {
          fetchedOptOutSessionData <- OptionT(savedData)
        } yield fetchedOptOutSessionData

        result.value.futureValue.get.sessionId shouldBe expectedSessionData.sessionId
        result.value.futureValue.get.journeyType shouldBe expectedSessionData.journeyType
        result.value.futureValue.get.optOutSessionData shouldBe expectedSessionData.optOutSessionData
      }
    }
  }

}

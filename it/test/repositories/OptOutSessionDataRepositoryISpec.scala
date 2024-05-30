package repositories

import cats.data.OptionT
import helpers.ComponentSpecBase
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.optout.OptOutSessionData
import org.mockito.Mockito
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.ScalaFutures
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import utils.OptOutJourney


class OptOutSessionDataRepositoryISpec extends ComponentSpecBase with ScalaFutures {

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




  "UIJourneySessionDataRepository.set xx" should {
    s"save opt-out session-data xx" should {
      s"fetch saved opt-out session-data xx" in {

        implicit val sessionService: SessionService = Mockito.mock(classOf[SessionService])
        class OptOutJourneyTarget(implicit sessionService: SessionService) extends OptOutJourney {

        }
      }
    }
  }

}

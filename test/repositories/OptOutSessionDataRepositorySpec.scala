package repositories

import config.FrontendAppConfig
import models.incomeSourceDetails.{TaxYear, UIJourneySessionData}
import models.optout.OptOutSessionData
import org.mockito.Mockito
import org.mockito.Mockito.{mock, reset}
import testUtils.TestSupport
import repositories.UIJourneySessionDataRepository
import uk.gov.hmrc.mongo.MongoComponent

import java.time.Clock

class OptOutSessionDataRepositorySpec extends TestSupport {

  val mongoComponent: MongoComponent = mock(classOf[MongoComponent])
  val appConfigMock: FrontendAppConfig = mock(classOf[FrontendAppConfig])
  val clock: Clock = mock(classOf[Clock])

  override def beforeEach(): Unit = {
    reset(mongoComponent)
    reset(appConfigMock)
    reset(clock)
  }

  "xxx" should {
    s"xxx" in {
      val repository = new UIJourneySessionDataRepository(mongoComponent, appConfigMock, clock)

      val optOutSessionData = OptOutSessionData(intent =  Some(TaxYear.forYearEnd(2024).toString))
      val data = UIJourneySessionData(sessionId = "123", "journeyType", optOutSessionData = Some(optOutSessionData))

      repository.set(data)

    }
  }

}

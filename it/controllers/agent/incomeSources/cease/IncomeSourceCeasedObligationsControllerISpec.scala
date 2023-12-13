package controllers.agent.incomeSources.cease

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.{CeaseIncomeSourceData, UIJourneySessionData}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status.OK
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.b1TradingName
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

import java.time.LocalDate

class IncomeSourceCeasedObligationsControllerISpec extends ComponentSpecBase {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val repository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val businessCeasedObligationsShowUrl: String = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(SelfEmployment).url
  val ukPropertyCeasedObligationsShowUrl: String = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(UkProperty).url
  val foreignPropertyCeasedObligationsShowUrl: String = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.showAgent(ForeignProperty).url
  val testDate: String = "2020-11-10"
  val prefix: String = "business-ceased.obligation"
  val continueButtonText: String = messagesAPI(s"$prefix.income-sources-button")
  val htmlTitle = " - Manage your Income Tax updates - GOV.UK"
  val day: LocalDate = LocalDate.of(2023, 1, 1)
  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(NextUpdatesModel("123", List(NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "EOPS")))))

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  s"calling GET $businessCeasedObligationsShowUrl" should {
    "render the Business Ceased obligations page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        When(s"I call GET $businessCeasedObligationsShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(testEndDate2022), ceasePropertyDeclare = None, journeyIsComplete = Some(true))))))

        val result = IncomeTaxViewChangeFrontend.getBusinessCeasedObligations(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        sessionService.getMongoKey(CeaseIncomeSourceData.journeyIsCompleteField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(Some(true))

        val expectedText: String = b1TradingName + " " + messagesAPI(s"$prefix.heading1.base")

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $ukPropertyCeasedObligationsShowUrl" should {
    "render the UK Property Ceased obligations page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        When(s"I call GET $ukPropertyCeasedObligationsShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId), endDate = Some(testEndDate2022), ceasePropertyDeclare = None, journeyIsComplete = Some(true))))))

        val result = IncomeTaxViewChangeFrontend.getUkPropertyCeasedObligations(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("business-ceased.obligation.heading1.uk-property.part2") + " " + messagesAPI("business-ceased.obligation.heading1.base")

        sessionService.getMongoKey(CeaseIncomeSourceData.journeyIsCompleteField, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(Some(true))

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $foreignPropertyCeasedObligationsShowUrl" should {
    "render the Foreign Property Ceased obligations page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)

        When(s"I call GET $foreignPropertyCeasedObligationsShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId), endDate = Some(testEndDate2022), ceasePropertyDeclare = None, journeyIsComplete = Some(true))))))


        val result = IncomeTaxViewChangeFrontend.getForeignPropertyCeasedObligations(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("business-ceased.obligation.heading1.foreign-property.part2") + " " + messagesAPI("business-ceased.obligation.heading1.base")

        sessionService.getMongoKey(CeaseIncomeSourceData.journeyIsCompleteField, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(Some(true))

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }


}

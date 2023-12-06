package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class IncomeSourceAddedBackErrorControllerISpec extends ComponentSpecBase{

  private lazy val backErrorController = controllers.incomeSources.add.routes.IncomeSourceAddedBackErrorController

  val selfEmploymentBackErrorUrl: String = backErrorController.showAgent(SelfEmployment).url
  val ukPropertyBackErrorUrl: String = backErrorController.showAgent(UkProperty).url
  val foreignPropertyBackErrorUrl: String = backErrorController.showAgent(ForeignProperty).url

  val title = messagesAPI("cannot-go-back.heading")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val UIJourneySessionDataRepository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  val journeyType: JourneyType = JourneyType(Add, SelfEmployment)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  s"calling GET $selfEmploymentBackErrorUrl" should {
    "render the self employment business not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
          addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("1234"), incomeSourceAdded = Some(true), reportingMethodSet = None)))))

        val result = IncomeTaxViewChangeFrontend
          .get("/income-sources/add/cannot-go-back-business-reporting-method", clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(s"$title")
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)
      stubAuthorisedAgentUser(authorised = true)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

      val result = IncomeTaxViewChangeFrontend
        .get("/income-sources/add/cannot-go-back-business-reporting-method", clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }

  s"calling GET $ukPropertyBackErrorUrl" should {
    "render the self employment business not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
          addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("1234"), incomeSourceAdded = Some(true), reportingMethodSet = None)))))

        val result = IncomeTaxViewChangeFrontend
          .get("/income-sources/add/cannot-go-back-uk-property-reporting-method", clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(s"$title")
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)
      stubAuthorisedAgentUser(authorised = true)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

      val result = IncomeTaxViewChangeFrontend
        .get(s"/income-sources/add/cannot-go-back-uk-property-reporting-method", clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }

  s"calling GET $foreignPropertyBackErrorUrl" should {
    "render the self employment business not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-FP",
          addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some("1234"), incomeSourceAdded = Some(true), reportingMethodSet = None)))))

        val result = IncomeTaxViewChangeFrontend
          .get(s"/income-sources/add/cannot-go-back-foreign-property-reporting-method", clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(s"$title")
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)
      stubAuthorisedAgentUser(authorised = true)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

      val result = IncomeTaxViewChangeFrontend
        .get(s"/income-sources/add/cannot-go-back-foreign-property-reporting-method", clientDetailsWithConfirmation)

      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }

}



package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino, testPropertyIncomeId, testSelfEmploymentId, testSessionId}
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, singleBusinessResponse, ukPropertyOnlyResponse}
import testConstants.PropertyDetailsIntegrationTestConstants.ukProperty

import java.time.LocalDate

class IncomeSourceAddedControllerISpec extends ComponentSpecBase{

  val incomeSourceAddedSelfEmploymentShowAgentUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(SelfEmployment).url

  val incomeSourceAddedSubmitAgentUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.agentSubmit().url
  val addIncomeSourceAgentUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url

  val testDate: String = "2020-11-10"
  val prefix: String = "business-added"
  val continueButtonText: String = messagesAPI(s"$prefix.income-sources-button")
  val htmlTitle = " - Manage your Income Tax updates - GOV.UK"
  val day: LocalDate = LocalDate.of(2023, 1, 1)
  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(NextUpdatesModel("123", List(NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "EOPS")))))

  val incomeSourceAddedForeignPropertyShowAgentUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(ForeignProperty).url

  val incomeSourceAddedUkPropertyShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(UkProperty).url
  val HomeControllerShowUrl: String = controllers.routes.HomeController.showAgent.url
  val pageTitle: String = messagesAPI("htmlTitle.agent", {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}".trim()
  })
  val confirmationPanelContent: String = {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}"
  }

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(AddIncomeSourceData()))

  s"calling GET $incomeSourceAddedSelfEmploymentShowAgentUrl" should {
    "render the Business Added page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $incomeSourceAddedSelfEmploymentShowAgentUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
          addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId))))))

        val result = IncomeTaxViewChangeFrontend.getAddBusinessObligations(clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI("business-added.sole-trader.head").nonEmpty) {
          messagesAPI("business-added.sole-trader.head") + " " + business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
        }
        else {
          business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
        }

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, JourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(true))

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $incomeSourceAddedSelfEmploymentShowAgentUrl" should {
    s"redirect to $addIncomeSourceAgentUrl" when {
      "called" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postAddedBusinessObligations(clientDetailsWithConfirmation)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/agents/income-sources/add/new-income-sources")
        )
      }
    }
  }

  s"calling GET $incomeSourceAddedForeignPropertyShowAgentUrl" should {
    "render the Foreign Property Added obligations page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $incomeSourceAddedForeignPropertyShowAgentUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-FP",
          addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId))))))

        val result = IncomeTaxViewChangeFrontend.getForeignPropertyAddedObligations(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("business-added.foreign-property.h1") + " " + messagesAPI("business-added.foreign-property.base")

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, JourneyType(Add, ForeignProperty)).futureValue shouldBe Right(Some(true))

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $incomeSourceAddedForeignPropertyShowAgentUrl" should {
    s"redirect to $addIncomeSourceAgentUrl" when {
      "called" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postForeignPropertyAddedObligations(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/agents/income-sources/add/new-income-sources")
        )
      }
    }
  }

  s"calling GET $incomeSourceAddedUkPropertyShowUrl" should {
    "render the UK Property Added Page" when {
      "UK Property start date is provided" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1330 getNextUpdates return a success response")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
          addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId))))))

        Then("user is shown UK property added page")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-added", clientDetailsWithConfirmation)
        And("Mongo storage is successfully set")
        sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, JourneyType(Add, UkProperty)).futureValue shouldBe Right(Some(true))

        result should have(
          httpStatus(OK),
          pageTitleCustom(pageTitle),
          elementTextBySelectorList(".govuk-panel.govuk-panel--confirmation")(confirmationPanelContent)
        )

      }
    }
    "render error page" when {
      "UK property income source is missing trading start date" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse.copy(properties = List(ukProperty.copy(tradingStartDate = None))))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
          addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId))))))

        Then("user is shown a error page")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-added", clientDetailsWithConfirmation)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleAgent("standardError.heading", isErrorPage = true)
        )

      }
    }
    s"redirect to $HomeControllerShowUrl" when {
      "Income Sources Feature Switch is disabled" in {
        Given("Income Sources FS is disabled")
        disable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse.copy(properties = List(ukProperty.copy(tradingStartDate = None))))


        Then(s"user is redirected to $HomeControllerShowUrl")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-added", clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(HomeControllerShowUrl)
        )
      }
    }
  }
}

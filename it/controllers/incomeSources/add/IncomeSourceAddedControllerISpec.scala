package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testPropertyIncomeId, testSelfEmploymentId, testSessionId}
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.testObligationsModel
import testConstants.PropertyDetailsIntegrationTestConstants.ukProperty

import java.time.LocalDate

class IncomeSourceAddedControllerISpec extends ComponentSpecBase{

  val incomeSourceAddedSelfEmploymentShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.show("", SelfEmployment).url
  val businessReportingMethodUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.show("").url

  val IncomeSourceAddedSubmitUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.submit().url
  val addIncomeSourceUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url

  val testDate: String = "2020-11-10"
  val prefix: String = "business-added"
  val continueButtonText: String = messagesAPI(s"$prefix.income-sources-button")
  val htmlTitle = " - Manage your Income Tax updates - GOV.UK"
  val day: LocalDate = LocalDate.of(2023, 1, 1)
  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(NextUpdatesModel("123", List(NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "EOPS")))))

  val incomeSourceAddedUkPropertyShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(testPropertyIncomeId, UkProperty).url
  val HomeControllerShowUrl: String = controllers.routes.HomeController.show().url
  val pageTitle: String = messagesAPI("htmlTitle", {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}".trim()
  })
  val confirmationPanelContent: String = {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}"
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  val incomeSourceAddedForeignPropertyShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceAddedController.show("", ForeignProperty).url
  val foreignPropertyReportingMethodShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show("").url

  val addIncomeSourceShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(AddIncomeSourceData()))

  s"calling GET $incomeSourceAddedSelfEmploymentShowUrl" should {
    "render the Business Added page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $incomeSourceAddedSelfEmploymentShowUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val incomeSourceId = testSelfEmploymentId
        val result = IncomeTaxViewChangeFrontend.getAddBusinessObligations(incomeSourceId)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI("business-added.sole-trader.head").nonEmpty) {
          messagesAPI("business-added.sole-trader.head") + " " + business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
        }
        else {
          business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
        }

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(AddIncomeSourceData.hasBeenAddedField, JourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(true))

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $incomeSourceAddedSelfEmploymentShowUrl" should {
    s"redirect to $addIncomeSourceUrl" when {
      "called" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postAddedBusinessObligations()
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/income-sources/add/new-income-sources")
        )
      }
    }
  }

  s"calling GET $incomeSourceAddedUkPropertyShowUrl" should {
    "render the UK Property Added Page" when {
      "UK Property start date is provided" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1330 getNextUpdates return a success response")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        Then("user is shown UK property added page")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-added?id=$testPropertyIncomeId")

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(AddIncomeSourceData.hasBeenAddedField, JourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(true))

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

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse.copy(properties = List(ukProperty.copy(tradingStartDate = None))))


        Then("user is shown a error page")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-added?id=$testPropertyIncomeId")

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleIndividual("standardError.heading", isErrorPage = true)
        )

      }
    }
    s"redirect to $HomeControllerShowUrl" when {
      "Income Sources Feature Switch is disabled" in {
        Given("Income Sources FS is disabled")
        disable(IncomeSources)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse.copy(properties = List(ukProperty.copy(tradingStartDate = None))))


        Then(s"user is redirected to $HomeControllerShowUrl")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/add/uk-property-added?id=$testPropertyIncomeId")

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(HomeControllerShowUrl)
        )
      }
    }
  }

  s"calling GET $incomeSourceAddedForeignPropertyShowUrl" should {
    "render the Foreign Property Added obligations page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $incomeSourceAddedForeignPropertyShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val incomeSourceId = testPropertyIncomeId
        val result = IncomeTaxViewChangeFrontend.getForeignPropertyAddedObligations(incomeSourceId)
        verifyIncomeSourceDetailsCall(testMtditid)

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(AddIncomeSourceData.hasBeenAddedField, JourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(true))

        val expectedText: String = messagesAPI("business-added.foreign-property.h1") + " " + messagesAPI("business-added.foreign-property.base")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $incomeSourceAddedForeignPropertyShowUrl" should {
    s"redirect to $addIncomeSourceShowUrl" when {
      "called" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.postForeignPropertyAddedObligations()
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/income-sources/add/new-income-sources")
        )
      }
    }
  }
}

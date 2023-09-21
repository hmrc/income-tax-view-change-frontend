package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

class IncomeSourceReportingMethodNotSavedControllerISpec extends ComponentSpecBase {

  val selfEmploymentReportingMethodNotSavedShowAgentUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(SelfEmployment.key).url
  val ukPropertyReportingMethodNotSavedShowAgentUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(UkProperty.key).url
  val foreignPropertyReportingMethodNotSavedShowAgentUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.showAgent(ForeignProperty.key).url
  val sessionIncomeSourceId = Map(forms.utils.SessionKeys.incomeSourceId -> testSelfEmploymentId)

  object TestConstants {
    val selfEmployment: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.se")
    val seParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", selfEmployment)

    val ukProperty: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.uk")
    val ukParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", ukProperty)

    val foreignProperty: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.fp")
    val foreignParagraph: String = messagesAPI("incomeSources.add.error.reportingMethodNotSaved.p1", foreignProperty)

    val continueButtonText: String = messagesAPI("base.continue")
  }


  s"calling GET $selfEmploymentReportingMethodNotSavedShowAgentUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        When(s"I call GET $selfEmploymentReportingMethodNotSavedShowAgentUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)


        val result = IncomeTaxViewChangeFrontend.getSEReportingMethodNotSaved(sessionIncomeSourceId ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("paragraph-1")(TestConstants.seParagraph)
        )
      }
    }
  }

  s"calling GET $ukPropertyReportingMethodNotSavedShowAgentUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        When(s"I call GET $ukPropertyReportingMethodNotSavedShowAgentUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)


        val result = IncomeTaxViewChangeFrontend.getUkPropertyReportingMethodNotSaved(sessionIncomeSourceId ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("paragraph-1")(TestConstants.ukParagraph)
        )
      }
    }
  }

  s"calling GET $foreignPropertyReportingMethodNotSavedShowAgentUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        When(s"I call GET $foreignPropertyReportingMethodNotSavedShowAgentUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)


        val result = IncomeTaxViewChangeFrontend.getForeignPropertyReportingMethodNotSaved(sessionIncomeSourceId ++ clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("paragraph-1")(TestConstants.foreignParagraph)
        )
      }
    }
  }

}

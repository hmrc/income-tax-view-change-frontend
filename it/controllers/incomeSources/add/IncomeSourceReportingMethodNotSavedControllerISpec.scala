package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

class IncomeSourceReportingMethodNotSavedControllerISpec extends ComponentSpecBase {

  val selfEmploymentReportingMethodNotSavedShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(SelfEmployment.key).url
  val ukPropertyReportingMethodNotSavedShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(UkProperty.key).url
  val foreignPropertyReportingMethodNotSavedShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodNotSavedController.show(ForeignProperty.key).url
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


  s"calling GET $selfEmploymentReportingMethodNotSavedShowUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $selfEmploymentReportingMethodNotSavedShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)


        val result = IncomeTaxViewChangeFrontend.getSEReportingMethodNotSaved(sessionIncomeSourceId)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("paragraph-1")(TestConstants.seParagraph)
        )
      }
    }
  }

  s"calling GET $ukPropertyReportingMethodNotSavedShowUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $ukPropertyReportingMethodNotSavedShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)


        val result = IncomeTaxViewChangeFrontend.getUkPropertyReportingMethodNotSaved(sessionIncomeSourceId)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("paragraph-1")(TestConstants.ukParagraph)
        )
      }
    }
  }

  s"calling GET $foreignPropertyReportingMethodNotSavedShowUrl" should {
    "render the reporting method not saved page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $foreignPropertyReportingMethodNotSavedShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)


        val result = IncomeTaxViewChangeFrontend.getForeignPropertyReportingMethodNotSaved(sessionIncomeSourceId)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("incomeSources.add.error.standardError")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("paragraph-1")(TestConstants.foreignParagraph)
        )
      }
    }
  }

}

package controllers.incomeSources.cease

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.utils.SessionKeys.ceaseBusinessIncomeSourceId
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.nextUpdates.{NextUpdateModel, NextUpdatesModel, ObligationsModel}
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId}
import testConstants.BusinessDetailsIntegrationTestConstants.b1TradingName
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

import java.time.LocalDate

class IncomeSourceCeasedObligationsControllerISpec extends ComponentSpecBase {

  val businessCeasedObligationsShowUrl: String = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(SelfEmployment).url
  val foreignPropertyCeasedObligationsShowUrl: String = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(ForeignProperty).url
  val ukPropertyCeasedObligationsShowUrl: String = controllers.incomeSources.cease.routes.IncomeSourceCeasedObligationsController.show(UkProperty).url
  val testDate: String = "2020-11-10"
  val prefix: String = "business-ceased.obligation"
  val continueButtonText: String = messagesAPI(s"$prefix.income-sources-button")
  val htmlTitle = " - Manage your Income Tax updates - GOV.UK"
  val day: LocalDate = LocalDate.of(2023, 1, 1)
  val testObligationsModel: ObligationsModel = ObligationsModel(Seq(NextUpdatesModel("123", List(NextUpdateModel(day, day.plusDays(1), day.plusDays(2), "EOPS", None, "EOPS")))))


  s"calling GET $businessCeasedObligationsShowUrl" should {
    "render the Business Ceased obligations page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $businessCeasedObligationsShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontend.getBusinessCeasedObligations(Map(ceaseBusinessIncomeSourceId -> testSelfEmploymentId))
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = b1TradingName + " " + messagesAPI(s"$prefix.heading1.base")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $ukPropertyCeasedObligationsShowUrl" should {
    "render the UK Property Ceased obligations page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $ukPropertyCeasedObligationsShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontend.getUkPropertyCeasedObligations(Map.empty)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("business-ceased.obligation.heading1.uk-property.part2") + " " + messagesAPI("business-ceased.obligation.heading1.base")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $foreignPropertyCeasedObligationsShowUrl" should {
    "render the Foreign Property Ceased obligations page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $foreignPropertyCeasedObligationsShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontend.getForeignPropertyCeasedObligations(Map.empty)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = messagesAPI("business-ceased.obligation.heading1.foreign-property.part2") + " " + messagesAPI("business-ceased.obligation.heading1.base")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

}

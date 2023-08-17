package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import forms.utils.SessionKeys.{addForeignPropertyAccountingMethod, foreignPropertyStartDate}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.createIncomeSource.CreateIncomeSourceResponse
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

import scala.collection.immutable.Seq

class ForeignPropertyCheckDetailsControllerISpec extends ComponentSpecBase{
  val foreignPropertyCheckDetailsShowAgentUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.showAgent().url
  val foreignPropertyAccountingMethodAgentUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.showAgent().url

  val foreignPropertyCheckDetailsSubmitAgentUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submitAgent().url
  val foreignPropertyReportingMethodShowAgentUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.showAgent("ABC123456789").url

  val errorPageUrl: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showForeignPropertyAgent().url

  val sessionData: Map[String, String] = Map(
    foreignPropertyStartDate -> "2023-01-01",
    addForeignPropertyAccountingMethod -> "ACCRUALS"
  )

  val testStartDate = "1 January 2023"
  val testAccountingMethod = "Traditional accounting"

  s"calling GET $foreignPropertyCheckDetailsShowAgentUrl" should {
    "render the FP check details page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

        When(s"I call $foreignPropertyCheckDetailsShowAgentUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-check-details", sessionData ++ clientDetailsWithConfirmation)
        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.add.foreign-property-check-details.title"),
          elementTextByID("foreign-property-date-value")(testStartDate),
          elementTextByID("business-accounting-value")(testAccountingMethod)
        )
      }
      "return an INTERNAL_SERVER_ERROR" when {
        "User is missing session data" in {
          Given("Income Sources FS is enabled")
          stubAuthorisedAgentUser(authorised = true)
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
          IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

          When(s"I call $foreignPropertyCheckDetailsShowAgentUrl")
          val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-check-details", Map(
            foreignPropertyStartDate -> "",
            addForeignPropertyAccountingMethod -> ""
          ) ++ clientDetailsWithConfirmation)
          result should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }

  s"calling POST $foreignPropertyCheckDetailsSubmitAgentUrl" should {
    s"redirect to $foreignPropertyReportingMethodShowAgentUrl" when {
      "user selects 'confirm and continue'" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = Map(
          "foreignPropertyStartDate" -> Seq("2023-01-01"),
          "addForeignPropertyAccountingMethod" -> Seq("ACCRUALS")
        )

        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

        When(s"I call $foreignPropertyCheckDetailsSubmitAgentUrl")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-check-details", sessionData ++ clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyReportingMethodShowAgentUrl)
        )
      }
    }
    s"redirect to $errorPageUrl" when {
      "error in response from API" in {
        Given("Income Sources FS is enabled")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = Map(
          "foreignPropertyStartDate" -> Seq("2023-01-01"),
          "addForeignPropertyAccountingMethod" -> Seq("ACCRUALS")
        )

        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, List.empty)

        When(s"I call $foreignPropertyCheckDetailsSubmitAgentUrl")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-check-details", sessionData ++ clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorPageUrl)
        )
      }
    }
  }
}

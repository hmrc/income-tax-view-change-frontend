package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class ForeignPropertyStartDateControllerISpec extends ComponentSpecBase {
  val foreignPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyStartDateController.show().url
  val foreignPropertyStartDateSubmitUrl: String = controllers.incomeSources.add.routes.ForeignPropertyStartDateController.submit().url
  val foreignPropertyStartDateCheckUrl: String = controllers.incomeSources.add.routes.ForeignPropertyStartDateCheckController.show().url

  val hintText: String = messagesAPI("incomeSources.add.foreignProperty.startDate.hint") + " " +
    messagesAPI("incomeSources.add.foreignProperty.startDate.hintExample")
  val continueButtonText: String = messagesAPI("base.continue")

  s"calling GET $foreignPropertyStartDateShowUrl" should {
    "render the foreign property start date page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $foreignPropertyStartDateShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-start-date")
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.add.foreignProperty.startDate.heading"),
          elementTextByID("foreign-property-start-date-hint")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $foreignPropertyStartDateSubmitUrl" should {
    s"redirect to $foreignPropertyStartDateCheckUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("foreign-property-start-date.day" -> Seq("20"), "foreign-property-start-date.month" -> Seq("12"),
            "foreign-property-start-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-start-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyStartDateCheckUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("foreign-property-start-date.day" -> Seq("aa"), "foreign-property-start-date.month" -> Seq("02"),
            "foreign-property-start-date.year" -> Seq("2023"))
        }

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-start-date")(formData)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("foreign-property-start-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.add.foreignProperty.startDate.error.invalid"))
        )
      }
    }
  }
}
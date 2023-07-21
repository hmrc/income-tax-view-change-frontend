package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import forms.utils.SessionKeys.{addBusinessAccountingMethod, addBusinessAccountingPeriodEndDate, addBusinessAddressLine1, addBusinessPostalCode, addForeignPropertyAccountingMethod, businessName, businessStartDate, businessTrade, foreignPropertyStartDate}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.createIncomeSource.CreateIncomeSourceResponse
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndUkProperty, noPropertyOrBusinessResponse}

class ForeignPropertyCheckDetailsControllerISpec extends ComponentSpecBase{

  val foreignPropertyCheckDetailsShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.show().url
  val foreignPropertyAccountingMethodUrl: String = controllers.incomeSources.add.routes.ForeignPropertyAccountingMethodController.show().url

  val foreignPropertyCheckDetailsSubmitUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submit().url
  val foreignPropertyReportingMethodShowUrl: String = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.show("ABC123456789").url

  val sessionData: Map[String, String] = Map(
    foreignPropertyStartDate -> "2023-01-01",
    addForeignPropertyAccountingMethod -> "ACCRUALS"
  )

  s"calling GET $foreignPropertyCheckDetailsShowUrl" should {
    "render the FP check details page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

        When(s"I call $foreignPropertyCheckDetailsShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-check-details", sessionData)
        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.add.foreign-property-check-details.title")
        )
      }
      "return an INTERNAL_SERVER_ERROR" when {
        "User is missing session data" in {
          Given("Income Sources FS is enabled")
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
          IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

          When(s"I call $foreignPropertyCheckDetailsShowUrl")
          val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-check-details", Map(
            foreignPropertyStartDate -> "",
            addForeignPropertyAccountingMethod -> ""
          ))
          result should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
  }

  s"calling POST $foreignPropertyCheckDetailsSubmitUrl" should {
    s"redirect to $foreignPropertyReportingMethodShowUrl" when {
      "user selects 'confirm and continue'" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = Map(
          "foreignPropertyStartDate" -> Seq("2023-01-01"),
          "addForeignPropertyAccountingMethod" -> Seq("ACCRUALS")
        )

        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

        When(s"I call $foreignPropertyCheckDetailsSubmitUrl")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-check-details", sessionData)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyReportingMethodShowUrl)
        )
      }
    }
  }
}

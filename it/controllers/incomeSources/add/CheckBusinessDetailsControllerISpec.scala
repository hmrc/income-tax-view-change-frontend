package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import forms.utils.SessionKeys.{addBusinessAccountingMethod, addBusinessAddressLine1, addBusinessPostCode, businessName, businessStartDate, businessTrade}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{noPropertyOrBusinessResponse}

class CheckBusinessDetailsControllerISpec extends ComponentSpecBase{

  val checkBusinessDetailsShowUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url
  val checkBusinessDetailsSubmitUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.submit().url
  val checkBusinessReportingMethodUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.changeBusinessReportingMethod().url + "?IncomeSourceID=123"

  val sessionData: Map[String, String] = Map(businessName -> "Test Business",
                                            businessStartDate -> "2022-01-01",
                                            businessTrade -> "Plumbing",
                                            addBusinessAddressLine1 -> "Test Road",
                                            addBusinessPostCode -> "B32 1PQ",
                                            addBusinessAccountingMethod -> "Quarterly")
  val testBusinessName: String = "Test Business"
  val testBusinessStartDate: String = "1 January 2022"
  val testBusinessTrade: String = "Plumbing"
  val testBusinessAddressLine1: String = "Test Road"
  val testBusinessPostCode: String = "B32 1PQ"
  val testBusinessAccountingMethod: String = "Quarterly"
  val id: String = "123"
  val continueButtonText: String = messagesAPI("check-business-details.confirm-button")


  s"calling GET $checkBusinessDetailsShowUrl" should {
    "render the Check Business details page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $checkBusinessDetailsShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/business-check-details", sessionData)
        println("LOOOOK" + result)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("check-business-details.heading"),
          elementTextByID("business-name-value")(testBusinessName),
          elementTextByID("business-date-value")(testBusinessStartDate),
          elementTextByID("business-trade-value")(testBusinessTrade),
          elementTextByID("business-address-value")(testBusinessAddressLine1 + " " + testBusinessPostCode),
          elementTextByID("business-accounting-value")(testBusinessAccountingMethod),
          elementTextByID("confirm-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $checkBusinessDetailsSubmitUrl" should {
    s"redirect to $checkBusinessReportingMethodUrl+?IncomeSourceID=123" when {
      "user selects 'confirm and continue'" in {
        val formData: Map[String,Seq[String]] = Map("addBusinessName" -> Seq("Test Business Name"),
          "addBusinessTrade" -> Seq("Test Business Name"),
          "addBusinessStartDate" -> Seq("Test Business Name"),
          "addBusinessAddressLine1" -> Seq("Test Business Name"),
          "addBusinessPostalCode" -> Seq("Test Business Name"),
          "addBusinessAccountingMethod" -> Seq("Test Business Name"))
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/add/business-check-details", sessionData)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkBusinessReportingMethodUrl)
        )
      }
    }

    s"return BAD_REQUEST $checkBusinessDetailsShowUrl" when {
      "user does not select anything" in {
        val formData: Map[String, Seq[String]] = Map("addBusinessName" -> Seq(""),
          "addBusinessTrade" -> Seq(""),
          "addBusinessStartDate" -> Seq(""),
          "addBusinessAddressLine1" -> Seq(""),
          "addBusinessPostalCode" -> Seq(""),
          "addBusinessAccountingMethod" -> Seq(""))
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-accounting-method")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
        )
      }
    }
  }

}

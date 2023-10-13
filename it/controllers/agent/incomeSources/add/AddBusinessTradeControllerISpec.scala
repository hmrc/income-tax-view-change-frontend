package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.BusinessTradeForm
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.AddIncomeSourceData.{businessNameField, businessTradeField}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSessionId, testSessionIdAgent}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesResponse, noPropertyOrBusinessResponse}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}

class AddBusinessTradeControllerISpec extends ComponentSpecBase {

  val addBusinessTradeControllerShowUrl: String = controllers.incomeSources.add.routes.AddBusinessTradeController.show(isAgent = true, isChange = false).url
  val addBusinessTradeSubmitUrl = controllers.incomeSources.add.routes.AddBusinessTradeController.submit(isAgent = true, isChange = false).url
  val changeBusinessTradeUrl: String = controllers.incomeSources.add.routes.AddBusinessTradeController.show(isAgent = true, isChange = true).url
  val submitChangeBusinessTradeUrl: String = controllers.incomeSources.add.routes.AddBusinessTradeController.submit(isAgent = true, isChange = true).url

  val addBusinessAddressUrl = controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent(isChange = false).url
  val incomeSourcesUrl: String = controllers.routes.HomeController.showAgent.url
  val checkDetailsUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url

  val pageTitleMsgKey: String = messagesAPI("add-business-trade.heading")
  val pageHint: String = messagesAPI("add-business-trade.p1")
  val button: String = messagesAPI("base.continue")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val UIJourneySessionDataRepository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]
  val journeyType: JourneyType = JourneyType(Add, SelfEmployment)
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(testSessionIdAgent)))


  s"calling GET $addBusinessTradeControllerShowUrl" should {
    "render the Add Business trade page for an Agent" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple businesses and a uk property")
        stubAuthorisedAgentUser(true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
        When(s"I call GET $addBusinessTradeControllerShowUrl")
        val res = IncomeTaxViewChangeFrontend.getAddBusinessTrade(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextByID("business-trade-hint")(pageHint),
          elementTextByID("continue-button")(button)
        )
      }
    }
    "303 SEE_OTHER - redirect to home page" when {
      "Income Sources FS disabled" in {
        stubAuthorisedAgentUser(true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${addBusinessTradeControllerShowUrl}")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessTrade(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourcesUrl)
        )
      }
    }
  }

  s"calling POST ${addBusinessTradeSubmitUrl}" should {
    s"303 SEE_OTHER and redirect to $addBusinessAddressUrl" when {
      "User is authorised and business trade is valid" in {
        stubAuthorisedAgentUser(true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val testBusinessTrade: String = ("Test Business Trade")
        val testBusinessName: String = ("Test Business Name")

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessTradeForm.businessTrade -> Seq(testBusinessTrade)
          )
        }

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
          addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

        And("Mongo storage is successfully set")

        When(s"I call POST ${addBusinessTradeSubmitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-trade", clientDetailsWithConfirmation)(formData)

        sessionService.getMongoKeyTyped[String](businessTradeField, JourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(testBusinessTrade))

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessAddressUrl)
        )
      }
    }
    "show error when form is filled incorrectly" in {
      stubAuthorisedAgentUser(true)
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

      val formData: Map[String, Seq[String]] = {
        Map(
          BusinessTradeForm.businessTrade -> Seq("")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-trade", clientDetailsWithConfirmation)(formData)

      result should have(
        httpStatus(BAD_REQUEST),
        elementTextByID("business-trade-error")(messagesAPI("base.error-prefix") + " " +
          messagesAPI("add-business-trade.form.error.empty"))
      )
    }
  }

  s"calling GET $changeBusinessTradeUrl" should {
    "render the Change Business Trade page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple businesses and a uk property")
        stubAuthorisedAgentUser(true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

        When(s"I call GET $changeBusinessTradeUrl")
        val res = IncomeTaxViewChangeFrontend.getAddBusinessTrade(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        res should have(
          httpStatus(OK),
          pageTitleAgent(pageTitleMsgKey),
          elementTextByID("business-trade-hint")(pageHint),
          elementTextByID("continue-button")(button)
        )
      }
    }
    "303 SEE_OTHER - redirect to home page" when {
      "Income Sources FS disabled" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(true)
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${changeBusinessTradeUrl}")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessTrade(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourcesUrl)
        )
      }
    }
  }

  s"calling POST ${submitChangeBusinessTradeUrl}" should {
    s"303 SEE_OTHER and redirect to $addBusinessAddressUrl" when {
      "User is authorised and business trade is valid" in {
        stubAuthorisedAgentUser(true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessTradeForm.businessTrade -> Seq("Test Business Trade")
          )
        }

        When(s"I call POST ${changeBusinessTradeUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-trade", clientDetailsWithConfirmation)(formData)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkDetailsUrl)
        )
      }
    }
    "show error when form is filled incorrectly" in {
      stubAuthorisedAgentUser(true)
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

      val formData: Map[String, Seq[String]] = {
        Map(
          BusinessTradeForm.businessTrade -> Seq("")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-trade", clientDetailsWithConfirmation)(formData)

      result should have(
        httpStatus(BAD_REQUEST),
        elementTextByID("business-trade-error")(messagesAPI("base.error-prefix") + " " +
          messagesAPI("add-business-trade.form.error.empty"))
      )
    }
  }
}

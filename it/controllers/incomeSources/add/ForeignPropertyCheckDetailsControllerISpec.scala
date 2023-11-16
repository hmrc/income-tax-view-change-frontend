package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.ForeignProperty
import enums.JourneyType.{Add, JourneyType}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.createIncomeSource.CreateIncomeSourceResponse
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testDate, testMtditid, testSelfEmploymentId, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

import java.time.LocalDate

class ForeignPropertyCheckDetailsControllerISpec extends ComponentSpecBase{

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val uiRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val showUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.show().url
  val foreignPropertyAccountingMethodUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(ForeignProperty).url
  val submitUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.submit().url
  val foreignPropertyReportingMethodShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceReportingMethodController.show(isAgent = false, ForeignProperty, "ABC123456789").url
  val errorPageUrl: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(ForeignProperty).url

  val testPropertyStartDateLong: String = "1 January 2023"
  val testPropertyStartDate: LocalDate = LocalDate.of(2023, 1, 1)
  val testPropertyAccountingMethod: String = "CASH"
  val testPropertyAccountingMethodView: String = "Cash basis accounting"
  val continueButtonText: String = messagesAPI("base.confirm-and-continue")
  val testJourneyType: JourneyType = JourneyType(Add, ForeignProperty)
  val testJourneyTypeString: String = JourneyType(Add, ForeignProperty).toString

  val testAddIncomeSourceData = AddIncomeSourceData(
    dateStarted = Some(testPropertyStartDate),
    incomeSourcesAccountingMethod = Some(testPropertyAccountingMethod)
  )

  val testUIJourneySessionData: UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = testJourneyTypeString,
    addIncomeSourceData = Some(testAddIncomeSourceData))

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(uiRepository.deleteOne(UIJourneySessionData(testSessionId, testJourneyTypeString)))
  }

  s"calling GET $showUrl" should {
    "render the FP check details page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData))

        When(s"I call $showUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-check-details")
        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.add.foreign-property-check-details.title"),
          elementTextByID("foreign-property-date-value")(testPropertyStartDateLong),
          elementTextByID("business-accounting-value")(testPropertyAccountingMethodView)
        )
      }
      "return an INTERNAL_SERVER_ERROR" when {
        "User is missing session data" in {
          Given("Income Sources FS is enabled")
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-FP", addIncomeSourceData =
            Some(AddIncomeSourceData(dateStarted = Some(testDate), incomeSourcesAccountingMethod = None)))))


          val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
          IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

          When(s"I call $showUrl")

          val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-check-details")
          result should have(
            httpStatus(INTERNAL_SERVER_ERROR)
          )
        }
      }
    }
    "303 SEE_OTHER and redirect to home page" when {
      "Income Sources FS disabled" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData))

        When(s"I call POST $showUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-check-details")

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI("/report-quarterly/income-and-expenses/view")
        )
      }
    }
  }


  s"calling POST $submitUrl" should {
    s"redirect to $foreignPropertyReportingMethodShowUrl" when {
      "user selects 'confirm and continue'" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        Given("I wiremock stub a successful Create Income Sources (Foreign Property) response")
        val createResponseJson = List(CreateIncomeSourceResponse("ABC123456789"))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, createResponseJson)

        await(sessionService.setMongoData(testUIJourneySessionData))

        val formData: Map[String, Seq[String]] = Map(
          "tradingStartDate" -> Seq("2023-01-01"),
          "cashOrAccrualsFlag" -> Seq("CASH")
        )

        When(s"I call POST $submitUrl")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-check-details")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyReportingMethodShowUrl)
        )
      }
    }
    s"redirect to $errorPageUrl" when {
      "error in response from API 1776" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        Given("I wiremock stub an unsuccessful Create Income Sources (Foreign Property) response")

        val formData: Map[String, Seq[String]] = Map(
          "tradingStartDate" -> Seq("2023-01-01"),
          "cashOrAccrualsFlag" -> Seq("CASH")
        )

        IncomeTaxViewChangeStub.stubCreateBusinessDetailsErrorResponse(testMtditid)

        When(s"I call POST $submitUrl")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-check-details")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorPageUrl)
        )
      }
    }
    "500 ISE" when {
      "User has not completed the Add Foreign Property journey" in {
        Given("I wiremock stub a successful Create Income Sources (Foreign Property) response")
        enable(IncomeSources)
        val createResponseJson = List(CreateIncomeSourceResponse("1234567890"))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, createResponseJson)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-FP", addIncomeSourceData =
          Some(AddIncomeSourceData(dateStarted = Some(testDate), incomeSourcesAccountingMethod = None)))))

        val formData: Map[String, Seq[String]] = Map(
          "tradingStartDate" -> Seq("2021-01-01"),
          "cashOrAccrualsFlag" -> Seq("CASH")
        )

        When(s"I call POST $submitUrl")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-check-details")(formData)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }
}


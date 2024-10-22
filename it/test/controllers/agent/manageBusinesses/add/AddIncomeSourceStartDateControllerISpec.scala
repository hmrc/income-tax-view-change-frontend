/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.agent.manageBusinesses.add

import models.admin.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import helpers.agent.AgentComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.AddIncomeSourceData.dateStartedField
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, noPropertyOrBusinessResponse}

import java.time.LocalDate

class AddIncomeSourceStartDateControllerISpec extends AgentComponentSpecBase {

  val addBusinessStartDateChangeShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = true).url
  val addBusinessStartDateShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false).url
  val addBusinessStartDateSubmitUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.submit(incomeSourceType = SelfEmployment, isAgent = true, isChange = false).url
  val addBusinessStartDateCheckShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = false).url
  val addBusinessStartDateCheckChangeShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = true, isChange = true).url
  val prefixSoleTraderBusiness: String = "add-business-start-date"
  val continueButtonText: String = messagesAPI("base.continue")

  val hintTextBusiness: String = messagesAPI("add-business-start-date.hint") + " " + messagesAPI("add-business-start-date.hint2") + " " +
    messagesAPI("dateForm.hint")

  val addUKPropertyStartDateShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = true, isChange = false).url
  val addUKPropertyStartDateChangeShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = true, isChange = true).url
  val addUKPropertyStartDateCheckChangeShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = true, isChange = true).url
  val addUKPropertyStartDateSubmitUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.submit(incomeSourceType = UkProperty, isAgent = true, isChange = false).url
  val checkUKPropertyStartDateShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = true, isChange = false).url

  val hintTextUKProperty: String =  messagesAPI("incomeSources.add.UKPropertyStartDate.hint") + " " + messagesAPI("incomeSources.add.UKPropertyStartDate.hint2") + " " +
    messagesAPI("dateForm.hint")

  val foreignPropertyStartDateShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = false).url
  val foreignPropertyStartDateSubmitUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.submit(incomeSourceType = ForeignProperty, isAgent = true, isChange = false).url
  val foreignPropertyStartDateCheckUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = false).url
  val addForeignPropertyStartDateChangeShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = true).url
  val addForeignPropertyStartDateCheckChangeShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = true, isChange = true).url

  val prefixForeignProperty = "incomeSources.add.foreignProperty.startDate"

  val hintTextForeignProperty: String = messagesAPI("incomeSources.add.foreignProperty.startDate.hint") + " " + messagesAPI("incomeSources.add.foreignProperty.startDate.hint2") + " " +
    messagesAPI("dateForm.hint")

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val journeyTypeSE: JourneyType = JourneyType(Add, SelfEmployment)
  val journeyTypeUK: JourneyType = JourneyType(Add, UkProperty)
  val journeyTypeFP: JourneyType = JourneyType(Add, ForeignProperty)
  val testBusinessStartDate: LocalDate = LocalDate.of(2022, 10, 10)
  val testBusinessName: String = "Test Business"
  val testBusinessTrade: String = "Plumbing"

  val testAddIncomeSourceData: IncomeSourceType => AddIncomeSourceData = (incomeSourceType: IncomeSourceType) =>
    if (incomeSourceType.equals(SelfEmployment)) {
      AddIncomeSourceData(
        businessName = Some(testBusinessName),
        businessTrade = Some(testBusinessTrade),
      )
    } else {
      AddIncomeSourceData(
        businessName = None,
        businessTrade = None
      )
    }

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(testAddIncomeSourceData(incomeSourceType)))

  val testAddIncomeSourceDataWithStartDate: IncomeSourceType => AddIncomeSourceData = (incomeSourceType: IncomeSourceType) =>
    testAddIncomeSourceData(incomeSourceType).copy(dateStarted = Some(testBusinessStartDate))

  def testUIJourneySessionDataWithStartDate(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(testAddIncomeSourceDataWithStartDate(incomeSourceType)))

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  s"calling GET $addBusinessStartDateShowUrl" should {
    "render the Add Business Start Date Page" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $addBusinessStartDateShowUrl")
        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.getAddBusinessStartDate(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent(s"$prefixSoleTraderBusiness.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addBusinessStartDateSubmitUrl" should {
    s"redirect to $addBusinessStartDateCheckShowUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map(
            "value.day" -> Seq("10"),
            "value.month" -> Seq("10"),
            "value.year" -> Seq("2022")
          )
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend
          .post("/manage-your-businesses/add-sole-trader/business-start-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateCheckShowUrl)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeSE).futureValue shouldBe Right(Some(testBusinessStartDate))
      }
    }
    s"return a BAD_REQUEST" when {
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map(
            "value.day" -> Seq("$"),
            "value.month" -> Seq("%"),
            "value.year" -> Seq("&")
          )
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend
          .post("/manage-your-businesses/add-sole-trader/business-start-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("value-error")(messagesAPI("base.error-prefix") + ": " +
            messagesAPI(s"$prefixSoleTraderBusiness.error.invalid"))
        )
      }
    }
  }
  s"calling GET $addUKPropertyStartDateShowUrl" should {
    "render the Add UK Property Business Start Date" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $addUKPropertyStartDateShowUrl")

        await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

        val result = IncomeTaxViewChangeFrontend.get("/manage-your-businesses/add-uk-property/business-start-date", clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.add.UKPropertyStartDate.heading"),
          elementTextByID("value-hint")(hintTextUKProperty),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addUKPropertyStartDateSubmitUrl" should {
    s"redirect to $checkUKPropertyStartDateShowUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("10"), "value.month" -> Seq("10"),
            "value.year" -> Seq("2022"))
        }
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/add-uk-property/business-start-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkUKPropertyStartDateShowUrl)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeUK).futureValue shouldBe Right(Some(testBusinessStartDate))
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("aa"), "value.month" -> Seq("12"),
            "value.year" -> Seq("2022"))
        }

        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/add-uk-property/business-start-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("value-error")(messagesAPI("base.error-prefix") + ": " +
            messagesAPI("incomeSources.add.UKPropertyStartDate.error.invalid"))
        )
      }
    }
  }
  s"calling GET $foreignPropertyStartDateShowUrl" should {
    "render the foreign property start date page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $foreignPropertyStartDateShowUrl")

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.get("/manage-your-businesses/add-foreign-property/business-start-date", clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.add.foreignProperty.startDate.heading"),
          elementTextByID("value-hint")(hintTextForeignProperty),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $foreignPropertyStartDateSubmitUrl" should {
    s"redirect to $foreignPropertyStartDateCheckUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("10"), "value.month" -> Seq("10"),
            "value.year" -> Seq("2022"))
        }
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/add-foreign-property/business-start-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyStartDateCheckUrl)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeFP).futureValue shouldBe Right(Some(testBusinessStartDate))
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("aa"), "value.month" -> Seq("02"),
            "value.year" -> Seq("2023"))
        }
        stubAuthorisedAgentUser(authorised = true)
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/add-foreign-property/business-start-date", clientDetailsWithConfirmation)(formData)
        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("value-error")(messagesAPI("base.error-prefix") + ": " +
            messagesAPI("incomeSources.add.foreignProperty.startDate.error.invalid"))
        )
      }
    }
  }
  s"calling GET $addUKPropertyStartDateChangeShowUrl" should {
    "render the Add UK Property Business Start Date" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $addUKPropertyStartDateChangeShowUrl")

        await(sessionService.setMongoData(testUIJourneySessionDataWithStartDate(UkProperty)))

        val result = IncomeTaxViewChangeFrontend.get("/manage-your-businesses/add-uk-property/change-business-start-date", clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.add.UKPropertyStartDate.heading"),
          elementTextByID("value-hint")(hintTextUKProperty),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling GET $addForeignPropertyStartDateChangeShowUrl" should {
    "render the Add Foreign Property Business Start Date" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $addForeignPropertyStartDateChangeShowUrl")

        await(sessionService.setMongoData(testUIJourneySessionDataWithStartDate(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.get("/manage-your-businesses/add-foreign-property/change-business-start-date", clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.add.foreignProperty.startDate.heading"),
          elementTextByID("value-hint")(hintTextForeignProperty),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling GET $addBusinessStartDateChangeShowUrl" should {
    "render the Add Business Start Date" when {
      "User is authorised" in {
        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $addBusinessStartDateChangeShowUrl")
        await(sessionService.setMongoData(testUIJourneySessionDataWithStartDate(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.get("/manage-your-businesses/add-sole-trader/change-business-start-date", clientDetailsWithConfirmation )
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("add-business-start-date.heading"),
          elementTextByID("value-hint")(hintTextBusiness),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addBusinessStartDateChangeShowUrl" should {
    "render the Add Business Start Date" when {
      "User is authorised" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("10"), "value.month" -> Seq("10"),
            "value.year" -> Seq("2022"))
        }

        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/add-sole-trader/change-business-start-date", clientDetailsWithConfirmation)(formData)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateCheckChangeShowUrl)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeSE).futureValue shouldBe Right(Some(testBusinessStartDate))
      }
    }
  }
  s"calling POST $addForeignPropertyStartDateChangeShowUrl" should {
    "render the Add Foreign Property Start Date" when {
      "User is authorised" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("10"), "value.month" -> Seq("10"),
            "value.year" -> Seq("2022"))
        }

        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/add-foreign-property/change-business-start-date", clientDetailsWithConfirmation)(formData)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addForeignPropertyStartDateCheckChangeShowUrl)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeFP).futureValue shouldBe Right(Some(testBusinessStartDate))
      }
    }
  }
  s"calling POST $addUKPropertyStartDateChangeShowUrl" should {
    "render the Add UK Property Start Date" when {
      "User is authorised" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("10"), "value.month" -> Seq("10"),
            "value.year" -> Seq("2022"))
        }

        stubAuthorisedAgentUser(authorised = true)

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/add-uk-property/change-business-start-date", clientDetailsWithConfirmation)(formData)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addUKPropertyStartDateCheckChangeShowUrl)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeUK).futureValue shouldBe Right(Some(testBusinessStartDate))
      }
    }
  }
}

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

package controllers.agent.manageBusinesses.cease

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.CeaseIncomeSourceData.{dateCeasedField, incomeSourceIdField}
import models.incomeSourceDetails.{CeaseIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyAndCeasedBusiness, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

import java.time.LocalDate

class IncomeSourceEndDateControllerISpec extends ComponentSpecBase {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val repository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val dateBusinessShowAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.showAgent(Some(testSelfEmploymentId), SelfEmployment).url
  val dateBusinessSubmitAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submitAgent(Some(testSelfEmploymentId), SelfEmployment).url
  val dateBusinessShowChangeAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.showChangeAgent(Some(testSelfEmploymentId), SelfEmployment).url
  val dateBusinessSubmitChangeAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submitChangeAgent(Some(testSelfEmploymentId), SelfEmployment).url
  val checkCeaseBusinessDetailsShowAgentUrl: String = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(SelfEmployment).url

  val dateUKPropertyShowAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.showAgent(None, UkProperty).url
  val dateUKPropertySubmitAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submitAgent(None, UkProperty).url
  val dateUKPropertyShowChangeAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.showChangeAgent(None, UkProperty).url
  val dateUKPropertySubmitChangeAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submitChangeAgent(None, UkProperty).url
  val checkYourCeaseDetailsUkPropertyShowAgentUrl: String = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(UkProperty).url

  val dateForeignPropertyShowAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.showAgent(None, ForeignProperty).url
  val dateForeignPropertySubmitAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submitAgent(None, ForeignProperty).url
  val dateForeignPropertyShowChangeAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.showChangeAgent(None, ForeignProperty).url
  val dateForeignPropertySubmitChangeAgentUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submitChangeAgent(None, ForeignProperty).url
  val checkYourCeaseDetailsForeignPropertyShowAgentUrl: String = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.showAgent(ForeignProperty).url

  def hintText(incomeSourceType: IncomeSourceType): String = {
    if (!(incomeSourceType == SelfEmployment)) {
      messagesAPI(s"${incomeSourceType.endDateMessagePrefix}.hint-1") + " " + messagesAPI("dateForm.hint")
    }
    else {
      messagesAPI("dateForm.hint")
    }
  }

  val continueButtonText: String = messagesAPI("base.continue")
  val testChangeDay: String = "10"
  val testChangeMonth: String = "10"
  val testChangeYear: String = "2022"
  val testSessionEndDateValue: LocalDate = LocalDate.of(2022, 8, 27)
  val testSessionEndDateValueProperty: LocalDate = LocalDate.of(2022, 12, 20)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  s"calling GET $dateBusinessShowAgentUrl" should {
    "render the Date Business Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a business")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $dateBusinessShowAgentUrl")
        val result = IncomeTaxViewChangeFrontend.getBusinessEndDate(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.cease.endDate.selfEmployment.heading"),
          elementTextByID("income-source-end-date-hint")(hintText(SelfEmployment)),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateBusinessSubmitAgentUrl" should {
    "redirect to showIncomeSourceEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("10"), "income-source-end-date.month" -> Seq("10"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)


        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        val result = IncomeTaxViewChangeFrontend.post(
          s"/manage-your-businesses/cease/business-end-date?id=$testSelfEmploymentIdHashed",
          additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkCeaseBusinessDetailsShowAgentUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(Some(LocalDate.parse(testEndDate2022)))
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(Some(testSelfEmploymentId))

      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("5"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/manage-your-businesses/cease/business-end-date?id=$testSelfEmploymentIdHashed", additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.selfEmployment.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(None)
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(None)

      }
    }
  }

  s"calling GET $dateBusinessShowChangeAgentUrl" should {
    "render the Date Business Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a business")
        disable(IncomeSources)
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        When(s"I call GET $dateBusinessShowChangeAgentUrl")
        val result = IncomeTaxViewChangeFrontend
          .get(s"/manage-your-businesses/cease/change-business-end-date?id=$testSelfEmploymentIdHashed", clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.cease.endDate.selfEmployment.heading"),
          elementTextByID("income-source-end-date-hint")(hintText(SelfEmployment)),
          elementAttributeBySelector("input[id=income-source-end-date.day]", "value")(testChangeDay),
          elementAttributeBySelector("input[id=income-source-end-date.month]", "value")(testChangeMonth),
          elementAttributeBySelector("input[id=income-source-end-date.year]", "value")(testChangeYear),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateBusinessSubmitChangeAgentUrl" should {
    "redirect to showIncomeSourceEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("27"), "income-source-end-date.month" -> Seq("08"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))


        val result = IncomeTaxViewChangeFrontend.post(s"/manage-your-businesses/cease/change-business-end-date?id=$testSelfEmploymentIdHashed", additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkCeaseBusinessDetailsShowAgentUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(Some(testSessionEndDateValue))
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(Some(testSelfEmploymentId))
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("5"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/manage-your-businesses/cease/change-business-end-date?id=$testSelfEmploymentIdHashed", additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.selfEmployment.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(None)
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(None)

      }
    }
  }

  s"calling GET $dateUKPropertyShowAgentUrl" should {
    "render the Date UK Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $dateUKPropertyShowAgentUrl")
        val result = IncomeTaxViewChangeFrontend.getUKPropertyEndDate(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.cease.endDate.ukProperty.heading"),
          elementTextByID("income-source-end-date-hint")(hintText(UkProperty)),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateUKPropertySubmitAgentUrl" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("10"), "income-source-end-date.month" -> Seq("10"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))


        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/cease/uk-property-end-date", additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsUkPropertyShowAgentUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(Some(LocalDate.parse(testEndDate2022)))

      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/cease/uk-property-end-date", additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.ukProperty.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(None)

      }
    }
  }

  s"calling GET $dateUKPropertyShowChangeAgentUrl" should {
    "render the Date UK Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        When(s"I call GET $dateUKPropertyShowChangeAgentUrl")
        val result = IncomeTaxViewChangeFrontend.get(s"/manage-your-businesses/cease/change-uk-property-end-date", clientDetailsWithConfirmation)

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.cease.endDate.ukProperty.heading"),
          elementTextByID("income-source-end-date-hint")(hintText(UkProperty)),
          elementAttributeBySelector("input[id=income-source-end-date.day]", "value")(testChangeDay),
          elementAttributeBySelector("input[id=income-source-end-date.month]", "value")(testChangeMonth),
          elementAttributeBySelector("input[id=income-source-end-date.year]", "value")(testChangeYear),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateUKPropertySubmitChangeAgentUrl" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("20"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))


        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/cease/change-uk-property-end-date", additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsUkPropertyShowAgentUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(Some(testSessionEndDateValueProperty))

      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/cease/change-uk-property-end-date", additionalCookies = clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.ukProperty.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(None)

      }
    }
  }

  s"calling GET $dateForeignPropertyShowAgentUrl" should {
    "render the Date Foreign Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        When(s"I call GET $dateForeignPropertyShowAgentUrl")
        val result = IncomeTaxViewChangeFrontend.getForeignPropertyEndDate(clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.cease.endDate.foreignProperty.heading"),
          elementTextByID("income-source-end-date-hint")(hintText(ForeignProperty)),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateForeignPropertySubmitAgentUrl" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("20"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))


        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/cease/foreign-property-end-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsForeignPropertyShowAgentUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(Some(testSessionEndDateValueProperty))

      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/cease/foreign-property-end-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.foreignProperty.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(None)

      }
    }
  }

  s"calling GET $dateForeignPropertyShowChangeAgentUrl" should {
    "render the Date Foreign Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyAndCeasedBusiness)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        When(s"I call GET $dateForeignPropertyShowChangeAgentUrl")
        val result = IncomeTaxViewChangeFrontend.get(s"/manage-your-businesses/cease/change-foreign-property-end-date", clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleAgent("incomeSources.cease.endDate.foreignProperty.heading"),
          elementTextByID("income-source-end-date-hint")(hintText(ForeignProperty)),
          elementAttributeBySelector("input[id=income-source-end-date.day]", "value")(testChangeDay),
          elementAttributeBySelector("input[id=income-source-end-date.month]", "value")(testChangeMonth),
          elementAttributeBySelector("input[id=income-source-end-date.year]", "value")(testChangeYear),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateForeignPropertySubmitChangeAgentUrl" should {
    "redirect to showFPPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("20"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))


        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/cease/change-foreign-property-end-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsForeignPropertyShowAgentUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(Some(testSessionEndDateValueProperty))

      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        stubAuthorisedAgentUser(authorised = true)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/manage-your-businesses/cease/change-foreign-property-end-date", clientDetailsWithConfirmation)(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.foreignProperty.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(None)

      }
    }
  }
}


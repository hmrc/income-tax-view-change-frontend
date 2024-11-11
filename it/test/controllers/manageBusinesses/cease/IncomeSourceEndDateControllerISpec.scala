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

package controllers.manageBusinesses.cease

import models.admin.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.CeaseIncomeSourceData.{dateCeasedField, incomeSourceIdField}
import models.incomeSourceDetails.{CeaseIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

import java.time.LocalDate

class IncomeSourceEndDateControllerISpec extends ComponentSpecBase {

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val isAgent: Boolean = false

  val manageBusinessesUrl: String = controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url

  val dateBusinessShowUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(Some(testSelfEmploymentId), SelfEmployment, isAgent, isChange = false).url
  val dateBusinessSubmitUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submit(Some(testSelfEmploymentId), SelfEmployment , isAgent, isChange = false).url
  val dateBusinessShowChangeUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(Some(testSelfEmploymentId), SelfEmployment, isAgent, isChange = true).url
  val dateBusinessSubmitChangeUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submit(Some(testSelfEmploymentId), SelfEmployment, isAgent, isChange = true).url
  val checkCeaseBusinessDetailsShowUrl: String = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.show(SelfEmployment).url

  val dateUKPropertyShowUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(None, UkProperty, isAgent, isChange = false).url
  val dateUKPropertySubmitUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submit(None, UkProperty, isAgent, isChange = false).url
  val dateUKPropertyShowChangeUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(None, UkProperty, isAgent, isChange = true).url
  val dateUKPropertySubmitChangeUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submit(None, UkProperty, isAgent, isChange = true).url
  val checkYourCeaseDetailsUkPropertyShowUrl: String = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.show(UkProperty).url

  val dateForeignPropertyShowUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(None, ForeignProperty, isAgent, isChange = false).url
  val dateForeignPropertySubmitUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submit(None, ForeignProperty, isAgent, isChange = false).url
  val dateForeignPropertyShowChangeUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.show(None, ForeignProperty, isAgent, isChange = true).url
  val dateForeignPropertySubmitChangeUrl: String = controllers.manageBusinesses.cease.routes.IncomeSourceEndDateController.submit(None, ForeignProperty, isAgent, isChange = true).url
  val checkYourCeaseDetailsForeignPropertyShowUrl: String = controllers.manageBusinesses.cease.routes.CeaseCheckIncomeSourceDetailsController.show(ForeignProperty).url

  val testSessionEndDateValue: String = "2022-08-27"
  val testSessionEndDateValueProperty: String = "2022-12-20"

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

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Cease))
  }

  s"calling GET $dateBusinessShowUrl" should {
    "render the Date Business Ceased Page" when {
      "User is authorised with session data" in {
        Given("I wiremock stub a successful Income Source Details response with a business")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE")))

        When(s"I call GET $dateBusinessShowUrl")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.getBusinessEndDate
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.selfEmployment.heading"),
          elementTextByID("value-hint")(hintText(SelfEmployment)),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }

    "redirect to the Manage Your Businesses Landing Page" when {
      "User is authorised without session data" in {
        Given("I wiremock stub a successful Income Source Details response with a business")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $dateBusinessShowUrl")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.getBusinessEndDate
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(manageBusinessesUrl)
        )
      }
    }
  }
  s"calling POST $dateBusinessSubmitUrl" should {
    s"redirect to $checkCeaseBusinessDetailsShowUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("10"), "value.month" -> Seq("10"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post(s"/manage-your-businesses/cease/business-end-date?id=$testSelfEmploymentIdHashed")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkCeaseBusinessDetailsShowUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(Some(LocalDate.parse(testEndDate2022)))
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(Some(testSelfEmploymentId))

      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("aa"), "value.month" -> Seq("5"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post(s"/manage-your-businesses/cease/business-end-date?id=$testSelfEmploymentIdHashed")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("value-error")(messagesAPI("base.error-prefix") + ": " +
            messagesAPI("incomeSources.cease.endDate.selfEmployment.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(None)
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(None)

      }
    }
  }

  s"calling GET $dateBusinessShowChangeUrl" should {
    "render the Date Business Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a business")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = None, journeyIsComplete = Some(false))))))


        When(s"I call GET $dateBusinessShowChangeUrl")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.get(s"/manage-your-businesses/cease/change-business-end-date?id=$testSelfEmploymentIdHashed")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.selfEmployment.heading"),
          elementTextByID("value-hint")(hintText(SelfEmployment)),
          elementAttributeBySelector("input[id=value.day]", "value")(testChangeDay),
          elementAttributeBySelector("input[id=value.month]", "value")(testChangeMonth),
          elementAttributeBySelector("input[id=value.year]", "value")(testChangeYear),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $dateBusinessSubmitChangeUrl" should {
    s"redirect to $checkCeaseBusinessDetailsShowUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("10"), "value.month" -> Seq("10"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-SE", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post(s"/manage-your-businesses/cease/change-business-end-date?id=$testSelfEmploymentIdHashed")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkCeaseBusinessDetailsShowUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(Some(LocalDate.parse(testEndDate2022)))
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(Some(testSelfEmploymentId))

      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("aa"), "value.month" -> Seq("5"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post(s"/manage-your-businesses/cease/change-business-end-date?id=$testSelfEmploymentIdHashed")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("value-error")(messagesAPI("base.error-prefix") + ": " +
            messagesAPI("incomeSources.cease.endDate.selfEmployment.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(None)
        sessionService.getMongoKey(incomeSourceIdField, JourneyType(Cease, SelfEmployment)).futureValue shouldBe Right(None)

      }
    }
  }


  s"calling GET $dateUKPropertyShowUrl" should {
    "render the Date UK Property Ceased Page" when {
      "User is authorised with session data" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK")))

        When(s"I call GET $dateUKPropertyShowUrl")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.getUKPropertyEndDate
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.ukProperty.heading"),
          elementTextByID("value-hint")(hintText(UkProperty)),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }

    "redirect to the Manage Your Businesses Landing Page" when {
      "User is authorised without session data" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $dateUKPropertyShowUrl")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.getUKPropertyEndDate
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(manageBusinessesUrl)
        )
      }
    }
  }
  s"calling POST $dateUKPropertySubmitUrl" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("10"), "value.month" -> Seq("10"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/cease/uk-property-end-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsUkPropertyShowUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(Some(LocalDate.parse(testEndDate2022)))
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("aa"), "value.month" -> Seq("12"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/cease/uk-property-end-date")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("value-error")(messagesAPI("base.error-prefix") + ": " +
            messagesAPI("incomeSources.cease.endDate.ukProperty.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(None)

      }
    }
  }

  s"calling GET $dateUKPropertyShowChangeUrl" should {
    "render the Date UK Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        When(s"I call GET $dateUKPropertyShowChangeUrl")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.get(s"/manage-your-businesses/cease/change-uk-property-end-date")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.ukProperty.heading"),
          elementTextByID("value-hint")(hintText(UkProperty)),
          elementAttributeBySelector("input[id=value.day]", "value")(testChangeDay),
          elementAttributeBySelector("input[id=value.month]", "value")(testChangeMonth),
          elementAttributeBySelector("input[id=value.year]", "value")(testChangeYear),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateUKPropertySubmitChangeUrl" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("10"), "value.month" -> Seq("10"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-UK", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/cease/change-uk-property-end-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsUkPropertyShowUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(Some(LocalDate.parse(testEndDate2022)))

      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("aa"), "value.month" -> Seq("12"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/cease/change-uk-property-end-date")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("value-error")(messagesAPI("base.error-prefix") + ": " +
            messagesAPI("incomeSources.cease.endDate.ukProperty.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, UkProperty)).futureValue shouldBe Right(None)

      }
    }
  }

  s"calling GET $dateForeignPropertyShowUrl" should {
    "render the Date Foreign Property Ceased Page" when {
      "User is authorised with session data" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP")))

        When(s"I call GET $dateForeignPropertyShowUrl")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.getForeignPropertyEndDate
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.foreignProperty.heading"),
          elementTextByID("value-hint")(hintText(ForeignProperty)),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }

    "redirect to the Manage Your Businesses Landing Page" when {
      "User is authorised without session data" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        When(s"I call GET $dateForeignPropertyShowUrl")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.getForeignPropertyEndDate
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(manageBusinessesUrl)
        )
      }
    }
  }
  s"calling POST $dateForeignPropertySubmitUrl" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("10"), "value.month" -> Seq("10"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/cease/foreign-property-end-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsForeignPropertyShowUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(Some(LocalDate.parse(testEndDate2022)))

      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("aa"), "value.month" -> Seq("12"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/cease/foreign-property-end-date")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("value-error")(messagesAPI("base.error-prefix") + ": " +
            messagesAPI("incomeSources.cease.endDate.foreignProperty.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(None)

      }
    }
  }

  s"calling GET $dateForeignPropertyShowChangeUrl" should {
    "render the Date Foreign Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))

        When(s"I call GET $dateForeignPropertyShowChangeUrl")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.get(s"/manage-your-businesses/cease/change-foreign-property-end-date")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.foreignProperty.heading"),
          elementTextByID("value-hint")(hintText(ForeignProperty)),
          elementAttributeBySelector("input[id=value.day]", "value")(testChangeDay),
          elementAttributeBySelector("input[id=value.month]", "value")(testChangeMonth),
          elementAttributeBySelector("input[id=value.year]", "value")(testChangeYear),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateForeignPropertySubmitChangeUrl" should {
    "redirect to showForeignPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("10"), "value.month" -> Seq("10"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "CEASE-FP", ceaseIncomeSourceData =
          Some(CeaseIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId), endDate = Some(LocalDate.parse(testEndDate2022)), ceaseIncomeSourceDeclare = Some(stringTrue), journeyIsComplete = Some(false))))))


        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/cease/change-foreign-property-end-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsForeignPropertyShowUrl)
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(Some(LocalDate.parse(testEndDate2022)))

      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("value.day" -> Seq("aa"), "value.month" -> Seq("12"), "value.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.post("/manage-your-businesses/cease/change-foreign-property-end-date")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("value-error")(messagesAPI("base.error-prefix") + ": " +
            messagesAPI("incomeSources.cease.endDate.foreignProperty.error.invalid"))
        )

        sessionService.getMongoKey(dateCeasedField, JourneyType(Cease, ForeignProperty)).futureValue shouldBe Right(None)

      }
    }
  }
}


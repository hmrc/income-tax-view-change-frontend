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

package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.AddIncomeSourceData.{accountingPeriodEndDateField, accountingPeriodStartDateField, dateStartedField}
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, clientDetailsWithStartDate, testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, noPropertyOrBusinessResponse, ukPropertyOnlyResponse}

import java.time.LocalDate

class AddIncomeSourceStartDateCheckControllerISpec extends ComponentSpecBase {
  val testDate: String = "2020-11-1"
  val addBusinessStartDateCheckShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url
  val addBusinessTradeShowUrl: String = controllers.incomeSources.add.routes.AddBusinessTradeController.show(isAgent = false, isChange = false).url
  val addBusinessStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url
  val addBusinessStartDateCheckSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url
  val continueButtonText: String = messagesAPI("base.continue")
  val incomeSourcePrefix: String = "start-date-check"
  val soleTraderBusinessPrefix: String = SelfEmployment.addStartDateCheckMessagesPrefix
  val ukPropertyPrefix: String = UkProperty.addStartDateCheckMessagesPrefix
  val foreignPropertyPrefix: String = ForeignProperty.addStartDateCheckMessagesPrefix
  val addBusinessStartDateCheckChangeSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = SelfEmployment, isAgent = false, isChange = true).url
  val addBusinessStartDateCheckChangeShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = true).url
  val addBusinessStartDateCheckDetailsShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url


  val foreignPropertyStartDateCheckShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = false).url
  val foreignPropertyStartDateCheckSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = false).url
  val foreignPropertyAccountingMethodShowUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(ForeignProperty).url
  val foreignPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = false).url
  val addForeignPropertyStartDateCheckChangeShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = ForeignProperty, isAgent = false, isChange = true).url
  val addForeignPropertyStartDateCheckChangeSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = ForeignProperty, isAgent = false, isChange = true).url
  val addForeignPropertyStartDateCheckDetailsShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(ForeignProperty).url

  val dateText: String = "10 October 2022"

  val checkUKPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false).url
  val checkUKPropertyStartDateSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = false).url
  val addUKPropertyStartDateShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = UkProperty, isAgent = false, isChange = false).url
  val ukPropertyAccountingMethodShowUrl: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.show(UkProperty).url
  val addUKPropertyStartDateCheckChangeSubmitUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.submit(incomeSourceType = UkProperty, isAgent = false, isChange = true).url
  val addUKPropertyStartDateCheckChangeShowUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateCheckController.show(incomeSourceType = UkProperty, isAgent = false, isChange = true).url
  val addUKPropertyStartDateCheckDetailsShowUrl: String = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(UkProperty).url

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val journeyTypeSE: JourneyType = JourneyType(Add, SelfEmployment)
  val journeyTypeUK: JourneyType = JourneyType(Add, UkProperty)
  val journeyTypeFP: JourneyType = JourneyType(Add, ForeignProperty)
  val testBusinessStartDate: LocalDate = LocalDate.of(2022, 10, 10)
  val testAccountingPeriodStartDate: LocalDate = LocalDate.of(2022, 10, 10)
  val testAccountingPeriodEndDate: LocalDate = LocalDate.of(2023, 4, 5)
  val testBusinessName: String = "Test Business"
  val testBusinessTrade: String = "Plumbing"


  val testAddIncomeSourceDataWithStartDate: IncomeSourceType => AddIncomeSourceData = (incomeSourceType: IncomeSourceType) =>
    if (incomeSourceType.equals(SelfEmployment)) {
      AddIncomeSourceData(
        businessName = Some(testBusinessName),
        businessTrade = Some(testBusinessTrade),
        dateStarted = Some(testBusinessStartDate)
      )
    } else {
      AddIncomeSourceData(
        businessName = None,
        businessTrade = None,
        dateStarted = Some(testBusinessStartDate)
      )
    }

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = JourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(testAddIncomeSourceDataWithStartDate(incomeSourceType)))

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }


  s"calling GET $addBusinessStartDateCheckShowUrl" should {
    "render the Add Business Start Date Check Page" when {
      "User is authorised" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addBusinessStartDateCheckShowUrl")

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.getAddBusinessStartDateCheck(clientDetailsWithStartDate )
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("dateForm.check.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $addBusinessStartDateCheckSubmitUrl" should {
    s"redirect to $addBusinessTradeShowUrl" when {
      "form response is Yes" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(Some("Yes"))(clientDetailsWithConfirmation)

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeSE).futureValue shouldBe Right(Some(testBusinessStartDate))
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodStartDateField, journeyTypeSE).futureValue shouldBe
          Right(Some(testAccountingPeriodStartDate))
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodEndDateField, journeyTypeSE).futureValue shouldBe Right(Some(testAccountingPeriodEndDate))

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessTradeShowUrl)
        )
      }
    }
    s"redirect to $addBusinessStartDateShowUrl" when {
      "form response is No" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(Some("No"))(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateShowUrl)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeSE).futureValue shouldBe Right(None)
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodStartDateField, journeyTypeSE).futureValue shouldBe Right(None)
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodEndDateField, journeyTypeSE).futureValue shouldBe Right(None)
      }
    }
    "return a BAD_REQUEST" when {
      "form is empty" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheck(None)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID(s"$incomeSourcePrefix-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI(s"$soleTraderBusinessPrefix.error"))
        )
      }
    }
    "return INTERNAL_SERVER_ERROR" when {
      "invalid entry given" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend
          .postAddBusinessStartDateCheck(Some("@"))(clientDetailsWithConfirmation)

        result should have(
          httpStatus(BAD_REQUEST)
        )
      }
    }
  }
  s"calling GET $foreignPropertyStartDateCheckShowUrl" should {
    "render the foreign property start date check page" when {
      "User is authorised" in {

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $foreignPropertyStartDateCheckShowUrl")

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/foreign-property-start-date-check", clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("dateForm.check.heading"),
          elementTextByID(s"$incomeSourcePrefix-hint")(dateText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $foreignPropertyStartDateCheckSubmitUrl" should {
    s"redirect to $foreignPropertyAccountingMethodShowUrl" when {
      "form is filled correctly with input Yes" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.postAddForeignPropertyStartDateCheck(Some("Yes"))(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyAccountingMethodShowUrl)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeFP).futureValue shouldBe Right(Some(testBusinessStartDate))
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodStartDateField, journeyTypeFP).futureValue shouldBe Right(None)
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodEndDateField, journeyTypeFP).futureValue shouldBe Right(None)
      }
      "form is filled correctly with input No" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.postAddForeignPropertyStartDateCheck(Some("No"))(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(foreignPropertyStartDateShowUrl)
        )
      }
      "form is filled incorrectly" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.postAddForeignPropertyStartDateCheck(None)(clientDetailsWithConfirmation)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID(s"$incomeSourcePrefix-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI(s"$foreignPropertyPrefix.error"))
        )
      }
    }
  }
  s"calling GET $checkUKPropertyStartDateShowUrl" should {
    "render the Check UK Property Business Start Date page" when {
      "User is authorised" in {

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $checkUKPropertyStartDateShowUrl")
        await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/uk-property-start-date-check", clientDetailsWithConfirmation)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("radioForm.checkDate.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $checkUKPropertyStartDateSubmitUrl" should {
    s"redirect to $ukPropertyAccountingMethodShowUrl" when {
      "user selects 'yes' the date entered is correct" in {

        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

        val result = IncomeTaxViewChangeFrontend.postAddUKPropertyStartDateCheck(Some("Yes"))(clientDetailsWithConfirmation)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(ukPropertyAccountingMethodShowUrl)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeUK).futureValue shouldBe Right(Some(testBusinessStartDate))
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodStartDateField, journeyTypeUK).futureValue shouldBe Right(None)
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodEndDateField, journeyTypeUK).futureValue shouldBe Right(None)
      }
      s"redirect to $addUKPropertyStartDateShowUrl" when {
        "user selects 'no' the date entered is not correct" in {

          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

          val result = IncomeTaxViewChangeFrontend.postAddUKPropertyStartDateCheck(Some("No"))(clientDetailsWithConfirmation)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(addUKPropertyStartDateShowUrl)
          )

          sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeUK).futureValue shouldBe Right(None)
        }
      }
      s"return BAD_REQUEST $checkUKPropertyStartDateShowUrl" when {
        "user does not select anything" in {

          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

          val result = IncomeTaxViewChangeFrontend.postAddUKPropertyStartDateCheck(Some(""))(clientDetailsWithConfirmation)

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByID(s"$incomeSourcePrefix-error")(messagesAPI("base.error-prefix") + " " +
              messagesAPI(s"$ukPropertyPrefix.error"))
          )
        }
      }
    }
  }
  s"calling GET $addBusinessStartDateCheckChangeShowUrl" should {
    s"render the Start Date Check Change Page" when {
      "User is authorised" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addBusinessStartDateCheckChangeShowUrl")

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.getAddBusinessStartDateCheckChange()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("dateForm.check.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling GET $addForeignPropertyStartDateCheckChangeShowUrl" should {
    s"render the Start Date Check Change Page" when {
      "User is authorised" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addForeignPropertyStartDateCheckChangeShowUrl")
        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.getAddForeignPropertyStartDateCheckChange()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("dateForm.check.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling GET $addUKPropertyStartDateCheckChangeShowUrl" should {
    s"render the Start Date Check Change Page" when {
      "User is authorised" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addUKPropertyStartDateCheckChangeSubmitUrl")
        await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

        val result = IncomeTaxViewChangeFrontend.getAddUKPropertyStartDateCheckChange()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("dateForm.check.heading"),
          elementTextByID("continue-button")(continueButtonText)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeUK).futureValue shouldBe Right(Some(testBusinessStartDate))
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodStartDateField, journeyTypeUK).futureValue shouldBe Right(None)
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodEndDateField, journeyTypeUK).futureValue shouldBe Right(None)
      }
    }
  }
  s"calling POST $addBusinessStartDateCheckChangeSubmitUrl" should {
    s"render the Check Business Details Page" when {
      "User selects 'Yes" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $addBusinessStartDateCheckChangeSubmitUrl")

        await(sessionService.setMongoData(testUIJourneySessionData(SelfEmployment)))

        val result = IncomeTaxViewChangeFrontend.postAddBusinessStartDateCheckChange(Some("Yes"))()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateCheckDetailsShowUrl)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeSE).futureValue shouldBe Right(Some(testBusinessStartDate))
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodStartDateField, journeyTypeSE).futureValue shouldBe
          Right(Some(testAccountingPeriodStartDate))
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodEndDateField, journeyTypeSE).futureValue shouldBe Right(Some(testAccountingPeriodEndDate))
      }
    }
  }
  s"calling POST $addForeignPropertyStartDateCheckChangeSubmitUrl" should {
    s"render the Check Foreign Property Details Page" when {
      "User selects 'Yes" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        When(s"I call GET $addForeignPropertyStartDateCheckChangeSubmitUrl")

        await(sessionService.setMongoData(testUIJourneySessionData(ForeignProperty)))

        val result = IncomeTaxViewChangeFrontend.postAddForeignPropertyStartDateCheckChange(Some("Yes"))()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addForeignPropertyStartDateCheckDetailsShowUrl)
        )
      }
    }
  }
  s"calling POST $addUKPropertyStartDateCheckChangeSubmitUrl" should {
    s"render the Check UK Property Details Page" when {
      "User selects 'Yes" in {

        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $addUKPropertyStartDateCheckChangeSubmitUrl")
        await(sessionService.setMongoData(testUIJourneySessionData(UkProperty)))

        val result = IncomeTaxViewChangeFrontend.postAddUKPropertyStartDateCheckChange(Some("Yes"))()
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addUKPropertyStartDateCheckDetailsShowUrl)
        )

        sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyTypeUK).futureValue shouldBe Right(Some(testBusinessStartDate))
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodStartDateField, journeyTypeUK).futureValue shouldBe Right(None)
        sessionService.getMongoKeyTyped[LocalDate](accountingPeriodEndDateField, journeyTypeUK).futureValue shouldBe Right(None)
      }
    }
  }
}

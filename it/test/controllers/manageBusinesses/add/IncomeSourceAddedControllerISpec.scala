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

package controllers.manageBusinesses.add

import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.IncomeSourcesFs
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.testObligationsModel
import testConstants.PropertyDetailsIntegrationTestConstants.ukProperty

import java.time.LocalDate

class IncomeSourceAddedControllerISpec extends ComponentSpecBase {

  val incomeSourceAddedSelfEmploymentShowUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceAddedController.show(SelfEmployment).url

  val addIncomeSourceUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceController.show().url

  val testDate: String = "2020-11-10"
  val prefix: String = "business-added"
  val viewAllBusinessesLinkText: String = messagesAPI(s"$prefix.view-all-businesses")
  val htmlTitle = " - Manage your Income Tax updates - GOV.UK"
  val day: LocalDate = LocalDate.of(2023, 1, 1)

  val incomeSourceAddedUkPropertyShowUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceAddedController.show(UkProperty).url
  val HomeControllerShowUrl: String = controllers.routes.HomeController.show().url
  val pageTitle: String = messagesAPI("htmlTitle", {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}".trim()
  })
  val confirmationPanelContent: String = {
    s"${messagesAPI("business-added.uk-property.h1")} " +
      s"${messagesAPI("business-added.uk-property.base")}"
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  val incomeSourceAddedForeignPropertyShowUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceAddedController.show(ForeignProperty).url

  val addIncomeSourceShowUrl: String = controllers.manageBusinesses.add.routes.AddIncomeSourceController.show().url

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  def testUIJourneySessionData(incomeSourceType: IncomeSourceType): UIJourneySessionData = UIJourneySessionData(
    sessionId = testSessionId,
    journeyType = IncomeSourceJourneyType(Add, incomeSourceType).toString,
    addIncomeSourceData = Some(AddIncomeSourceData()))

  s"calling GET $incomeSourceAddedSelfEmploymentShowUrl" should {
    "render the Business Added page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSourcesFs)

        When(s"I call GET $incomeSourceAddedSelfEmploymentShowUrl")

        await(sessionService.createSession(IncomeSourceJourneyType(Add, SelfEmployment)))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
          addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testSelfEmploymentId), dateStarted = Some(LocalDate.of(2024, 1, 1)))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getAddBusinessObligations
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI("business-added.sole-trader.head").nonEmpty) {
          messagesAPI("business-added.sole-trader.head") + " " + business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
        }
        else {
          business1.tradingName.getOrElse("") + " " + messagesAPI("business-added.sole-trader.base")
        }

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, IncomeSourceJourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(true))

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("view-all-businesses-link")(viewAllBusinessesLinkText)
        )
      }
    }
  }

  s"calling GET $incomeSourceAddedUkPropertyShowUrl" should {
    "render the UK Property Added Page" when {
      "UK Property start date is provided" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSourcesFs)

        await(sessionService.createSession(IncomeSourceJourneyType(Add, UkProperty)))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
          addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId), dateStarted = Some(LocalDate.of(2024, 1, 1)))))))

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1330 getNextUpdates return a success response")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

        Then("user is shown UK property added page")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.get(s"/manage-your-businesses/add-uk-property/uk-property-added")

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, IncomeSourceJourneyType(Add, UkProperty)).futureValue shouldBe Right(Some(true))

        result should have(
          httpStatus(OK),
          pageTitleCustom(pageTitle),
          elementTextBySelectorList(".govuk-panel.govuk-panel--confirmation")(confirmationPanelContent)
        )

      }
    }
    "render error page" when {
      "UK property income source is missing trading start date" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSourcesFs)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse.copy(properties = List(ukProperty.copy(tradingStartDate = None))))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-UK",
          addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId))))))

        Then("user is shown a error page")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.get(s"/manage-your-businesses/add-uk-property/uk-property-added")

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR),
          pageTitleIndividual("standardError.heading", isErrorPage = true)
        )

      }
    }
    s"redirect to $HomeControllerShowUrl" when {
      "Income Sources Feature Switch is disabled" in {
        Given("Income Sources FS is disabled")
        disable(IncomeSourcesFs)

        And("API 1171 getIncomeSourceDetails returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse.copy(properties = List(ukProperty.copy(tradingStartDate = None))))


        Then(s"user is redirected to $HomeControllerShowUrl")
        val result = IncomeTaxViewChangeFrontendManageBusinesses.get(s"/manage-your-businesses/add-uk-property/uk-property-added")

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(HomeControllerShowUrl)
        )
      }
    }
  }

  s"calling GET $incomeSourceAddedForeignPropertyShowUrl" should {
    "render the Foreign Property Added obligations page" when {
      "User is authorised" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSourcesFs)

        await(sessionService.createSession(IncomeSourceJourneyType(Add, ForeignProperty)))

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-FP",
          addIncomeSourceData = Some(AddIncomeSourceData(incomeSourceId = Some(testPropertyIncomeId), dateStarted = Some(LocalDate.of(2024, 1, 1)))))))

        When(s"I call GET $incomeSourceAddedForeignPropertyShowUrl")

        And("API 1771 returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getForeignPropertyAddedObligations
        verifyIncomeSourceDetailsCall(testMtditid)

        And("Mongo storage is successfully set")
        sessionService.getMongoKey(AddIncomeSourceData.journeyIsCompleteField, IncomeSourceJourneyType(Add, ForeignProperty)).futureValue shouldBe Right(Some(true))

        val expectedText: String = messagesAPI("business-added.foreign-property.h1") + " " + messagesAPI("business-added.foreign-property.base")

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("view-all-businesses-link")(viewAllBusinessesLinkText)
        )
      }
    }
  }
}

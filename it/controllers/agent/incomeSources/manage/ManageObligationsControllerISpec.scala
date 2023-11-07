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

package controllers.agent.incomeSources.manage

import auth.MtdItUser
import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{SelfEmployment, UkProperty}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.incomeSourceDetails.{IncomeSourceDetailsModel, ManageIncomeSourceData, TaxYear, UIJourneySessionData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid, testNino, testSaUtr, testSessionId}
import testConstants.BusinessDetailsIntegrationTestConstants.{business1, business2, business3}
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.testObligationsModel
import testConstants.PropertyDetailsIntegrationTestConstants.{foreignProperty, ukProperty}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import java.time.LocalDate

class ManageObligationsControllerISpec extends ComponentSpecBase {

  val annual = "annual"
  val quarterly = "quarterly"
  val taxYear = "2023-2024"

  val manageSEObligationsShowUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.showAgentSelfEmployment(annual, taxYear).url
  val manageUKObligationsShowUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.showAgentUKProperty(annual, taxYear).url
  val manageFPObligationsShowUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.showAgentForeignProperty(annual, taxYear).url

  val manageConfirmShowUrl: String = controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController.show(taxYear, annual, incomeSourceType = UkProperty, isAgent = true).url

  val manageObligationsSubmitUrl: String = controllers.incomeSources.manage.routes.ManageObligationsController.agentSubmit().url
  val manageIncomeSourcesShowUrl: String = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true).url

  val prefix: String = "incomeSources.add.manageObligations"
  val reusedPrefix: String = "business-added"

  val continueButtonText: String = messagesAPI(s"$reusedPrefix.income-sources-button")

  val year = 2022
  val obligationsViewModel: ObligationsViewModel = ObligationsViewModel(
    quarterlyObligationsDatesYearOne = Seq(DatesModel(
      LocalDate.of(year, 1, 6),
      LocalDate.of(year, 4, 5),
      LocalDate.of(year, 5, 5),
      "Quarterly",
      false,
      obligationType = "Quarterly"
    ),
      DatesModel(
        LocalDate.of(year, 1, 6),
        LocalDate.of(year, 4, 5),
        LocalDate.of(year, 5, 5),
        "Quarterly",
        false,
        obligationType = "Quarterly"
      )),
    Seq.empty, Seq.empty, Seq.empty, 2023, showPrevTaxYears = false
  )

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  s"calling GET $manageSEObligationsShowUrl" should {
    "render the self employment obligations page" when {
      "given valid url params" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some("123"))))))

        When(s"I call GET $manageSEObligationsShowUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontend.getManageSEObligations(annual, taxYear, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI(s"$prefix.h1").nonEmpty) {
          messagesAPI(s"$prefix.h1") + " " + business1.tradingName.getOrElse("") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }
        else {
          business1.tradingName.getOrElse("") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }

    "return an error" when {
      "there is no incomeSourceId in session storage" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("Income Sources FS is enabled")
        enable(IncomeSources)
        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = None)))

        When(s"I call GET $manageSEObligationsShowUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontend.getManageSEObligations(annual, taxYear, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  s"calling GET $manageUKObligationsShowUrl" should {
    "render the self employment obligations page" when {
      "given valid url params" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $manageUKObligationsShowUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontend.getManageUKObligations(annual, taxYear, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI(s"$prefix.h1").nonEmpty) {
          messagesAPI(s"$prefix.h1") + " " + messagesAPI(s"$prefix.uk-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }
        else {
          messagesAPI(s"$prefix.uk-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $manageFPObligationsShowUrl" should {
    "render the self employment obligations page" when {
      "given valid url params" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        When(s"I call GET $manageFPObligationsShowUrl")

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontend.getManageFPObligations(quarterly, taxYear, clientDetailsWithConfirmation)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI(s"$prefix.h1").nonEmpty) {
          messagesAPI(s"$prefix.h1") + " " + messagesAPI(s"$prefix.foreign-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.quarterly") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }
        else {
          messagesAPI(s"$prefix.foreign-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.quarterly") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }

        result should have(
          httpStatus(OK),
          pageTitleAgent(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $manageObligationsSubmitUrl" should {
    s"redirect to $manageIncomeSourcesShowUrl" when {
      "called" in {
        stubAuthorisedAgentUser(authorised = true)
        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        val resultSE = IncomeTaxViewChangeFrontend.postManageObligations("business", clientDetailsWithConfirmation)
        resultSE should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/agents/income-sources/manage/view-and-manage-income-sources")
        )
        val resultUK = IncomeTaxViewChangeFrontend.postManageObligations("uk-property", clientDetailsWithConfirmation)
        resultUK should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/agents/income-sources/manage/view-and-manage-income-sources")
        )
        val resultFP = IncomeTaxViewChangeFrontend.postManageObligations("foreign-property", clientDetailsWithConfirmation)
        resultFP should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/agents/income-sources/manage/view-and-manage-income-sources")
        )
      }
    }
  }

}

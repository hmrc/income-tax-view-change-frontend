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

package controllers.manageBusinesses.manage

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import enums.JourneyType.Manage
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.IncomeSourcesFs
import models.incomeSourceDetails.viewmodels.ObligationsViewModel
import models.incomeSourceDetails.{ManageIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.business1
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.{testObligationsModel, testQuarterlyObligationDates}


class ManageObligationsControllerISpec extends ComponentSpecBase {

  val annual = "annual"
  val quarterly = "quarterly"
  val taxYear = "2023-2024"

  val manageSEObligationsShowUrl: String = controllers.manageBusinesses.manage.routes.ManageObligationsController.show(isAgent = false, SelfEmployment).url
  val manageUKObligationsShowUrl: String = controllers.manageBusinesses.manage.routes.ManageObligationsController.show(isAgent = false, UkProperty).url
  val manageFPObligationsShowUrl: String = controllers.manageBusinesses.manage.routes.ManageObligationsController.show(isAgent = false, ForeignProperty).url

  val manageConfirmShowUrl: String = controllers.manageBusinesses.manage.routes.ConfirmReportingMethodSharedController.show(taxYear, quarterly, incomeSourceType = UkProperty).url

  val manageObligationsSubmitUrl: String = controllers.manageBusinesses.manage.routes.ManageObligationsController.submit(false).url
  val manageIncomeSourcesShowUrl: String = controllers.manageBusinesses.manage.routes.ManageIncomeSourceController.show(false).url

  val prefix: String = "incomeSources.add.manageObligations"
  val reusedPrefix: String = "business-added"

  val continueButtonText: String = messagesAPI(s"$reusedPrefix.income-sources-button")

  val year = 2022
  val obligationsViewModel: ObligationsViewModel = ObligationsViewModel(
    testQuarterlyObligationDates, Seq.empty, 2023, showPrevTaxYears = false
  )

  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Manage))
  }

  s"calling GET $manageSEObligationsShowUrl" should {
    "render the self employment obligations page" when {
      "given valid url params" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSourcesFs)

        When(s"I call GET $manageSEObligationsShowUrl")

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId), Some(annual), Some(2024), Some(true))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getManageSEObligations(annual, taxYear)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI(s"$prefix.h1").nonEmpty) {
          messagesAPI(s"$prefix.h1") + " " + business1.tradingName.getOrElse("") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }
        else {
          business1.tradingName.getOrElse("") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
    "return an error" when {
      "there is no incomeSourceId in the session storage" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSourcesFs)

        When(s"I call GET $manageSEObligationsShowUrl")

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-SE",
          manageIncomeSourceData = None)))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testNino, testObligationsModel)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getManageSEObligations(annual, taxYear)
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
        Given("Income Sources FS is enabled")
        enable(IncomeSourcesFs)

        When(s"I call GET $manageUKObligationsShowUrl")

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-UK",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testSelfEmploymentId), Some(annual), Some(2024), Some(true))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getManageUKObligations(annual, taxYear)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI(s"$prefix.h1").nonEmpty) {
          messagesAPI(s"$prefix.h1") + " " + messagesAPI(s"$prefix.uk-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }
        else {
          messagesAPI(s"$prefix.uk-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.annually") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling GET $manageFPObligationsShowUrl" should {
    "render the self employment obligations page" when {
      "given valid url params" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSourcesFs)

        When(s"I call GET $manageFPObligationsShowUrl")

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "MANAGE-FP",
          manageIncomeSourceData = Some(ManageIncomeSourceData(Some(testMtditid), Some(quarterly), Some(2024), Some(true))))))

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        And("API 1330 getNextUpdates returns a success response with a valid ObligationsModel")
        IncomeTaxViewChangeStub.stubGetNextUpdates(testMtditid, testObligationsModel)

        val result = IncomeTaxViewChangeFrontendManageBusinesses.getManageFPObligations(quarterly, taxYear)
        verifyIncomeSourceDetailsCall(testMtditid)

        val expectedText: String = if (messagesAPI(s"$prefix.h1").nonEmpty) {
          messagesAPI(s"$prefix.h1") + " " + messagesAPI(s"$prefix.foreign-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.quarterly") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }
        else {
          messagesAPI(s"$prefix.foreign-property") + " " + messagesAPI(s"$prefix.h2") + " " + messagesAPI(s"$prefix.quarterly") + " " + messagesAPI(s"$prefix.tax-year") + " " + "2023 to 2024"
        }

        result should have(
          httpStatus(OK),
          pageTitleIndividual(expectedText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $manageObligationsSubmitUrl" should {
    s"redirect to $manageIncomeSourcesShowUrl" when {
      "called" in {
        Given("Income Sources FS is enabled")
        enable(IncomeSourcesFs)

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponse)

        val resultSE = IncomeTaxViewChangeFrontendManageBusinesses.postManageObligations("business")
        resultSE should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/manage-your-businesses/manage/view-and-manage-income-sources")
        )
        val resultUK = IncomeTaxViewChangeFrontendManageBusinesses.postManageObligations("uk-property")
        resultUK should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/manage-your-businesses/manage/view-and-manage-income-sources")
        )
        val resultFP = IncomeTaxViewChangeFrontendManageBusinesses.postManageObligations("foreign-property")
        resultFP should have(
          httpStatus(SEE_OTHER),
          redirectURI(s"/report-quarterly/income-and-expenses/view/manage-your-businesses/manage/view-and-manage-income-sources")
        )
      }
    }
  }
}

/*
 * Copyright 2025 HM Revenue & Customs
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

package businessDetails.controllers.triggeredMigration

import businessDetails.models.triggeredMigration.TriggeredMigrationSessionData
import helpers.servicemocks.{IncomeTaxCalculationStub, IncomeTaxViewChangeStub}
import common.controllers.ControllerISpecHelper
import common.enums.JourneyType.TriggeredMigrationJourney
import common.enums.{MTDIndividual, MTDUserRole}
import common.helpers.servicemocks.ITSAStatusDetailsStub
import common.models.admin.TriggeredMigration
import common.models.incomeSourceDetails.TaxYear
import common.models.itsaStatus.ITSAStatus
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import common.testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import common.testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessfulNotCrystallised
import common.testConstants.IncomeSourceDetailsTestConstants.singleBusinessIncome
import shared.models.UIJourneySessionData
import shared.repositories.UIJourneySessionDataRepository

class CheckCompleteControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val prefix =
      if(mtdRole == MTDIndividual) ""
      else "/agents"

    s"$prefix/check-your-active-businesses/complete"
  }

  object CheckCompleteMessages {
    val title = "Check complete"
    val continueText = "Continue"
    val errorSummaryHeading = "There is a problem"
    val errorMessage = "Select yes if you’ve checked that HMRC records only list your active businesses"
  }

  private def checkPageContent(result: WSResponse, mtdRole: MTDUserRole): Unit = {
    result should have(
      httpStatus(OK),
      pageTitle(mtdRole, CheckCompleteMessages.title),
      elementTextByID("continue-button")(CheckCompleteMessages.continueText)
    )
  }

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  mtdAllRoles.foreach { mtdRole =>
    val path = getPath(mtdRole)
    val additionalCookies = getAdditionalCookies(mtdRole)
    val homePageUrl: String =
      if(mtdRole == MTDIndividual) hub.controllers.routes.HomeController.show().url
      else hub.controllers.routes.HomeController.showAgent().url

    s"GET $path" when {
      s"user is $mtdRole" should {
        "render the page when TriggeredMigration FS is enabled" in {
          eventually {
            repository.set(
              UIJourneySessionData(testSessionId,
                TriggeredMigrationJourney.toString,
                triggeredMigrationData =
                  Some(TriggeredMigrationSessionData(recentlyConfirmed = true))
            ))
          }

          stubAuthorised(mtdRole, List(TriggeredMigration))
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2023, 2024), ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary, "AB123456C")
          IncomeTaxCalculationStub.stubGetCalculationResponse("AB123456C", "2018", Some("LATEST"))(
            status = OK,
            body = liabilityCalculationModelSuccessfulNotCrystallised
          )

          val result = buildGETMTDClient(path, additionalCookies).futureValue
          checkPageContent(result, mtdRole)
        }

        "redirect to home page when TriggeredMigration FS is disabled" in {
          stubAuthorised(mtdRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)

          val result = buildGETMTDClient(path, additionalCookies).futureValue
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(homePageUrl)
          )
        }

        testAuthFailures(path, mtdRole)
      }
    }

    s"POST $path" when {
      s"user is $mtdRole" should {
        "redirect to home page when form is valid and 'Continue' is selected" in {
          stubAuthorised(mtdRole, List(TriggeredMigration))
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2023, 2024), ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary, "AB123456C")
          IncomeTaxCalculationStub.stubGetCalculationResponse("AB123456C", "2018", Some("LATEST"))(
            status = OK,
            body = liabilityCalculationModelSuccessfulNotCrystallised
          )

          val formData = Map(
            "check-complete-confirm" -> Seq("Continue")
          )

          val result = buildPOSTMTDPostClient(path, additionalCookies, formData).futureValue
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(homePageUrl)
          )
        }

        testAuthFailures(path, mtdRole)
      }
    }
  }
}

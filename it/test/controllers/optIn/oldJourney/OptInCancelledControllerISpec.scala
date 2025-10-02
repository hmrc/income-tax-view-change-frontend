/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.optIn.oldJourney

import config.featureswitch.FeatureSwitching
import controllers.ControllerISpecHelper
import enums.JourneyType.{Opt, OptInJourney}
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.UIJourneySessionData
import models.admin.{ReportingFrequencyPage, SignUpFs}
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatus
import models.itsaStatus.ITSAStatus.{Annual, Mandated}
import models.optin.{OptInContextData, OptInSessionData}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import repositories.ITSAStatusRepositorySupport.statusToString
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino, testSessionId}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.businessAndPropertyResponseWoMigration
import uk.gov.hmrc.http.client.HttpClientV2

class OptInCancelledControllerISpec extends ControllerISpecHelper with FeatureSwitching {

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf(classOf[HttpClientV2])

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/opt-in/cancelled"
  }

  val forYearEnd = 2023
  val currentTaxYear = TaxYear.forYearEnd(forYearEnd)
  val nextTaxYear = currentTaxYear.nextYear

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  private def setupOptInSessionData(currentTaxYear: TaxYear, currentYearStatus: ITSAStatus.Value, nextYearStatus: ITSAStatus.Value): Unit = {
    await(repository.set(
      UIJourneySessionData(testSessionId,
        Opt(OptInJourney).toString,
        optInSessionData =
          Some(OptInSessionData(
            Some(OptInContextData(
              currentTaxYear.toString,
              statusToString(currentYearStatus),
              statusToString(nextYearStatus))), None))))
          )
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the choose tax year page" when {
            "only single tax year is Annual, CY-1 = Mandated, CY = Annual, CY+1 = Mandated" in {
              enable(ReportingFrequencyPage, SignUpFs)
              val previousTaxYear = dateService.getCurrentTaxYearEnd - 1
              stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

              CalculationListStub.stubGetLegacyCalculationList(testNino, previousTaxYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

              ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                taxYear = dateService.getCurrentTaxYear,
                `itsaStatusCY-1` = ITSAStatus.Mandated,
                itsaStatusCY = ITSAStatus.Annual,
                `itsaStatusCY+1` = ITSAStatus.Mandated
              )

              setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Mandated)

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "optin.cancelled.title")
              )
            }
          }

          "render the error page" when {

            "no tax year is voluntary, CY-1 = Mandated, CY = Mandated, CY+1 = Mandated" in {
              enable(ReportingFrequencyPage, SignUpFs)
              val previousTaxYear = dateService.getCurrentTaxYearEnd - 1

              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

              CalculationListStub.stubGetLegacyCalculationList(testNino, previousTaxYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

              setupOptInSessionData(currentTaxYear, currentYearStatus = Mandated, nextYearStatus = Mandated)

              ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                taxYear = dateService.getCurrentTaxYear,
                `itsaStatusCY-1` = ITSAStatus.Mandated,
                itsaStatusCY = ITSAStatus.Mandated,
                `itsaStatusCY+1` = ITSAStatus.Mandated
              )

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(INTERNAL_SERVER_ERROR),
                pageTitleCustom("Sorry, there is a problem with the service - GOV.UK")
              )
            }

            "multiple tax years are Annual, CY-1 = Mandated, CY = Annual, CY+1 = Annual" in {
              enable(ReportingFrequencyPage, SignUpFs)
              val previousTaxYear = dateService.getCurrentTaxYearEnd - 1

              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

              CalculationListStub.stubGetLegacyCalculationList(testNino, previousTaxYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

              ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                taxYear = dateService.getCurrentTaxYear,
                `itsaStatusCY-1` = ITSAStatus.Mandated,
                itsaStatusCY = ITSAStatus.Annual,
                `itsaStatusCY+1` = ITSAStatus.Annual
              )

              setupOptInSessionData(currentTaxYear, currentYearStatus = Annual, nextYearStatus = Annual)

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(INTERNAL_SERVER_ERROR),
                pageTitleCustom("Sorry, there is a problem with the service - GOV.UK")
              )

            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}

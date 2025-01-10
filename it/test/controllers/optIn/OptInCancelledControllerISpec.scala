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

package controllers.optIn

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.itsaStatus.ITSAStatus
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.businessAndPropertyResponseWoMigration
import uk.gov.hmrc.http.client.HttpClientV2

class OptInCancelledControllerISpec extends ControllerISpecHelper with FeatureSwitching {

  override val appConfig: FrontendAppConfig = testAppConfig

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf(classOf[HttpClientV2])

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/opt-in/cancelled"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the choose tax year page" when {
            "only single tax year is voluntary, CY-1 = Mandated, CY = Voluntary, CY+1 = Mandated" in {
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

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "optin.cancelled.title")
              )
            }
          }

          "render the error page" when {

            "no tax year is voluntary, CY-1 = Mandated, CY = Mandated, CY+1 = Mandated" in {
              val previousTaxYear = dateService.getCurrentTaxYearEnd - 1

              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

              CalculationListStub.stubGetLegacyCalculationList(testNino, previousTaxYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

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

            "multiple tax years are voluntary, CY-1 = Mandated, CY = Voluntary, CY+1 = Voluntary" in {

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

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

package controllers.optOut

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.OptOutFs
import models.itsaStatus.ITSAStatus
import play.api.http.Status.OK
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.CalculationListIntegrationTestConstants
import testConstants.IncomeSourceIntegrationTestConstants.businessAndPropertyResponseWoMigration
import uk.gov.hmrc.http.client.HttpClientV2

class OptOutCancelledControllerISpec extends ControllerISpecHelper {

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf(classOf[HttpClientV2])

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/optout/cancelled"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)

    s"GET $path" when {

      s"a user is a $mtdUserRole" that {

        "is authenticated, with a valid enrolment" should {

          "render the choose tax year page" when {

            "only single tax year is voluntary, CY-1 = Mandated, CY = Voluntary, CY+1 = Mandated" in {
              enable(OptOutFs)
              val previousTaxYear = dateService.getCurrentTaxYearEnd - 1
              stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

              CalculationListStub.stubGetLegacyCalculationList(testNino, previousTaxYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

              ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                taxYear = dateService.getCurrentTaxYear,
                `itsaStatusCY-1` = ITSAStatus.Mandated,
                itsaStatusCY = ITSAStatus.Voluntary,
                `itsaStatusCY+1` = ITSAStatus.Mandated
              )

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "optout.cancelled.title")
              )
            }

            "no tax year is voluntary, CY-1 = Mandated, CY = Mandated, CY+1 = Mandated" in {
              enable(OptOutFs)

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
                httpStatus(OK),
                pageTitle(mtdUserRole, "optout.cancelled.title")
              )
            }

            "multiple tax years are voluntary, CY-1 = Mandated, CY = Voluntary, CY+1 = Voluntary" in {
              enable(OptOutFs)

              val previousTaxYear = dateService.getCurrentTaxYearEnd - 1

              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

              CalculationListStub.stubGetLegacyCalculationList(testNino, previousTaxYear.toString)(CalculationListIntegrationTestConstants.successResponseNotCrystallised.toString())

              ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                taxYear = dateService.getCurrentTaxYear,
                `itsaStatusCY-1` = ITSAStatus.Mandated,
                itsaStatusCY = ITSAStatus.Voluntary,
                `itsaStatusCY+1` = ITSAStatus.Voluntary
              )

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "optout.cancelled.title")
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}

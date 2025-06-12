/*
 * Copyright 2017 HM Revenue & Customs
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

package controllers

import audit.models.TaxDueResponseAuditModel
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.servicemocks.AuditStub.verifyAuditEvent
import helpers.servicemocks._
import models.liabilitycalculation.viewmodels.TaxDueSummaryViewModel
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants._
import testConstants.IncomeSourcesObligationsIntegrationTestConstants.testObligationsModel
import testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessful
import testConstants.NewCalcDataIntegrationTestConstants._
import testConstants.messages.TaxDueSummaryMessages._


class TaxDueSummaryControllerISpec extends ControllerISpecHelper with FeatureSwitching {

  def testUser(mtdUserRole: MTDUserRole): MtdItUser[_] = getTestUser(mtdUserRole, multipleBusinessesAndPropertyResponse)

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/$testYear/tax-calculation"
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            "render the Tax due summary page" that {
              "has additional charges, student and graduate payment plan" in {

                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
                  status = OK,
                  body = liabilityCalculationModelSuccessful
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

                verifyAuditEvent(TaxDueResponseAuditModel(testUser(mtdUserRole), TaxDueSummaryViewModel(liabilityCalculationModelSuccessful, testObligationsModel), testYearInt))

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "taxCal_breakdown.heading"),
                  elementTextByID("additional_charges")(additionCharges),
                  elementTextByID("student-repayment-plan0X")(studentPlan),
                  elementTextByID("graduate-repayment-plan")(postgraduatePlan),
                )
              }

              "has just Gift Aid Additional charges" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
                  status = OK,
                  body = liabilityCalculationGiftAid
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "taxCal_breakdown.heading"),
                  elementTextByID("additional_charges")(additionCharges)
                )
              }

              "has just Pension Lump Sum Additional charges" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

                IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
                  status = OK,
                  body = liabilityCalculationPensionLumpSums
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "taxCal_breakdown.heading"),
                  elementTextByID("additional_charges")(additionCharges)
                )
              }

              "has just Pension Savings Additional charges" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
                  status = OK,
                  body = liabilityCalculationPensionSavings
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "taxCal_breakdown.heading"),
                  elementTextByID("additional_charges")(additionCharges)
                )
              }

              "uses minimal calculation with no Additional Charges" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
                  status = OK,
                  body = liabilityCalculationMinimal
                )

                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

                res should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "taxCal_breakdown.heading")
                )

                res shouldNot have(
                  elementTextByID("additional_charges")(additionCharges)
                )
              }

              "has class2VoluntaryContributions as false" when {
                "the flag is missing from the calc data" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                  IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
                    status = OK,
                    body = liabilityCalculationNonVoluntaryClass2Nic
                  )

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "taxCal_breakdown.heading"),
                    elementTextBySelector("#national-insurance-contributions-table tbody:nth-child(3) td:nth-child(1)")(nonVoluntaryClass2Nics)
                  )
                }
              }

              "has class2VoluntaryContributions as true" when {
                "the flag is returned in the calc data" in {
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                  IncomeTaxCalculationStub.stubGetCalculationResponse(testNino, testYear)(
                    status = OK,
                    body = liabilityCalculationVoluntaryClass2Nic
                  )

                  val res = buildGETMTDClient(path, additionalCookies).futureValue

                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)
                  IncomeTaxCalculationStub.verifyGetCalculationResponse(testNino, testYear)

                  res should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, "taxCal_breakdown.heading"),
                    elementTextBySelector("#national-insurance-contributions-table tbody:nth-child(3) td:nth-child(1)")(voluntaryClass2Nics)
                  )
                }
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
  }
}

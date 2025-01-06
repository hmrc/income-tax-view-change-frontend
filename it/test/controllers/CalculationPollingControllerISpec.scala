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

import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import forms.utils.SessionKeys
import helpers.servicemocks._
import models.liabilitycalculation.LiabilityCalculationError
import play.api.http.Status._
import testConstants.BaseIntegrationTestConstants._
import testConstants.IncomeSourceIntegrationTestConstants.multipleBusinessesAndUkProperty
import testConstants.NewCalcBreakdownItTestConstants._

class CalculationPollingControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole, isFinalCalc: Boolean = false): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    if(isFinalCalc) {
      pathStart + s"/$testYear/final-tax-overview/calculate"
    } else {
      pathStart + s"/calculation/$testYear/submitted"
    }
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val path = getPath(mtdUserRole)
    val finalCalcPath = getPath(mtdUserRole, true)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            "redirects to Tax summary" when {
              "the session key contains calculationId" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
                IncomeTaxCalculationStub.stubGetCalculationResponseByCalcId(testNino, testTaxYear, "idOne")(
                  status = OK,
                  body = liabilityCalculationModelSuccessful
                )

                val result = buildGETMTDClient(path, additionalCookies ++ Map(SessionKeys.calculationId -> "idOne")).futureValue
                val expectedRedirectUrl = if(isAgent) {
                  routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(testYearInt).url
                } else {
                  routes.TaxYearSummaryController.renderTaxYearSummaryPage(testYearInt).url
                }
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(expectedRedirectUrl)
                )
              }

              "calculation service returns retryable response back initially and then returns success response before interval time completed" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idFour", testTaxYear)(NO_CONTENT,
                  LiabilityCalculationError(NO_CONTENT, "no content"))

                val res = buildGETMTDClient(path, additionalCookies ++ Map(SessionKeys.calculationId -> "idFour"))

                //After 1.75 seconds responding with success message
                Thread.sleep(1750)
                IncomeTaxCalculationStub.stubGetCalculationResponseByCalcId(testNino, testTaxYear, "idFour")(
                  status = OK,
                  body = liabilityCalculationModelSuccessful
                )
                val taxSummUrl = if(mtdUserRole == MTDIndividual) {
                  routes.TaxYearSummaryController.renderTaxYearSummaryPage(testYearInt).url
                } else {
                  routes.TaxYearSummaryController.renderAgentTaxYearSummaryPage(testYearInt).url
                }
                res.futureValue should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(taxSummUrl)
                )
                IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idFour", testTaxYear, noOfCalls = 6)

              }
            }

            "render the error page" when {
              "calculation service returns non-retryable response back" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idTwo", testTaxYear)(INTERNAL_SERVER_ERROR,
                  LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error"))

                val result = buildGETMTDClient(path, additionalCookies ++ Map(SessionKeys.calculationId -> "idTwo")).futureValue

                IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idTwo", testTaxYear)

                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "calculation service returns retryable response back" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idThree", testTaxYear)(NO_CONTENT,
                  LiabilityCalculationError(NO_CONTENT, "no content"))

                val result = buildGETMTDClient(path, additionalCookies ++ Map(SessionKeys.calculationId -> "idThree")).futureValue
                IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idThree", testTaxYear, noOfCalls = 8)

                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "calculation service returns retryable response back initially and then returns non-retryable error before interval time completed" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idFive", testTaxYear)(NO_CONTENT,
                  LiabilityCalculationError(NO_CONTENT, "no content"))

                val res = buildGETMTDClient(path, additionalCookies ++ Map(SessionKeys.calculationId -> "idFive"))

                //After 1.75 seconds responding with success message
                Thread.sleep(1750)
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idFive", testTaxYear)(INTERNAL_SERVER_ERROR,
                  LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error"))
                res.futureValue should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
                IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idFive", testTaxYear, noOfCalls = 6)
              }
            }
          }
        }
        testAuthFailures(path, mtdUserRole)

      }
    }

    s"GET $finalCalcPath" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          if (mtdUserRole == MTDSupportingAgent) {
            testSupportingAgentAccessDenied(path, additionalCookies)
          } else {
            "redirects to final tax calculation page" when {
              "the session key contains calculationId" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
                IncomeTaxCalculationStub.stubGetCalculationResponseByCalcId(testNino, testTaxYear, "idOne")(
                  status = OK,
                  body = liabilityCalculationModelSuccessful
                )

                val result = buildGETMTDClient(finalCalcPath, additionalCookies ++ Map(SessionKeys.calculationId -> "idOne")).futureValue
                val expectedRedirectUrl = if(isAgent) {
                  routes.FinalTaxCalculationController.showAgent(testYearInt).url
                } else {
                  routes.FinalTaxCalculationController.show(testYearInt).url
                }
                result should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(expectedRedirectUrl)
                )
              }

              "calculation service returns retryable response back initially and then returns success response before interval time completed" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idFour", testTaxYear)(NO_CONTENT,
                  LiabilityCalculationError(NO_CONTENT, "no content"))

                val res = buildGETMTDClient(finalCalcPath, additionalCookies ++ Map(SessionKeys.calculationId -> "idFour"))

                //After 1.75 seconds responding with success message
                Thread.sleep(1750)
                IncomeTaxCalculationStub.stubGetCalculationResponseByCalcId(testNino, testTaxYear, "idFour")(
                  status = OK,
                  body = liabilityCalculationModelSuccessful
                )
                val finalTaxCalUrl = if(mtdUserRole == MTDIndividual) {
                  routes.FinalTaxCalculationController.show(testYearInt).url
                } else {
                  routes.FinalTaxCalculationController.showAgent(testYearInt).url
                }
                res.futureValue should have(
                  httpStatus(SEE_OTHER),
                  redirectURI(finalTaxCalUrl)
                )
                IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idFour", testTaxYear, noOfCalls = 6)

              }
            }

            "render the error page" when {
              "calculation service returns non-retryable response back" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idTwo", testTaxYear)(INTERNAL_SERVER_ERROR,
                  LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error"))

                val result = buildGETMTDClient(finalCalcPath, additionalCookies ++ Map(SessionKeys.calculationId -> "idTwo")).futureValue

                IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idTwo", testTaxYear)

                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "calculation service returns retryable response back" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idThree", testTaxYear)(NO_CONTENT,
                  LiabilityCalculationError(NO_CONTENT, "no content"))

                val result = buildGETMTDClient(finalCalcPath, additionalCookies ++ Map(SessionKeys.calculationId -> "idThree")).futureValue
                IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idThree", testTaxYear, noOfCalls = 8)

                result should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
              "calculation service returns retryable response back initially and then returns non-retryable error before interval time completed" in {
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idFive", testTaxYear)(NO_CONTENT,
                  LiabilityCalculationError(NO_CONTENT, "no content"))

                val res = buildGETMTDClient(finalCalcPath, additionalCookies ++ Map(SessionKeys.calculationId -> "idFive"))

                //After 1.75 seconds responding with success message
                Thread.sleep(1750)
                IncomeTaxCalculationStub.stubGetCalculationErrorResponseByCalcId(testNino, "idFive", testTaxYear)(INTERNAL_SERVER_ERROR,
                  LiabilityCalculationError(INTERNAL_SERVER_ERROR, "error"))
                res.futureValue should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
                IncomeTaxCalculationStub.verifyGetCalculationResponseByCalcId(testNino, "idFive", testTaxYear, noOfCalls = 6)
              }
            }
          }
        }
        testAuthFailures(finalCalcPath, mtdUserRole)
      }
    }
  }
}

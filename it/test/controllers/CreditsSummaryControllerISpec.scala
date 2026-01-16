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

package controllers

import audit.models.IncomeSourceDetailsResponseAuditModel
import auth.authV2.models.AuthorisedAndEnrolledRequest
import enums.{MTDIndividual, MTDSupportingAgent, MTDUserRole}
import helpers.CreditsSummaryDataHelper
import helpers.servicemocks.{AuditStub, IncomeTaxViewChangeStub}
import play.api.http.Status.OK
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.ws.WSResponse
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants.*
import testConstants.IncomeSourceIntegrationTestConstants.{propertyOnlyResponseWithMigrationData, testValidFinancialDetailsModelCreditAndRefundsJson, testValidFinancialDetailsModelCreditAndRefundsJsonV2}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

class CreditsSummaryControllerISpec extends ControllerISpecHelper with CreditsSummaryDataHelper {

  val calendarYear = "2018"
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())
  implicit val msgs: MessagesApi = app.injector.instanceOf[MessagesApi]
  val incomeSources = propertyOnlyResponseWithMigrationData(testTaxYear - 1, Some(testTaxYear.toString))

  def testUser(mtdUserRole: MTDUserRole): AuthorisedAndEnrolledRequest[_] = {
    AuthorisedAndEnrolledRequest(
      testMtditid, mtdUserRole, defaultAuthUserDetails(mtdUserRole),
      if(mtdUserRole == MTDIndividual) None else Some(defaultClientDetails)
    )(FakeRequest())
  }

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + s"/credits-from-hmrc/$calendarYear"
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
            "render the credit summary page" when {
              "a valid response is received" in {
                import audit.models.CreditSummaryAuditing._
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, incomeSources)
                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  testNino,
                  s"${testTaxYear - 1}-04-06",
                  s"$testTaxYear-04-05")(
                  OK,
                  testValidFinancialDetailsModelCreditAndRefundsJson(
                    -1400,
                    -1400,
                    testTaxYear.toString,
                    fixedDate.plusYears(1).toString)
                )

                whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid, 1)
                  IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

                  AuditStub.verifyAuditContainsDetail(
                    IncomeSourceDetailsResponseAuditModel(
                      mtdItUser = testUser(mtdUserRole),
                      nino = testNino,
                      selfEmploymentIds = List.empty,
                      propertyIncomeIds = List("1234"),
                      yearOfMigration = Some(testTaxYear.toString)
                    ).detail
                  )

                  result should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, messages("credits.heading", s"$calendarYear"))
                  )

                  AuditStub.verifyAuditContainsDetail(
                    CreditsSummaryModel(
                      saUTR = testSaUtr,
                      nino = testNino,
                      userType = {if(mtdUserRole == MTDIndividual) Individual else Agent}.toString,
                      credId = credId,
                      mtdRef = testMtditid,
                      creditOnAccount = "5",
                      creditDetails = toCreditSummaryDetailsSeq(chargesList)(msgs)
                    ).detail
                  )
                }
              }
            }

            "correctly audit a list of credits" when {
              "the list contains Balancing Charge Credits" in {
                import audit.models.CreditSummaryAuditing._
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, incomeSources)

                IncomeTaxViewChangeStub.stubGetFinancialDetailsByDateRange(
                  testNino,
                  s"${testTaxYear - 1}-04-06",
                  s"$testTaxYear-04-05")(
                  OK,
                  testValidFinancialDetailsModelCreditAndRefundsJsonV2(
                    -1400,
                    -1400,
                    testTaxYear.toString,
                    fixedDate.plusYears(1).toString)
                )

                whenReady(buildGETMTDClient(path, additionalCookies)) { result =>
                  IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid, 1)
                  IncomeTaxViewChangeStub.verifyGetFinancialDetailsByDateRange(testNino, s"${testTaxYear - 1}-04-06", s"$testTaxYear-04-05")

                  AuditStub.verifyAuditContainsDetail(
                    IncomeSourceDetailsResponseAuditModel(
                      mtdItUser = testUser(mtdUserRole),
                      nino = testNino,
                      selfEmploymentIds = List.empty,
                      propertyIncomeIds = Nil,
                      yearOfMigration = Some("2018")
                    ).detail
                  )

                  result should have(
                    httpStatus(OK),
                    pageTitle(mtdUserRole, messages("credits.heading", s"$calendarYear"))
                  )

                  AuditStub.verifyAuditContainsDetail(
                    CreditsSummaryModel(
                      saUTR = testSaUtr,
                      nino = testNino,
                      userType = {if(mtdUserRole == MTDIndividual) Individual else Agent}.toString,
                      credId = credId,
                      mtdRef = testMtditid,
                      creditOnAccount = "5",
                      creditDetails = toCreditSummaryDetailsSeq(chargesListV2)(msgs)
                    ).detail
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

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

package controllers.agent

import audit.models.NextUpdatesResponseAuditModel
import auth.MtdItUser
import controllers.ControllerISpecHelper
import enums.MTDSupportingAgent
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.{ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.admin.{IncomeSourcesFs, IncomeSourcesNewJourney, NavBarFs}
import models.core.{AccountingPeriodModel, CessationModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, TaxYear}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.{address, b2CessationDate, b2TradingStart}
import testConstants.NextUpdatesIntegrationTestConstants.currentDate
import testConstants.messages.HomeMessages.{overdue, overdueUpdates}
import uk.gov.hmrc.auth.core.retrieve.Name

class HomeControllerSupportingAgentISpec extends ControllerISpecHelper {

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val clientName = Name(Some("Test"), Some("User"))

  val incomeSourceDetailsModel: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = Some(getCurrentTaxYearEnd.getYear.toString),
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(currentDate, currentDate.plusYears(1))),
      None,
      Some(getCurrentTaxYearEnd),
      Some(b2TradingStart),
      None,
      Some(CessationModel(Some(b2CessationDate))),
      address = Some(address),
      cashOrAccruals = false
    )),
    properties = Nil
  )

  val testUser: MtdItUser[_] = getTestUser(MTDSupportingAgent, incomeSourceDetailsModel)

  val path = "/agents"

  import implicitDateFormatter.longDate

  "GET /" when {
    val additionalCookies = getAgentClientDetailsForCookie(true, true)
    val mtdUserRole = MTDSupportingAgent
    s"there is a supporting agent" that {
      s"is a authenticated for a client" should {
        "render the home page" which {
          "displays the next updates" when {
            "nothing is overdue" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = incomeSourceDetailsModel
              )
              ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))
              val currentObligations: ObligationsModel = ObligationsModel(Seq(
                GroupedObligationsModel(
                  identification = "testId",
                  obligations = List(
                    SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate, "Quarterly", None, "testPeriodKey", StatusFulfilled)
                  ))
              ))

              IncomeTaxViewChangeStub.stubGetNextUpdates(
                nino = testNino,
                deadlines = currentObligations
              )


              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "home.agent.heading"),
                elementTextBySelector("#updates-tile p:nth-child(2)")(currentDate.toLongDate),
              )

              verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
            }
          }

          "displays an overdue obligation" when {
            "there is a single obligation overdue" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = incomeSourceDetailsModel
              )

              val currentObligations: ObligationsModel = ObligationsModel(Seq(
                GroupedObligationsModel(
                  identification = "testId",
                  obligations = List(
                    SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                  ))
              ))

              ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))

              IncomeTaxViewChangeStub.stubGetNextUpdates(
                nino = testNino,
                deadlines = currentObligations
              )

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "home.agent.heading"),
                elementTextBySelector("#updates-tile p:nth-child(2)")(s"$overdue ${currentDate.minusDays(1).toLongDate}"),
              )

              verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
            }
          }

          "display a count of the overdue obligations" when {
            "there is more than one obligation overdue" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = incomeSourceDetailsModel
              )

              val currentObligations: ObligationsModel =
                ObligationsModel(
                  Seq(
                    GroupedObligationsModel(
                      identification = "testId",
                      obligations = List(
                        SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled),
                        SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate.minusDays(2), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                      ))
                  ))

              ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))
              IncomeTaxViewChangeStub.stubGetNextUpdates(
                nino = testNino,
                deadlines = currentObligations
              )

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "home.agent.heading"),
                elementTextBySelector("#updates-tile p:nth-child(2)")(overdueUpdates(numberOverdue = "2")),
              )

              verifyAuditContainsDetail(NextUpdatesResponseAuditModel(testUser, "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
            }
          }
          "display Income Sources tile" when {
            "IncomeSources feature switch is enabled" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              enable(IncomeSourcesFs)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = incomeSourceDetailsModel
              )

              val currentObligations: ObligationsModel = ObligationsModel(Seq(
                GroupedObligationsModel(
                  identification = "testId",
                  obligations = List(
                    SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate, "Quarterly", None, "testPeriodKey", StatusFulfilled)
                  ))
              ))

              ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))

              IncomeTaxViewChangeStub.stubGetNextUpdates(
                nino = testNino,
                deadlines = currentObligations
              )

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "home.agent.heading"),
                elementTextBySelector("#updates-tile p:nth-child(2)")(currentDate.toLongDate),
                elementTextBySelector("#income-sources-tile h2:nth-child(1)")("Income Sources")
              )
            }
          }
          "display Your Businesses tile" when {
            "IncomeSources and IncomeSourcesNewJourney feature switches are enabled" in {
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              enable(IncomeSourcesFs)
              enable(IncomeSourcesNewJourney)

              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                status = OK,
                response = incomeSourceDetailsModel
              )

              val currentObligations: ObligationsModel = ObligationsModel(Seq(
                GroupedObligationsModel(
                  identification = "testId",
                  obligations = List(
                    SingleObligationModel(currentDate, currentDate.plusDays(1), currentDate, "Quarterly", None, "testPeriodKey", StatusFulfilled)
                  ))
              ))

              ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))

              IncomeTaxViewChangeStub.stubGetNextUpdates(
                nino = testNino,
                deadlines = currentObligations
              )

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "home.agent.heading"),
                elementTextBySelector("#updates-tile p:nth-child(2)")(currentDate.toLongDate),
                elementTextBySelector("#income-sources-tile h2:nth-child(1)")("Your businesses")
              )
            }
          }
        }

        "render the error page" when {

          "retrieving the obligations was unsuccessful" in {
            disable(NavBarFs)
            stubAuthorised(mtdUserRole)

            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetailsModel
            )

            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))

            IncomeTaxViewChangeStub.stubGetNextUpdatesError(testNino)

            val result = buildGETMTDClient(path, additionalCookies).futureValue

            result should have(
              httpStatus(INTERNAL_SERVER_ERROR),
              pageTitle(mtdUserRole, titleInternalServer, isErrorPage = true)
            )
          }
          "retrieving the income sources was unsuccessful" in {
            disable(NavBarFs)
            enable(IncomeSourcesFs)
            stubAuthorised(mtdUserRole)

            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2022, 2023))

            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsErrorResponse(testMtditid)(
              status = INTERNAL_SERVER_ERROR)

            val result = buildGETMTDClient(path, additionalCookies).futureValue

            result should have(
              httpStatus(INTERNAL_SERVER_ERROR),
              pageTitleAgentLogin(titleInternalServer, isErrorPage = true)
            )
          }
        }
      }

      testAuthFailures(path, MTDSupportingAgent)
    }
    testNoClientDataFailure(path)
  }
}

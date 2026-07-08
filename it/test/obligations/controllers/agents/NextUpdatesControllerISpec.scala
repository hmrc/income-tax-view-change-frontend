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

package obligations.controllers.agents

import common.controllers.ControllerISpecHelper
import common.enums.{MTDPrimaryAgent, MTDSupportingAgent}
import common.helpers.CalculationListStub
import common.helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import common.helpers.servicemocks.ITSAStatusDetailsStub
import common.implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import common.models.admin.OptOutFs
import common.models.core.AccountingPeriodModel
import common.models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, TaxYear}
import common.models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api.http.Status.*
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import common.testConstants.BaseIntegrationTestConstants.*

import obligations.helpers.NextUpdatesStub
import shared.models.audit.NextUpdatesResponseAuditModel
import shared.testConstants.CalculationListIntegrationTestConstants

import java.time.LocalDate
import common.helpers.GetInsourceDetailsStub

class NextUpdatesControllerISpec extends ControllerISpecHelper {

  lazy val fixedDate: LocalDate = LocalDate.of(2024, 6, 5)

  val incomeSourceDetails: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    nino = testNino,
    mtdbsa = testMtditid,
    yearOfMigration = None,
    businesses = List(BusinessDetailsModel(
      "testId",
      incomeSource = Some(testIncomeSource),
      Some(AccountingPeriodModel(fixedDate, fixedDate.plusYears(1))),
      None,
      None,
      Some(getCurrentTaxYearEnd),
      None,
      None,
      address = Some(address)
    )),
    properties = Nil
  )

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val path = "/agents/submission-deadlines"

  s"GET $path" when {
    List(MTDPrimaryAgent, MTDSupportingAgent).foreach { mtdUserRole =>
      val additionalCookies = getAdditionalCookies(mtdUserRole)

      s"there is a ${mtdUserRole.toString}" that {

        testAuthFailures(path, mtdUserRole)

        "is authenticated with a client" that {

          "has obligations" in {
            stubAuthorised(mtdUserRole)

            val currentObligations: ObligationsModel = ObligationsModel(Seq(
              GroupedObligationsModel(
                identification = "testId",
                obligations = List(
                  SingleObligationModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                ))
            ))

            GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetails
            )

            NextUpdatesStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            val res = buildGETMTDClient(path, additionalCookies).futureValue

            GetInsourceDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

            NextUpdatesStub.verifyGetNextUpdates(testNino)

            Then("the next update view displays the correct title")
            res should have(
              httpStatus(OK),
              pageTitleAgent("nextUpdates.heading")
            )

            verifyAuditContainsDetail(NextUpdatesResponseAuditModel(getTestUser(MTDPrimaryAgent, incomeSourceDetails), "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
          }

          "has no obligations" in {
            stubAuthorised(mtdUserRole)
            GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetails
            )

            NextUpdatesStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = ObligationsModel(Seq())
            )

            val res = buildGETMTDClient(path, additionalCookies).futureValue

            GetInsourceDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

            NextUpdatesStub.verifyGetNextUpdates(testNino)

            Then("then Internal server error is returned")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR)
            )
          }

          "has obligations and the Opt Out feature switch enabled" in {
            stubAuthorised(mtdUserRole, List(OptOutFs))
            val currentTaxYear = dateService.getCurrentTaxYearEnd
            val previousYear = currentTaxYear - 1
            val currentObligations: ObligationsModel = ObligationsModel(Seq(
              GroupedObligationsModel(
                identification = "testId",
                obligations = List(
                  SingleObligationModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                ))
            ))

            GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetails
            )

            NextUpdatesStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              taxYear = dateService.getCurrentTaxYear
            )
            CalculationListStub.stubGetCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


            val res = buildGETMTDClient(path, additionalCookies).futureValue

            GetInsourceDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

            NextUpdatesStub.verifyGetNextUpdates(testNino)

            Then("the next update view displays the correct title")
            res should have(
              httpStatus(OK),
              pageTitleAgent("nextUpdates.heading"),
              isElementVisibleById("updates-and-deadlines-tabs")(expectedValue = true),
            )

            verifyAuditContainsDetail(NextUpdatesResponseAuditModel(getTestUser(MTDPrimaryAgent, incomeSourceDetails), "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
          }

          "has obligations and the Opt Out feature switch disabled" in {
            stubAuthorised(mtdUserRole)

            val currentObligations: ObligationsModel = ObligationsModel(Seq(
              GroupedObligationsModel(
                identification = "testId",
                obligations = List(
                  SingleObligationModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                ))
            ))

            GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetails
            )

            NextUpdatesStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            val res = buildGETMTDClient(path, additionalCookies).futureValue

            GetInsourceDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

            NextUpdatesStub.verifyGetNextUpdates(testNino)

            Then("the next update view displays the correct title")
            res should have(
              httpStatus(OK),
              pageTitleAgent("nextUpdates.heading"),
              isElementVisibleById("#updates-software-heading")(expectedValue = false),
              isElementVisibleById("#updates-software-link")(expectedValue = false),
            )

            verifyAuditContainsDetail(NextUpdatesResponseAuditModel(getTestUser(MTDPrimaryAgent, incomeSourceDetails), "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
          }

          "Opt Out feature switch is enabled" should {
            "take the user to an error page" when {

              "there is an ITSA Status API failure" in {
                stubAuthorised(mtdUserRole, List(OptOutFs))
                val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
                val previousYear = currentTaxYear.addYears(-1)
                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                    ))
                ))

                GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetails
                )

                NextUpdatesStub.stubGetNextUpdates(
                  nino = testNino,
                  deadlines = currentObligations
                )
                ITSAStatusDetailsStub.stubGetITSAStatusDetailsError(previousYear.formatAsShortYearRange, futureYears = true)
                CalculationListStub.stubGetCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


                val res = buildGETMTDClient(path, additionalCookies).futureValue

                GetInsourceDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

                NextUpdatesStub.verifyGetNextUpdates(testNino)

                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }

              "there is an Calculation API failure" in {
                stubAuthorised(mtdUserRole, List(OptOutFs))
                val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
                val previousYear = currentTaxYear.addYears(-1)
                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                    ))
                ))

                GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetails
                )

                NextUpdatesStub.stubGetNextUpdates(
                  nino = testNino,
                  deadlines = currentObligations
                )
                ITSAStatusDetailsStub.stubGetITSAStatusDetails(previousYear.formatAsShortYearRange)
                CalculationListStub.stubGetCalculationListError(testNino, previousYear.endYear.toString)


                val res = buildGETMTDClient(path, additionalCookies).futureValue

                GetInsourceDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

                NextUpdatesStub.verifyGetNextUpdates(testNino)

                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }

              "there is both an ITSA Status and Calculation API failure" in {
                stubAuthorised(mtdUserRole, List(OptOutFs))
                val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
                val previousYear = currentTaxYear.addYears(-1)
                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                    ))
                ))

                GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetails
                )

                NextUpdatesStub.stubGetNextUpdates(
                  nino = testNino,
                  deadlines = currentObligations
                )
                ITSAStatusDetailsStub.stubGetITSAStatusDetailsError(previousYear.formatAsShortYearRange, futureYears = true)
                CalculationListStub.stubGetCalculationListError(testNino, previousYear.endYear.toString)


                val res = buildGETMTDClient(path, additionalCookies).futureValue

                GetInsourceDetailsStub.verifyGetIncomeSourceDetails(testMtditid)

                NextUpdatesStub.verifyGetNextUpdates(testNino)

                res should have(
                  httpStatus(INTERNAL_SERVER_ERROR)
                )
              }
            }
          }
        }
      }
    }

    testNoClientDataFailure(path)
  }

}

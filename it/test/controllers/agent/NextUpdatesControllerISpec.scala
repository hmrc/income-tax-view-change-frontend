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
import controllers.ControllerISpecHelper
import enums.{MTDPrimaryAgent, MTDSupportingAgent}
import helpers.servicemocks.AuditStub.verifyAuditContainsDetail
import helpers.servicemocks.{CalculationListStub, ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.admin.OptOutFs
import models.core.AccountingPeriodModel
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, TaxYear}
import models.obligations.{GroupedObligationsModel, ObligationsModel, SingleObligationModel, StatusFulfilled}
import play.api.http.Status._
import play.api.i18n.{Messages, MessagesApi}
import play.api.test.FakeRequest
import testConstants.BaseIntegrationTestConstants._
import testConstants.BusinessDetailsIntegrationTestConstants.address
import testConstants.CalculationListIntegrationTestConstants

import java.time.LocalDate

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
      address = Some(address),
      cashOrAccruals = Some(false)
    )),
    properties = Nil
  )

  val implicitDateFormatter: ImplicitDateFormatter = app.injector.instanceOf[ImplicitDateFormatterImpl]
  implicit val messages: Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  val path = "/agents/next-updates"

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

            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetails
            )

            IncomeTaxViewChangeStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            val res = buildGETMTDClient(path, additionalCookies).futureValue

            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)

            Then("the next update view displays the correct title")
            res should have(
              httpStatus(OK),
              pageTitleAgent("nextUpdates.heading")
            )

            verifyAuditContainsDetail(NextUpdatesResponseAuditModel(getTestUser(MTDPrimaryAgent, incomeSourceDetails), "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
          }

          "has no obligations" in {
            stubAuthorised(mtdUserRole)
            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetails
            )

            IncomeTaxViewChangeStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = ObligationsModel(Seq())
            )

            val res = buildGETMTDClient(path, additionalCookies).futureValue

            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)

            Then("then Internal server error is returned")
            res should have(
              httpStatus(INTERNAL_SERVER_ERROR)
            )
          }

          "has obligations and the Opt Out feature switch enabled" in {
            stubAuthorised(mtdUserRole)
            enable(OptOutFs)
            val currentTaxYear = dateService.getCurrentTaxYearEnd
            val previousYear = currentTaxYear - 1
            val currentObligations: ObligationsModel = ObligationsModel(Seq(
              GroupedObligationsModel(
                identification = "testId",
                obligations = List(
                  SingleObligationModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                ))
            ))

            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetails
            )

            IncomeTaxViewChangeStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )
            ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
              taxYear = dateService.getCurrentTaxYear
            )
            CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


            val res = buildGETMTDClient(path, additionalCookies).futureValue

            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)

            Then("the next update view displays the correct title")
            res should have(
              httpStatus(OK),
              pageTitleAgent("nextUpdates.heading"),
              elementTextBySelector("#updates-software-heading")(expectedValue = "Submitting updates in software"),
              elementTextBySelector("#updates-software-link")
              (expectedValue = "Use your compatible record keeping software (opens in new tab) " +
                "to keep digital records of all your business income and expenses. You must submit these " +
                "updates through your software by each date shown."),
            )

            verifyAuditContainsDetail(NextUpdatesResponseAuditModel(getTestUser(MTDPrimaryAgent, incomeSourceDetails), "testId", currentObligations.obligations.flatMap(_.obligations)).detail)
          }

          "has obligations and the Opt Out feature switch disabled" in {
            stubAuthorised(mtdUserRole)
            disable(OptOutFs)

            val currentObligations: ObligationsModel = ObligationsModel(Seq(
              GroupedObligationsModel(
                identification = "testId",
                obligations = List(
                  SingleObligationModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                ))
            ))

            IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
              status = OK,
              response = incomeSourceDetails
            )

            IncomeTaxViewChangeStub.stubGetNextUpdates(
              nino = testNino,
              deadlines = currentObligations
            )

            val res = buildGETMTDClient(path, additionalCookies).futureValue

            IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

            IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)

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
            "show next updates" when {

              "there is an ITSA Status API failure" in {
                stubAuthorised(mtdUserRole)
                enable(OptOutFs)
                val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
                val previousYear = currentTaxYear.addYears(-1)
                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                    ))
                ))

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetails
                )

                IncomeTaxViewChangeStub.stubGetNextUpdates(
                  nino = testNino,
                  deadlines = currentObligations
                )
                ITSAStatusDetailsStub.stubGetITSAStatusDetailsError(previousYear.formatAsShortYearRange, futureYears = true)
                CalculationListStub.stubGetLegacyCalculationList(testNino, previousYear.endYear.toString)(CalculationListIntegrationTestConstants.successResponseCrystallised.toString())


                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)

                Then("the next update view displays the correct title even if the OptOut fail")
                res should have(
                  httpStatus(OK),
                  pageTitleAgent("nextUpdates.heading")
                )
              }

              "there is an Calculation API failure" in {
                stubAuthorised(mtdUserRole)
                enable(OptOutFs)
                val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
                val previousYear = currentTaxYear.addYears(-1)
                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                    ))
                ))

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetails
                )

                IncomeTaxViewChangeStub.stubGetNextUpdates(
                  nino = testNino,
                  deadlines = currentObligations
                )
                ITSAStatusDetailsStub.stubGetITSAStatusDetails(previousYear.formatAsShortYearRange)
                CalculationListStub.stubGetLegacyCalculationListError(testNino, previousYear.endYear.toString)


                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)

                Then("the next update view displays the correct title even if the OptOut fail")
                res should have(
                  httpStatus(OK),
                  pageTitleAgent("nextUpdates.heading")
                )
              }

              "there is both an ITSA Status and Calculation API failure" in {
                stubAuthorised(mtdUserRole)
                enable(OptOutFs)
                val currentTaxYear = TaxYear.forYearEnd(dateService.getCurrentTaxYearEnd)
                val previousYear = currentTaxYear.addYears(-1)
                val currentObligations: ObligationsModel = ObligationsModel(Seq(
                  GroupedObligationsModel(
                    identification = "testId",
                    obligations = List(
                      SingleObligationModel(fixedDate, fixedDate.plusDays(1), fixedDate.minusDays(1), "Quarterly", None, "testPeriodKey", StatusFulfilled)
                    ))
                ))

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(
                  status = OK,
                  response = incomeSourceDetails
                )

                IncomeTaxViewChangeStub.stubGetNextUpdates(
                  nino = testNino,
                  deadlines = currentObligations
                )
                ITSAStatusDetailsStub.stubGetITSAStatusDetailsError(previousYear.formatAsShortYearRange, futureYears = true)
                CalculationListStub.stubGetLegacyCalculationListError(testNino, previousYear.endYear.toString)


                val res = buildGETMTDClient(path, additionalCookies).futureValue

                IncomeTaxViewChangeStub.verifyGetIncomeSourceDetails(testMtditid)

                IncomeTaxViewChangeStub.verifyGetNextUpdates(testNino)

                Then("the next update view displays the correct title even if the OptOut fail")
                res should have(
                  httpStatus(OK),
                  pageTitleAgent("nextUpdates.heading")
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

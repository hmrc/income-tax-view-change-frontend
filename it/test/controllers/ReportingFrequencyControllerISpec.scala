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

import enums.{MTDIndividual, MTDUserRole}
import helpers.WiremockHelper
import helpers.servicemocks.{ITSAStatusDetailsStub, IncomeTaxViewChangeStub}
import models.admin.{OptInOptOutContentUpdateR17, OptOutFs, ReportingFrequencyPage, SignUpFs}
import models.itsaStatus.ITSAStatus.{Annual, Mandated, NoStatus, Voluntary}
import org.jsoup.Jsoup
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import repositories.UIJourneySessionDataRepository
import testConstants.BaseIntegrationTestConstants.{testMtditid, testNino}
import testConstants.IncomeSourceIntegrationTestConstants._

class ReportingFrequencyControllerISpec extends ControllerISpecHelper {

  val repository: UIJourneySessionDataRepository = app.injector.instanceOf[UIJourneySessionDataRepository]

  val previousStartYear = dateService.getCurrentTaxYear.previousYear.startYear.toString
  val previousEndYear = dateService.getCurrentTaxYear.previousYear.endYear.toString
  val currentStartYear = dateService.getCurrentTaxYear.startYear.toString
  val currentEndYear = dateService.getCurrentTaxYear.endYear.toString
  val nextStartYear = dateService.getCurrentTaxYear.nextYear.startYear.toString
  val nextEndYear = dateService.getCurrentTaxYear.nextYear.endYear.toString

  val fromTheCurrentTaxYear =  " from the current tax year"
  val fromTheNextTaxYear =  " from the next tax year"
  val optOutFromTheCurrentTaxYear =  s"Opt out$fromTheCurrentTaxYear"
  val optOutForTheCurrentTaxYear =  "Opt out for the current tax year"
  val optOutFromTheNextTaxYear =  s"Opt out$fromTheNextTaxYear"
  val optOutOfTheLastTaxYear =  "Opt out of the last tax year"
  val optOutFromTheLastTaxYear =  "Opt out from the last tax year"
  val signUpFromTheCurrentTaxYear =  s"Sign up$fromTheCurrentTaxYear"
  val signUpToTheCurrentTaxYear =  "Sign up to the current tax year"
  val signUpFromTheNextTaxYear =  s"Sign up$fromTheNextTaxYear"

  def optInOptOutLinks(i: Int): String = s"#main-content > div > div > div > ul > li:nth-child($i) > a"

  def latencyDetailsHeader: String = s"#main-content > div > div > div > details > summary > span"

  def latencyDetailsLink: String = "#your-businesses > a"

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if (mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/reporting-frequency"
  }

  def stubCalculationListResponseBody(taxYearEnd: String) = {
    val responseBody =
      """
        |{
        |  "calculationId": "TEST_ID",
        |  "calculationTimestamp": "TEST_STAMP",
        |  "calculationType": "TEST_TYPE",
        |  "crystallised": false
        |}
        |""".stripMargin
    WiremockHelper.stubGet(s"/income-tax-view-change/list-of-calculation-results/$testNino/$taxYearEnd", OK, responseBody)
  }

  mtdAllRoles.foreach { case mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          s"render reporting frequency page with R17 feature switch on" that {
            "has summary cards for opt out/sign up" when {
              "CY is Quaterly and CY+1 is Quaterly" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Voluntary,
                  Voluntary
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-1")(optOutFromTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-1")(optOutFromTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-1")(s"You can stop using Making Tax Digital for Income Tax from the $currentStartYear to $currentEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-1")("Stop now"),
                  elementTextByID("manage-reporting-obligations-card-heading-2")(optOutFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-2")(optOutFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-2")(s"You can stop using Making Tax Digital for Income Tax from the $nextStartYear to $nextEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-2")(s"Stop from 6 April $nextStartYear")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has two sign up summary cards" when {
              "CY is Annual and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Annual,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-1")(signUpFromTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-1")(signUpFromTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-1")(s"You can start using Making Tax Digital for Income Tax from the $currentStartYear to $currentEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-1")("Start now"),
                  elementTextByID("manage-reporting-obligations-card-heading-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-2")(s"You can choose to use Making Tax Digital for Income Tax from the $nextStartYear to $nextEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-2")(s"Start from 6 April $nextStartYear"),
                  elementTextByID("manage-reporting-obligations-note")(s"If you sign up to the current tax year after 7 August $currentStartYear, you would have at least one quarterly update overdue.")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has an opt out and a sign up summary card" when {
              "CY is Quaterly and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Voluntary,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-1")(optOutForTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-1")(optOutForTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-1")(s"You can stop using Making Tax Digital for Income Tax for the $currentStartYear to $currentEndYear tax year."),
                  elementTextByID("manage-reporting-obligations-card-text-1")("Stop now"),
                  elementTextByID("manage-reporting-obligations-card-heading-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-2")(s"You can choose to use Making Tax Digital for Income Tax from the $nextStartYear to $nextEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-2")(s"Start from 6 April $nextStartYear")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has a sign up and an opt out summary card" when {

              "CY is Annual and CY+1 is Quaterly" in {

                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)

                stubAuthorised(mtdUserRole)

                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)

                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Annual,
                  Voluntary
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-1")(signUpToTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-1")(signUpToTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-1")(s"You can start using Making Tax Digital for Income Tax for the $currentStartYear to $currentEndYear tax year."),
                  elementTextByID("manage-reporting-obligations-card-text-1")("Start now"),
                  elementTextByID("manage-reporting-obligations-card-heading-2")(optOutFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-2")(optOutFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-2")(s"You can stop using Making Tax Digital for Income Tax from the $nextStartYear to $nextEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-2")(s"Stop from 6 April $nextStartYear"),
                  elementTextByID("manage-reporting-obligations-note")(s"If you sign up to the current tax year after 7 August $currentStartYear, you would have at least one quarterly update overdue.")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has a sign up and an opt out summary card" when {
              "CY is Annual and CY+1 is Quaterly Mandated" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Annual,
                  Mandated
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-card-heading-1")(signUpToTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-1")(signUpToTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-1")(s"You can start using Making Tax Digital for Income Tax for the $currentStartYear to $currentEndYear tax year."),
                  elementTextByID("manage-reporting-obligations-card-text-1")("Start now"),
                  elementTextByID("manage-reporting-obligations-note")(s"If you sign up to the current tax year after 7 August $currentStartYear, you would have at least one quarterly update overdue.")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has a single sign up summary card" when {
              "CY is Quaterly Mandated and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Mandated,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-2")(s"You can choose to use Making Tax Digital for Income Tax from the $nextStartYear to $nextEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-2")(s"Start from 6 April $nextStartYear")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }

              "CY-1 and CY are Voluntary and CY+1 is Annual but opt out is disabled" in {
                enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17, SignUpFs)
                disable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Voluntary,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-2")(s"You can choose to use Making Tax Digital for Income Tax from the $nextStartYear to $nextEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-2")(s"Start from 6 April $nextStartYear")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page."),
                  elementTextByID("manage-reporting-obligations-card-heading-0")("Opt out from the previous tax year"),
                  elementTextByID("manage-reporting-obligations-card-heading-1")(optOutFromTheCurrentTaxYear)
                )
              }
            }

            "has a single opt out summary card" when {
              "CY is Quaterly Mandated and CY+1 is Quaterly" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Mandated,
                  Voluntary
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-2")(optOutFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-2")(optOutFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-2")(s"You can stop using Making Tax Digital for Income Tax from the $nextStartYear to $nextEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-2")(s"Stop from 6 April $nextStartYear")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has two opt out and one sign up summary card" when {
              "CY-1 is Quarterly, CY is Quarterly and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Voluntary,
                  Annual
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-0")(optOutFromTheLastTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-0")(optOutFromTheLastTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-0")(s"You can stop using Making Tax Digital for Income Tax from the $previousStartYear to $previousEndYear tax year."),
                  elementTextByID("manage-reporting-obligations-card-text-0")("Stop now"),
                  elementTextByID("manage-reporting-obligations-card-heading-1")(optOutForTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-1")(optOutForTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-1")(s"You can stop using Making Tax Digital for Income Tax for the $currentStartYear to $currentEndYear tax year."),
                  elementTextByID("manage-reporting-obligations-card-text-1")(s"Stop from 6 April $currentStartYear"),
                  elementTextByID("manage-reporting-obligations-card-heading-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-2")(s"You can choose to use Making Tax Digital for Income Tax from the $nextStartYear to $nextEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-2")(s"Start from 6 April $nextStartYear")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has one opt out and two sign up summary cards" when {
              "CY-1 is Quaterly, CY is Annual and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Annual,
                  Annual
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-0")(optOutOfTheLastTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-0")(optOutOfTheLastTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-0")(s"You can stop using Making Tax Digital for Income Tax for the $previousStartYear to $previousEndYear tax year."),
                  elementTextByID("manage-reporting-obligations-card-text-0")("Stop now"),
                  elementTextByID("manage-reporting-obligations-card-heading-1")(signUpFromTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-1")(signUpFromTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-1")(s"You can start using Making Tax Digital for Income Tax from the $currentStartYear to $currentEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-1")(s"Start from 6 April $currentStartYear"),
                  elementTextByID("manage-reporting-obligations-card-heading-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-2")(signUpFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-2")(s"You can choose to use Making Tax Digital for Income Tax from the $nextStartYear to $nextEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-2")(s"Start from 6 April $nextStartYear"),
                  elementTextByID("manage-reporting-obligations-note")(s"If you sign up to the current tax year after 7 August $currentStartYear, you would have at least one quarterly update overdue.")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has three opt out summary cards" when {
              "CY-1 is Quaterly, CY is Quaterly and CY+1 is Quaterly" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Voluntary,
                  Voluntary
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-0")(optOutFromTheLastTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-0")(optOutFromTheLastTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-0")(s"You can stop using Making Tax Digital for Income Tax from the $previousStartYear to $previousEndYear tax year."),
                  elementTextByID("manage-reporting-obligations-card-text-0")("Stop now"),
                  elementTextByID("manage-reporting-obligations-card-heading-1")(optOutFromTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-1")(optOutFromTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-1")(s"You can stop using Making Tax Digital for Income Tax from the $currentStartYear to $currentEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-1")(s"Stop from 6 April $currentStartYear"),
                  elementTextByID("manage-reporting-obligations-card-heading-2")(optOutFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-2")(optOutFromTheNextTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-2")(s"You can stop using Making Tax Digital for Income Tax from the $nextStartYear to $nextEndYear tax year onwards."),
                  elementTextByID("manage-reporting-obligations-card-text-2")(s"Stop from 6 April $nextStartYear")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has one CY-1 opt out summary card" when {
              "CY-1 is Quaterly, CY is Mandated and CY+1 is Mandated" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Mandated,
                  Mandated
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-0")(optOutOfTheLastTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-0")(optOutOfTheLastTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-0")(s"You can stop using Making Tax Digital for Income Tax for the $previousStartYear to $previousEndYear tax year."),
                  elementTextByID("manage-reporting-obligations-card-text-0")("Stop now")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }

              "CY-1 is Quarterly, CY and CY+1 are annual with Sign Up disabled" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17)
                disable(SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Annual,
                  Annual
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("manage-reporting-obligations-card-heading-0")(optOutOfTheLastTaxYear),
                  elementTextByID("manage-reporting-obligations-card-link-0")(optOutOfTheLastTaxYear),
                  elementTextByID("manage-reporting-obligations-card-desc-0")(s"You can stop using Making Tax Digital for Income Tax for the $previousStartYear to $previousEndYear tax year."),
                  elementTextByID("manage-reporting-obligations-card-text-0")("Stop now")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page."),
                  elementTextByID("manage-reporting-obligations-card-heading-1")(signUpFromTheCurrentTaxYear),
                  elementTextByID("manage-reporting-obligations-card-heading-2")(signUpFromTheNextTaxYear)
                )
              }
            }

            "does not have Manage your reporting frequency section" when {
              "all business are ceased" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignAndSoleTraderCeasedBusiness)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Annual,
                  Annual
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK)
                )

                result shouldNot have(
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses")
                )
              }
              "CY, CY-1 and CY+1 are mandated" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Mandated,
                  Mandated
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "Your reporting obligations")
                )
                result shouldNot have(
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses")
                )
              }

              "Sign up and Opt Out are disabled" in {
                enable(ReportingFrequencyPage, OptInOptOutContentUpdateR17)
                disable(OptOutFs)
                disable(SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Mandated,
                  Mandated
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "Your reporting obligations")
                )
                result shouldNot have(
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses")
                )
              }

              "CY and CY+1 is noStatus with CY-1 being mandated" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  NoStatus,
                  NoStatus
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  pageTitle(mtdUserRole, "Your reporting obligations")
                )
                result shouldNot have(
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations")
                )

                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses")
                )
              }
            }
            "has a ceased business warning" when {
              "all businesses have ceased" in {
                enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignAndSoleTraderCeasedBusiness)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Annual,
                  Annual
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  pageTitle(mtdUserRole, "Your reporting obligations"),
                  httpStatus(OK),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )

                result shouldNot have(
                  elementTextByID("manage-reporting-obligations-heading")("Changing your reporting obligations"),
                  elementTextByID("different-obligations-heading")("What the different reporting obligations are")
                )
              }
            }

            "have latency related information section" when {

              Seq(("sole trader", businessWithLatency), ("property", propertyWithLatency), ("all", allBusinessesWithLatency)).foreach { response =>

                s"${response._1} business is latent" in {

                  enable(ReportingFrequencyPage, OptOutFs, OptInOptOutContentUpdateR17, SignUpFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, response._2)
                  ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                    dateService.getCurrentTaxYear,
                    Voluntary,
                    Annual,
                    Annual
                  )

                  stubCalculationListResponseBody("2022")

                  val result = buildGETMTDClient(path, additionalCookies).futureValue
                  result should have(
                    pageTitle(mtdUserRole, "Your reporting obligations"),
                    httpStatus(OK),
                    elementTextBySelector(latencyDetailsHeader)("You can have different reporting obligations for your new businesses"),
                    elementTextByID("separately-choose-to-opt-out")("For tax years you are using Making Tax Digital for Income Tax, you can separately choose to opt out for any new sole trader or property income source:"),
                    elementTextByID("latency-section-1-bullet-1")("started less than 2 years ago"),
                    elementTextByID("latency-section-1-bullet-2")("that you start in future"),
                    elementTextByID("options-available")("This option is available to your new businesses:"),
                    elementTextByID("latency-section-2-bullet-1")("for up to 2 tax years"),
                    elementTextByID("latency-section-2-bullet-2")("only when you use Making Tax Digital for Income Tax for your other businesses"),
                    elementTextByID("latency-section-2-bullet-3")("even if your total gross income from self-employment or property, or both, exceeds the £50,000 threshold"),
                    elementTextByID("change-reporting-obligations-heading")("How to change your reporting obligations for a new income source"),
                    elementTextByID("your-businesses")("You can do this at any time in the your businesses section.")
                  )

                  if (mtdUserRole == MTDIndividual) {
                    result should have(
                      elementAttributeBySelector(latencyDetailsLink, "href")("/report-quarterly/income-and-expenses/view/manage-your-businesses")
                    )
                  } else {
                    result should have(
                      elementAttributeBySelector(latencyDetailsLink, "href")("/report-quarterly/income-and-expenses/view/agents/manage-your-businesses")
                    )
                  }
                }
              }
            }
          }

          "render the error page" when {
            "ReportingFrequencyPage feature switch is disabled" in {
              disable(ReportingFrequencyPage)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
              stubCalculationListResponseBody("2022")
              val result = buildGETMTDClient(path, additionalCookies).futureValue
              result should have(
                httpStatus(INTERNAL_SERVER_ERROR)
              )
              Jsoup.parse(result.body).title shouldBe "Sorry, there is a problem with the service - GOV.UK"

            }
          }

          s"render reporting frequency page with R17 feature switch off" that {
            "just has generic link for opt out" when {
              "CY is Quaterly and CY+1 is Quaterly" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Voluntary,
                  Voluntary
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))("Opt out of quarterly reporting and report annually")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "just has generic link for opt in" when {
              "CY is Annual and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Annual,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))("Opt in to quarterly reporting")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has tax year link for opt out and onwards link for opt in" when {
              "CY is Quaterly and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Voluntary,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually for the $currentStartYear to $currentEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting from the $nextStartYear to $nextEndYear tax year onwards")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has only the tax year link for opt in" when {
              "CY is Quarterly and CY+1 is Annual but opt out is disabled" in {
                enable(ReportingFrequencyPage, SignUpFs)
                disable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Voluntary,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt in to quarterly reporting from the $nextStartYear to $nextEndYear tax year onwards")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page."),
                    elementTextBySelector(optInOptOutLinks(0))(s"Opt out of quarterly reporting and report annually for the $currentStartYear to $currentEndYear tax year")
                )
              }
            }

            "has only the tax year link for opt out" when {
              "CY is Quarterly and CY+1 is Annual but sign up is disabled" in {
                enable(ReportingFrequencyPage, OptOutFs)
                disable(SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Voluntary,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually for the $currentStartYear to $currentEndYear tax year")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page."),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting from the $nextStartYear to $nextEndYear tax year onwards")
                )
              }
            }

            "has tax year link for opt in and onwards link for opt out" when {
              "CY is Annual and CY+1 is Quaterly" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Annual,
                  Voluntary
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt in to quarterly reporting for the $currentStartYear to $currentEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt out of quarterly reporting and report annually from the $nextStartYear to $nextEndYear tax year onwards")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "just has tax year link for opt in" when {
              "CY is Annual and CY+1 is Quaterly Mandated" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Annual,
                  Mandated
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt in to quarterly reporting for the $currentStartYear to $currentEndYear tax year")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }

              "CY is Quaterly Mandated and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Mandated,
                  Annual
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt in to quarterly reporting from the $nextStartYear to $nextEndYear tax year onwards")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "just has tax year onwards link for opt out" when {
              "CY is Quaterly Mandated and CY+1 is Quaterly" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Mandated,
                  Voluntary
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually from the $nextStartYear to $nextEndYear tax year onwards")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has generic link for opt out and tax year onward link for opt in" when {
              "CY-1 is Quaterly, CY is Quaterly and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Voluntary,
                  Annual
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))("Opt out of quarterly reporting and report annually"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting from the $nextStartYear to $nextEndYear tax year onwards")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "has tax year link for opt out and generic link for opt in" when {
              "CY-1 is Quaterly, CY is Annual and CY+1 is Annual" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Annual,
                  Annual
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually for the $previousStartYear to $previousEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )
              }
            }

            "does not have Manage your reporting frequency section" when {
              "all business are ceased" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignAndSoleTraderCeasedBusiness)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Annual,
                  Annual
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually for the $previousStartYear to $previousEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting"),
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency")
                )
              }
              "CY, CY-1 and CY+1 are mandated" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Mandated,
                  Mandated,
                  Mandated
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK)
                )
                result shouldNot have(
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually for the $previousStartYear to $previousEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency")
                )
              }

              "opt out and sign up are disabled" in {
                enable(ReportingFrequencyPage)
                disable(SignUpFs)
                disable(OptOutFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessAndPropertyResponseWoMigration)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Annual,
                  Voluntary
                )
                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK)
                )
                result shouldNot have(
                  elementTextByID("manage-reporting-frequency-heading")("Manage your reporting frequency for all your businesses"),
                  elementTextBySelector(optInOptOutLinks(1))(s"Opt out of quarterly reporting and report annually for the $previousStartYear to $previousEndYear tax year"),
                  elementTextBySelector(optInOptOutLinks(2))(s"Opt in to quarterly reporting")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
                result shouldNot have(
                  elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency")
                )
              }
            }
            "has a ceased business warning" when {
              "all businesses have ceased" in {
                enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                stubAuthorised(mtdUserRole)
                IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignAndSoleTraderCeasedBusiness)
                ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                  dateService.getCurrentTaxYear,
                  Voluntary,
                  Annual,
                  Annual
                )

                stubCalculationListResponseBody("2022")

                val result = buildGETMTDClient(path, additionalCookies).futureValue
                result should have(
                  httpStatus(OK),
                  elementTextByID("ceased-business-warning")("Warning There are currently no businesses on this account. You can add a sole trader or property business on the your businesses page.")
                )

                result shouldNot have(
                  elementTextByID("different-obligations-heading")("What the different reporting obligations are")
                )
                if(isEnabled(OptInOptOutContentUpdateR17)){
                  pageTitle(mtdUserRole, "reporting.frequency.title.new")
                }else{
                  pageTitle(mtdUserRole, "reporting.frequency.title")
                }
              }
            }

            "have latency related information section" when {

              Seq(("sole trader", businessWithLatency), ("property", propertyWithLatency), ("all", allBusinessesWithLatency)).foreach { response =>

                s"${response._1} business is latent" in {

                  enable(ReportingFrequencyPage, OptOutFs, SignUpFs)
                  stubAuthorised(mtdUserRole)
                  IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, response._2)
                  ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(
                    dateService.getCurrentTaxYear,
                    Voluntary,
                    Annual,
                    Annual
                  )

                  stubCalculationListResponseBody("2022")

                  val result = buildGETMTDClient(path, additionalCookies).futureValue
                  result should have(
                    pageTitle(mtdUserRole, "reporting.frequency.title"),
                    httpStatus(OK),
                    elementTextBySelector(latencyDetailsHeader)("Your new businesses can have a different reporting frequency"),
                    elementTextByID("separately-choose-to-opt-out")("For tax years you report quarterly, you can separately choose to report annually for any new sole trader or property income source:"),
                    elementTextByID("latency-section-1-bullet-1")("started less than 2 years ago"),
                    elementTextByID("latency-section-1-bullet-2")("that you start in future"),
                    elementTextByID("options-available")("This option is available to new businesses:"),
                    elementTextByID("latency-section-2-bullet-1")("for up to 2 tax years"),
                    elementTextByID("latency-section-2-bullet-2")("only when you report quarterly for your other businesses"),
                    elementTextByID("latency-section-2-bullet-3")("even if your income from self-employment or property, or both, exceeds the income threshold"),
                    elementTextByID("change-reporting-obligations-heading")("How to change a new income source’s reporting frequency"),
                    elementTextByID("your-businesses")("You can do this at any time in the all businesses section.")
                  )

                  if (mtdUserRole == MTDIndividual) {
                    result should have(
                      elementAttributeBySelector(latencyDetailsLink, "href")("/report-quarterly/income-and-expenses/view/manage-your-businesses")
                    )
                  } else {
                    result should have(
                      elementAttributeBySelector(latencyDetailsLink, "href")("/report-quarterly/income-and-expenses/view/agents/manage-your-businesses")
                    )
                  }
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

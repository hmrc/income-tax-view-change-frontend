/*
 * Copyright 2025 HM Revenue & Customs
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

package businessDetails.controllers.triggeredMigration

import businessDetails.helpers.TriggeredMigrationStub
import businessDetails.models.audit.TriggeredMigrationCompleteAuditModel
import businessDetails.testConstants.NewCalcBreakdownItTestConstants.liabilityCalculationModelSuccessfulNotCrystallised
import common.controllers.ControllerISpecHelper
import common.enums.{MTDIndividual, MTDUserRole}
import common.helpers.GetInsourceDetailsStub
import common.helpers.servicemocks.{AuditStub, ITSAStatusDetailsStub, IncomeTaxCalculationStub, YearOfMigrationStub}
import common.models.admin.TriggeredMigration
import common.models.incomeSourceDetails.TaxYear
import common.models.itsaStatus.ITSAStatus
import common.testConstants.BaseIntegrationTestConstants.testMtditid
import common.testConstants.IncomeSourceDetailsTestConstants.{singleBusinessIncome, singleBusinessIncomeUnconfirmed, singleBusinessIncomeWithYearOfMigration}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse

class CheckActiveBusinessesConfirmControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val prefix = if (mtdRole == MTDIndividual) "" else "/agents"
    s"$prefix/check-your-active-businesses/confirm"
  }

  def getCompletePath(mtdRole: MTDUserRole): String = {
    val prefix = if (mtdRole == MTDIndividual) "" else "/agents"
    s"${appConfig.basePath}$prefix/check-your-active-businesses/complete"
  }

  def getCheckHmrcRecordsPath(mtdRole: MTDUserRole): String = {
    val prefix = if (mtdRole == MTDIndividual) "" else "/agents"
    s"${appConfig.basePath}$prefix/check-your-active-businesses/hmrc-record"
  }

  object CheckActiveBusinessesConfirmMessages {
    val title = "Have you checked that HMRC records only list your active businesses?"
    val continueText = "Continue"
    val errorSummaryHeading = "There is a problem"
    val errorMessage = "Select yes if you’ve checked that HMRC records only list your active businesses"
  }

  private def checkPageContent(result: WSResponse, mtdRole: MTDUserRole): Unit = {
    result should have(
      httpStatus(OK),
      pageTitle(mtdRole, CheckActiveBusinessesConfirmMessages.title),
      elementTextByID("continue-button")(CheckActiveBusinessesConfirmMessages.continueText)
    )
  }

  mtdAllRoles.foreach { mtdRole =>
    val path = getPath(mtdRole)
    val additionalCookies = getAdditionalCookies(mtdRole)
    val expectedRedirect: String = appConfig.homePageUrl(mtdRole.isAgent)

    s"GET $path" when {
      s"user is $mtdRole" should {
        "render the page when TriggeredMigration FS is enabled - With Year of Migration" in {
          stubAuthorised(mtdRole, List(TriggeredMigration))
          GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncomeWithYearOfMigration)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2023, 2024), ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary, "AA123456A")
          IncomeTaxCalculationStub.stubGetCalculationResponse("AA123456A", "2018", Some("LATEST"))(
            status = OK,
            body = liabilityCalculationModelSuccessfulNotCrystallised
          )
          YearOfMigrationStub.stubGetYearOfMigration("2018")

          val result = buildGETMTDClient(path, additionalCookies).futureValue
          checkPageContent(result, mtdRole)
        }

        "render the page when TriggeredMigration FS is enabled - With no Year of Migration" in {
          stubAuthorised(mtdRole, List(TriggeredMigration))
          GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncomeUnconfirmed)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2023, 2024), ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary, "AA123456A")
          IncomeTaxCalculationStub.stubGetCalculationResponse("AA123456A", "2018", Some("LATEST"))(
            status = OK,
            body = liabilityCalculationModelSuccessfulNotCrystallised
          )
          YearOfMigrationStub.stubGetNoYearOfMigration()

          val result = buildGETMTDClient(path, additionalCookies).futureValue

          result should have(
            httpStatus(BAD_REQUEST)
          )
        }

        "redirect to home page when TriggeredMigration FS is disabled" in {
          stubAuthorised(mtdRole)
          GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)

          val result = buildGETMTDClient(path, additionalCookies).futureValue
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(expectedRedirect)
          )
        }

        testAuthFailures(path, mtdRole)
      }
    }

    s"POST $path" when {
      s"user is $mtdRole" should {
        "redirect to the complete page when 'Yes' is selected" in {
          stubAuthorised(mtdRole, List(TriggeredMigration))
          GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncomeUnconfirmed)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2023, 2024), ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary, "AA123456A")
          IncomeTaxCalculationStub.stubGetCalculationResponse("AA123456A", "2018", Some("LATEST"))(
            status = OK,
            body = liabilityCalculationModelSuccessfulNotCrystallised
          )

          TriggeredMigrationStub.stubUpdateCustomerFacts(testMtditid)(OK)

          YearOfMigrationStub.stubGetYearOfMigration("2018")

          val result = buildPOSTMTDPostClient(
            path,
            additionalCookies,
            body = Map("check-active-businesses-confirm-form" -> Seq("Yes"))
          ).futureValue

          val completePath = getCompletePath(mtdRole)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(s"$completePath")
          )

          AuditStub.verifyAuditEvent(TriggeredMigrationCompleteAuditModel()(getTestUser(mtdRole, singleBusinessIncomeUnconfirmed)))
        }

        "redirect to Check HMRC Records when 'No' is selected" in {
          stubAuthorised(mtdRole, List(TriggeredMigration))
          GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncomeUnconfirmed)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2023, 2024), ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary, "AA123456A")
          IncomeTaxCalculationStub.stubGetCalculationResponse("AA123456A", "2018", Some("LATEST"))(
            status = OK,
            body = liabilityCalculationModelSuccessfulNotCrystallised
          )
          YearOfMigrationStub.stubGetYearOfMigration("2018")

          val result = buildPOSTMTDPostClient(
            path,
            additionalCookies,
            body = Map("check-active-businesses-confirm-form" -> Seq("No"))
          ).futureValue

          val checkHmrcRecordsPath = getCheckHmrcRecordsPath(mtdRole)

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(s"$checkHmrcRecordsPath")
          )
        }

        "return BAD_REQUEST when no option is selected" in {
          stubAuthorised(mtdRole, List(TriggeredMigration))
          GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncomeUnconfirmed)
          ITSAStatusDetailsStub.stubGetITSAStatusFutureYearsDetails(TaxYear(2023, 2024), ITSAStatus.Voluntary, ITSAStatus.Voluntary, ITSAStatus.Voluntary, "AA123456A")
          IncomeTaxCalculationStub.stubGetCalculationResponse("AA123456A", "2018", Some("LATEST"))(
            status = OK,
            body = liabilityCalculationModelSuccessfulNotCrystallised
          )
          YearOfMigrationStub.stubGetYearOfMigration("2018")

          val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty).futureValue

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByClass("govuk-error-summary__title")(CheckActiveBusinessesConfirmMessages.errorSummaryHeading),
            elementTextByClass("govuk-error-summary__list")(CheckActiveBusinessesConfirmMessages.errorMessage)
          )
        }

        "redirect to home page when TriggeredMigration FS is disabled" in {
          stubAuthorised(mtdRole)
          GetInsourceDetailsStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)
          YearOfMigrationStub.stubGetYearOfMigration("2018")

          val result = buildPOSTMTDPostClient(
            path,
            additionalCookies,
            body = Map("check-active-businesses-confirm-form" -> Seq("Yes"))
          ).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(expectedRedirect)
          )
        }

        testAuthFailures(path, mtdRole, optBody = Some(Map("check-active-businesses-confirm-form" -> Seq("Yes"))))
      }
    }
  }
}

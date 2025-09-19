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

package controllers.triggeredMigration

import controllers.ControllerISpecHelper
import enums.{MTDIndividual, MTDUserRole}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.TriggeredMigration
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncome

class CheckActiveBusinessesConfirmControllerISpec extends ControllerISpecHelper {

  def getPath(mtdRole: MTDUserRole): String = {
    val prefix = if (mtdRole == MTDIndividual) "" else "/agents"
    s"$prefix/check-your-active-businesses/confirm"
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
    val expectedRedirect: String = if (mtdRole == MTDIndividual) {
      controllers.routes.HomeController.show().url
    } else {
      controllers.routes.HomeController.showAgent().url
    }

    s"GET $path" when {
      s"user is $mtdRole" should {
        "render the page when TriggeredMigration FS is enabled" in {
          enable(TriggeredMigration)
          stubAuthorised(mtdRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)

          val result = buildGETMTDClient(path, additionalCookies).futureValue
          checkPageContent(result, mtdRole)
        }

        "redirect to home page when TriggeredMigration FS is disabled" in {
          disable(TriggeredMigration)
          stubAuthorised(mtdRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)

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
        "redirect back to the same page when 'Yes' is selected" in {
          enable(TriggeredMigration)
          stubAuthorised(mtdRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)

          val result = buildPOSTMTDPostClient(
            path,
            additionalCookies,
            body = Map("check-active-businesses-confirm" -> Seq("Yes"))
          ).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(s"/report-quarterly/income-and-expenses/view$path")          )
        }

        "redirect back to the same page when 'No' is selected" in {
          enable(TriggeredMigration)
          stubAuthorised(mtdRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)

          val result = buildPOSTMTDPostClient(
            path,
            additionalCookies,
            body = Map("check-active-businesses-confirm" -> Seq("No"))
          ).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(s"/report-quarterly/income-and-expenses/view$path")          )
        }

        "return BAD_REQUEST when no option is selected" in {
          enable(TriggeredMigration)
          stubAuthorised(mtdRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)

          val result = buildPOSTMTDPostClient(path, additionalCookies, body = Map.empty).futureValue

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByClass("govuk-error-summary__title")(CheckActiveBusinessesConfirmMessages.errorSummaryHeading),
            elementTextByClass("govuk-error-summary__list")(CheckActiveBusinessesConfirmMessages.errorMessage)
          )
        }

        "redirect to home page when TriggeredMigration FS is disabled" in {
          disable(TriggeredMigration)
          stubAuthorised(mtdRole)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, singleBusinessIncome)

          val result = buildPOSTMTDPostClient(
            path,
            additionalCookies,
            body = Map("check-active-businesses-confirm" -> Seq("Yes"))
          ).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(expectedRedirect)
          )
        }

        testAuthFailures(path, mtdRole, optBody = Some(Map("check-active-businesses-confirm" -> Seq("Yes"))))
      }
    }
  }
}

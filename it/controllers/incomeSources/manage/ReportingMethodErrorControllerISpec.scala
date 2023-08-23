/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.incomeSources.manage

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.incomeSources.manage.ConfirmReportingMethodForm
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.updateIncomeSource.UpdateIncomeSourceResponseModel
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.mvc.Http.Status
import testConstants.BaseIntegrationTestConstants.{testMtditid, testPropertyIncomeId, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, noPropertyOrBusinessResponse, ukPropertyOnlyResponse}

class ReportingMethodErrorControllerISpec extends ComponentSpecBase {

  private lazy val reportingMethodChangeErrorController = controllers.incomeSources.manage.routes.ReportingMethodChangeErrorController

  val reportingMethodChangeErrorUKPropertyUrl: String = reportingMethodChangeErrorController
    .show(None, UkProperty.key, isAgent = false).url
  val reportingMethodChangeErrorForeignPropertyUrl: String = reportingMethodChangeErrorController
    .show(None, ForeignProperty.key, isAgent = false).url
  val reportingMethodChangeErrorBusinessUrl: String = reportingMethodChangeErrorController
    .show(Some(testSelfEmploymentId), SelfEmployment.key, isAgent = false).url

  val continueButtonText: String = messagesAPI("base.continue")

  val pageTitle: String = messagesAPI("standardError.heading")

  s"calling GET $reportingMethodChangeErrorUKPropertyUrl" should {
    "render the UK Property Reporting Method Change Error page" when {
      s"return ${Status.INTERNAL_SERVER_ERROR}" when {
        "Income Sources FS is disabled" in {

          Given("Income Sources FS is enabled")
          disable(IncomeSources)

          And("API 1771  returns a success response")
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

          val result = IncomeTaxViewChangeFrontend
            .get(s"/income-sources/manage/error-change-reporting-method-not-saved-uk-property")

          verifyIncomeSourceDetailsCall(testMtditid)

          result should have(
            httpStatus(SEE_OTHER)
          )
        }
      }
      "Income Sources FS is enabled" in {

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/income-sources/manage/error-change-reporting-method-not-saved-uk-property")

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitle),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "the user does not have a UK property Income Source" in {

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/income-sources/manage/error-change-reporting-method-not-saved-uk-property")

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  s"calling GET $reportingMethodChangeErrorBusinessUrl" should {
    "render the Sole Trader Business Reporting Method Change Error page" when {
      "Income Sources FS is enabled" in {

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/income-sources/manage/error-change-reporting-method-not-saved?id=$testSelfEmploymentId")

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitle),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
    s"return ${Status.INTERNAL_SERVER_ERROR}" when {
      "Sole Trader Income Source Id does not exist" in {

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val invalidId = "INVALID"

        val result = IncomeTaxViewChangeFrontend
          .get(s"/income-sources/manage/error-change-reporting-method-not-saved?id=$invalidId")

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(INTERNAL_SERVER_ERROR)
        )
      }
    }
  }

  s"calling GET $reportingMethodChangeErrorForeignPropertyUrl" should {
    "render the Foreign Property Reporting Method Change Error page" when {
      "Income Sources FS is enabled" in {

        Given("Income Sources FS is enabled")
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend
          .get(s"/income-sources/manage/error-change-reporting-method-not-saved-foreign-property")

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitle),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"return ${Status.INTERNAL_SERVER_ERROR}" when {
    "the user does not have a Foreign property Income Source" in {

      Given("Income Sources FS is enabled")
      enable(IncomeSources)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

      val result = IncomeTaxViewChangeFrontend
        .get(s"/income-sources/manage/error-change-reporting-method-not-saved-foreign-property")

      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(INTERNAL_SERVER_ERROR)
      )
    }
  }
}

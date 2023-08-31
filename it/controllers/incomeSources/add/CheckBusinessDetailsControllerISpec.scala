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

package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.SelfEmployment
import forms.utils.SessionKeys._
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.createIncomeSource.CreateIncomeSourceResponse
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesAndUkProperty, noPropertyOrBusinessResponse}

class CheckBusinessDetailsControllerISpec extends ComponentSpecBase {

  val checkBusinessDetailsShowUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url
  val checkBusinessDetailsSubmitUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.submit().url
  val addBusinessReportingMethodUrl: String = controllers.incomeSources.add.routes.BusinessReportingMethodController.show(testSelfEmploymentId).url
  val errorPageUrl: String = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(SelfEmployment.key).url

  val sessionData: Map[String, String] = Map(businessName -> "Test Business",
    businessStartDate -> "2022-01-01",
    businessTrade -> "Plumbing",
    addBusinessAddressLine1 -> "Test Road",
    addBusinessPostalCode -> "B32 1PQ",
    addIncomeSourcesAccountingMethod -> "ACCRUALS",
    addBusinessAccountingPeriodEndDate -> "2023-11-11")

  val testBusinessName: String = "Test Business"
  val testBusinessStartDate: String = "1 January 2022"
  val testBusinessTrade: String = "Plumbing"
  val testBusinessAddressLine1: String = "Test Road"
  val testBusinessPostCode: String = "B32 1PQ"
  val testBusinessAccountingMethod: String = "Traditional accounting"
  val continueButtonText: String = messagesAPI("base.confirm-and-continue")

  val noAccountingMethod: String = ""


  s"calling GET $checkBusinessDetailsShowUrl" should {
    "render the Check Business details page with accounting method" when {
      "User is authorised and has no existing businesses" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

        When(s"I call GET $checkBusinessDetailsShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/business-check-details", sessionData)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("check-business-details.heading"),
          elementTextByID("business-name-value")(testBusinessName),
          elementTextByID("business-date-value")(testBusinessStartDate),
          elementTextByID("business-trade-value")(testBusinessTrade),
          elementTextByID("business-address-value")(testBusinessAddressLine1 + " " + testBusinessPostCode),
          elementTextByID("business-accounting-value")(testBusinessAccountingMethod),
          elementTextByID("confirm-button")(continueButtonText)
        )
      }
    }
    "render the Check Business details page without accounting method" when {
      "User is authorised and has existing businesses" in {
        Given("I wiremock stub a successful Income Source Details response with multiple businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesAndUkProperty)

        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

        When(s"I call GET $checkBusinessDetailsShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/business-check-details", sessionData)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("check-business-details.heading"),
          elementTextByID("business-name-value")(testBusinessName),
          elementTextByID("business-date-value")(testBusinessStartDate),
          elementTextByID("business-trade-value")(testBusinessTrade),
          elementTextByID("business-address-value")(testBusinessAddressLine1 + " " + testBusinessPostCode),
          elementTextByID("business-accounting-value")(noAccountingMethod),
          elementTextByID("confirm-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $checkBusinessDetailsSubmitUrl" should {
    s"redirect to $addBusinessReportingMethodUrl" when {
      "user selects 'confirm and continue'" in {
        val formData: Map[String, Seq[String]] = Map("addBusinessName" -> Seq("Test Business Name"),
          "addBusinessTrade" -> Seq("Test Business Name"),
          "addBusinessStartDate" -> Seq("2011-11-11"),
          "addBusinessAddressLine1" -> Seq("Test Business Name"),
          "addBusinessPostalCode" -> Seq("SE15 1WR"),
          "addIncomeSourcesAccountingMethod" -> Seq("CASH"),
          "addBusinessAccountingPeriodEndDate" -> Seq("2023-11-11"))
        enable(IncomeSources)
        val response = List(CreateIncomeSourceResponse(testSelfEmploymentId))
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)
        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, response)

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/add/business-check-details", sessionData)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessReportingMethodUrl)
        )
      }
    }

    s"return BAD_REQUEST $checkBusinessDetailsShowUrl" when {
      "user session has no details" in {
        val formData: Map[String, Seq[String]] = Map("addBusinessName" -> Seq(""),
          "addBusinessTrade" -> Seq(""),
          "addBusinessStartDate" -> Seq(""),
          "addBusinessAddressLine1" -> Seq(""),
          "addBusinessPostalCode" -> Seq(""),
          "addIncomeSourcesAccountingMethod" -> Seq(""))
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-accounting-method")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
        )
      }
    }

    s"redirect to $errorPageUrl" when {
      "error in response from API" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = Map(
          "addBusinessName" -> Seq(""),
          "addBusinessTrade" -> Seq(""),
          "addBusinessStartDate" -> Seq(""),
          "addBusinessAddressLine1" -> Seq(""),
          "addBusinessPostalCode" -> Seq(""),
          "addIncomeSourcesAccountingMethod" -> Seq("")
        )

        IncomeTaxViewChangeStub.stubCreateBusinessDetailsResponse(testMtditid)(OK, List.empty)

        When(s"I call $checkBusinessDetailsSubmitUrl")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-check-details", sessionData)(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(errorPageUrl)
        )
      }
    }
  }
}

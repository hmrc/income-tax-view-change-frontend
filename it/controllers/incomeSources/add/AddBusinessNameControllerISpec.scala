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
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.BusinessNameForm
import forms.utils.SessionKeys.businessName
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.AddIncomeSourceData.businessNameField
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class AddBusinessNameControllerISpec extends ComponentSpecBase {

  val addBusinessNameShowUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.show().url
  val addBusinessNameSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submit().url
  val changeBusinessNameShowUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.changeBusinessName().url
  val changeBusinessNameSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.submitChange().url
  val addBusinessStartDateUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url
  val checkBusinessDetailsUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url
  val addIncomeSourceUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
  val incomeSourcesUrl: String = controllers.routes.HomeController.show().url


  val prefix: String = "add-business-name"
  val htmlTitle = messagesAPI("htmlTitle")
  val dateCookie: Map[String, String] = Map(businessName -> "Test Business")
  val formHint: String = messagesAPI("add-business-name.p1") + " " +
    messagesAPI("add-business-name.p2")
  val continueButtonText: String = messagesAPI("base.continue")

  val testBusinessName: String = "Test Business"
  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val journeyTypeSE: JourneyType = JourneyType(Add, SelfEmployment)

  s"calling GET $addBusinessNameShowUrl" should {
    "render the Add Business Name page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $addBusinessNameShowUrl")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessName

        result should have(
          httpStatus(OK),
          pageTitleIndividual("add-business-name.heading"),
          elementTextByID("business-name-hint > p")(formHint),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
    "303 SEE_OTHER - redirect to home page" when {
      "Income Sources FS disabled" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${addBusinessNameShowUrl}")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessName

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourcesUrl)
        )
      }
    }
  }

  s"calling POST ${addBusinessNameSubmitUrl}" should {
    s"303 SEE_OTHER and redirect to $addBusinessStartDateUrl" when {
      "User is authorised and business name is valid" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessNameForm.businessName -> Seq("Test Business")
          )
        }

        When(s"I call POST ${addBusinessNameSubmitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-name")(formData)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessStartDateUrl)
        )

        sessionService.getMongoKeyTyped[String](businessNameField, journeyTypeSE).futureValue shouldBe Right(Some(testBusinessName))
      }
    }
    "show error when form is filled incorrectly" in {
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

      val formData: Map[String, Seq[String]] = {
        Map(
          BusinessNameForm.businessName -> Seq("£££")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-name")(formData)
      result should have(
        httpStatus(BAD_REQUEST),
        elementTextByID("business-name-error")(messagesAPI("base.error-prefix") + " " +
          messagesAPI("add-business-name.form.error.invalidNameFormat"))
      )
    }
  }


  s"calling GET $changeBusinessNameShowUrl" should {
    "render the Add Business Name page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET $changeBusinessNameShowUrl")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/change-business-name")

        result should have(
          httpStatus(OK),
          pageTitleIndividual("add-business-name.heading"),
          elementTextByID("business-name-hint > p")(formHint),
          elementTextByID("continue-button")(continueButtonText)
        )

        sessionService.getMongoKeyTyped[String](businessNameField, journeyTypeSE).futureValue shouldBe Right(Some(testBusinessName))
      }
    }
    "303 SEE_OTHER - redirect to home page" when {
      "Income Sources FS disabled" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${changeBusinessNameShowUrl}")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/change-business-name")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourcesUrl)
        )
      }
    }
  }

  s"calling POST ${changeBusinessNameSubmitUrl}" should {
    s"303 SEE_OTHER and redirect to $checkBusinessDetailsUrl" when {
      "User is authorised and business name is valid" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessNameForm.businessName -> Seq("Test Business")
          )
        }

        When(s"I call POST ${changeBusinessNameSubmitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-name")(formData)
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkBusinessDetailsUrl)
        )

        sessionService.getMongoKeyTyped[String](businessNameField, journeyTypeSE).futureValue shouldBe Right(Some(testBusinessName))
      }
    }
    "show error when form is filled incorrectly" in {
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

      val formData: Map[String, Seq[String]] = {
        Map(
          BusinessNameForm.businessName -> Seq("£££")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-name")(formData)
      result should have(
        httpStatus(BAD_REQUEST),
        elementTextByID("business-name-error")(messagesAPI("base.error-prefix") + " " +
          messagesAPI("add-business-name.form.error.invalidNameFormat"))
      )
    }
  }
}

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

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSources}
import forms.incomeSources.add.BusinessNameForm
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.admin.{IncomeSourcesFs, NavBarFs}
import models.incomeSourceDetails.AddIncomeSourceData.businessNameField
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class AddBusinessNameControllerISpec extends ControllerISpecHelper {

  val addBusinessStartDateUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(incomeSourceType = SelfEmployment, isAgent = false, isChange = false).url
  val addIncomeSourceUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
  val incomeSourcesUrl: String = controllers.routes.HomeController.show().url


  val formHint: String = messagesAPI("add-business-name.p1") + " " +
    messagesAPI("add-business-name.p2")
  val continueButtonText: String = messagesAPI("base.continue")

  val testBusinessName: String = "Test Business"
  val sessionService: SessionService = app.injector.instanceOf[SessionService]

  val journeyType: IncomeSources = IncomeSources(Add, SelfEmployment)

  def backUrl(isChange: Boolean): String = {
    if (isChange) {
      controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url
    } else {
      controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
    }
  }

  val path = "/income-sources/add/business-name"
  val changePath = "/income-sources/add/change-business-name"

  s"GET $path" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "render the Add Business Name page" when {
        "income sources feature is enabled" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = buildGETMTDClient(path).futureValue

          lazy val document: Document = Jsoup.parse(result.body)
          document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl(isChange = false)

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
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          disable(IncomeSourcesFs)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = buildGETMTDClient(path).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(incomeSourcesUrl)
          )
        }
      }
    }
    testAuthFailuresForMTDIndividual(path)
  }

  s"GET $changePath" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "render the Add Business Name page" when {
        "User is authorised" in {
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          enable(IncomeSourcesFs)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = buildGETMTDClient(changePath).futureValue

          lazy val document: Document = Jsoup.parse(result.body)
          document.getElementsByClass("govuk-back-link").attr("href") shouldBe backUrl(isChange = true)

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
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          disable(IncomeSourcesFs)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = buildGETMTDClient(changePath).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(incomeSourcesUrl)
          )
        }
      }
    }
    testAuthFailuresForMTDIndividual(changePath)
  }

  s"POST $path" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      s"303 SEE_OTHER and redirect to $addBusinessStartDateUrl" when {
        "the income sources is enabled" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val formData: Map[String, Seq[String]] = {
            Map(
              BusinessNameForm.businessName -> Seq("Test Business")
            )
          }

          val result = buildPOSTMTDPostClient(path, body = formData).futureValue
          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(addBusinessStartDateUrl)
          )

          sessionService.getMongoKeyTyped[String](businessNameField, journeyType).futureValue shouldBe Right(Some(testBusinessName))
        }
      }
      "show error when form is filled incorrectly" in {
        enable(IncomeSourcesFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessNameForm.businessName -> Seq("£££")
          )
        }

        val result = buildPOSTMTDPostClient(path, body = formData).futureValue

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("business-name-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("add-business-name.form.error.invalidNameFormat"))
        )
      }
    }
    testAuthFailuresForMTDIndividual(path, Some(Map(
      BusinessNameForm.businessName -> Seq("Test Business")
    )))
  }

  s"POST $changePath" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      val expectedUrl = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url
      s"303 SEE_OTHER and redirect to $expectedUrl" when {
        "the income sources is enabled" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val formData: Map[String, Seq[String]] = {
            Map(
              BusinessNameForm.businessName -> Seq("Test Business")
            )
          }

          val result = buildPOSTMTDPostClient(changePath, body = formData).futureValue

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(expectedUrl)
          )

          sessionService.getMongoKeyTyped[String](businessNameField, journeyType).futureValue shouldBe Right(Some(testBusinessName))
        }
      }
      "show error when form is filled incorrectly" in {
        enable(IncomeSourcesFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()

        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessNameForm.businessName -> Seq("£££")
          )
        }

        val result = buildPOSTMTDPostClient(changePath, body = formData).futureValue

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("business-name-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("add-business-name.form.error.invalidNameFormat"))
        )
      }
    }
    testAuthFailuresForMTDIndividual(changePath, Some(Map(
      BusinessNameForm.businessName -> Seq("Test Business")
    )))
  }
}

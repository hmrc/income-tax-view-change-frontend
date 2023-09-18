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

package controllers.incomeSources.cease

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import forms.utils.SessionKeys
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{testMtditid, testPropertyIncomeId, testSelfEmploymentId}
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

class IncomeSourceEndDateControllerISpec extends ComponentSpecBase {
  val dateBusinessShowUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(Some(testPropertyIncomeId), SelfEmployment.key).url
  val dateBusinessSubmitUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.submit(Some(testPropertyIncomeId), SelfEmployment.key).url
  val dateBusinessShowChangeUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showChange(Some(testPropertyIncomeId), SelfEmployment.key).url
  val dateBusinessSubmitChangeUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.submitChange(Some(testPropertyIncomeId), SelfEmployment.key).url
  val checkCeaseBusinessDetailsShowUrl: String = controllers.incomeSources.cease.routes.CheckCeaseBusinessDetailsController.show().url

  val dateUKPropertyShowUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(None, UkProperty.key).url
  val dateUKPropertySubmitUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.submit(None, UkProperty.key).url
  val dateUKPropertyShowChangeUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showChange(None, UkProperty.key).url
  val dateUKPropertySubmitChangeUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.submitChange(None, UkProperty.key).url
  val checkYourCeaseDetailsUkPropertyShowUrl: String = controllers.incomeSources.cease.routes.CheckCeaseUKPropertyDetailsController.show().url

  val dateForeignPropertyShowUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.show(None, ForeignProperty.key).url
  val dateForeignPropertySubmitUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.submit(None, ForeignProperty.key).url
  val dateForeignPropertyShowChangeUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.showChange(None, ForeignProperty.key).url
  val dateForeignPropertySubmitChangeUrl: String = controllers.incomeSources.cease.routes.IncomeSourceEndDateController.submitChange(None, ForeignProperty.key).url
  val checkYourCeaseDetailsForeignPropertyShowUrl: String = controllers.incomeSources.cease.routes.CheckCeaseForeignPropertyDetailsController.show().url


  val hintText: String = messagesAPI("dateForm.hint")
  val continueButtonText: String = messagesAPI("base.continue")
  val testChangeDay: String = "20"
  val testChangeMonth: String = "12"
  val testChangeYear: String = "2022"


  s"calling GET $dateBusinessShowUrl" should {
    "render the Date Business Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a business")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        When(s"I call GET $dateBusinessShowUrl")
        val result = IncomeTaxViewChangeFrontend.getBusinessEndDate
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.selfEmployment.heading"),
          elementTextByID("income-source-end-date-hint")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateBusinessSubmitUrl" should {
    s"redirect to $checkCeaseBusinessDetailsShowUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("27"), "income-source-end-date.month" -> Seq("8"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/cease/business-end-date?id=$testSelfEmploymentId")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkCeaseBusinessDetailsShowUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("5"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/cease/business-end-date?id=$testSelfEmploymentId")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.selfEmployment.error.invalid"))
        )
      }
    }
  }

  s"calling GET $dateBusinessShowChangeUrl" should {
    "render the Date Business Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with a business")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)


        When(s"I call GET $dateBusinessShowChangeUrl")
        val testChangeCeaseBusinessEndDate: Map[String, String] = Map(SelfEmployment.endDateSessionKey -> "2022-10-10")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/cease/change-business-end-date?id=$testSelfEmploymentId", testChangeCeaseBusinessEndDate)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.selfEmployment.heading"),
          elementTextByID("income-source-end-date-hint")(hintText),
          elementTextByID("income-source-end-date.day")("10"),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }

  s"calling POST $dateBusinessSubmitChangeUrl" should {
    s"redirect to $checkCeaseBusinessDetailsShowUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("27"), "income-source-end-date.month" -> Seq("8"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/cease/change-business-end-date?id=$testSelfEmploymentId")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkCeaseBusinessDetailsShowUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("5"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post(s"/income-sources/cease/change-business-end-date?id=$testSelfEmploymentId")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.selfEmployment.error.invalid"))
        )
      }
    }
  }


  s"calling GET $dateUKPropertyShowUrl" should {
    "render the Date UK Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $dateUKPropertyShowUrl")
        val result = IncomeTaxViewChangeFrontend.getUKPropertyEndDate
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.ukProperty.heading"),
          elementTextByID("income-source-end-date-hint")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateUKPropertySubmitUrl" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("20"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/uk-property-end-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsUkPropertyShowUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/uk-property-end-date")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.ukProperty.error.invalid"))
        )
      }
    }
  }

  s"calling GET $dateUKPropertyShowChangeUrl" should {
    "render the Date UK Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with UK property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        When(s"I call GET $dateUKPropertyShowChangeUrl")
        val testChangeCeaseUkPropertyEndDate: Map[String, String] = Map(UkProperty.endDateSessionKey -> "2022-10-10")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/cease/change-uk-property-end-date", testChangeCeaseUkPropertyEndDate)


        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.ukProperty.heading"),
          elementTextByID("income-source-end-date-hint")(hintText),
          elementTextByID("income-source-end-date.day")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateUKPropertySubmitChangeUrl" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("20"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/change-uk-property-end-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsUkPropertyShowUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/change-uk-property-end-date")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.ukProperty.error.invalid"))
        )
      }
    }
  }

  s"calling GET $dateForeignPropertyShowUrl" should {
    "render the Date Foreign Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        When(s"I call GET $dateForeignPropertyShowUrl")
        val result = IncomeTaxViewChangeFrontend.getForeignPropertyEndDate
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.foreignProperty.heading"),
          elementTextByID("income-source-end-date-hint")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateForeignPropertySubmitUrl" should {
    "redirect to showUKPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("20"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/foreign-property-end-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsForeignPropertyShowUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/foreign-property-end-date")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.foreignProperty.error.invalid"))
        )
      }
    }
  }

  s"calling GET $dateForeignPropertyShowChangeUrl" should {
    "render the Date Foreign Property Ceased Page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with Foreign property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        When(s"I call GET $dateForeignPropertyShowChangeUrl")
        val testChangeCeaseForeignPropertyEndDate: Map[String, String] = Map(ForeignProperty.endDateSessionKey -> "2022-10-10")
        val result = IncomeTaxViewChangeFrontend.get(s"/income-sources/cease/change-foreign-property-end-date", testChangeCeaseForeignPropertyEndDate)


        result should have(
          httpStatus(OK),
          pageTitleIndividual("incomeSources.cease.endDate.foreignProperty.heading"),
          elementTextByID("income-source-end-date-hint")(hintText),
          elementTextByID("continue-button")(continueButtonText)
        )
      }
    }
  }
  s"calling POST $dateForeignPropertySubmitChangeUrl" should {
    "redirect to showForeignPropertyEndDateControllerUrl" when {
      "form is filled correctly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("20"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/change-foreign-property-end-date")(formData)

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkYourCeaseDetailsForeignPropertyShowUrl)
        )
      }
      "form is filled incorrectly" in {
        val formData: Map[String, Seq[String]] = {
          Map("income-source-end-date.day" -> Seq("aa"), "income-source-end-date.month" -> Seq("12"), "income-source-end-date.year" -> Seq("2022"))
        }
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontend.post("/income-sources/cease/change-foreign-property-end-date")(formData)

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("income-source-end-date-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("incomeSources.cease.endDate.foreignProperty.error.invalid"))
        )
      }
    }
  }
}


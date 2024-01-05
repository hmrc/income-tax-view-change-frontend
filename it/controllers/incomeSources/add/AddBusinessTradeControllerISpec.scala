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


package controllers.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.BusinessTradeForm
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.incomeSourceDetails.AddIncomeSourceData.businessTradeField
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesResponse, noPropertyOrBusinessResponse}

class AddBusinessTradeControllerISpec extends ComponentSpecBase {

  val addBusinessTradeControllerShowUrl: String = controllers.incomeSources.add.routes.AddBusinessTradeController.show(isAgent = false, isChange = false).url
  val addBusinessTradeSubmitUrl: String = controllers.incomeSources.add.routes.AddBusinessTradeController.submit(isAgent = false, isChange = false).url
  val changeBusinessTradeUrl: String = controllers.incomeSources.add.routes.AddBusinessTradeController.show(isAgent = false, isChange = true).url
  val submitChangeBusinessTradeUrl: String = controllers.incomeSources.add.routes.AddBusinessTradeController.submit(isAgent = false, isChange = true).url

  val addBusinessAddressUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.show(isChange = false).url
  val incomeSourcesUrl: String = controllers.routes.HomeController.show().url
  val checkDetailsUrl: String = controllers.incomeSources.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url

  val pageTitleMsgKey: String = messagesAPI("add-business-trade.heading")
  val pageHint: String = messagesAPI("add-business-trade.p1")
  val button: String = messagesAPI("base.continue")
  val testBusinessName: String = "Test Business Name"
  val testBusinessTrade: String = "Test Business Trade"

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val journeyType: JourneyType = JourneyType(Add, SelfEmployment)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  s"calling GET $addBusinessTradeControllerShowUrl" should {
    "render the Add Business trade page for an Individual" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with multiple businesses and a uk property")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
          addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

        When(s"I call GET $addBusinessTradeControllerShowUrl")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessTrade
        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          elementTextByID("business-trade-hint")(pageHint),
          elementTextByID("continue-button")(button)
        )
      }
    }
    "303 SEE_OTHER - redirect to home page" when {
      "Income Sources FS disabled" in {

        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${addBusinessTradeControllerShowUrl}")
        val result = IncomeTaxViewChangeFrontend.getAddBusinessTrade

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourcesUrl)
        )
      }
    }
  }

  s"calling POST ${addBusinessTradeSubmitUrl}" should {
    s"303 SEE_OTHER and redirect to $addBusinessAddressUrl" when {
      "User is authorised and business trade is valid" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessTradeForm.businessTrade -> Seq(testBusinessTrade)
          )
        }

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
          addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

        And("Mongo storage is successfully set")
        When(s"I call POST ${addBusinessTradeSubmitUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-trade")(formData)

        sessionService.getMongoKeyTyped[String](businessTradeField, JourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(testBusinessTrade))

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(addBusinessAddressUrl)
        )
      }
    }
    "show error when form is filled incorrectly" in {
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

      await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
        addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

      val formData: Map[String, Seq[String]] = {
        Map(
          BusinessTradeForm.businessTrade -> Seq("")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-trade")(formData)
      result should have(
        httpStatus(BAD_REQUEST),
        elementTextByID("business-trade-error")(messagesAPI("base.error-prefix") + " " +
          messagesAPI("add-business-trade.form.error.empty"))
      )
    }
  }


  s"calling GET $changeBusinessTradeUrl" should {
    "render the Change Business Trade page" when {
      "User is authorised" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
          addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName), Some(testBusinessTrade))))))

        When(s"I call GET $changeBusinessTradeUrl")
        val result = IncomeTaxViewChangeFrontend.getChangeAddBusinessTrade
        verifyIncomeSourceDetailsCall(testMtditid)

        sessionService.getMongo(journeyType.toString)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitleMsgKey),
          printSelector("#business-trade"),
          elementValueByID("business-trade")(testBusinessTrade),
          elementTextByID("business-trade-hint")(pageHint),
          elementTextByID("continue-button")(button)
        )
      }
    }
    "303 SEE_OTHER - redirect to home page" when {
      "Income Sources FS disabled" in {
        Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
        disable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        When(s"I call GET ${changeBusinessTradeUrl}")
        val result = IncomeTaxViewChangeFrontend.get("/income-sources/add/change-business-trade")
        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(incomeSourcesUrl)
        )
      }
    }
  }

  s"calling POST ${submitChangeBusinessTradeUrl}" should {
    s"303 SEE_OTHER and redirect to $addBusinessAddressUrl" when {
      "User is authorised and business trade is valid" in {
        enable(IncomeSources)
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
          addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName), Some(testBusinessTrade))))))

        val changedTrade = "Updated Business Trade"
        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessTradeForm.businessTrade -> Seq(changedTrade)
          )
        }

        When(s"I call POST ${submitChangeBusinessTradeUrl}")
        val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-trade")(formData)

        sessionService.getMongoKeyTyped[String](businessTradeField, JourneyType(Add, SelfEmployment)).futureValue shouldBe Right(Some(changedTrade))

        result should have(
          httpStatus(SEE_OTHER),
          redirectURI(checkDetailsUrl)
        )
      }
    }
    "show error when form is filled incorrectly" in {
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

      await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
        addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

      val formData: Map[String, Seq[String]] = {
        Map(
          BusinessTradeForm.businessTrade -> Seq("")
        )
      }

      val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/change-business-trade")(formData)
      result should have(
        httpStatus(BAD_REQUEST),
        elementTextByID("business-trade-error")(messagesAPI("base.error-prefix") + " " +
          messagesAPI("add-business-trade.form.error.empty"))
      )
    }
  }
}

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

package controllers.manageBusinesses.add

import controllers.ControllerISpecHelper
import enums.IncomeSourceJourney.SelfEmployment
import enums.JourneyType.{Add, IncomeSources, JourneyType}
import forms.manageBusinesses.add.BusinessTradeForm
import helpers.servicemocks.{IncomeTaxViewChangeStub, MTDIndividualAuthStub}
import models.admin.{IncomeSourcesFs, NavBarFs}
import models.incomeSourceDetails.AddIncomeSourceData.businessTradeField
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import services.SessionService
import testConstants.BaseIntegrationTestConstants.{testMtditid, testSessionId}
import testConstants.IncomeSourceIntegrationTestConstants.{multipleBusinessesResponse, noPropertyOrBusinessResponse}

class AddBusinessTradeControllerISpec extends ControllerISpecHelper {

  val addBusinessAddressUrl: String = controllers.manageBusinesses.add.routes.AddBusinessAddressController.show(isChange = false).url
  val incomeSourcesUrl: String = controllers.routes.HomeController.show().url
  val checkDetailsUrl: String = controllers.manageBusinesses.add.routes.IncomeSourceCheckDetailsController.show(SelfEmployment).url

  val pageTitleMsgKey: String = messagesAPI("add-trade.heading")
  val pageHint: String = messagesAPI("add-trade.trade-info-1") + " " + messagesAPI("add-trade.trade-info-2")
  val button: String = messagesAPI("base.continue")
  val testBusinessName: String = "Test Business Name"
  val testBusinessTrade: String = "Test Business Trade"

  val sessionService: SessionService = app.injector.instanceOf[SessionService]
  val journeyType: JourneyType = IncomeSources(Add, SelfEmployment)

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(sessionService.deleteSession(Add))
  }

  val path = "/manage-your-businesses/add-sole-trader/business-trade"
  val changePath = "/manage-your-businesses/add-sole-trader/change-business-trade"

  s"GET $path" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      "render the Add Business trade page for an Agent" when {
        "Income Sources FS enabled" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
            addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

          val res = buildGETMTDClient(path).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          res should have(
            httpStatus(OK),
            pageTitleIndividual(pageTitleMsgKey),
            elementTextByID("business-trade-hint")(pageHint),
            elementTextByID("continue-button")(button)
          )
        }
      }
      "303 SEE_OTHER - redirect to home page" when {
        "Income Sources FS disabled" in {
          disable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
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
      "render the Add Business trade page for an Agent" when {
        "Income Sources FS enabled" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)
          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
            addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))
          val res = buildGETMTDClient(changePath).futureValue
          verifyIncomeSourceDetailsCall(testMtditid)

          res should have(
            httpStatus(OK),
            pageTitleIndividual(pageTitleMsgKey),
            elementTextByID("business-trade-hint")(pageHint),
            elementTextByID("continue-button")(button)
          )
        }
      }
      "303 SEE_OTHER - redirect to home page" when {
        "Income Sources FS disabled" in {
          disable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
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
      s"303 SEE_OTHER and redirect to $addBusinessAddressUrl" when {
        "User is authorised and business trade is valid" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val formData: Map[String, Seq[String]] = {
            Map(
              BusinessTradeForm.businessTrade -> Seq(testBusinessTrade)
            )
          }

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
            addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

          val result = buildPOSTMTDPostClient(path, body = formData).futureValue

          sessionService.getMongoKeyTyped[String](businessTradeField, IncomeSources(Add, SelfEmployment)).futureValue shouldBe Right(Some(testBusinessTrade))

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(addBusinessAddressUrl)
          )
        }
      }
      "show error when form is filled incorrectly" in {
        enable(IncomeSourcesFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
          addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName))))))

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessTradeForm.businessTrade -> Seq("")
          )
        }

        val result = buildPOSTMTDPostClient(path, body = formData).futureValue

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("business-trade-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("add-business-trade.form.error.empty"))
        )
      }
    }
    testAuthFailuresForMTDIndividual(path, optBody = Some(Map(
      BusinessTradeForm.businessTrade -> Seq(testBusinessTrade)
    )))
  }

  s"POST $changePath" when {
    "the user is authenticated, with a valid MTD enrolment" should {
      s"303 SEE_OTHER and redirect to $checkDetailsUrl" when {
        "User is authorised and business trade is valid" in {
          enable(IncomeSourcesFs)
          disable(NavBarFs)
          MTDIndividualAuthStub.stubAuthorised()
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val changedTrade = "Updated Business Trade"
          val formData: Map[String, Seq[String]] = {
            Map(
              BusinessTradeForm.businessTrade -> Seq(changedTrade)
            )
          }

          await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
            addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName), Some(testBusinessTrade))))))

          val result = buildPOSTMTDPostClient(changePath, body = formData).futureValue

        sessionService.getMongoKeyTyped[String](businessTradeField, IncomeSources(Add, SelfEmployment)).futureValue shouldBe Right(Some(changedTrade))

          result should have(
            httpStatus(SEE_OTHER),
            redirectURI(checkDetailsUrl)
          )
        }
      }
      "show error when form is filled incorrectly" in {
        enable(IncomeSourcesFs)
        disable(NavBarFs)
        MTDIndividualAuthStub.stubAuthorised()
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, multipleBusinessesResponse)

        await(sessionService.setMongoData(UIJourneySessionData(testSessionId, "ADD-SE",
          addIncomeSourceData = Some(AddIncomeSourceData(Some(testBusinessName), Some(testBusinessTrade))))))

        val formData: Map[String, Seq[String]] = {
          Map(
            BusinessTradeForm.businessTrade -> Seq("")
          )
        }

        val result = buildPOSTMTDPostClient(changePath, body = formData).futureValue

        result should have(
          httpStatus(BAD_REQUEST),
          elementTextByID("business-trade-error")(messagesAPI("base.error-prefix") + " " +
            messagesAPI("add-business-trade.form.error.empty"))
        )
      }
    }
    testAuthFailuresForMTDIndividual(changePath, optBody = Some(Map(
      BusinessTradeForm.businessTrade -> Seq(testBusinessTrade)
    )))
  }
}

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

package controllers.agent.incomeSources.add

import config.featureswitch.IncomeSources
import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.agent.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.{clientDetailsWithConfirmation, testMtditid}
import testConstants.IncomeSourceIntegrationTestConstants.noPropertyOrBusinessResponse

class IncomeSourcesAccountingMethodControllerISpec extends ComponentSpecBase {

  val addIncomeSourcesAccountingMethodShowUrlSoleTrader: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(SelfEmployment).url
  val addIncomeSourcesAccountingMethodShowUrlUK: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(UkProperty).url
  val addIncomeSourcesAccountingMethodShowUrlForeign: String = controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.showAgent(ForeignProperty).url

  val checkBusinessDetailsShowAgentUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url
  val checkUKPropertyDetailsShowAgentUrl: String = controllers.incomeSources.add.routes.CheckUKPropertyDetailsController.showAgent().url
  val foreignPropertyCheckDetailsShowAgentUrl: String = controllers.incomeSources.add.routes.ForeignPropertyCheckDetailsController.showAgent().url

  val selfEmploymentAccountingMethod: String = "incomeSources.add." + SelfEmployment.key + ".AccountingMethod"
  val UKPropertyAccountingMethod: String = "incomeSources.add." + UkProperty.key + ".AccountingMethod"
  val foreignPropertyAccountingMethod: String = "incomeSources.add." + ForeignProperty.key + ".AccountingMethod"

  val continueButtonText: String = messagesAPI("base.continue")

  def authorisedUserTest(addIncomeSourcesAccountingMethodShowUrl: String, url: String, messageKey: String): Unit = {
    "User is authorised" in {
      Given("I wiremock stub a successful Income Source Details response with no businesses or properties")
      stubAuthorisedAgentUser(authorised = true)
      enable(IncomeSources)
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

      When(s"I call GET $addIncomeSourcesAccountingMethodShowUrl")
      val result = IncomeTaxViewChangeFrontend.get(url, clientDetailsWithConfirmation)
      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(OK),
        pageTitleAgent(messageKey),
        elementTextByID("continue-button")(continueButtonText)
      )
    }
  }

  def userSelectionValueTest(checkDetailsShowAgentUrl: String, url:String, formData: Map[String, Seq[String]]): Unit = {
    stubAuthorisedAgentUser(authorised = true)
    enable(IncomeSources)
    IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

    val result = IncomeTaxViewChangeFrontend.post(url, clientDetailsWithConfirmation)(formData)

    result should have(
      httpStatus(SEE_OTHER),
      redirectURI(checkDetailsShowAgentUrl)
    )
  }

  s"calling GET $addIncomeSourcesAccountingMethodShowUrlSoleTrader" should {
    "render the Business Accounting Method page" when {
      authorisedUserTest(addIncomeSourcesAccountingMethodShowUrlSoleTrader, "/income-sources/add/business-accounting-method", "incomeSources.add.SE.AccountingMethod.heading")
    }
  }
  s"calling GET $addIncomeSourcesAccountingMethodShowUrlUK" should {
    "render the Business Accounting Method page" when {
      authorisedUserTest(addIncomeSourcesAccountingMethodShowUrlUK, "/income-sources/add/uk-property-accounting-method", "incomeSources.add.UK.AccountingMethod.heading")
    }
  }
  s"calling GET $addIncomeSourcesAccountingMethodShowUrlForeign" should {
    "render the Business Accounting Method page" when {
      authorisedUserTest(addIncomeSourcesAccountingMethodShowUrlForeign, "/income-sources/add/foreign-property-business-accounting-method", "incomeSources.add.FP.AccountingMethod.heading")
    }
  }
  s"calling POST $addIncomeSourcesAccountingMethodShowUrlSoleTrader" should {
    s"redirect to $checkBusinessDetailsShowAgentUrl" when {
      "user selects 'cash basis accounting', 'cash' should be added to session storage" in {
        val formData: Map[String, Seq[String]] = Map(selfEmploymentAccountingMethod -> Seq("cash"))
        userSelectionValueTest(checkBusinessDetailsShowAgentUrl, "/income-sources/add/business-accounting-method", formData)
      }
      s"redirect to $checkBusinessDetailsShowAgentUrl" when {
        "user selects 'traditional accounting', 'accruals' should be added to session storage" in {
          val formData: Map[String, Seq[String]] = Map(selfEmploymentAccountingMethod -> Seq("traditional"))
          userSelectionValueTest(checkBusinessDetailsShowAgentUrl, "/income-sources/add/business-accounting-method", formData)
        }
      }
      s"return BAD_REQUEST $checkBusinessDetailsShowAgentUrl" when {
        "user does not select anything" in {
          stubAuthorisedAgentUser(authorised = true)
          val formData: Map[String, Seq[String]] = Map(selfEmploymentAccountingMethod -> Seq(""))
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/business-accounting-method", clientDetailsWithConfirmation)(formData)

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByID("error-summary-heading")(messagesAPI("base.error_summary.heading"))
          )
        }
      }
    }
  }
  s"calling POST $addIncomeSourcesAccountingMethodShowUrlUK" should {
    s"redirect to $checkUKPropertyDetailsShowAgentUrl" when {
      "user selects 'cash basis accounting', 'cash' should be added to session storage" in {
        val formData: Map[String, Seq[String]] = Map(UKPropertyAccountingMethod -> Seq("cash"))
        userSelectionValueTest(checkUKPropertyDetailsShowAgentUrl, "/income-sources/add/uk-property-accounting-method", formData)
      }
      s"redirect to $checkUKPropertyDetailsShowAgentUrl" when {
        "user selects 'traditional accounting', 'accruals' should be added to session storage" in {
          val formData: Map[String, Seq[String]] = Map(UKPropertyAccountingMethod -> Seq("traditional"))
          userSelectionValueTest(checkUKPropertyDetailsShowAgentUrl, "/income-sources/add/uk-property-accounting-method", formData)
        }
      }
      s"return BAD_REQUEST $checkUKPropertyDetailsShowAgentUrl" when {
        "user does not select anything" in {
          stubAuthorisedAgentUser(authorised = true)
          val formData: Map[String, Seq[String]] = Map(UKPropertyAccountingMethod -> Seq(""))
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/uk-property-accounting-method", clientDetailsWithConfirmation)(formData)

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByID("error-summary-heading")(messagesAPI("base.error_summary.heading"))
          )
        }
      }
    }
  }
  s"calling POST $addIncomeSourcesAccountingMethodShowUrlForeign" should {
    s"redirect to $foreignPropertyCheckDetailsShowAgentUrl" when {
      "user selects 'cash basis accounting', 'cash' should be added to session storage" in {
        val formData: Map[String, Seq[String]] = Map(foreignPropertyAccountingMethod -> Seq("cash"))
        userSelectionValueTest(foreignPropertyCheckDetailsShowAgentUrl, "/income-sources/add/foreign-property-business-accounting-method", formData)
      }
      s"redirect to $foreignPropertyCheckDetailsShowAgentUrl" when {
        "user selects 'traditional accounting', 'accruals' should be added to session storage" in {
          val formData: Map[String, Seq[String]] = Map(foreignPropertyAccountingMethod -> Seq("traditional"))
          userSelectionValueTest(foreignPropertyCheckDetailsShowAgentUrl, "/income-sources/add/foreign-property-business-accounting-method", formData)
        }
      }
      s"return BAD_REQUEST $foreignPropertyCheckDetailsShowAgentUrl" when {
        "user does not select anything" in {
          stubAuthorisedAgentUser(authorised = true)
          val formData: Map[String, Seq[String]] = Map(foreignPropertyAccountingMethod -> Seq(""))
          enable(IncomeSources)
          IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, noPropertyOrBusinessResponse)

          val result = IncomeTaxViewChangeFrontend.post("/income-sources/add/foreign-property-business-accounting-method", clientDetailsWithConfirmation)(formData)

          result should have(
            httpStatus(BAD_REQUEST),
            elementTextByID("error-summary-heading")(messagesAPI("base.error_summary.heading"))
          )
        }
      }
    }
  }
}

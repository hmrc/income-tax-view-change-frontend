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

import enums.IncomeSourceJourney.{ForeignProperty, SelfEmployment, UkProperty}
import helpers.ComponentSpecBase
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.IncomeSources
import play.api.http.Status.{OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.{businessOnlyResponse, foreignPropertyOnlyResponse, ukPropertyOnlyResponse}

class IncomeSourceNotAddedControllerISpec extends ComponentSpecBase {

  private lazy val incomeSourceNotAddedController = controllers.manageBusinesses.add.routes.IncomeSourceNotAddedController

  val selfEmploymentNotSavedErrorUrl: String = incomeSourceNotAddedController.show(incomeSourceType = SelfEmployment).url
  val ukPropertyNotSavedErrorUrl: String = incomeSourceNotAddedController.show(incomeSourceType = UkProperty).url
  val foreignPropertyNotSavedErrorUrl: String = incomeSourceNotAddedController.show(incomeSourceType = ForeignProperty).url

  val continueButtonText: String = messagesAPI("")
  val pageTitle: String = messagesAPI("standardError.heading")

  s"calling GET $selfEmploymentNotSavedErrorUrl" should {
    "render the self employment business not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses
          .get(s"/manage-your-businesses/add-sole-trader/error-business-not-added")

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitle)
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

      val result = IncomeTaxViewChangeFrontendManageBusinesses
        .get(s"/manage-your-businesses/add-sole-trader/error-business-not-added")

      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }

  s"calling GET $ukPropertyNotSavedErrorUrl" should {
    "render the UK Property not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses
          .get(s"/manage-your-businesses/add-uk-property/error-uk-property-not-added")

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitle)
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, ukPropertyOnlyResponse)

      val result = IncomeTaxViewChangeFrontendManageBusinesses
        .get(s"/manage-your-businesses/add-uk-property/error-uk-property-not-added")

      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }

  s"calling GET $foreignPropertyNotSavedErrorUrl" should {
    "render the Foreign property not added error page" when {
      "Income Sources FS is enabled" in {
        enable(IncomeSources)

        And("API 1771  returns a success response")
        IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

        val result = IncomeTaxViewChangeFrontendManageBusinesses
          .get(s"/manage-your-businesses/add-foreign-property/error-foreign-property-not-added")

        verifyIncomeSourceDetailsCall(testMtditid)

        result should have(
          httpStatus(OK),
          pageTitleIndividual(pageTitle)
        )
      }
    }
    "Income Sources FS is disabled" in {
      Given("Income Sources FS is enabled")
      disable(IncomeSources)

      And("API 1771  returns a success response")
      IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, foreignPropertyOnlyResponse)

      val result = IncomeTaxViewChangeFrontendManageBusinesses
        .get(s"/manage-your-businesses/add-foreign-property/error-foreign-property-not-added")

      verifyIncomeSourceDetailsCall(testMtditid)

      result should have(
        httpStatus(SEE_OTHER)
      )
    }
  }
}
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
import enums.IncomeSourceJourney.{ForeignProperty, UkProperty}
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent, MTDUserRole}
import forms.manageBusinesses.add.AddProprertyForm
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{IncomeSourcesNewJourney, NavBarFs}
import models.core.NormalMode
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class AddPropertyControllerISpec extends ControllerISpecHelper {

  val startDateUkPropertyUrl = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent = true, incomeSourceType = UkProperty, mode = NormalMode).url
  val startDateForeignPropertyUrl = controllers.manageBusinesses.add.routes.AddIncomeSourceStartDateController.show(isAgent = true, incomeSourceType = ForeignProperty, mode = NormalMode).url

  val continueButtonText: String = messagesAPI("base.continue")

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/manage-your-businesses/add-property/property-type"
  }

  List(MTDPrimaryAgent, MTDSupportingAgent).foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the Add Property page" when {
            "income source feature is enabled" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "manageBusinesses.type-of-property.heading"),
                elementTextByID("continue-button")(continueButtonText)
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
    s"POST $path" when {
      s"a user is a $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "redirect to the add uk property start date page" when {
            "form response is UK" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              val result = buildPOSTMTDPostClient(path, additionalCookies,
                body = Map(AddProprertyForm.response -> Seq(AddProprertyForm.responseUK))).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(startDateUkPropertyUrl)
              )
            }
          }
          "redirect to the add foreign property start date page" when {
            "form response is foreign propery" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              val result = buildPOSTMTDPostClient(path, additionalCookies,
                body = Map(AddProprertyForm.response -> Seq(AddProprertyForm.responseForeign))).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(startDateForeignPropertyUrl)
              )
            }
          }
          "return a BAD_REQUEST" when {
            "form is empty" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              val result = buildPOSTMTDPostClient(path, additionalCookies,
                body = Map(AddProprertyForm.response -> Seq())).futureValue

              result should have(
                httpStatus(BAD_REQUEST)
              )
            }
            "form is invalid" in {
              enable(IncomeSourcesNewJourney)
              disable(NavBarFs)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              val result = buildPOSTMTDPostClient(path, additionalCookies,
                body = Map(AddProprertyForm.response -> Seq("£"))).futureValue

              result should have(
                httpStatus(BAD_REQUEST)
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole, optBody = Some(Map(AddProprertyForm.response -> Seq(AddProprertyForm.responseUK))))
      }
    }
  }
}

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
import forms.manageBusinesses.add.{AddProprertyForm, ChooseSoleTraderAddressForm}
import helpers.servicemocks.IncomeTaxViewChangeStub
import models.admin.{NavBarFs, OverseasBusinessAddress}
import models.core.NormalMode
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import testConstants.BaseIntegrationTestConstants.testMtditid
import testConstants.IncomeSourceIntegrationTestConstants.businessOnlyResponse

class ChooseSoleTraderAddressViewSpec extends ControllerISpecHelper {

  val continueButtonText: String = messagesAPI("base.continue")

  def getPath(mtdRole: MTDUserRole): String = {
    val pathStart = if(mtdRole == MTDIndividual) "" else "/agents"
    pathStart + "/manage-your-businesses/add-sole-trader/choose-address"
  }

  List(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent).foreach { mtdUserRole =>
    val path = getPath(mtdUserRole)
    val additionalCookies = getAdditionalCookies(mtdUserRole)
    s"GET $path" when {
      s"user is ${if(mtdUserRole != MTDSupportingAgent) "an" else "a"} $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "render the ChooseSoleTraderAddress page" when {
            "OverseasBusinessAddress FS is enabled" in {
              disable(NavBarFs)
              enable(OverseasBusinessAddress)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              val result = buildGETMTDClient(path, additionalCookies).futureValue

              result should have(
                httpStatus(OK),
                pageTitle(mtdUserRole, "manageBusinesses.add.chooseSoleTraderAddress.heading")
              )
            }
          }
        }
        testAuthFailures(path, mtdUserRole)
      }
    }
    s"POST $path" when {
      s"user is ${if(mtdUserRole != MTDSupportingAgent) "an" else "a"} $mtdUserRole" that {
        "is authenticated, with a valid enrolment" should {
          "reload the page" when {
            //TODO nav ticket should implement proper redirection test here
            "form response is Existing Address" in {
              val isAgent: Boolean = mtdUserRole != MTDIndividual
              disable(NavBarFs)
              enable(OverseasBusinessAddress)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              val result = buildPOSTMTDPostClient(path, additionalCookies,
                body = Map(ChooseSoleTraderAddressForm.response -> Seq(ChooseSoleTraderAddressForm.existingAddress))).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.show(isAgent).url)
              )
            }
            "form response is New Address" in {
              val isAgent: Boolean = mtdUserRole != MTDIndividual
              disable(NavBarFs)
              enable(OverseasBusinessAddress)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              val result = buildPOSTMTDPostClient(path, additionalCookies,
                body = Map(ChooseSoleTraderAddressForm.response -> Seq(ChooseSoleTraderAddressForm.newAddress))).futureValue

              result should have(
                httpStatus(SEE_OTHER),
                redirectURI(controllers.manageBusinesses.add.routes.ChooseSoleTraderAddressController.show(isAgent).url)
              )
            }
          }
          "return a BAD_REQUEST" when {
            "form is empty" in {
              disable(NavBarFs)
              enable(OverseasBusinessAddress)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              val result = buildPOSTMTDPostClient(path, additionalCookies,
                body = Map(ChooseSoleTraderAddressForm.response -> Seq())).futureValue

              result should have(
                httpStatus(BAD_REQUEST)
              )
            }
            "form is invalid" in {
              disable(NavBarFs)
              enable(OverseasBusinessAddress)
              stubAuthorised(mtdUserRole)
              IncomeTaxViewChangeStub.stubGetIncomeSourceDetailsResponse(testMtditid)(OK, businessOnlyResponse)

              val result = buildPOSTMTDPostClient(path, additionalCookies,
                body = Map(ChooseSoleTraderAddressForm.response -> Seq("£"))).futureValue

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

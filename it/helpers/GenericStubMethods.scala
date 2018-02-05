/*
 * Copyright 2017 HM Revenue & Customs
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
package helpers

import helpers.IntegrationTestConstants.{testMtditid, testNino}
import helpers.servicemocks._
import play.api.libs.json.{JsNull, JsValue}

trait GenericStubMethods extends CustomMatchers {

  def isAuthorisedUser(bool: Boolean): Unit = {
    if(bool){
      Given("I wiremock stub an isAuthorisedUser user response")
      AuthStub.stubAuthorised()
    } else {
      Given("I wiremock stub an unatuhorised user response")
      AuthStub.stubUnauthorised()
    }
  }

  def stubUserDetails(): Unit = {
    And("I wiremock stub a response from the User Details service")
    UserDetailsStub.stubGetUserDetails()
  }

  def stubUserDetailsError(): Unit= {
    And("I wiremock stub a Error Response from the User Details service")
    UserDetailsStub.stubGetUserDetailsError()
  }

  def stubPartial(): Unit = {
    And("I wiremock stub a ServiceInfo Partial response")
    BtaPartialStub.stubGetServiceInfoPartial()
  }

  def getBizDeets(response: JsValue = JsNull): Unit = {
    if(response == JsNull) {
      And("I wiremock stub a success business details response, with no Business Income Source")
      SelfAssessmentStub.stubGetNoBusinessDetails(testNino)
    } else {
      And("I wiremock stub a success business details response")
      SelfAssessmentStub.stubGetBusinessDetails(testNino, response)
    }
  }

  def getPropDeets(response: JsValue): Unit = {
    And("I wiremock stub a successful Property Details response")
    SelfAssessmentStub.stubGetPropertyDetails(testNino, response)
  }

  def verifyBizDeetsCall(): Unit = {
    Then("Verify business details has been called")
    SelfAssessmentStub.verifyGetBusinessDetails(testNino)
  }

  def verifyPropDeetsCall(): Unit = {
    Then("Verify property details has been called")
    SelfAssessmentStub.verifyGetPropertyDetails(testNino)
  }

  def verifyBizObsCall(employmentIds: String*): Unit = {
    Then("Verify that business obligations has been called")
    for(employmentId <- employmentIds){
      SelfAssessmentStub.verifyGetBusinessReportDeadlines(testNino, employmentId)
    }
  }

  def verifyPropObsCall(): Unit = {
    Then("Verify that property obligations has been called")
    SelfAssessmentStub.verifyGetPropertyReportDeadlines(testNino)
  }

  def verifyFinancialTransactionsCall(): Unit = {
    Then("Verify that Financial Transactions has been called")
    FinancialTransactionsStub.verifyGetFinancialTransactions(testMtditid)
  }

}

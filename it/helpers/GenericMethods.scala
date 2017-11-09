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

import helpers.IntegrationTestConstants.testNino
import helpers.servicemocks.{AuthStub, BtaPartialStub, SelfAssessmentStub, UserDetailsStub}
import org.scalatest.Assertion
import play.api.libs.json.{JsNull, JsValue}
import play.api.libs.ws.WSResponse

trait GenericMethods extends CustomMatchers {

  def hasTitle(actual: WSResponse, expected: String): Assertion = {
    Then("the page title should be " + expected)
    actual should have(
      pageTitle(expected)
    )
  }

  def hasStatus(actual: WSResponse, expected: Int): Assertion = {
    Then("the result should have a HTTP status " + expected)
    actual should have(
      httpStatus(expected)
    )
  }

  def authorised(bool: Boolean): Unit = {
    if(bool){
      Given("I wiremock stub an authorised user response")
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

}

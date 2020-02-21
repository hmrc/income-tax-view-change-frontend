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

package helpers.servicemocks

import assets.BaseIntegrationTestConstants.testUserName
import helpers.{ComponentSpecBase, WiremockHelper}
import play.api.http.Status

object UserDetailsStub extends ComponentSpecBase {

  val getUserDetailsUrl = userDetailsUrl

  def stubGetUserDetails(): Unit = {
    WiremockHelper.stubGet(getUserDetailsUrl, Status.OK,
        s"""{
           |    "name":"$testUserName",
           |    "email":"test@test.com",
           |    "affinityGroup" : "affinityGroup",
           |    "credentialRole": "n/a",
           |    "description" : "description",
           |    "lastName":"test",
           |    "dateOfBirth":"1980-06-30",
           |    "postCode":"NW94HD",
           |    "authProviderId": "12345-credId",
           |    "authProviderType": "GovernmentGateway"
           |}""".stripMargin)
  }

  def stubGetUserDetailsError(): Unit = {
    WiremockHelper.stubGet(getUserDetailsUrl, Status.INTERNAL_SERVER_ERROR, "Error")
  }
}



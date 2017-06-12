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
package controllers

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import helpers.ComponentSpecBase
import helpers.IntegrationTestConstants._
import helpers.servicemocks.{AuthStub, SelfAssessmentStub}
import models.{ObligationModel, ObligationsModel}
import play.api.http.Status

class ObligationsControllerSpec extends ComponentSpecBase {

  val localDate: String => LocalDate = date => LocalDate.parse(date, DateTimeFormatter.ofPattern("uuuu-M-d"))

  "Calling the ObligationsController" when {
    "authorised with an active enrolment" should {
      "return the correct page with valid obligations data" in {

        val testObligationsModel =
          ObligationsModel(
            List(
              ObligationModel(
                start = localDate("2017-04-06"),
                end = localDate("2017-07-05"),
                due = localDate("2017-08-05"),
                met = true)
            ))

        AuthStub.stubAuthorised()
        SelfAssessmentStub.stubGetObligations(testNino, testSelfEmploymentId, testObligationsModel)
        val res = IncomeTaxViewChangeFrontend.getObligations
        SelfAssessmentStub.verifyGetObligations(testNino, testSelfEmploymentId)

        res.status shouldBe Status.OK
      }
    }
  }
}

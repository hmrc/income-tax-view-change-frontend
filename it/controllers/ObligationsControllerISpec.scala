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
import org.jsoup.Jsoup
import play.api.http.Status
import utils.ImplicitLongDate._

class ObligationsControllerISpec extends ComponentSpecBase {

  val localDate: String => LocalDate = date => LocalDate.parse(date, DateTimeFormatter.ofPattern("uuuu-M-d"))

  "Calling the ObligationsController" when {

    "authorised with an active enrolment" which {
      "has a single obligation" should {
        "display a single obligation with the correct dates and status" in {

          val testObligation = ObligationModel(
            start = localDate("2017-04-06"),
            end = localDate("2017-07-05"),
            due = LocalDate.now(),
            met = true)

          AuthStub.stubAuthorised()
          SelfAssessmentStub.stubGetObligations(testNino, testSelfEmploymentId, ObligationsModel(List(testObligation)))
          val res = IncomeTaxViewChangeFrontend.getObligations
          SelfAssessmentStub.verifyGetObligations(testNino, testSelfEmploymentId)

          //Check status
          res.status shouldBe Status.OK

          val document = Jsoup.parse(res.body)
          //Check obligation details
          document.title() shouldBe "Your Income Tax reports"
          document.getElementById("quarter-1-accounting-period-dates").text shouldBe "6 April 2017 to 5 July 2017"
          document.getElementsByAttributeValue("class", "obligation").size() shouldBe 1
          document.getElementById("quarter-1-obligation-status").text() shouldBe "Received"
        }
      }
      "has multiple obligations" should {
        "display the correct amount of obligations with the correct statuses" in {
          val testObligationModelList = List(ObligationModel(
            start = localDate("2017-04-06"),
            end = localDate("2017-07-05"),
            due = LocalDate.now(),
            met = true
          ), ObligationModel(
            start = localDate("2017-07-06"),
            end = localDate("2017-10-05"),
            due = LocalDate.now().plusDays(1),
            met = false
          ), ObligationModel(
            start = localDate("2017-10-06"),
            end = localDate("2018-01-05"),
            due = LocalDate.now().minusDays(1),
            met = false
          ))
          AuthStub.stubAuthorised()
          SelfAssessmentStub.stubGetObligations(testNino, testSelfEmploymentId, ObligationsModel(testObligationModelList))
          val res = IncomeTaxViewChangeFrontend.getObligations
          SelfAssessmentStub.verifyGetObligations(testNino, testSelfEmploymentId)

          //Check Status
          res.status shouldBe Status.OK

          val document = Jsoup.parse(res.body)
          //Check obligation details
          document.title() shouldBe "Your Income Tax reports"
          document.getElementsByAttributeValue("class", "obligation").size() shouldBe 3
          //Quarter 1
          document.getElementById("quarter-1-accounting-period-dates").text shouldBe "6 April 2017 to 5 July 2017"
          document.getElementById("quarter-1-obligation-status").text shouldBe "Received"
          //Quarter 2
          document.getElementById("quarter-2-accounting-period-dates").text shouldBe "6 July 2017 to 5 October 2017"
          document.getElementById("quarter-2-obligation-status").text shouldBe "Due by " + LocalDate.now().plusDays(1).toLongDate
          //Quarter 3
          document.getElementById("quarter-3-accounting-period-dates").text shouldBe "6 October 2017 to 5 January 2018"
          document.getElementById("quarter-3-obligation-status").text shouldBe "Overdue"
        }
      }
    }
  }
}
/*
 * Copyright 2018 HM Revenue & Customs
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

package assets

import assets.IncomeSourceDetailsTestConstants._
import auth.{MtdItUser, MtdItUserOptionNino, MtdItUserWithNino}
import models.core.{UserDetailsError, UserDetailsModel}
import play.api.http.Status
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}

object BaseTestConstants {

  val testMtditid = "XAIT0000123456"
  val testNino = "AB123456C"
  val testTaxYear = 2018
  val testUserName = "Albert Einstein"
  val testUserDetails = UserDetailsModel(testUserName, None, "n/a", "n/a")
  val testUserDetailsError = UserDetailsError
  val testUserDetailsUrl = "/user/oid/potato"
  val testPaymentRedirectUrl = "http://localhost:9081/report-quarterly/income-and-expenses/view"
  lazy val testMtdUserNoNino: MtdItUserOptionNino[_] = MtdItUserOptionNino(testMtditid, None, None)(FakeRequest())
  lazy implicit val testMtdUserNino: MtdItUserWithNino[_] = MtdItUserWithNino(testMtditid, testNino, None)(FakeRequest())
  lazy val testMtdItUser: MtdItUser[_] = MtdItUser(testMtditid, testNino, Some(testUserDetails), businessesAndPropertyIncome)(FakeRequest())
  lazy val testMtdItUserNoUserDetails: MtdItUser[_] = MtdItUser(testMtditid, testNino, None, businessesAndPropertyIncome)(FakeRequest())
  val testSelfEmploymentId  = "XA00001234"
  val testSelfEmploymentId2 = "XA00001235"
  val testPropertyIncomeId = "1234"
  val testTaxCalculationId = "CALCID"
  val testErrorStatus = Status.INTERNAL_SERVER_ERROR
  val testErrorMessage = "Dummy Error Message"
  val testAuthSuccessResponse = new ~(Enrolments(Set(
    Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", testMtditid)), "activated"),
    Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", testNino)), "activated")
  )),Option(testUserDetailsUrl))
  val testReferrerUrl = "/test/url"

}

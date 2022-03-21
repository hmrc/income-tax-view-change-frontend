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

package testConstants

import controllers.agent.utils.SessionKeys
import play.api.http.Status

import java.time.LocalDate
import models.btaNavBar.{NavContent, NavLinks}

object BaseIntegrationTestConstants {

  val testDate: LocalDate = LocalDate.of(2018, 5, 5)

  val testUserTypeIndividual = "Individual"
  val testUserTypeAgent = "Agent"

  val testMtditidEnrolmentKey = "HMRC-MTD-IT"
  val testMtditidEnrolmentIdentifier = "MTDITID"
  val testMtditid = "XAITSA123456"
  val testUserName = "Albert Einstein"

  val testSaUtrEnrolmentKey = "IR-SA"
  val testSaUtrEnrolmentIdentifier = "UTR"
  val testSaUtr = "1234567890"

  val testNinoEnrolmentKey = "HMRC-NI"
  val testNinoEnrolmentIdentifier = "NINO"
  val testNino = "AA123456A"
  val testCalcId = "01234567"
  val testCalcId2 = "01234568"

  val testTaxYear = 2018
  val taxYear: String = "2020-04-05"
  val testYear = "2018"
  val testYearPlusOne = "2019"
  val testYearInt = 2018
  val testYearPlusOneInt = 2019

  val testCalcType = "it"

  val testSelfEmploymentId = "ABC123456789"
  val otherTestSelfEmploymentId = "ABC123456780"
  val testPropertyIncomeId = "1234"

  val testTradeName = "business"
  val testErrorStatus: Int = Status.INTERNAL_SERVER_ERROR
  val testErrorNotFoundStatus: Int = Status.NOT_FOUND
  val testErrorMessage = "Dummy Error Message"

  val testTaxCalculationId = "CALCID"
  val testTimeStampString = "2017-07-06T12:34:56.789Z"

  val testNavLinks: NavContent = NavContent(
    NavLinks("testEnHome", "testCyHome", "testUrl"),
    NavLinks("testEnAccount", "testCyAccount", "testUrl"),
    NavLinks("testEnMessages", "testCyMessages", "testUrl"),
    NavLinks("testEnHelp", "testCyHelp", "testUrl"),
    NavLinks("testEnForm", "testCyForm", "testUrl", Some(1)),
  )

  val clientDetailsWithoutConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid
  )

  val clientDetailsWithConfirmation: Map[String, String] = Map(
    SessionKeys.clientFirstName -> "Test",
    SessionKeys.clientLastName -> "User",
    SessionKeys.clientUTR -> "1234567890",
    SessionKeys.clientNino -> testNino,
    SessionKeys.clientMTDID -> testMtditid,
    SessionKeys.confirmedClient -> "true"
  )

  val getCurrentTaxYearEnd: LocalDate = {
    val currentDate: LocalDate = LocalDate.now
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, 4, 6))) LocalDate.of(currentDate.getYear, 4, 5)
    else LocalDate.of(currentDate.getYear + 1, 4, 5)
  }
}

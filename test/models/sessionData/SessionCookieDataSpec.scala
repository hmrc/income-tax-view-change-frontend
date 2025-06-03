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

package models.sessionData

import controllers.agent.sessionUtils.SessionKeys
import testConstants.BaseTestConstants.{testFirstName, testMandationStatusOn, testMtditid, testNino, testSaUtr, testSecondName}
import testUtils.UnitSpec

class SessionCookieDataSpec extends UnitSpec {

  "SessionCookieData.toSessionDataModel" should {
    "convert a SessionCookieData to a SessionDataModel" in {
      val testSessionCookieData: SessionCookieData =
        SessionCookieData(mtditid = testMtditid, nino = testNino, utr = testSaUtr,
          clientFirstName = Some(testFirstName), clientLastName = Some(testSecondName),
          isSupportingAgent = true)

      testSessionCookieData.toSessionDataModel shouldBe SessionDataModel(mtditid = testMtditid, nino = testNino, utr = testSaUtr,
        isSupportingAgent = true)
    }
  }

  "SessionCookieData.toSessionCookieSeq" should {
    "convert a SessionCookieData to a sequence of tuples which would be KV pairs to add to a session" when {
      "first name and last name are present" in {
        val testSessionCookieData: SessionCookieData = SessionCookieData(
          mtditid = testMtditid, nino = testNino, utr = testSaUtr, clientFirstName = Some(testFirstName),
          clientLastName = Some(testSecondName), isSupportingAgent = true
        )

        val expectedResult: Seq[(String, String)] =
          Seq(
            SessionKeys.clientMTDID -> testMtditid,
            SessionKeys.clientNino -> testNino,
            SessionKeys.clientUTR -> testSaUtr,
            SessionKeys.isSupportingAgent -> "true",
            SessionKeys.clientFirstName -> testFirstName,
            SessionKeys.clientLastName -> testSecondName
          )

        testSessionCookieData.toSessionCookieSeq shouldBe expectedResult
      }
      "first name and last name are missing" in {
        val testSessionCookieData: SessionCookieData = SessionCookieData(
          mtditid = testMtditid, nino = testNino, utr = testSaUtr, clientFirstName = None,
          clientLastName = None, isSupportingAgent = true,
        )

        val expectedResult: Seq[(String, String)] =
          Seq(
            SessionKeys.clientMTDID -> testMtditid,
            SessionKeys.clientNino -> testNino,
            SessionKeys.clientUTR -> testSaUtr,
            SessionKeys.isSupportingAgent -> "true"
          )

        testSessionCookieData.toSessionCookieSeq shouldBe expectedResult
      }
      "last name is missing" in {
        val testSessionCookieData: SessionCookieData = SessionCookieData(
          mtditid = testMtditid, nino = testNino, utr = testSaUtr, clientFirstName = Some(testFirstName),
          clientLastName = None, isSupportingAgent = true
        )

        val expectedResult: Seq[(String, String)] =
          Seq(
            SessionKeys.clientMTDID -> testMtditid,
            SessionKeys.clientNino -> testNino,
            SessionKeys.clientUTR -> testSaUtr,
            SessionKeys.isSupportingAgent -> "true",
            SessionKeys.clientFirstName -> testFirstName
          )

        testSessionCookieData.toSessionCookieSeq shouldBe expectedResult
      }
      "first name is missing" in {
        val testSessionCookieData: SessionCookieData = SessionCookieData(
          mtditid = testMtditid, nino = testNino, utr = testSaUtr, clientFirstName = None,
          clientLastName = Some(testSecondName), isSupportingAgent = false
        )

        val expectedResult: Seq[(String, String)] =
          Seq(
            SessionKeys.clientMTDID -> testMtditid,
            SessionKeys.clientNino -> testNino,
            SessionKeys.clientUTR -> testSaUtr,
            SessionKeys.isSupportingAgent -> "false",
            SessionKeys.clientLastName -> testSecondName)

        testSessionCookieData.toSessionCookieSeq shouldBe expectedResult
      }
    }
  }

}

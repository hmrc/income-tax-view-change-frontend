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

package audit.models

import assets.TestConstants._
import utils.TestSupport

class IncomeSourceDetailsResponseAuditModelSpec extends TestSupport {

  val transactionName = "income-source-details-response"
  val auditEvent = "incomeSourceDetailsResponse"
  val seIdsKey = "selfEmploymentIncomeSourceIds"
  val propertyIdKey = "propertyIncomeSourceId"

  "The IncomeSourceDetailsResponseAuditModel" when {

    "Supplied with Multiple Business IDs and a Property ID" should {

      val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
        List(testSelfEmploymentId, testSelfEmploymentId2),
        Some(testPropertyIncomeId)
      )(testMtdItUser)

      s"Have the correct transaction name of '$transactionName'" in {
        testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        testIncomeSourceDetailsResponseAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtdItUser.mtditid,
          "nino" -> testMtdItUser.nino,
          seIdsKey -> s"""["$testSelfEmploymentId","$testSelfEmploymentId2"]""",
          propertyIdKey -> testPropertyIncomeId
        )
      }
    }

    "Supplied with Single Business IDs and a Property ID" should {

      val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
        List(testSelfEmploymentId),
        Some(testPropertyIncomeId)
      )(testMtdItUser)

      s"Have the correct transaction name of '$transactionName'" in {
        testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        testIncomeSourceDetailsResponseAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtdItUser.mtditid,
          "nino" -> testMtdItUser.nino,
          seIdsKey -> s"""["$testSelfEmploymentId"]""",
          propertyIdKey -> testPropertyIncomeId
        )
      }
    }

    "Supplied with No Business IDs and a Property ID" should {

      val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
        List(),
        Some(testPropertyIncomeId)
      )(testMtdItUser)

      s"Have the correct transaction name of '$transactionName'" in {
        testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        testIncomeSourceDetailsResponseAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtdItUser.mtditid,
          "nino" -> testMtdItUser.nino,
          propertyIdKey -> testPropertyIncomeId
        )
      }
    }

    "Supplied with Single Business IDs and No Property ID" should {

      val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
        List(testSelfEmploymentId),
        None
      )(testMtdItUser)

      s"Have the correct transaction name of '$transactionName'" in {
        testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        testIncomeSourceDetailsResponseAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtdItUser.mtditid,
          "nino" -> testMtdItUser.nino,
          seIdsKey -> s"""["$testSelfEmploymentId"]"""
        )
      }
    }

    "Supplied with Multiple Business IDs and No Property ID" should {

      val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
        List(testSelfEmploymentId, testSelfEmploymentId2),
        None
      )(testMtdItUser)

      s"Have the correct transaction name of '$transactionName'" in {
        testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        testIncomeSourceDetailsResponseAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtdItUser.mtditid,
          "nino" -> testMtdItUser.nino,
          seIdsKey -> s"""["$testSelfEmploymentId","$testSelfEmploymentId2"]"""
        )
      }
    }

    "Supplied with No Business IDs and No Property ID" should {

      val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
        List(),
        None
      )(testMtdItUser)

      s"Have the correct transaction name of '$transactionName'" in {
        testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
      }

      s"Have the correct audit event type of '$auditEvent'" in {
        testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditEvent
      }

      "Have the correct details for the audit event" in {
        testIncomeSourceDetailsResponseAuditModel.detail shouldBe Seq(
          "mtditid" -> testMtdItUser.mtditid,
          "nino" -> testMtdItUser.nino
        )
      }
    }
  }
}

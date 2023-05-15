/*
 * Copyright 2023 HM Revenue & Customs
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

import testConstants.BaseTestConstants._
import auth.MtdItUserWithNino
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.test.FakeRequest
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core.retrieve.Name

class IncomeSourceDetailsResponseAuditModelSpec extends TestSupport {

  val transactionName = "income-source-details-response"
  val auditType = "incomeSourceDetailsResponse"
  val seIdsKey = "selfEmploymentIncomeSourceIds"
  val propertyIdsKey = "propertyIncomeSourceIds"

  "The IncomeSourceDetailsResponseAuditModel" when {

    "Supplied with Multiple Business IDs and Property IDs for individual which display no arn and Individual as user type" should {
      s"return $OK" when {
        val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
          testMtdUserNino,
          List(testSelfEmploymentId, testSelfEmploymentId2),
          List(testPropertyIncomeId, testPropertyIncomeId2),
          Some(testMigrationYear2019)
        )

        s"Have the correct transaction name of '$transactionName'" in {
          testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
        }

        s"Have the correct audit event type of '$auditType'" in {
          testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
        }

        "Have the correct details for the audit event" in {
          testIncomeSourceDetailsResponseAuditModel.detail shouldBe Json.obj(
            propertyIdsKey -> Json.toJson(List(testPropertyIncomeId, testPropertyIncomeId2)),
            "saUtr" -> testSaUtr,
            "nationalInsuranceNumber" -> testMtdItUser.nino,
            "userType" -> testUserType,
            "dateOfMigration" -> testMigrationYear2019,
            "credId" -> testCredId,
            seIdsKey -> Json.toJson(List(testSelfEmploymentId, testSelfEmploymentId2)),
            "mtditid" -> testMtdItUser.mtditid
          )
        }
      }
    }

    "Supplied with Multiple Business IDs and Property IDs for agent" should {
      s"return $OK" when {
        val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
          mtdItUser = MtdItUserWithNino(
            btaNavPartial = None,
            saUtr = Some(testSaUtr),
            nino = testNino,
            mtditid = testMtditidAgent,
            arn = Some("arn"),
            userType = Some(Agent),
            credId = Some(testCredId),
            userName = Some(Name(Some("firstName"), Some("lastName")))
          )(FakeRequest()),
          List(testSelfEmploymentId, testSelfEmploymentId2),
          List(testPropertyIncomeId, testPropertyIncomeId2),
          Some(testMigrationYear2019)
        )

        s"Have the correct transaction name of '$transactionName'" in {
          testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
        }

        s"Have the correct audit event type of '$auditType'" in {
          testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
        }

        "Have the correct details for the audit event which display the arn and Agent as user type" in {
          testIncomeSourceDetailsResponseAuditModel.detail shouldBe Json.obj(
            propertyIdsKey -> Json.toJson(List(testPropertyIncomeId, testPropertyIncomeId2)),
            "saUtr" -> testSaUtr,
            "nationalInsuranceNumber" -> testNino,
            "agentReferenceNumber" -> "arn",
            "userType" -> "Agent",
            "dateOfMigration" -> testMigrationYear2019,
            "credId" -> testCredId,
            seIdsKey -> Json.toJson(List(testSelfEmploymentId, testSelfEmploymentId2)),
            "mtditid" -> testMtditidAgent
          )
        }
      }
    }

    "Supplied with Single Business IDs and a Property ID" should {
      s"return $OK" when {
        val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
          testMtdUserNino,
          List(testSelfEmploymentId),
          List(testPropertyIncomeId),
          Some(testMigrationYear2019)
        )

        s"Have the correct transaction name of '$transactionName'" in {
          testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
        }

        s"Have the correct audit event type of '$auditType'" in {
          testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
        }

        "Have the correct details for the audit event" in {
          testIncomeSourceDetailsResponseAuditModel.detail shouldBe Json.obj(
            propertyIdsKey -> Json.toJson(List(testPropertyIncomeId)),
            "saUtr" -> testSaUtr,
            "nationalInsuranceNumber" -> testMtdItUser.nino,
            "userType" -> testUserType,
            "dateOfMigration" -> testMigrationYear2019,
            "credId" -> testCredId,
            seIdsKey -> Json.toJson(List(testSelfEmploymentId)),
            "mtditid" -> testMtdItUser.mtditid
          )
        }
      }
    }

    "Supplied with No Business IDs and a Property ID" should {
      s"return $OK" when {
        val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
          testMtdUserNino,
          List(testSelfEmploymentId),
          List(testPropertyIncomeId),
          Some(testMigrationYear2019)
        )

        s"Have the correct transaction name of '$transactionName'" in {
          testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
        }

        s"Have the correct audit event type of '$auditType'" in {
          testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
        }

        "Have the correct details for the audit event" in {
          testIncomeSourceDetailsResponseAuditModel.detail shouldBe Json.obj(
            propertyIdsKey -> Json.toJson(List(testPropertyIncomeId)),
            "saUtr" -> testSaUtr,
            "nationalInsuranceNumber" -> testMtdItUser.nino,
            "userType" -> testUserType,
            "dateOfMigration" -> testMigrationYear2019,
            "credId" -> testCredId,
            seIdsKey -> Json.toJson(List(testSelfEmploymentId)),
            "mtditid" -> testMtdItUser.mtditid
          )
        }
      }
    }

    "Supplied with Single Business IDs and MtditUser, No Property ID, no year of Migration fields" should {
      s"return $OK" when {
        val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
          testMtdUserNino,
          List(testSelfEmploymentId),
          Nil, None
        )

        s"Have the correct transaction name of '$transactionName'" in {
          testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
        }

        s"Have the correct audit event type of '$auditType'" in {
          testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
        }

        "Have the correct details for the audit event" in {
          testIncomeSourceDetailsResponseAuditModel.detail shouldBe Json.obj(
            "saUtr" -> testSaUtr,
            "nationalInsuranceNumber" -> testMtdItUser.nino,
            "userType" -> testUserType,
            "credId" -> testCredId,
            seIdsKey -> Json.toJson(List(testSelfEmploymentId)),
            propertyIdsKey -> Json.toJson(List.empty[String]),
            "mtditid" -> testMtdItUser.mtditid
          )
        }
      }
    }

    "Supplied with Multiple Business IDs and No Property ID" should {
      s"return $OK" when {
        val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
          testMtdUserNino,
          List(testSelfEmploymentId),
          Nil, None
        )

        s"Have the correct transaction name of '$transactionName'" in {
          testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
        }

        s"Have the correct audit event type of '$auditType'" in {
          testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
        }

        "Have the correct details for the audit event" in {
          testIncomeSourceDetailsResponseAuditModel.detail shouldBe Json.obj(
            "saUtr" -> testSaUtr,
            "nationalInsuranceNumber" -> testMtdItUser.nino,
            "userType" -> testUserType,
            "credId" -> testCredId,
            seIdsKey -> Json.toJson(List(testSelfEmploymentId)),
            propertyIdsKey -> Json.toJson(List.empty[String]),
            "mtditid" -> testMtdItUser.mtditid
          )
        }
      }
    }

    "Supplied with Multiple Property IDs and No Business IDs" should {
      s"return $OK" when {
        val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
          testMtdUserNino,
          Nil,
          List(testPropertyIncomeId, testPropertyIncomeId2), None
        )

        s"Have the correct transaction name of '$transactionName'" in {
          testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
        }

        s"Have the correct audit event type of '$auditType'" in {
          testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
        }

        "Have the correct details for the audit event" in {
          testIncomeSourceDetailsResponseAuditModel.detail shouldBe Json.obj(
            "saUtr" -> testSaUtr,
            "nationalInsuranceNumber" -> testMtdItUser.nino,
            "userType" -> testUserType,
            "credId" -> testCredId,
            seIdsKey -> Json.toJson(List.empty[String]),
            propertyIdsKey -> Json.toJson(List(testPropertyIncomeId, testPropertyIncomeId2)),
            "mtditid" -> testMtdItUser.mtditid
          )
        }
      }
    }

    "Supplied with No Business IDs and No Property ID" should {
      s"return $OK" when {
        val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
          testMtdUserNino,
          Nil,
          Nil, None
        )

        s"Have the correct transaction name of '$transactionName'" in {
          testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
        }

        s"Have the correct audit event type of '$auditType'" in {
          testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
        }

        "Have the correct details for the audit event" in {
          testIncomeSourceDetailsResponseAuditModel.detail shouldBe Json.obj(
            "saUtr" -> testSaUtr,
            "nationalInsuranceNumber" -> testMtdItUser.nino,
            "userType" -> testUserType,
            "credId" -> testCredId,
            seIdsKey -> Json.toJson(List.empty[String]),
            propertyIdsKey -> Json.toJson(List.empty[String]),
            "mtditid" -> testMtdItUser.mtditid
          )
        }
      }
    }
  }
}

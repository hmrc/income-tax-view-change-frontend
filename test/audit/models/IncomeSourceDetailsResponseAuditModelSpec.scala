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

import authV2.AuthActionsTestData.defaultAuthorisedAndEnrolledRequest
import enums.{MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent}
import play.api.libs.json.{JsArray, Json}
import play.api.test.FakeRequest
import testConstants.BaseTestConstants._
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual}

class IncomeSourceDetailsResponseAuditModelSpec extends TestSupport {

  val transactionName = "income-source-details-response"
  val auditType = "incomeSourceDetailsResponse"
  val seIdsKey = "selfEmploymentIncomeSourceIds"
  val propertyIdsKey = "propertyIncomeSourceIds"

  "The IncomeSourceDetailsResponseAuditModel" when {
    List(MTDIndividual, MTDPrimaryAgent, MTDSupportingAgent).foreach { mtdUserRole =>
      s"the user is a $mtdUserRole" that {
        "has Multiple Business IDs and Property IDs" should {
          val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
            defaultAuthorisedAndEnrolledRequest(mtdUserRole, FakeRequest()),
            testNino,
            List(testSelfEmploymentId, testSelfEmploymentId2),
            List(testPropertyIncomeId, testPropertyIncomeId2),
            Some(testMigrationYear2019)
          )
          "render the expected audit details" that {
            "has the expected transaction name" in {
              testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
            }
            "has the expected audit type" in {
              testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
            }

            "has the expected details" in {
              val (af, isSupportingAgent) = mtdUserRole match {
                case MTDIndividual => (Individual, false)
                case ur => (Agent, ur == MTDSupportingAgent)
              }
              assertJsonEquals(testIncomeSourceDetailsResponseAuditModel.detail, commonAuditDetails(af, isSupportingAgent) ++ Json.obj(
                propertyIdsKey -> Json.toJson(List(testPropertyIncomeId, testPropertyIncomeId2)),
                "dateOfMigration" -> testMigrationYear2019,
                seIdsKey -> Json.toJson(List(testSelfEmploymentId, testSelfEmploymentId2))
              ))
            }
          }
        }

        "has Single Business IDs and a Property ID" should {
          val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
            defaultAuthorisedAndEnrolledRequest(mtdUserRole, FakeRequest()),
            testNino,
            List(testSelfEmploymentId),
            List(testPropertyIncomeId),
            Some(testMigrationYear2019)
          )
          "render the expected audit details" that {
            "has the expected transaction name" in {
              testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
            }
            "has the expected audit type" in {
              testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
            }

            "has the expected details" in {
              val (af, isSupportingAgent) = mtdUserRole match {
                case MTDIndividual => (Individual, false)
                case ur => (Agent, ur == MTDSupportingAgent)
              }
              assertJsonEquals(testIncomeSourceDetailsResponseAuditModel.detail, commonAuditDetails(af, isSupportingAgent) ++ Json.obj(
                propertyIdsKey -> Json.toJson(List(testPropertyIncomeId)),
                "dateOfMigration" -> testMigrationYear2019,
                seIdsKey -> Json.toJson(List(testSelfEmploymentId))
              ))
            }
          }
        }

        "has no Business IDs or Property IDs" should {
          val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
            defaultAuthorisedAndEnrolledRequest(mtdUserRole, FakeRequest()),
            testNino,
            List(),
            List(),
            Some(testMigrationYear2019)
          )
          "render the expected audit details" that {
            "has the expected transaction name" in {
              testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
            }
            "has the expected audit type" in {
              testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
            }

            "has the expected details" in {
              val (af, isSupportingAgent) = mtdUserRole match {
                case MTDIndividual => (Individual, false)
                case ur => (Agent, ur == MTDSupportingAgent)
              }
              assertJsonEquals(testIncomeSourceDetailsResponseAuditModel.detail, commonAuditDetails(af, isSupportingAgent) ++ Json.obj(
                propertyIdsKey -> JsArray(),
                "dateOfMigration" -> testMigrationYear2019,
                seIdsKey -> JsArray()
              ))
            }
          }
        }

        "has single Business IDs, no Property IDs and no year of migration" should {
          val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
            defaultAuthorisedAndEnrolledRequest(mtdUserRole, FakeRequest()),
            testNino,
            List(testSelfEmploymentId),
            Nil, None
          )
          "render the expected audit details" that {
            "has the expected transaction name" in {
              testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
            }
            "has the expected audit type" in {
              testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
            }

            "has the expected details" in {
              val (af, isSupportingAgent) = mtdUserRole match {
                case MTDIndividual => (Individual, false)
                case ur => (Agent, ur == MTDSupportingAgent)
              }
              assertJsonEquals(testIncomeSourceDetailsResponseAuditModel.detail, commonAuditDetails(af, isSupportingAgent) ++ Json.obj(
                propertyIdsKey -> JsArray(),
                seIdsKey -> Json.toJson(List(testSelfEmploymentId))
              ))
            }
          }
        }

        "has multiple Business IDs, no Property IDs and no year of migration" should {
          val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
            defaultAuthorisedAndEnrolledRequest(mtdUserRole, FakeRequest()),
            testNino,
            List(testSelfEmploymentId, testSelfEmploymentId2),
            Nil, None
          )
          "render the expected audit details" that {
            "has the expected transaction name" in {
              testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
            }
            "has the expected audit type" in {
              testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
            }

            "has the expected details" in {
              val (af, isSupportingAgent) = mtdUserRole match {
                case MTDIndividual => (Individual, false)
                case ur => (Agent, ur == MTDSupportingAgent)
              }
              assertJsonEquals(testIncomeSourceDetailsResponseAuditModel.detail, commonAuditDetails(af, isSupportingAgent) ++ Json.obj(
                propertyIdsKey -> JsArray(),
                seIdsKey -> Json.toJson(List(testSelfEmploymentId, testSelfEmploymentId2))
              ))
            }
          }
        }

        "has multiple Property IDs, no Business IDs" should {
          val testIncomeSourceDetailsResponseAuditModel = IncomeSourceDetailsResponseAuditModel(
            defaultAuthorisedAndEnrolledRequest(mtdUserRole, FakeRequest()),
            testNino,
            Nil,
            List(testPropertyIncomeId, testPropertyIncomeId2),
            None
          )
          "render the expected audit details" that {
            "has the expected transaction name" in {
              testIncomeSourceDetailsResponseAuditModel.transactionName shouldBe transactionName
            }
            "has the expected audit type" in {
              testIncomeSourceDetailsResponseAuditModel.auditType shouldBe auditType
            }

            "has the expected details" in {
              val (af, isSupportingAgent) = mtdUserRole match {
                case MTDIndividual => (Individual, false)
                case ur => (Agent, ur == MTDSupportingAgent)
              }
              assertJsonEquals(testIncomeSourceDetailsResponseAuditModel.detail, commonAuditDetails(af, isSupportingAgent) ++ Json.obj(
                seIdsKey -> Json.toJson(List.empty[String]),
                propertyIdsKey -> Json.toJson(List(testPropertyIncomeId, testPropertyIncomeId2))
              ))
            }
          }
        }
      }
    }
  }
}

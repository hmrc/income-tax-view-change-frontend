/*
 * Copyright 2021 HM Revenue & Customs
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

package services.agent

import mocks.connectors.{MockAgentClientRelationshipsConnector, MockCitizenDetailsConnector, MockIncomeTaxViewChangeConnector}
import models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}
import services.agent.ClientRelationshipService.{BusinessDetailsNotFound, CitizenDetailsNotFound, NoAgentClientRelationship, UnexpectedResponse}
import testUtils.TestSupport

import scala.concurrent.Future

class ClientRelationshipServiceSpec extends TestSupport with MockAgentClientRelationshipsConnector
  with MockCitizenDetailsConnector with MockIncomeTaxViewChangeConnector {

  object TestClientRelationshipService extends ClientRelationshipService(
    mockAgentClientRelationshipsConnector,
    mockCitizenDetailsConnector,
    mockIncomeTaxViewChangeConnector
  )

  ".checkAgentClientRelationship" should {
    "return client details" when {
      "a successful citizen details response contains a nino" when {
        "income source details are returned with an mtdbsa identifer" when {
          "an agent client relationship exists" in {

            setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsModel(Some("James"), Some("Bond"), Some("TESTNINO123"))))
            setupBusinessDetails("TESTNINO123")(Future.successful(IncomeSourceDetailsModel("mtdbsaId", None, List(), None)))
            mockAgentClientRelationship("testArn", "mtdbsaId")(Future.successful(true))

            val result = await(TestClientRelationshipService.checkAgentClientRelationship("testSaUtr", "testArn"))

            result shouldBe Right(ClientRelationshipService.ClientDetails(Some("James"), Some("Bond"), "TESTNINO123", "mtdbsaId"))
          }
        }
      }
    }

    "return a NoAgentClientRelationship error" when {
      "an agent client relationship does not exist" in {

        setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsModel(Some("James"), Some("Bond"), Some("TESTNINO123"))))
        setupBusinessDetails("TESTNINO123")(Future.successful(IncomeSourceDetailsModel("mtdbsaId", None, List(), None)))
        mockAgentClientRelationship("testArn", "mtdbsaId")(Future.successful(false))

        val result = await(TestClientRelationshipService.checkAgentClientRelationship("testSaUtr", "testArn"))

        result shouldBe Left(NoAgentClientRelationship)

      }
    }

    "return a BusinessDetailsNotFound" when {
      "a successful citizen details response contains a nino" when {
        "an income source details error is returned and the code is 404" in {

          setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsModel(Some("James"), Some("Bond"), Some("TESTNINO123"))))
          setupBusinessDetails("TESTNINO123")(Future.successful(IncomeSourceDetailsError(404, "not found")))

          val result = await(TestClientRelationshipService.checkAgentClientRelationship("testSaUtr", "testArn"))

          result shouldBe Left(BusinessDetailsNotFound)
        }
      }
    }

    "return a UnexpectedResponse" when {
      "a successful citizen details response contains a nino" when {
        "an income source details error is returned and the code is 500" in {

          setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsModel(Some("James"), Some("Bond"), Some("TESTNINO123"))))
          setupBusinessDetails("TESTNINO123")(Future.successful(IncomeSourceDetailsError(500, "internal server error")))

          val result = await(TestClientRelationshipService.checkAgentClientRelationship("testSaUtr", "testArn"))

          result shouldBe Left(UnexpectedResponse)
        }
      }
    }

    "return a CitizenDetailsNotFound" when {
      "an citizen details error is returned and the code is 404" in {

        setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsErrorModel(404, "not found")))

        val result = await(TestClientRelationshipService.checkAgentClientRelationship("testSaUtr", "testArn"))

        result shouldBe Left(CitizenDetailsNotFound)
      }

    }

    "return a UnexpectedResponse" when {
      "an citizen details error is returned and the code is 500" in {

        setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsErrorModel(500, "internal server error")))

        val result = await(TestClientRelationshipService.checkAgentClientRelationship("testSaUtr", "testArn"))

        result shouldBe Left(UnexpectedResponse)
      }

    }
  }

}

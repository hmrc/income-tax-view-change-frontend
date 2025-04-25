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

package services.agent

import mocks.connectors._
import models.citizenDetails.{CitizenDetailsErrorModel, CitizenDetailsModel}
import models.incomeSourceDetails.{IncomeSourceDetailsError, IncomeSourceDetailsModel}
import services.agent.ClientDetailsService.{APIError, BusinessDetailsNotFound, CitizenDetailsNotFound}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier}

import scala.concurrent.Future

class ClientDetailsServiceSpec extends TestSupport
  with MockCitizenDetailsConnector with MockBusinessDetailsConnector {

  object TestClientDetailsService extends ClientDetailsService(
    mockCitizenDetailsConnector,
    mockBusinessDetailsConnector
  )

  def createDelegatedEnrolment(id: String): Enrolment = {
    Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", id)), "Activated", Some("mtd-it-auth"))
  }

  ".checkClientDetails" should {
    "return client details" when {
      "a successful citizen details response contains a nino" when {
        "income source details are returned with an mtdbsa identifer" in {

          setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsModel(Some("James"), Some("Bond"), Some("TESTNINO123"))))
          setupBusinessDetails("TESTNINO123")(Future.successful(IncomeSourceDetailsModel("TESTNINO123", "mtdbsaId", None, List(), List())))

          val result = TestClientDetailsService.checkClientDetails("testSaUtr").futureValue

          result shouldBe Right(ClientDetailsService.ClientDetails(Some("James"), Some("Bond"), "TESTNINO123", "mtdbsaId"))
        }
      }
    }

    "return a BusinessDetailsNotFound" when {
      "a successful citizen details response contains a nino" when {
        "an income source details error is returned and the code is 404" in {

          setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsModel(Some("James"), Some("Bond"), Some("TESTNINO123"))))
          setupBusinessDetails("TESTNINO123")(Future.successful(IncomeSourceDetailsError(404, "not found")))

          val result = TestClientDetailsService.checkClientDetails("testSaUtr").futureValue

          result shouldBe Left(BusinessDetailsNotFound)
        }
      }
    }

    "return an APIError" when {
      "a successful citizen details response contains a nino" when {
        "an income source details error is returned and the code is 500" in {

          setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsModel(Some("James"), Some("Bond"), Some("TESTNINO123"))))
          setupBusinessDetails("TESTNINO123")(Future.successful(IncomeSourceDetailsError(500, "internal server error")))

          val result = TestClientDetailsService.checkClientDetails("testSaUtr").futureValue

          result shouldBe Left(APIError)
        }
      }
    }

    "return a CitizenDetailsNotFound" when {
      "an citizen details error is returned and the code is 404" in {

        setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsErrorModel(404, "not found")))

        val result = TestClientDetailsService.checkClientDetails("testSaUtr").futureValue

        result shouldBe Left(CitizenDetailsNotFound)
      }

      "a successful citizen details response does not contain a nino" in {

        setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsModel(Some("James"), Some("Bond"), None)))

        val result = TestClientDetailsService.checkClientDetails("testSaUtr").futureValue

        result shouldBe Left(CitizenDetailsNotFound)

      }

    }

    "return an APIError" when {
      "an citizen details error is returned and the code is 500" in {

        setupMockCitizenDetails("testSaUtr")(Future.successful(CitizenDetailsErrorModel(500, "internal server error")))

        val result = (TestClientDetailsService.checkClientDetails("testSaUtr")).futureValue

        result shouldBe Left(APIError)
      }

    }
  }

}

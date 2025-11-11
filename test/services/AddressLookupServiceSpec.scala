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

package services

import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import connectors.AddressLookupConnector
import models.core.IncomeSourceId.mkIncomeSourceId
import models.core.{CheckMode, NormalMode}
import models.incomeSourceDetails.viewmodels.httpparser.GetAddressLookupDetailsHttpParser.UnexpectedGetStatusFailure
import models.incomeSourceDetails.viewmodels.httpparser.PostAddressLookupHttpParser.{PostAddressLookupSuccessResponse, UnexpectedPostStatusFailure}
import models.incomeSourceDetails.{Address, BusinessAddressModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.scalactic.Fail
import testUtils.TestSupport

import scala.concurrent.Future
import scala.util.Success

class AddressLookupServiceSpec extends TestSupport
  with FeatureSwitching {

  case class AddressError(status: String) extends RuntimeException

  val testBusinessAddressModel: BusinessAddressModel = BusinessAddressModel("auditRef", Address(Seq("Line 1", "Line 2"), Some("AA1 1AA")))

  val mockAddressLookupConnector: AddressLookupConnector = mock(classOf[AddressLookupConnector])

  object TestAddressLookupService extends AddressLookupService(
    app.injector.instanceOf[FrontendAppConfig],
    addressLookupConnector = mockAddressLookupConnector
  )

  "AddressLookupService" should {
    "initialiseAddressJourney" should {
      "return an error when connector lookup fails" in {
        disableAllSwitches()

        when(mockAddressLookupConnector.initialiseAddressLookup(any(), any())(any()))
          .thenReturn(Future(Left(UnexpectedPostStatusFailure(418))))

        val result: Future[Either[Throwable, Option[String]]] = TestAddressLookupService.initialiseAddressJourney(isAgent = false, mode = NormalMode)
        result map {
          case Left(AddressError(value)) => value shouldBe "status: 418"
          case Right(_) => Fail("Error not returned")
          case Left(_) => Fail("Wrong error type returned")
        }
      }

      "return an error when an empty location is returned" in {
        disableAllSwitches()

        when(mockAddressLookupConnector.initialiseAddressLookup(any(), any())(any()))
          .thenReturn(Future(Right(PostAddressLookupSuccessResponse(None))))

        val result: Future[Either[Throwable, Option[String]]] = TestAddressLookupService.initialiseAddressJourney(isAgent = false, mode = NormalMode)
        result map {
          case Left(_) => Fail("Error returned from connector")
          case Right(None) => Success
          case Right(Some(_)) => Fail("Shouldn't return redirect location")
        }
      }

      "return a redirect location when connector lookup works" in {
        disableAllSwitches()

        when(mockAddressLookupConnector.initialiseAddressLookup(any(), any())(any()))
          .thenReturn(Future(Right(PostAddressLookupSuccessResponse(Some("sample location")))))

        val result: Future[Either[Throwable, Option[String]]] = TestAddressLookupService.initialiseAddressJourney(isAgent = false, mode = NormalMode)
        result map {
          case Left(_) => Fail("Error returned from connector")
          case Right(None) => Fail("No redirect location returned from connector")
          case Right(Some(value)) => value shouldBe "sample location"
        }
      }
    }
    "initialiseAddressJourney on change page" should {
      "return an error when connector lookup fails and isChange = true" in {
        disableAllSwitches()

        when(mockAddressLookupConnector.initialiseAddressLookup(any(), any())(any()))
          .thenReturn(Future(Left(UnexpectedPostStatusFailure(418))))

        val result: Future[Either[Throwable, Option[String]]] = TestAddressLookupService.initialiseAddressJourney(isAgent = false, mode = CheckMode)
        result map {
          case Left(AddressError(value)) => value shouldBe "status: 418"
          case Left(_) => Fail("Unexpected error. Should be an AddressError")
          case Right(_) => Fail("Error not returned")
        }
      }

      "return an error when an empty location is returned and isChange = true" in {
        disableAllSwitches()

        when(mockAddressLookupConnector.initialiseAddressLookup(any(), any())(any()))
          .thenReturn(Future(Right(PostAddressLookupSuccessResponse(None))))

        val result: Future[Either[Throwable, Option[String]]] = TestAddressLookupService.initialiseAddressJourney(isAgent = false, mode = CheckMode)
        result map {
          case Left(_) => Fail("Error returned from connector")
          case Right(None) => Success
          case Right(Some(_)) => Fail("Shouldn't return redirect location")
        }
      }

      "return a redirect location when connector lookup works and isChange = true" in {
        disableAllSwitches()

        when(mockAddressLookupConnector.initialiseAddressLookup(any(), any())(any()))
          .thenReturn(Future(Right(PostAddressLookupSuccessResponse(Some("sample location")))))

        val result: Future[Either[Throwable, Option[String]]] = TestAddressLookupService.initialiseAddressJourney(isAgent = false, mode = CheckMode)
        result map {
          case Left(_) => Fail("Error returned from connector")
          case Right(None) => Fail("No redirect location returned from connector")
          case Right(Some(value)) => value shouldBe "sample location"
        }
      }
    }

    "fetchAddress" should {
      "return an error when getting address details fails" in {
        disableAllSwitches()

        when(mockAddressLookupConnector.getAddressDetails(any())(any()))
          .thenReturn(Future(Left(UnexpectedGetStatusFailure(418))))

        val result: Future[Either[Throwable, BusinessAddressModel]] = TestAddressLookupService.fetchAddress( Some(mkIncomeSourceId("")))
        result map {
          case Left(AddressError(value)) => value shouldBe "status: 418"
          case Right(_) => Fail("Error not returned")
          case Left(_) => Fail("Wrong error type returned")
        }
      }

      "return an error when no id provided" in {
        disableAllSwitches()

        when(mockAddressLookupConnector.getAddressDetails(any())(any()))
          .thenReturn(Future(Right(None)))

        val result: Future[Either[Throwable, BusinessAddressModel]] = TestAddressLookupService.fetchAddress(None)
        result map {
          case Left(AddressError(value)) => value shouldBe "No id provided"
          case Right(_) => Fail("Error not returned")
          case Left(_) => Fail("Wrong error type returned")
        }
      }

      "return an error when no address found" in {
        disableAllSwitches()

        when(mockAddressLookupConnector.getAddressDetails(any())(any()))
          .thenReturn(Future(Right(None)))

        val result: Future[Either[Throwable, BusinessAddressModel]] = TestAddressLookupService.fetchAddress(Some(mkIncomeSourceId("")))
        result map {
          case Left(AddressError(value)) => value shouldBe "Not found"
          case Right(_) => Fail("Error not returned")
          case Left(_) => Fail("Wrong error type returned")
        }
      }

      "return a BusinessAddressModel when getting address details works" in {
        disableAllSwitches()

        when(mockAddressLookupConnector.getAddressDetails(any())(any()))
          .thenReturn(Future(Right(Some(testBusinessAddressModel))))

        val result: Future[Either[Throwable, BusinessAddressModel]] = TestAddressLookupService.fetchAddress(Some(mkIncomeSourceId("")))
        result map {
          case Left(_) => Fail("Error returned from connector")
          case Right(BusinessAddressModel(auditRef, address)) =>
            auditRef shouldBe "auditRef"
            address shouldBe Address(Seq("Line 1", "Line 2"), Some("AA1 1AA"))

        }
      }
    }
  }

}

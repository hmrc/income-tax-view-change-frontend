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

package controllers

import audit.models.IvOutcomeSuccessAuditModel
import authV2.AuthActionsTestData.defaultIncomeSourcesData
import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import models.itsaStatus.ITSAStatus.Voluntary
import models.itsaStatus.StatusReason.MtdItsaOptOut
import models.itsaStatus.{ITSAStatusResponseModel, StatusDetail}
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation}
import services.DateServiceInterface
import testConstants.BaseTestConstants.{testNino, testSaUtr}

import scala.concurrent.Future

class UpliftSuccessControllerSpec extends MockAuthActions {

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[UpliftSuccessController]

  val action = testController.success()
  val fakeRequest = fakeGetRequestBasedOnMTDUserType(MTDIndividual)

  ".success()" when {

    "the user is an authenticated individual" should {

      "audit and redirect to the home controller" in {

        setupMockSuccess(MTDIndividual)
        mockItsaStatusRetrievalAction()
        mockSingleBusinessIncomeSource()

        val incomeSourcesResponse = IncomeSourceDetailsModel(testNino, testSaUtr, Some("2012"), Nil, Nil)

        val itsaStatusResponses = List(
          ITSAStatusResponseModel(
            taxYear = "2024-25",
            itsaStatusDetails = Some(List(
              StatusDetail("ts", Voluntary, MtdItsaOptOut, None)
            ))
          ),
          ITSAStatusResponseModel(
            taxYear = "2025-26",
            itsaStatusDetails = Some(List(
              StatusDetail("ts", Voluntary, MtdItsaOptOut, None)
            ))
          )
        )

        when(mockBusinessDetailsConnector.getIncomeSources()(any(), any()))
          .thenReturn(Future(defaultIncomeSourcesData))

        when(mockItsaStatusConnector.getITSAStatusDetail(any(), any(), any(), any())(any()))
          .thenReturn(Future(Right(itsaStatusResponses)))

        when(mockBusinessDetailsConnector.getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future(incomeSourcesResponse))

        when(mockBusinessDetailsConnector.getIncomeSources()(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future(incomeSourcesResponse))

        when(mockDateServiceInterface.getCurrentTaxYear)
          .thenReturn(TaxYear(2025, 2026))

        val result = action(fakeRequest)
        val expectedIvOutcomeSuccessAuditModel = IvOutcomeSuccessAuditModel(testNino)

        whenReady(result) { response =>
          verifyAudit(expectedIvOutcomeSuccessAuditModel)
          response.header.status shouldBe Status.SEE_OTHER
          redirectLocation(result) shouldBe Some(controllers.routes.HomeController.show().url)
        }

      }
    }

testMTDAuthFailuresForRole(action, MTDIndividual)(fakeRequest)
  }
}

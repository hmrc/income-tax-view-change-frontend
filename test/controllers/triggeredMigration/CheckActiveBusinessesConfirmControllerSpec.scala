/*
 * Copyright 2025 HM Revenue & Customs
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

package controllers.triggeredMigration

import connectors.{BusinessDetailsConnector, ITSAStatusConnector, IncomeTaxCalculationConnector}
import enums.MTDIndividual
import mocks.auth.MockAuthActions
import models.admin.TriggeredMigration
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api
import play.api.Application
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, status}
import services.DateServiceInterface
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.singleBusinessIncome

import scala.concurrent.Future

class CheckActiveBusinessesConfirmControllerSpec extends MockAuthActions {

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface),
        api.inject.bind[IncomeTaxCalculationConnector].toInstance(mockIncomeTaxCalculationConnector)
      )
      .build()

  private lazy val controller =
    app.injector.instanceOf[CheckActiveBusinessesConfirmController]

  private def stubIncomeSourceDetails(): Unit =
    when(mockIncomeSourceDetailsService.getIncomeSourceDetails()(any(), any()))
      .thenReturn(Future.successful(singleBusinessIncome.copy(channel = "Hmrc-led-unconfirmed")))

  mtdAllRoles.foreach { mtdRole =>
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdRole)
    val isAgent = mtdRole != MTDIndividual

    s"show(isAgent = $isAgent)" when {
      val action = controller.show(isAgent)

      s"the user is authenticated as a $mtdRole" should {

        "render the page when the TriggeredMigration FS is enabled" in {

          enable(TriggeredMigration)
          setupMockSuccess(mtdRole)
          mockItsaStatusRetrievalAction()
          mockTriggeredMigrationRetrievalAction()
          stubIncomeSourceDetails()

          val result = action(fakeRequest)
          status(result) shouldBe 200
          contentAsString(result) should include("Have you checked that HMRC records only list your active businesses?")
        }

        "redirect to the home page when the TriggeredMigration FS is disabled" in {

          disable(TriggeredMigration)
          setupMockSuccess(mtdRole)
          mockItsaStatusRetrievalAction()
          stubIncomeSourceDetails()

          val result = action(fakeRequest)
          status(result) shouldBe 303
          redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view")
        }
      }

      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }

    s"submit(isAgent = $isAgent)" when {
      val action = controller.submit(isAgent)

      s"the user is authenticated as a $mtdRole" should {

        "redirect to the complete page when form is valid and 'Yes' is selected" in {

          enable(TriggeredMigration)

          setupMockSuccess(mtdRole)
          mockItsaStatusRetrievalAction()
          mockTriggeredMigrationRetrievalAction()
          stubIncomeSourceDetails()
          mockUpdateCustomerFacts()

          val result = action(
            fakePostRequestBasedOnMTDUserType(mtdRole)
              .withFormUrlEncodedBody("check-active-businesses-confirm-form" -> "Yes")
          )

          status(result) shouldBe 303
          redirectLocation(result).value should include(
            routes.CheckCompleteController.show(isAgent).url
          )
        }

        "redirect back to the Check HMRC Records page when form is valid and 'No' is selected" in {

          enable(TriggeredMigration)
          setupMockSuccess(mtdRole)
          mockItsaStatusRetrievalAction()
          mockTriggeredMigrationRetrievalAction()
          stubIncomeSourceDetails()

          val result = action(
            fakePostRequestBasedOnMTDUserType(mtdRole)
              .withFormUrlEncodedBody("check-active-businesses-confirm-form" -> "No")
          )

          status(result) shouldBe 303
          redirectLocation(result).value should include(
            routes.CheckHmrcRecordsController.show(isAgent).url
          )
        }

        "return BadRequest when no option is selected" in {

          enable(TriggeredMigration)
          setupMockSuccess(mtdRole)
          mockItsaStatusRetrievalAction()
          mockTriggeredMigrationRetrievalAction()
          stubIncomeSourceDetails()

          val result = action(
            fakePostRequestBasedOnMTDUserType(mtdRole)
              .withFormUrlEncodedBody()
          )

          status(result) shouldBe 400
          contentAsString(result) should include("Select yes if you’ve checked that HMRC records only list your active businesses")
        }

        "redirect to the home page when the TriggeredMigration FS is disabled" in {

          disable(TriggeredMigration)
          setupMockSuccess(mtdRole)
          mockItsaStatusRetrievalAction()
          stubIncomeSourceDetails()

          val result = action(
            fakePostRequestBasedOnMTDUserType(mtdRole)
              .withFormUrlEncodedBody("check-active-businesses-confirm-form" -> "Yes")
          )

          status(result) shouldBe 303
          redirectLocation(result).get should include("/report-quarterly/income-and-expenses/view")
        }
      }

      testMTDAuthFailuresForRole(action, mtdRole)(fakeRequest)
    }
  }
}

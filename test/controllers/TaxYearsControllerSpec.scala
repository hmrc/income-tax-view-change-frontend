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

import connectors.{BusinessDetailsConnector, ITSAStatusConnector}
import enums.{MTDIndividual, MTDSupportingAgent}
import implicits.ImplicitDateFormatter
import mocks.auth.MockAuthActions
import models.incomeSourceDetails.IncomeSourceDetailsError
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.DateServiceInterface
import testConstants.BaseTestConstants.{testErrorMessage, testErrorStatus}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants._

class TaxYearsControllerSpec extends MockAuthActions with ImplicitDateFormatter {

  override lazy val app: Application =
    applicationBuilderWithAuthBindings
      .overrides(
        api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
        api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
        api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
      ).build()

  lazy val testController = app.injector.instanceOf[TaxYearsController]

  mtdAllRoles.foreach { case mtdUserRole =>

    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.showAgentTaxYears() else testController.showTaxYears()
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)

    s"show${if (isAgent) "Agent"}TaxYears" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the Tax years page" when {
            "income source details contains a business firstAccountingPeriodEndDate" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction(businessesAndPropertyIncome)
              setupMockGetIncomeSourceDetails(businessesAndPropertyIncome)

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
            }
          }

          "render the error page" when {
            "there is no firstAccountingPeriodEndDate for business or property in incomeSources" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction(businessIncome2018and2019)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "income source retrieval returns an error" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction(IncomeSourceDetailsError(testErrorStatus, testErrorMessage))
              mockErrorIncomeSource()

              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
          }
        }
      }
      testMTDAuthFailuresForRole(action, mtdUserRole, false)(fakeRequest)
    }
  }
}

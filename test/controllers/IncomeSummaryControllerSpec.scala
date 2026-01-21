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
import mocks.auth.MockAuthActions
import mocks.services.MockCalculationService
import mocks.views.agent.MockIncomeSummary
import play.api
import play.api.Application
import play.api.http.Status
import play.api.test.Helpers._
import services.{CalculationService, DateServiceInterface}
import testConstants.BaseTestConstants.{testMtditid, testTaxYear}
import testConstants.incomeSources.IncomeSourceDetailsTestConstants.businessIncome2018and2019

class IncomeSummaryControllerSpec extends MockAuthActions
  with MockCalculationService
  with MockIncomeSummary {

  val testYear: Int = 2020

  override lazy val app: Application = applicationBuilderWithAuthBindings
    .overrides(
      api.inject.bind[CalculationService].toInstance(mockCalculationService),
      api.inject.bind[ITSAStatusConnector].toInstance(mockItsaStatusConnector),
      api.inject.bind[BusinessDetailsConnector].toInstance(mockBusinessDetailsConnector),
      api.inject.bind[DateServiceInterface].toInstance(mockDateServiceInterface)
    ).build()

  lazy val testController = app.injector.instanceOf[IncomeSummaryController]

  override def beforeEach(): Unit = {
    super.beforeEach()
    disableAllSwitches()
  }

  mtdAllRoles.foreach { mtdUserRole =>
    val isAgent = mtdUserRole != MTDIndividual
    val action = if (isAgent) testController.showIncomeSummaryAgent(testTaxYear) else testController.showIncomeSummary(testTaxYear)
    val fakeRequest = fakeGetRequestBasedOnMTDUserType(mtdUserRole)
    s"showIncomeSummary${if (isAgent) "Agent"}" when {
      s"the $mtdUserRole is authenticated" should {
        if (mtdUserRole == MTDSupportingAgent) {
          testSupportingAgentDeniedAccess(action)(fakeRequest)
        } else {
          "render the income summary page" when {
            "the given tax year is present in ETMP" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction(businessIncome2018and2019)
              mockCalculationSuccessWithFlag(testMtditid)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)

              val result = action(fakeRequest)
              status(result) shouldBe Status.OK
              contentType(result) shouldBe Some("text/html")
              charset(result) shouldBe Some("utf-8")
              lazy val document = result.toHtmlDocument
              val title = messages("income_breakdown.heading")
              document.title() shouldBe messages({
                if (isAgent) "htmlTitle.agent" else "htmlTitle"
              }, title)
            }
          }

          "render the error page" when {
            "given a tax year which can not be found in ETMP" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction(businessIncome2018and2019)
              mockCalculationSuccessWithFlagNotFound(testMtditid)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)
              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }
            "there is a downstream error which return NOT_FOUND" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction(businessIncome2018and2019)
              mockCalculationSuccessWithFlagNotFound(testMtditid)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)
              val result = action(fakeRequest)
              status(result) shouldBe Status.INTERNAL_SERVER_ERROR
            }

            "there is a downstream error which return INTERNAL_SERVER_ERROR" in {
              setupMockSuccess(mtdUserRole)
              mockItsaStatusRetrievalAction(businessIncome2018and2019)
              mockCalculationSuccessWithFlagError(testMtditid)
              setupMockGetIncomeSourceDetails(businessIncome2018and2019)
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

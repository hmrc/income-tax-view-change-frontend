/*
 * Copyright 2024 HM Revenue & Customs
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

package mocks.auth

import audit.AuditingService
import audit.mocks.MockAuditingService
import auth.FrontendAuthorisedFunctions
import auth.authV2.AuthActions
import authV2.AuthActionsTestData._
import config.featureswitch.FeatureSwitching
import mocks.MockItvcErrorHandler
import mocks.services.{MockIncomeSourceDetailsService, MockSessionDataService}
import org.mockito.Mockito.mock
import play.api
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.test.Helpers.stubMessagesControllerComponents
import services.{IncomeSourceDetailsService, SessionDataService}
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.{AffinityGroup, AuthorisationException, InvalidBearerToken}

trait MockAuthActions extends
  TestSupport with
  MockIncomeSourceDetailsService with
  MockAgentAuthorisedFunctions with
  MockAuditingService with
  MockItvcErrorHandler with
  MockSessionDataService with
  FeatureSwitching {

  implicit class Ops[A](a: A) {
    def ~[B](b: B): A ~ B = new ~(a, b)
  }

  lazy val mockAuthService: FrontendAuthorisedFunctions = mock(classOf[FrontendAuthorisedFunctions])

  def applicationBuilderWithAuthBindings(): GuiceApplicationBuilder = {
    new GuiceApplicationBuilder()
      .overrides(
        api.inject.bind[FrontendAuthorisedFunctions].toInstance(mockAuthService),
        api.inject.bind[MessagesControllerComponents].toInstance(stubMessagesControllerComponents()),
        api.inject.bind[IncomeSourceDetailsService].toInstance(mockIncomeSourceDetailsService),
        api.inject.bind[AuditingService].toInstance(mockAuditingService),
        api.inject.bind[SessionDataService].toInstance(mockSessionDataService)
      )
  }

  val mockAuthActions: AuthActions = app.injector.instanceOf[AuthActions]

  def setupMockAgentWithClientAuth[X, Y](isSupportingAgent: Boolean): Unit = {
    disableAllSwitches()
    setupMockGetSessionDataSuccess()
    val allEnrolments = getAllEnrolmentsAgent(true, true, hasDelegatedEnrolment = true)
    val retrievalValue = allEnrolments ~ Some(userName) ~ Some(credentials) ~ Some(AffinityGroup.Agent) ~ acceptedConfidenceLevel
    setupMockAgentWithClientAuthSuccess(retrievalValue, mtdId, isSupportingAgent)
    mockSingleBusinessIncomeSource()
  }

  def setupMockAgentWithClientAuthorisationException(exception: AuthorisationException = new InvalidBearerToken, isSupportingAgent: Boolean): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockAgentWithClientAuthException(exception, mtdId, isSupportingAgent)
  }


}

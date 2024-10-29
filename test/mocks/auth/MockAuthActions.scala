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

import audit.mocks.MockAuditingService
import auth.FrontendAuthorisedFunctions
import auth.authV2.AuthActions
import auth.authV2.actions._
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import mocks.MockItvcErrorHandler
import mocks.services.MockIncomeSourceDetailsService
import org.mockito.Mockito.mock
import play.api.test.Helpers.stubMessagesControllerComponents
import testUtils.TestSupport

trait MockAuthActions extends
  TestSupport with
  MockIncomeSourceDetailsService with
  MockAgentAuthorisedFunctions  with
  MockAuditingService with
  MockItvcErrorHandler {

  lazy val mockAuthService: FrontendAuthorisedFunctions = mock(classOf[FrontendAuthorisedFunctions])


  private val authoriseAndRetrieve = new AuthoriseAndRetrieve(
    authorisedFunctions = mockAuthService,
    appConfig = appConfig,
    config = conf,
    env = environment,
    mcc = stubMessagesControllerComponents(),
    auditingService = mockAuditingService
  )

  private val authoriseAndRetrieveIndividual = new AuthoriseAndRetrieveIndividual(
    authorisedFunctions = mockAuthService,
    appConfig = appConfig,
    config = conf,
    env = environment,
    mcc = stubMessagesControllerComponents(),
    auditingService = mockAuditingService
  )

  private val authoriseAndRetrieveAgent = new AuthoriseAndRetrieveAgent(
    authorisedFunctions = mockAuthService,
    appConfig = appConfig,
    config = conf,
    env = environment,
    mcc = stubMessagesControllerComponents()
  )

  private val authoriseAndRetrieveMtdAgent = new AuthoriseAndRetrieveMtdAgent(
    authorisedFunctions = mockAuthService,
    appConfig = appConfig,
    config = conf,
    env = environment,
    mcc = stubMessagesControllerComponents()
  )

  private val incomeSourceRetrievalAction = new IncomeSourceRetrievalAction(
    mockIncomeSourceDetailsService
  )(ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    stubMessagesControllerComponents())

  val mockAuthActions = new AuthActions(
    app.injector.instanceOf[SessionTimeoutAction],
    authoriseAndRetrieve,
    authoriseAndRetrieveIndividual,
    authoriseAndRetrieveAgent,
    authoriseAndRetrieveMtdAgent,
    app.injector.instanceOf[AgentHasClientDetails],
    app.injector.instanceOf[AgentHasConfirmedClientAction],
    app.injector.instanceOf[AgentIsPrimaryAction],
    app.injector.instanceOf[AsMtdUser],
    app.injector.instanceOf[NavBarRetrievalAction],
    incomeSourceRetrievalAction,
    app.injector.instanceOf[FeatureSwitchRetrievalAction]
  )(appConfig, ec)

}

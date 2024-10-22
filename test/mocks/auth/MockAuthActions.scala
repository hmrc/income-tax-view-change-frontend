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
import controllers.predicates.IncomeSourceDetailsPredicate
import mocks.MockItvcErrorHandler
import mocks.services.MockIncomeSourceDetailsService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.test.Helpers.stubMessagesControllerComponents
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthorisationException, InvalidBearerToken}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

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

  private val incomeSourceDetailsPredicate = new IncomeSourceDetailsPredicate(
    mockIncomeSourceDetailsService
  )(ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    stubMessagesControllerComponents())

  val mockAuthActions = new AuthActions(
    app.injector.instanceOf[SessionTimeoutPredicateV2],
    authoriseAndRetrieve,
    authoriseAndRetrieveAgent,
    authoriseAndRetrieveMtdAgent,
    app.injector.instanceOf[AgentHasClientDetails],
    app.injector.instanceOf[AsMtdUser],
    app.injector.instanceOf[NavBarPredicateV2],
    incomeSourceDetailsPredicate,
    app.injector.instanceOf[FeatureSwitchPredicateV2]
  )(appConfig, ec)

}

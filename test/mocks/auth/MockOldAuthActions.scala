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
import auth.authV2.AuthActions
import auth.authV2.actions._
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import mocks.services.{MockClientDetailsService, MockIncomeSourceDetailsService, MockSessionDataService}
import models.sessionData.SessionDataGetResponse
import models.sessionData.SessionDataGetResponse.{SessionDataGetSuccess, SessionDataNotFound}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import play.api.test.Helpers.stubMessagesControllerComponents
import services.SessionDataService
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthorisationException, InvalidBearerToken}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockOldAuthActions extends TestSupport with MockIncomeSourceDetailsService
  with MockFrontendAuthorisedFunctions with MockAuditingService with MockSessionDataService with MockClientDetailsService{

  private val authoriseAndRetrieve = new AuthoriseAndRetrieve(
    authorisedFunctions = mockAuthService,
    appConfig = appConfig,
    config = conf,
    env = environment,
    mcc = stubMessagesControllerComponents()
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

  private val incomeSourceRetrievalAction = new IncomeSourceRetrievalAction(
    mockIncomeSourceDetailsService
  )(ec,
    app.injector.instanceOf[ItvcErrorHandler],
    app.injector.instanceOf[AgentItvcErrorHandler],
    stubMessagesControllerComponents())

  private val agentHasClientDetails = new AgentHasClientDetails()(
    executionContext = ec,
    sessionDataService = mockSessionDataService,
    appConfig = appConfig
  )

  private val retrieveClientData = new RetrieveClientData(
    sessionDataService = mockSessionDataService,
    appConfig = appConfig,
    errorHandler = app.injector.instanceOf[AgentItvcErrorHandler],
    clientDetailsService = mockClientDetailsService,
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

  val mockAuthActions = new AuthActions(
    app.injector.instanceOf[SessionTimeoutAction],
    authoriseAndRetrieve,
    authoriseAndRetrieveIndividual,
    authoriseAndRetrieveAgent,
    authoriseAndRetrieveMtdAgent,
    agentHasClientDetails,
    app.injector.instanceOf[AgentHasConfirmedClientAction],
    app.injector.instanceOf[AgentIsPrimaryAction],
    app.injector.instanceOf[NavBarRetrievalAction],
    incomeSourceRetrievalAction,
    retrieveClientData,
    app.injector.instanceOf[FeatureSwitchRetrievalAction]
  )

  override def setupMockAuthRetrievalSuccess[X, Y](retrievalValue: X ~ Y): Unit = {
    setupMockGetSessionDataSuccess()
    setupMockGetClientDetailsSuccess()
    when(mockAuthService.authorised(any()))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = body.apply(retrievalValue.asInstanceOf[A])
          }
        })
  }

  override def setupMockAgentAuthorisationException(exception: AuthorisationException = new InvalidBearerToken, withClientPredicate: Boolean = true): Unit = {
    setupMockGetSessionDataNotFound()
    when(mockAuthService.authorised(any()))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def apply[A](body: => Future[A])(implicit hc: HeaderCarrier, executionContext: ExecutionContext) = Future.failed(exception)

          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, executionContext: ExecutionContext): Future[B] = Future.failed(exception)
          }
        })
  }

}

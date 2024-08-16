package mocks.auth

import audit.mocks.MockAuditingService
import auth.authV2.AuthActions
import auth.authV2.actions._
import config.ItvcErrorHandler
import controllers.predicates.{IncomeSourceDetailsPredicate, NavBarPredicate}
import mocks.services.MockIncomeSourceDetailsService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.test.Helpers.stubMessagesControllerComponents
import testUtils.TestSupport
import uk.gov.hmrc.auth.core.authorise.EmptyPredicate
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.{AuthorisationException, InvalidBearerToken}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockAuthActions extends TestSupport with MockIncomeSourceDetailsService with MockFrontendAuthorisedFunctions  with MockAuditingService{

  private val authoriseAndRetrieve = new AuthoriseAndRetrieve(
    authorisedFunctions = mockAuthService,
    appConfig = appConfig,
    config = conf,
    env = environment,
    mcc = stubMessagesControllerComponents(),
    auditingService = mockAuditingService
  )

  private val incomeSourceDetailsPredicate = new IncomeSourceDetailsPredicate(
    mockIncomeSourceDetailsService,
    app.injector.instanceOf[ItvcErrorHandler]
  )(ec, stubMessagesControllerComponents())

  val mockAuthActions = new AuthActions(
    app.injector.instanceOf[SessionTimeoutPredicateV2],
    authoriseAndRetrieve,
    app.injector.instanceOf[AgentHasClientDetails],
    app.injector.instanceOf[AsMtdUser],
    app.injector.instanceOf[NavBarPredicate],
    incomeSourceDetailsPredicate,
    app.injector.instanceOf[FeatureSwitchPredicateV2]
  )(appConfig, ec)

  override def setupMockAuthRetrievalSuccess[X, Y](retrievalValue: X ~ Y): Unit = {
    when(mockAuthService.authorised(any()))
      .thenReturn(
        new mockAuthService.AuthorisedFunction(EmptyPredicate) {
          override def retrieve[A](retrieval: Retrieval[A]) = new mockAuthService.AuthorisedFunctionWithResult[A](EmptyPredicate, retrieval) {
            override def apply[B](body: A => Future[B])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[B] = body.apply(retrievalValue.asInstanceOf[A])
          }
        })
  }

  override def setupMockAgentAuthorisationException(exception: AuthorisationException = new InvalidBearerToken, withClientPredicate: Boolean = true): Unit = {

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

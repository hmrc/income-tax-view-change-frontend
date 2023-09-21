package controllers.incomeSources.add

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import config.featureswitch.FeatureSwitching
import controllers.predicates.{NinoPredicate, SessionTimeoutPredicate}
import mocks.MockItvcErrorHandler
import mocks.auth.MockFrontendAuthorisedFunctions
import mocks.controllers.predicates.{MockAuthenticationPredicate, MockIncomeSourceDetailsPredicate, MockNavBarEnumFsPredicate}
import mocks.services.{MockClientDetailsService, MockNextUpdatesService}
import org.mockito.Mockito.mock
import play.api.mvc.MessagesControllerComponents
import services.DateService
import testUtils.TestSupport
import views.html.incomeSources.add.IncomeSourceAddedObligations

class IncomeSourceAddedControllerSpec extends TestSupport
  with MockFrontendAuthorisedFunctions
  with MockIncomeSourceDetailsPredicate
  with MockAuthenticationPredicate
  with MockItvcErrorHandler
  with MockNavBarEnumFsPredicate
  with MockClientDetailsService
  with MockNextUpdatesService
  with FeatureSwitching {

  val mockDateService: DateService = mock(classOf[DateService])

  object TestIncomeSourceAddedController extends IncomeSourceAddedController(
    MockAuthenticationPredicate,
    authorisedFunctions = mockAuthService,
    checkSessionTimeout = app.injector.instanceOf[SessionTimeoutPredicate],
    retrieveNino = app.injector.instanceOf[NinoPredicate],
    retrieveIncomeSources = MockIncomeSourceDetailsPredicate,
    retrieveBtaNavBar = MockNavBarPredicate,
    itvcErrorHandler = app.injector.instanceOf[ItvcErrorHandler],
    incomeSourceDetailsService = mockIncomeSourceDetailsService,
    obligationsView = app.injector.instanceOf[IncomeSourceAddedObligations],
    mockNextUpdatesService
  )(
    appConfig = app.injector.instanceOf[FrontendAppConfig],
    itvcErrorHandlerAgent = app.injector.instanceOf[AgentItvcErrorHandler],
    mcc = app.injector.instanceOf[MessagesControllerComponents],
    ec = ec,
    mockDateService
  )

}

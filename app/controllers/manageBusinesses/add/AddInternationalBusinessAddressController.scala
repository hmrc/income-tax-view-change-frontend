package controllers.manageBusinesses.add

import auth.authV2.AuthActions
import com.google.inject.Inject
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import models.core.Mode
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{AddressLookupService, SessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils

import scala.concurrent.ExecutionContext

class AddInternationalBusinessAddressController @Inject()(val authActions: AuthActions,
                                                          addressLookupService: AddressLookupService)
                                                         (implicit
                                                          val appConfig: FrontendAppConfig,
                                                          val ec: ExecutionContext,
                                                          val itvcErrorHandler: ItvcErrorHandler,
                                                          val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                          mcc: MessagesControllerComponents,
                                                          val sessionService: SessionService
                                                         )
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport with IncomeSourcesUtils {
  
  def show(isAgent: Boolean, mode: Mode, isTriggeredMigration: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
  }
  
  def handleRequest(isAgent, mode: Mode, isTriggeredMigration: Boolean)

}

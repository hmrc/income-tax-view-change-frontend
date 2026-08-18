package businessDetails.controllers.triggeredMigration

import businessDetails.services.SessionService
import businessDetails.services.triggeredMigration.TriggeredMigrationService
import businessDetails.utils.TriggeredMigrationUtils
import businessDetails.views.html.triggeredMigration.CheckHmrcRecordsView
import com.google.inject.{Inject, Singleton}
import common.auth.AuthActions
import common.config.FrontendAppConfig
import common.services.AuditingService
import play.api.i18n.I18nSupport
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CompleteStepsController @Inject()(view: CompleteStepsView,
                                        val auth: AuthActions,
                                        triggeredMigrationService: TriggeredMigrationService, 
                                        sessionService: SessionService,
                                        auditingService: AuditingService)
                                       (mcc: MessagesControllerComponents,
                                        implicit val appConfig: FrontendAppConfig,
                                        implicit val ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with TriggeredMigrationUtils {
  
  

  
  def show(isAgent: Boolean) = auth.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    withTriggeredMigrationFS {
      val sessionId = hc.sessionId.map(_.value) getOrElse {
        throw new Exception("Missing sessionId in HeaderCarrier")
      }

      sessionService.clearSession(sessionId)

      Future.successful(Ok(
        view(
          isAgent,
          appConfig.homePageUrl(isAgent),
          postAction = routes.CompleteStepsController.submit(isAgent)
        ))
      )
    }
  }
}

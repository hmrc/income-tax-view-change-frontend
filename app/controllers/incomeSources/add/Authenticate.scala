package controllers.incomeSources.add

import auth.MtdItUser
import config.FrontendAppConfig
import controllers.agent.predicates.{BaseAgentController, ClientConfirmedController}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, SessionTimeoutPredicate}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Authenticate @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                             val authenticate: AuthenticationPredicate,
                             val authorisedFunctions: AuthorisedFunctions,
                             val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                             val incomeSourceDetailsService: IncomeSourceDetailsService,
                             val retrieveBtaNavBar: NavBarPredicate)
                            (implicit val appConfig: FrontendAppConfig,
                             mcc: MessagesControllerComponents,
                             implicit val ec: ExecutionContext) extends ClientConfirmedController {

  def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent)
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              authenticatedCodeBlock(mtdItUser)
            }
      }
    else
      (checkSessionTimeout andThen authenticate
        andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }
}

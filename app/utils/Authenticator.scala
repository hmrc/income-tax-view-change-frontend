package utils

import auth.MtdItUser
import controllers.agent.predicates.{BaseAgentController, ClientConfirmedController}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, SessionTimeoutPredicate}
import play.api.mvc.{Action, AnyContent, Result}
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions

import scala.concurrent.{ExecutionContext, Future}

abstract class Authenticator extends BaseAgentController {
  val authenticate: AuthenticationPredicate
  val clientConfirmedController: ClientConfirmedController
  val checkSessionTimeout: SessionTimeoutPredicate
  val authorisedFunctions: AuthorisedFunctions
  val retrieveBtaNavBar: NavBarPredicate
  val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate
  val incomeSourceDetailsService: IncomeSourceDetailsService
  override implicit val ec: ExecutionContext

  def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent)
      Authenticated.async {
        implicit request =>
          implicit user =>
            clientConfirmedController.getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
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

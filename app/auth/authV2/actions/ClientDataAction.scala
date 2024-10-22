
package auth.authV2.actions

import auth.authV2.AuthExceptions.MissingAgentReferenceNumber
import auth.authV2.EnroledUser
import controllers.agent.routes
import controllers.agent.sessionUtils.SessionKeys
import play.api.mvc.{ActionRefiner, Result}
import play.api.mvc.Results.Redirect
import testOnly.models.SessionDataGetResponse.SessionGetResponse
import uk.gov.hmrc.auth.core.AffinityGroup.Agent

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientDataAction @Inject()(implicit val executionContext: ExecutionContext) extends ActionRefiner[ClientDataModel, ClientDataModel] {


  lazy val noClientDetailsRoute: Result = Redirect(routes.EnterClientsUTRController.show)

  override protected def refine[A](request: ClientDataModel[A]): Future[Either[Result, ClientDataModel[A]]] = {

    val hasConfirmedClient: Boolean = request.session.get(SessionKeys.confirmedClient).nonEmpty

    val hasClientDetails: Boolean = {
      request.session.get(SessionKeys.clientMTDID).nonEmpty &&
        request.session.get(SessionKeys.clientFirstName).nonEmpty &&
        request.session.get(SessionKeys.clientLastName).nonEmpty &&
        request.session.get(SessionKeys.clientUTR).nonEmpty
    }

    // This check might not be necessary now we authorise on the Agent enrolment?
    val hasArn: Boolean = request.arn.nonEmpty

    if (!request.affinityGroup.contains(Agent)) {
      Future.successful(Right(request))
    } else if (hasArn && hasConfirmedClient && hasClientDetails) {
      Future.successful(Right(request))
    } else if (!hasArn) {
      throw new MissingAgentReferenceNumber
    } else {
      Future.successful(Left(noClientDetailsRoute))
    }
  }
}

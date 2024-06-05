package testOnly.controllers

import config.FrontendAppConfig
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import repositories.UIJourneySessionDataRepository
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.OptOutJourney

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
class MongoSessionController @Inject()(
                                        sessionService: SessionService,
                                        implicit val mcc: MessagesControllerComponents,
                                        val appConfig: FrontendAppConfig,
                                        repository: UIJourneySessionDataRepository
                                      )(implicit val executionContext: ExecutionContext) extends FrontendController(mcc) with I18nSupport {

  val show: Action[AnyContent] = Action.async { implicit request =>
    // We don't need to show sensitive session keys
    val filterOutKeys = Seq("sessionId", "authToken", "csrfToken", "origin")
    repository.get(sessionId = "", OptOutJourney.Name).flatMap(_.map(v => v.optOutSessionData.map(d => d.intent.getOrElse(""))))
    //Future.successful(Ok(sessionDataStr))
  }

  def upsert(keyOpt: Option[String], valueOpt: Option[String]): Action[AnyContent] = Action.async {
    implicit request =>
      val res = for {
        key <- keyOpt
        value <- valueOpt
      } yield (key, value)
      res match {
        case Some((k, v)) =>
          sessionService.setMongoKey(k, v, OptOutJourney.Name.).map {
            case Right(_) =>
              Redirect("/report-quarterly/income-and-expenses/view/test-only/showSession")
                .withSession(request.session + (k -> v))
            case Left(_) => Ok("Unable to add data to session storage")
          }
        case None =>
          Future.successful(Ok("Unable to add data to session storage"))
      }
  }
}
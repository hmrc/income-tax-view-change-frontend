package auth.authV2.actions

import auth.MtdItUser
import auth.authV2.models.ItsaStatusRetrievalActionError
import config.{AgentItvcErrorHandler, ItvcErrorHandler}
import connectors.IncomeTaxCalculationConnector
import controllers.BaseController
import enums.TaxYearSummary.CalculationRecord.LATEST
import enums.TriggeredMigration.Channel
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import play.api.Logger
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import services.{DateServiceInterface, ITSAStatusService}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.http.HeaderCarrier
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TriggeredMigrationRetrievalAction @Inject()(
                                                   ITSAStatusService: ITSAStatusService,
                                                   incomeTaxConnector: IncomeTaxCalculationConnector,
                                                   dateService: DateServiceInterface
                                                 )
                                                 (implicit val executionContext: ExecutionContext,
                                                  individualErrorHandler: ItvcErrorHandler,
                                                  agentErrorHandler: AgentItvcErrorHandler,
                                                  mcc: MessagesControllerComponents)
  extends BaseController with ActionRefiner[MtdItUser, MtdItUser] {

  override protected def refine[A](request: MtdItUser[A]): Future[Either[Result, MtdItUser[A]]] = {
    implicit val req: MtdItUser[A] = request

    request.incomeSources.channel match {
      case Channel.CustomerLedValue | Channel.ConfirmedValue => Future(Right(req))
      case Channel.UnconfirmedValue =>
        isItsaStatusVoluntaryOrMandated() match {
          case false => Future(Right(req))
          case true => isCalculationCrystallised(req, req.incomeSources.startingTaxYear.toString).map {
            case Right(true) => Right(req)
            case Right(false) => Left(Redirect(controllers.triggeredMigration.routes.CheckHmrcRecordsController.show(req.isAgent())))
            case Left(errorResult) => Left(errorResult)
          }
        }
      case _ => Future(Right(req))
    }
  }

  private def isCalculationCrystallised(req: MtdItUser[_], taxYear: String)(implicit hc: HeaderCarrier) = {
    incomeTaxConnector.getCalculationResponse(
      req.mtditid,
      req.nino,
      taxYear,
      Some(LATEST)
    ).map {
      case calcError: LiabilityCalculationError if calcError.status == NO_CONTENT => Right(false)
      case _: LiabilityCalculationError => Left(internalServerErrorFor(req, "Error retrieving liability calculation during triggered migration retrieval", None))
      case calcResponse: LiabilityCalculationResponse => Right(calcResponse.metadata.isCalculationCrystallised)
    }
  }

  private def isItsaStatusVoluntaryOrMandated()(implicit hc: HeaderCarrier, user: MtdItUser[_]) = {
    ITSAStatusService.getITSAStatusDetail(
      taxYear = dateService.getCurrentTaxYear,
      futureYears = false,
      history = false
    ).map {
      case statusDetails if statusDetails.exists(_.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary))) => true
      case _ => false
    }
  }

  private def internalServerErrorFor(
                                      request: MtdItUser[_],
                                      context: String,
                                      optError: Option[ItsaStatusRetrievalActionError] = None
                                    ): Result = {

    val logPrefix = s"[TriggeredMigrationRetrievalAction][$context]"

    (request.authUserDetails.affinityGroup, optError) match {
      case (Some(Agent), Some(error)) =>
        Logger(getClass).error(s"$logPrefix Agent error: ${error.logMessage}")
        agentErrorHandler.showInternalServerError()(request)
      case (Some(Individual), Some(error)) =>
        Logger(getClass).error(s"$logPrefix Individual error: ${error.logMessage}")
        individualErrorHandler.showInternalServerError()(request)
      case (Some(Organisation), Some(error)) =>
        Logger(getClass).error(s"$logPrefix Organisation error: ${error.logMessage}")
        individualErrorHandler.showInternalServerError()(request)
      case _ =>
        Logger(getClass).error(s"$logPrefix Unknown user type or error")
        individualErrorHandler.showInternalServerError()(request)
    }
  }
}

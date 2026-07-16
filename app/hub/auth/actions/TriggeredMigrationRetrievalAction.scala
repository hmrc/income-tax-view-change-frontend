/*
 * Copyright 2026 HM Revenue & Customs
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

package hub.auth.actions

import common.auth.MtdItUser
import common.config.featureswitch.FeatureSwitching
import common.config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import common.connectors.IncomeTaxCalculationConnector
import common.controllers.BaseController
import common.enums.TaxYearSummary.CalculationRecord.LATEST
import common.models.admin.{BusinessDetailsFrontend, TriggeredMigration}
import common.models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import common.services.{CustomerFactsUpdateService, DateServiceInterface, ITSAStatusService}
import play.api.Logger
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TriggeredMigrationRetrievalAction @Inject()(
                                                   frontendAppConfig: FrontendAppConfig,
                                                   ITSAStatusService: ITSAStatusService,
                                                   incomeTaxConnector: IncomeTaxCalculationConnector,
                                                   dateService: DateServiceInterface,
                                                   customerFactsUpdateService: CustomerFactsUpdateService
                                                 )
                                                 (implicit val executionContext: ExecutionContext,
                                                  individualErrorHandler: ItvcErrorHandler,
                                                  agentErrorHandler: AgentItvcErrorHandler,
                                                  mcc: MessagesControllerComponents)
  extends BaseController with FeatureSwitching {

  override val appConfig: FrontendAppConfig = frontendAppConfig

  def apply(): ActionRefiner[MtdItUser, MtdItUser] =
    new ActionRefiner[MtdItUser, MtdItUser] {
      implicit val executionContext: ExecutionContext = mcc.executionContext

      override protected def refine[A](request: MtdItUser[A]): Future[Either[Result, MtdItUser[A]]] = {
        implicit val req: MtdItUser[A] = request

        lazy val authAction: Future[Either[Result, MtdItUser[A]]] = {
          (request.incomeSources.isConfirmedUser) match {
            case true => Future(Right(req))
            case false =>
              isItsaStatusVoluntaryOrMandated().flatMap {
                case Right(false) => confirmIneligibleUser(req)
                case Left(errorResult) => Future(Left(errorResult))
                case Right(true) =>
                  val taxYear = req.incomeSources.yearOfMigration.orElse(req.incomeSources.startingTaxYear).map(_.toString)
                  isCalculationCrystallised(req, taxYear)
                    .flatMap {
                      case Right(true) => confirmIneligibleUser(req)
                      case Right(false) => Future.successful(
                            Left(Redirect(appConfig.triggeredMigrationCheckHMRCRecordsUrl(req.isAgent, isEnabled(BusinessDetailsFrontend))))
                          )
                      case Left(errorResult) =>
                        Future.successful(Left(errorResult))
                    }
              }
          }
        }

        if (isEnabled(TriggeredMigration)) {
          authAction
        } else {
          Future(Right(req))
        }
      }
    }

  private def isCalculationCrystallised(req: MtdItUser[_], taxYearOpt: Option[String])(implicit hc: HeaderCarrier) = {

    def request(taxYear: String) =
      incomeTaxConnector.getCalculationResponse(
        mtditid = req.mtditid,
        nino = req.nino,
        taxYear = taxYear,
        calculationRecord = Some(LATEST)
      ).map {
        case calcError: LiabilityCalculationError if calcError.status == NO_CONTENT =>
          Right(false)
        case calcResponse: LiabilityCalculationResponse =>
          Right(calcResponse.metadata.isCalculationCrystallised)
        case _: LiabilityCalculationError =>
          Left(showErrorPageBasedOnContext(req, "Error retrieving liability calculation during triggered migration retrieval"))
      }

    taxYearOpt match {
      case Some(taxYear) =>
        request(taxYear)
      case None =>
        Future(Left(showErrorPageBasedOnContext(request = req, context = "startingTaxYearNone")))
    }
  }

  private def isItsaStatusVoluntaryOrMandated()(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[Either[Result, Boolean]] = {
    def redirectBasedOnUser: Future[Either[Result, Boolean]] =
      Future(Left(Redirect(appConfig.homePageUrl(user.isAgent))))

    ITSAStatusService.getITSAStatusDetail(dateService.getCurrentTaxYear, futureYears = true, history = false).flatMap {
      itsaStatusList =>
        itsaStatusList.find(_.taxYear == dateService.getCurrentTaxYear.shortenTaxYearEnd) match {
          case Some(status) if status.itsaStatusDetails.exists(_.exists(_.isMandatedOrVoluntary)) => Future(Right(true))
          case Some(status) if status.itsaStatusDetails.exists(_.exists(!_.isMandatedOrVoluntary)) => Future(Right(false))
          case _ => itsaStatusList.find(_.taxYear == dateService.getCurrentTaxYear.nextYear.shortenTaxYearEnd) match {
            case Some(_) => redirectBasedOnUser
            case _ => Future(Left(showErrorPageBasedOnContext(user, "Error retrieving ITSA status during triggered migration retrieval - no current or next year status found")))
          }
        }
    }
  }

  private def showErrorPageBasedOnContext(request: MtdItUser[_], context: String): Result = {

    Logger(getClass).error(s"[TriggeredMigrationRetrievalAction][$context]")

    (request.authUserDetails.affinityGroup, context) match {
      case (Some(Agent), "startingTaxYearNone") => agentErrorHandler.showBadRequestError()(request)
      case (_, "startingTaxYearNone") => individualErrorHandler.showBadRequestError()(request)
      case (Some(Agent), _) => agentErrorHandler.showInternalServerError()(request)
      case (_, _) => individualErrorHandler.showInternalServerError()(request)
    }
  }
  
  private def confirmIneligibleUser[A](req: MtdItUser[A])(implicit hc: HeaderCarrier) = {
    customerFactsUpdateService.updateCustomerFacts(req.mtditid).map {
      _ => Right(req)
    }
  }
}

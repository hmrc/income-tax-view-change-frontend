/*
 * Copyright 2023 HM Revenue & Customs
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

package controllers.incomeSources.manage

import audit.AuditingService
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{JourneyType, Manage}
import exceptions.MissingSessionKey
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.ManageIncomeSourceData
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.manage.{ManageIncomeSources, ReportingMethodChangeError}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingMethodChangeErrorController @Inject()(val manageIncomeSources: ManageIncomeSources,
                                                     val checkSessionTimeout: SessionTimeoutPredicate,
                                                     val authenticate: AuthenticationPredicate,
                                                     val authorisedFunctions: AuthorisedFunctions,
                                                     val updateIncomeSourceService: UpdateIncomeSourceService,
                                                     val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                                     val reportingMethodChangeError: ReportingMethodChangeError,
                                                     val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                     val sessionService: SessionService,
                                                     val retrieveBtaNavBar: NavBarPredicate)
                                                    (implicit val ec: ExecutionContext,
                                                     implicit val itvcErrorHandler: ItvcErrorHandler,
                                                     implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                     implicit override val mcc: MessagesControllerComponents,
                                                     implicit val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  def show(isAgent: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>
    withIncomeSourcesFS {
      if (incomeSourceType == SelfEmployment) {
        sessionService.getMongoKey(ManageIncomeSourceData.incomeSourceIdField, JourneyType(Manage, incomeSourceType)).flatMap {
          case Right(Some(incomeSourceId)) => handleShowRequest(Some(mkIncomeSourceId(incomeSourceId)), incomeSourceType, isAgent)
          case _ => Future.failed(MissingSessionKey(ManageIncomeSourceData.incomeSourceIdField))
        }
      }
      else handleShowRequest(None, incomeSourceType, isAgent)
    }.recover {
      case exception =>
        Logger("application").error(s"[ReportingMethodChangeErrorController][show] ${exception.getMessage}")
        showInternalServerError(isAgent)
    }
  }

  private def handleShowRequest(soleTraderBusinessId: Option[IncomeSourceId],
                                incomeSourceType: IncomeSourceType,
                                isAgent: Boolean
                               )(implicit user: MtdItUser[_]): Future[Result] = {
    Future.successful(
      user.incomeSources.getIncomeSourceId(incomeSourceType, soleTraderBusinessId.map(m => m.value)) match {
        case Some(id) =>
          Ok(
            reportingMethodChangeError(
              isAgent = isAgent,
              manageIncomeSourcesUrl = getManageIncomeSourcesUrl(isAgent),
              manageIncomeSourceDetailsUrl = getManageIncomeSourceDetailsUrl(id, isAgent, incomeSourceType),
              messagesPrefix = incomeSourceType.reportingMethodChangeErrorPrefix
            )
          )
        case None =>
          Logger("error").info(s"[ReportingMethodChangeErrorController][handleShowRequest]: " +
            s"could not find incomeSourceId for $incomeSourceType")
          showInternalServerError(isAgent)
      }
    )
  }

  private def getManageIncomeSourcesUrl(isAgent: Boolean): String = routes.ManageIncomeSourceController.show(isAgent).url

  private def getManageIncomeSourceDetailsUrl(incomeSourceId: IncomeSourceId, isAgent: Boolean, incomeSourceType: IncomeSourceType): String = {
    ((isAgent, incomeSourceType) match {
      case (false, SelfEmployment) => routes.ManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceId.value)
      case (_, SelfEmployment) => routes.ManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(incomeSourceId.value)
      case (false, UkProperty) => routes.ManageIncomeSourceDetailsController.showUkProperty()
      case (_, UkProperty) => routes.ManageIncomeSourceDetailsController.showUkPropertyAgent()
      case (false, _) => routes.ManageIncomeSourceDetailsController.showForeignProperty()
      case (_, _) => routes.ManageIncomeSourceDetailsController.showForeignPropertyAgent()
    }).url
  }

  private def showInternalServerError(isAgent: Boolean)(implicit user: MtdItUser[_]): Result = {
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler)
      .showInternalServerError()
  }

  private def authenticatedAction(isAgent: Boolean
                                 )(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
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

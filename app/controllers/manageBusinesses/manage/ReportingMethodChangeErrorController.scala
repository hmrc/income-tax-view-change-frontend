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

package controllers.manageBusinesses.manage

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import enums.JourneyType.{JourneyType, Manage}
import exceptions.MissingSessionKey
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.ManageIncomeSourceData
import play.api.Logger
import play.api.mvc._
import services.{SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{AuthenticatorPredicate, IncomeSourcesUtils}
import views.html.manageBusinesses.manage.{ManageIncomeSources, ReportingMethodChangeError}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingMethodChangeErrorController @Inject()(val manageIncomeSources: ManageIncomeSources,
                                                     val authorisedFunctions: AuthorisedFunctions,
                                                     val updateIncomeSourceService: UpdateIncomeSourceService,
                                                     val reportingMethodChangeError: ReportingMethodChangeError,
                                                     val sessionService: SessionService,
                                                     val auth: AuthenticatorPredicate)
                                                    (implicit val ec: ExecutionContext,
                                                     implicit val itvcErrorHandler: ItvcErrorHandler,
                                                     implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                     implicit override val mcc: MessagesControllerComponents,
                                                     implicit val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  def show(isAgent: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = auth.authenticatedAction(isAgent) { implicit user =>
    withIncomeSourcesFS {
      if (incomeSourceType == SelfEmployment) {
        sessionService.getMongoKey(ManageIncomeSourceData.incomeSourceIdField, JourneyType(Manage, incomeSourceType)).flatMap {
          case Right(Some(incomeSourceId)) => handleShowRequest(Some(mkIncomeSourceId(incomeSourceId)), incomeSourceType, isAgent)
          case _ => Future.failed(MissingSessionKey(ManageIncomeSourceData.incomeSourceIdField))
        }
      }
      else handleShowRequest(None, incomeSourceType, isAgent)
    }.recover {
      case ex =>
        Logger("application").error(s"[ReportingMethodChangeErrorController][show] - ${ex.getMessage} - ${ex.getCause}")
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
          Logger("error").info("[ReportingMethodChangeErrorController][handleShowRequest]: " +
            s"could not find incomeSourceId for $incomeSourceType")
          showInternalServerError(isAgent)
      }
    )
  }

  private def getManageIncomeSourcesUrl(isAgent: Boolean): String = routes.ManageIncomeSourceController.show(isAgent).url

  private def getManageIncomeSourceDetailsUrl(incomeSourceId: IncomeSourceId, isAgent: Boolean, incomeSourceType: IncomeSourceType): String = {
    (incomeSourceType match {
      case SelfEmployment => routes.ManageIncomeSourceDetailsController.show(isAgent, incomeSourceType, Some(incomeSourceId.value))
      case _ => routes.ManageIncomeSourceDetailsController.show(isAgent, incomeSourceType, None)
    }).url
  }

  private def showInternalServerError(isAgent: Boolean)(implicit user: MtdItUser[_]): Result = {
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler)
      .showInternalServerError()
  }
}

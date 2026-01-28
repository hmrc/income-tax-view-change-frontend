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
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney.*
import enums.IncomeSourceJourney.IncomeSourceType.SelfEmployment
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import exceptions.MissingSessionKey
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import models.incomeSourceDetails.ManageIncomeSourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{SessionService, UpdateIncomeSourceService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.IncomeSourcesUtils
import views.html.manageBusinesses.manage.ReportingMethodChangeErrorView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingMethodChangeErrorController @Inject()(val authActions: AuthActions,
                                                     val updateIncomeSourceService: UpdateIncomeSourceService,
                                                     val reportingMethodChangeError: ReportingMethodChangeErrorView,
                                                     val sessionService: SessionService,
                                                     implicit val itvcErrorHandler: ItvcErrorHandler,
                                                     implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                    (implicit val ec: ExecutionContext,
                                                     val mcc: MessagesControllerComponents,
                                                     val appConfig: FrontendAppConfig) extends FrontendController(mcc)
  with I18nSupport with IncomeSourcesUtils {

  def show(isAgent: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user => {
      if (incomeSourceType == SelfEmployment) {
        sessionService.getMongoKey(ManageIncomeSourceData.incomeSourceIdField, IncomeSourceJourneyType(Manage, incomeSourceType)).flatMap {
          case Right(Some(incomeSourceId)) => handleShowRequest(Some(mkIncomeSourceId(incomeSourceId)), incomeSourceType, isAgent)
          case _ => Future.failed(MissingSessionKey(ManageIncomeSourceData.incomeSourceIdField))
        }
      }
      else handleShowRequest(None, incomeSourceType, isAgent)
    }.recover {
      case ex =>
        Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
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
              manageIncomeSourcesUrl = getManageYourBusinessUrl(isAgent),
              manageIncomeSourceDetailsUrl = getManageIncomeSourceDetailsUrl(id, isAgent, incomeSourceType),
              messagesPrefix = incomeSourceType.reportingMethodChangeErrorPrefix
            )
          )
        case None =>
          Logger("error").info("" +
            s"could not find incomeSourceId for $incomeSourceType")
          showInternalServerError(isAgent)
      }
    )
  }

  private def getManageYourBusinessUrl(isAgent: Boolean): String =
    if(isAgent) controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent().url
    else controllers.manageBusinesses.routes.ManageYourBusinessesController.show().url

  private def getManageIncomeSourceDetailsUrl(incomeSourceId: IncomeSourceId, isAgent: Boolean, incomeSourceType: IncomeSourceType): String = {
    (incomeSourceType match {
      case SelfEmployment =>
        routes.ManageIncomeSourceDetailsController.show(isAgent, incomeSourceType, Some(incomeSourceId.value))
      case _ => routes.ManageIncomeSourceDetailsController.show(isAgent, incomeSourceType, None)
    }).url
  }

  private def showInternalServerError(isAgent: Boolean)(implicit user: MtdItUser[_]): Result = {
    (if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler)
      .showInternalServerError()
  }
}

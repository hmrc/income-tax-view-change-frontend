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

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, UpdateIncomeSourceService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.IncomeSourcesUtils
import views.html.incomeSources.manage.{ManageIncomeSources, ReportingMethodChangeError}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingMethodChangeErrorController @Inject()(val manageIncomeSources: ManageIncomeSources,
                                                     val checkSessionTimeout: SessionTimeoutPredicate,
                                                     val authenticate: AuthenticationPredicate,
                                                     val authorisedFunctions: AuthorisedFunctions,
                                                     val retrieveNino: NinoPredicate,
                                                     val updateIncomeSourceService: UpdateIncomeSourceService,
                                                     val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                     val reportingMethodChangeError: ReportingMethodChangeError,
                                                     val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                     val retrieveBtaNavBar: NavBarPredicate)
                                                    (implicit val ec: ExecutionContext,
                                                     implicit val itvcErrorHandler: ItvcErrorHandler,
                                                     implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                     implicit override val mcc: MessagesControllerComponents,
                                                     implicit val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {

  def show(id: Option[String],
           isAgent: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    handleShowRequest(id, incomeSourceType, isAgent)
  }

  private def handleShowRequest(soleTraderBusinessId: Option[String],
                                incomeSourceType: IncomeSourceType,
                                isAgent: Boolean
                               )(implicit user: MtdItUser[_]): Future[Result] = {
    withIncomeSourcesFS {
      Future.successful(
        user.incomeSources.getIncomeSourceId(incomeSourceType, soleTraderBusinessId) match {
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
  }

  private def getManageIncomeSourcesUrl(isAgent: Boolean): String = routes.ManageIncomeSourceController.show(isAgent).url

  private def getManageIncomeSourceDetailsUrl(incomeSourceId: String, isAgent: Boolean, incomeSourceType: IncomeSourceType): String = {
    ((isAgent, incomeSourceType) match {
      case (false, SelfEmployment) => routes.ManageIncomeSourceDetailsController.showSoleTraderBusiness(incomeSourceId)
      case (_,     SelfEmployment) => routes.ManageIncomeSourceDetailsController.showSoleTraderBusinessAgent(incomeSourceId)
      case (false, UkProperty)     => routes.ManageIncomeSourceDetailsController.showUkProperty()
      case (_,     UkProperty)     => routes.ManageIncomeSourceDetailsController.showUkPropertyAgent()
      case (false, _)              => routes.ManageIncomeSourceDetailsController.showForeignProperty()
      case (_, _)                  => routes.ManageIncomeSourceDetailsController.showForeignPropertyAgent()
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
      (checkSessionTimeout andThen authenticate andThen retrieveNino
        andThen retrieveIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }
}

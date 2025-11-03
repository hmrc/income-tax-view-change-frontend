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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.CannotGoBackPage
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyType.{IncomeSourceJourneyType, Manage}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.YouCannotGoBackError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CannotGoBackErrorController @Inject()(val authActions: AuthActions,
                                            val cannotGoBackError: YouCannotGoBackError,
                                            val sessionService: SessionService,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                           (implicit val appConfig: FrontendAppConfig,
                                            mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext) extends FrontendController(mcc)
  with I18nSupport with JourneyCheckerManageBusinesses {

  def show(isAgent: Boolean,
           incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      handleRequest(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType
      )
  }

  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] =
    withSessionData(IncomeSourceJourneyType(Manage, incomeSourceType), journeyState = CannotGoBackPage) { data =>
      data.manageIncomeSourceData match {
        case Some(manageData) if manageData.reportingMethod.isDefined && manageData.taxYear.isDefined =>
          val subheadingContent = getSubheadingContent(incomeSourceType, manageData.reportingMethod.get, manageData.taxYear.get)
          Future.successful {
            Ok(cannotGoBackError(isAgent, subheadingContent))
          }
        case _ =>
          val errorPrefix = if (isAgent) "[Agent]"
          else " "
          Logger("application").error(errorPrefix + s"Unable to retrieve manage data from Mongo for $incomeSourceType.")
          Future.successful {
            errorHandler(isAgent).showInternalServerError()
          }
      }
    }

  def getSubheadingContent(incomeSourceType: IncomeSourceType, reportingMethod: String, taxYear: Int)(implicit request: Request[_]): String = {
    val methodString = if (reportingMethod == "annual")
      messagesApi.preferred(request)("cannotGoBack.manage.annual")
    else
      messagesApi.preferred(request)("cannotGoBack.manage.quarterly")

    val prefix = messagesApi.preferred(request)(s"cannotGoBack.manage.${incomeSourceType.key}", (taxYear - 1).toString, taxYear.toString)

    s"$prefix ${messagesApi.preferred(request)("cannotGoBack.manage.reportingMethod", methodString)}"
  }

  lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) =>
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler

}


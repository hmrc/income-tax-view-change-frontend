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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyType.{JourneyType, Manage}
import models.incomeSourceDetails.{ManageIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.incomeSources.YouCannotGoBackError

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CannotGoBackErrorController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                            val authenticate: AuthenticationPredicate,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val retrieveBtaNavBar: NavBarPredicate,
                                            val cannotGoBackError: YouCannotGoBackError,
                                            val sessionService: SessionService)
                                           (implicit val appConfig: FrontendAppConfig,
                                            mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController with IncomeSourcesUtils {

  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] = authenticatedAction(isAgent) {
    implicit user =>
      handleRequest(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType
      )
  }

  private def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
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


  private def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    getManageData(incomeSourceType).flatMap {
      case Some(manageData) if manageData.reportingMethod.isDefined && manageData.taxYear.isDefined =>
        val subheadingContent = getSubheadingContent(incomeSourceType, manageData.reportingMethod.get, manageData.taxYear.get)
        Future.successful {
          Ok(cannotGoBackError(isAgent, subheadingContent))
        }
      case _ =>
        val errorPrefix = if (isAgent) "[Agent][CannotGoBackErrorController][handleRequest]: "
        else "[CannotGoBackErrorController][handleRequest]: "
        Logger("application").error(errorPrefix + s"Unable to retrieve manage data from Mongo for $incomeSourceType.")
        Future.successful {
          errorHandler(isAgent).showInternalServerError()
        }
    }
  }

  private def getManageData(incomeSourceType: IncomeSourceType)(implicit hc: HeaderCarrier): Future[Option[ManageIncomeSourceData]] = {
    val journeyType = JourneyType(Manage, incomeSourceType).toString
    sessionService.getMongo(journeyType).map {
      case Right(Some(data: UIJourneySessionData)) =>
        println(Console.MAGENTA + s"Successfully retrieved $data" + Console.WHITE)
        data.manageIncomeSourceData
      case Right(None) => println(Console.MAGENTA + s"Successfully retrieved None" + Console.WHITE)
      None
      case _ => None
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


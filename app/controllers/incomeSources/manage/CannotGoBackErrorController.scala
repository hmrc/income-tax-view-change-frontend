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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.IncomeSourceType
import models.core.IncomeSourceId
import models.core.IncomeSourceId.mkIncomeSourceId
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
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
                                            val cannotGoBackError: YouCannotGoBackError)
                                           (implicit val appConfig: FrontendAppConfig,
                                            mcc: MessagesControllerComponents,
                                            val ec: ExecutionContext,
                                            val itvcErrorHandler: ItvcErrorHandler,
                                            val itvcErrorHandlerAgent: AgentItvcErrorHandler) extends ClientConfirmedController with IncomeSourcesUtils {

  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType, reportingMethod: String, taxYear: String, id: Option[String]): Action[AnyContent] = authenticatedAction(isAgent) {
    implicit user =>
      val incomeSourceIdMaybe = id.map(mkIncomeSourceId)
      handleRequest(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType,
        reportingMethod = reportingMethod,
        taxYear = taxYear,
        id = incomeSourceIdMaybe
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


  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType, reportingMethod: String, taxYear: String, id: Option[IncomeSourceId])
                   (implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {
    val subheadingContent = getSubheadingContent(incomeSourceType, reportingMethod, taxYear)
    Future.successful(Ok(cannotGoBackError(isAgent, subheadingContent)))
  }

  def getSubheadingContent(incomeSourceType: IncomeSourceType, reportingMethod: String, taxYear: String)(implicit request: Request[_]): String = {
    val methodString = if (reportingMethod == "annual")
      messagesApi.preferred(request)("cannotGoBack.manage.annual")
    else
      messagesApi.preferred(request)("cannotGoBack.manage.quarterly")

    val prefix = messagesApi.preferred(request)(s"cannotGoBack.manage.${incomeSourceType.key}", taxYear.take(4), taxYear.takeRight(4))

    s"$prefix ${messagesApi.preferred(request)("cannotGoBack.manage.reportingMethod", methodString)}"
  }

}


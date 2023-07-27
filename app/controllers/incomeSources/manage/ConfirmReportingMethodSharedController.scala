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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import play.api.Logger
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.manage.{ConfirmSEReportingMethod, ManageIncomeSources}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmReportingMethodSharedController @Inject()(val manageIncomeSources: ManageIncomeSources,
                                                       val checkSessionTimeout: SessionTimeoutPredicate,
                                                       val authenticate: AuthenticationPredicate,
                                                       val authorisedFunctions: AuthorisedFunctions,
                                                       val retrieveNino: NinoPredicate,
                                                       val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                       val itvcErrorHandler: ItvcErrorHandler,
                                                       val customNotFoundErrorView: CustomNotFoundError,
                                                       implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                       val confirmReportingMethod: ConfirmSEReportingMethod,
                                                       val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                       val retrieveBtaNavBar: NavBarPredicate)
                                                      (implicit val ec: ExecutionContext,
                                                      implicit override val mcc: MessagesControllerComponents,
                                                      val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching {

  def show(id: String, taxYear: String, changeTo: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        id = id,
        isAgent = true,
        taxYear = taxYear,
        changeTo = changeTo,
        itvcErrorHandler = itvcErrorHandler
      )
  }

  def showAgent(id: String, taxYear: String, changeTo: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              id = id,
              isAgent = true,
              taxYear = taxYear,
              changeTo = changeTo,
              itvcErrorHandler = itvcErrorHandler
            )
        }
  }

  private def handleRequest(id: String,
                    isAgent: Boolean,
                    taxYear: String,
                    changeTo: String,
                    itvcErrorHandler: ShowInternalServerError)
                   (implicit user: MtdItUser[_]): Future[Result] = {
    Future(
      (isEnabled(IncomeSources), getTaxYearStartYearEndYear(taxYear), getReportingMethodKey(changeTo)) match {
        case (false, _, _) =>
          Ok(
            customNotFoundErrorView()
          )
        case (_, _, None) =>
          Logger("application")
            .error(s"[ConfirmReportingMethodSharedController][handleRequest]: Could not parse reporting method: $changeTo")
          itvcErrorHandler.showInternalServerError()
        case (_, None, _) =>
          Logger("application")
            .error(s"[ConfirmReportingMethodSharedController][handleRequest]: Could not parse taxYear: $taxYear")
          itvcErrorHandler.showInternalServerError()
        case (_, Some(taxYear), Some(reportingMethodKey)) =>
          Ok(
            confirmReportingMethod(
              isAgent = isAgent,
              taxYearEndYear = taxYear.endYear,
              taxYearStartYear = taxYear.startYear,
              reportingMethodKey = reportingMethodKey
            )
          )
        case _ =>
          Logger("application")
            .error(s"[ConfirmReportingMethodSharedController][handleRequest]: Could not find property or business with incomeSourceId: $id")
          itvcErrorHandler.showInternalServerError()
      }
    )
  }

  private def getTaxYearStartYearEndYear(year: String): Option[TaxYear] = {

    def isValidYear(year: String) = year.length == 4 && year.forall(_.isDigit)

    year.split('-') match {
      case Array(yearOne, yearTwo) if isValidYear(yearOne) && isValidYear(yearTwo) =>
        Some(TaxYear(yearOne, yearTwo))
      case _ => None
    }
  }

  private def getReportingMethodKey(reportingMethod: String): Option[String] = {
    reportingMethod.toLowerCase match {
      case "annual" => Some("annual")
      case "quarterly" => Some("quarterly")
      case _ => None
    }
  }
}

case class TaxYear(startYear: String, endYear: String)

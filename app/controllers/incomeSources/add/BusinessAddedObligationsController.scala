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

package controllers.incomeSources.add

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import controllers.routes
import forms.utils.SessionKeys
import models.incomeSourceDetails.BusinessDetailsModel
import models.incomeSourceDetails.viewmodels.{DatesModel, ObligationsViewModel}
import models.nextUpdates.{NextUpdateModel, NextUpdatesErrorModel, NextUpdatesResponseModel, ObligationsModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateServiceInterface, IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.incomeSources.add.BusinessAddedObligations

import java.time.LocalDate
import java.time.Month.APRIL
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.{Duration, MILLISECONDS}
import scala.concurrent.{Await, CanAwait, ExecutionContext, Future}

class BusinessAddedObligationsController @Inject()(authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   checkSessionTimeout: SessionTimeoutPredicate,
                                                   retrieveNino: NinoPredicate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   nextUpdatesService: NextUpdatesService,
                                                   val obligationsView: BusinessAddedObligations)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext,
                                                   dateService: DateServiceInterface)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching{

  lazy val backUrl = controllers.incomeSources.add.routes.AddBusinessReportingMethod.show().url
  lazy val agentBackUrl = controllers.incomeSources.add.routes.AddBusinessReportingMethod.showAgent().url

  def show(id: Option[String]): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(isAgent = false, id)
  }

  def showAgent(id: Option[String]): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              handleRequest(isAgent = true, id)
          }
    }

  def handleRequest(isAgent: Boolean, id: Option[String])(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      id match {
        case None => Logger("application").error(
          s"[BusinessAddedObligationsController][handleRequest] - Error: No id supplied by reporting method page")
          Future(itvcErrorHandler.showInternalServerError())
        case Some(value) => {
          if (!user.incomeSources.businesses.exists(x => x.incomeSourceId.contains(value))) {
            Logger("application").error(
              s"[BusinessAddedObligationsController][handleRequest] - No income source with supplied id")
            Future(itvcErrorHandler.showInternalServerError())
          }
          else {
            val addedBusiness: BusinessDetailsModel = user.incomeSources.businesses.find(x => x.incomeSourceId.contains(value)).get
            val businessName: String = addedBusiness.tradingName match {
              case Some(value) => value
              case None => Logger("application").error(
                s"[BusinessAddedObligationsController][handleRequest] - No business name for business with provided id")
                itvcErrorHandler.showInternalServerError()
                ""
            }
            val dates: Seq[DatesModel] = Await.result(nextUpdatesService.getNextUpdates() map {
              case NextUpdatesErrorModel(code, message) => Logger("application").error(
                s"[BusinessAddedObligationsController][handleRequest] - Error: $message, code $code")
                itvcErrorHandler.showInternalServerError()
                Seq.empty
              case NextUpdateModel(start, end, due, obligationType, dateReceived, periodKey) =>
                Seq(DatesModel(Some(start), Some(end), Some(due), obligationType, periodKey))
              case ObligationsModel(obligations) =>
                obligations.flatMap(obligation => obligation.obligations.map(x => DatesModel(Some(x.start), Some(x.end), Some(x.due), x.obligationType, x.periodKey)))
            }, Duration(100, MILLISECONDS)) //REMOVE
            val showPreviousTaxYears: Boolean = if (addedBusiness.tradingStartDate.isDefined){
              addedBusiness.tradingStartDate.get.isBefore(getStartOfCurrentTaxYear)
            }
            else{
              Logger("application").error(s"[BusinessAddedObligationsController][handleRequest] - No business start date for business with provided id")
              Future(itvcErrorHandler.showInternalServerError())
              false
            }
            Future {
              if (isAgent) Ok(obligationsView(businessName, ObligationsViewModel(dates.filter(x => x.periodKey.nonEmpty), dates.filter(x => x.obligationType == "EOPS"), dateService.getCurrentTaxYearEnd()),
                controllers.incomeSources.add.routes.BusinessAddedObligationsController.agentSubmit(), agentBackUrl, true, showPreviousTaxYears))
              else Ok(obligationsView(businessName, ObligationsViewModel(dates.filter(x => x.periodKey.nonEmpty), dates.filter(x => x.obligationType == "EOPS"), dateService.getCurrentTaxYearEnd()),
                controllers.incomeSources.add.routes.BusinessAddedObligationsController.submit(), backUrl, false, showPreviousTaxYears))
            }
          }
        }
      }
    }
  }

  def getStartOfCurrentTaxYear: LocalDate = {
    val currentDate = dateService.getCurrentDate()
    if (currentDate.isBefore(LocalDate.of(currentDate.getYear, APRIL, 6))) LocalDate.of(currentDate.getYear - 1, APRIL, 6)
    else LocalDate.of(currentDate.getYear, APRIL, 6)
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      Future.successful {
        Redirect(controllers.incomeSources.add.routes.AddIncomeSourceController.show().url)
      }
  }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            Future.successful {
              Redirect(controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url)
            }
        }
  }

}

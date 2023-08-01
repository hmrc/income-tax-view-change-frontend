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
import forms.incomeSources.manage.ConfirmReportingMethodForm
import models.incomeSourceDetails.{IncomeSourceDetailsModel, TaxYear}
import play.api.Logger
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.manage.{ConfirmReportingMethod, ManageIncomeSources}

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
                                                       val confirmReportingMethod: ConfirmReportingMethod,
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
        itvcErrorHandler = itvcErrorHandler,
        backUrl = getBackUrl(id, isAgent = false),
        postAction = getPostAction(id, isAgent = false, taxYear, changeTo)
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
              itvcErrorHandler = itvcErrorHandlerAgent,
              backUrl = getBackUrl(id, isAgent = true),
              postAction = getPostAction(id, isAgent = true, taxYear, changeTo)
            )
        }
  }

  def submit(id: String, taxYear: String, changeTo: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(
        id = id,
        isAgent = true,
        taxYear = taxYear,
        changeTo = changeTo,
        itvcErrorHandler = itvcErrorHandler,
        backUrl = getBackUrl(id, isAgent = false),
        postAction = getPostAction(id, isAgent = false, taxYear, changeTo)
      )
  }

  def submitAgent(id: String, taxYear: String, changeTo: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              id = id,
              isAgent = true,
              taxYear = taxYear,
              changeTo = changeTo,
              itvcErrorHandler = itvcErrorHandlerAgent,
              backUrl = getBackUrl(id, isAgent = true),
              postAction = getPostAction(id, isAgent = true, taxYear, changeTo)
            )
        }
  }

  private def handleRequest(id: String,
                    isAgent: Boolean,
                    taxYear: String,
                    changeTo: String,
                    backUrl: String,
                    postAction: Call,
                    itvcErrorHandler: ShowInternalServerError)
                   (implicit user: MtdItUser[_]): Future[Result] = {
    Future(
      (isEnabled(IncomeSources), TaxYear.getTaxYearStartYearEndYear(taxYear), getReportingMethodKey(changeTo), idIsValid(id)) match {
        case (false, _, _, _) =>
          Ok(customNotFoundErrorView())
        case (_, None, _, _) =>
          Logger("application")
            .error(s"[ConfirmReportingMethodSharedController][handleRequest]: Could not parse taxYear: $taxYear")
          itvcErrorHandler.showInternalServerError()
        case (_, _, None, _) =>
          Logger("application")
            .error(s"[ConfirmReportingMethodSharedController][handleRequest]: Could not parse reporting method: $changeTo")
          itvcErrorHandler.showInternalServerError()
        case (_, _, _, false) =>
          Logger("application")
            .error(s"[ConfirmReportingMethodSharedController][handleRequest]: Could not find property or business with incomeSourceId: $id")
          itvcErrorHandler.showInternalServerError()
        case (_, Some(taxYears), Some(reportingMethodKey), true) =>
          Ok(
            confirmReportingMethod(
              form = ConfirmReportingMethodForm.form,
              postAction = postAction,
              isAgent = isAgent,
              backUrl = backUrl,
              taxYearEndYear = taxYears.endYear,
              taxYearStartYear = taxYears.startYear,
              reportingMethodKey = reportingMethodKey
            )
          )
      }
    ) recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting confirmReportingMethod page: ${ex.getMessage}")
        itvcErrorHandler.showInternalServerError()
    }
  }

  private def handleSubmitRequest(id: String,
                                  isAgent: Boolean,
                                  taxYear: String,
                                  changeTo: String,
                                  backUrl: String,
                                  postAction: Call,
                                  itvcErrorHandler: ShowInternalServerError)
                                 (implicit user: MtdItUser[_]): Future[Result] = {

    (isEnabled(IncomeSources), TaxYear.getTaxYearStartYearEndYear(taxYear), getReportingMethodKey(changeTo)) match {
      case (false, _, _) =>
        Future(Ok(customNotFoundErrorView()))
      case (_, None, _) =>
        Logger("application")
          .error(s"[ConfirmReportingMethodSharedController][handleSubmitRequest]: Could not parse taxYear: $taxYear")
        Future(itvcErrorHandler.showInternalServerError())
      case (_, _, None) =>
        Logger("application")
          .error(s"[ConfirmReportingMethodSharedController][handleSubmitRequest]: Could not parse reporting method: $changeTo")
        Future(itvcErrorHandler.showInternalServerError())
      case (_, Some(taxYears), Some(reportingMethodKey)) =>
        ConfirmReportingMethodForm.form.bindFromRequest().fold(
          formWithErrors => {
            Future(
              BadRequest(
                confirmReportingMethod(
                  form = formWithErrors,
                  postAction = postAction,
                  isAgent = isAgent,
                  backUrl = backUrl,
                  taxYearStartYear = taxYears.startYear,
                  taxYearEndYear = taxYears.endYear,
                  reportingMethodKey = reportingMethodKey
                )
              )
            )},
          formData =>
            Future.successful(NotImplemented)
        )
    }
  }

  private def getReportingMethodKey(reportingMethod: String): Option[String] = {
    Set("annual", "quarterly")
      .find(_ == reportingMethod.toLowerCase)
  }

  private def getBackUrl(id: String, isAgent: Boolean): String = {
    if (isAgent) controllers.incomeSources.manage.routes.ManageSelfEmploymentController.showAgent(id).url
    else controllers.incomeSources.manage.routes.ManageSelfEmploymentController.show(id).url
  }

  private def idIsValid(id: String)(implicit user: MtdItUser[_]): Boolean = user.incomeSources.isOngoingBusinessOrPropertyIncome(id)

  private def getPostAction(id: String,
                            isAgent: Boolean,
                            taxYear: String,
                            changeTo: String): Call = {
    if (isAgent) controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController
      .submitAgent(id = id, taxYear = taxYear, changeTo = changeTo)
    else controllers.incomeSources.manage.routes.ConfirmReportingMethodSharedController
      .submit(id = id, taxYear = taxYear, changeTo = changeTo)
  }

  private def getRedirectCall(id: String,
                            isAgent: Boolean,
                            changeTo: String,
                            taxYear: String)
                           (implicit user: MtdItUser[_]): Either[Throwable, Call] = {

    lazy val isUkProperty = user.incomeSources.properties.exists(p => p.incomeSourceId.contains(id) && p.isUkProperty)
    lazy val isForeignProperty = user.incomeSources.properties.exists(p => p.incomeSourceId.contains(id) && p.isForeignProperty)
    lazy val isSoleTraderBusiness = user.incomeSources.businesses.exists(_.incomeSourceId.contains(id))

    lazy val redirectController = controllers.incomeSources.manage.routes.ManageObligationsController

    (isAgent, isUkProperty, isForeignProperty, isSoleTraderBusiness) match {
      case (false, true, false, false) =>
        Right(redirectController.showUKProperty(changeTo, taxYear))
      case (false, false, true, false) =>
        Right(redirectController.showForeignProperty(changeTo, taxYear))
      case (false, false, false, true) =>
        Right(redirectController.showSelfEmployment(id, changeTo, taxYear))
      case (true, true, false, false) =>
        Right(redirectController.showAgentUKProperty(changeTo, taxYear))
      case (true, false, true, false) =>
        Right(redirectController.showAgentForeignProperty(changeTo, taxYear))
      case (true, false, false, true) =>
        Right(redirectController.showAgentSelfEmployment(id, changeTo, taxYear))
      case _ =>
        Left(new Error(s"Income source type not found"))
    }
  }
}

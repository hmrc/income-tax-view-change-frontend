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
import models.incomeSourceDetails.{IncomeSourceDetailsModel, PropertyDetailsModel, TaxYear}
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, UpdateIncomeSourceService}
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
                                                       val updateIncomeSourceService: UpdateIncomeSourceService,
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

  def show(incomeSourceId: Option[String], taxYear: String, changeTo: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        id = incomeSourceId,
        isAgent = false,
        taxYear = taxYear,
        changeTo = changeTo,
        itvcErrorHandler = itvcErrorHandler
      )
  }

  def showAgent(incomeSourceId: Option[String], taxYear: String, changeTo: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              id = incomeSourceId,
              isAgent = true,
              taxYear = taxYear,
              changeTo = changeTo,
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  def submit(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(
        id = incomeSourceId,
        isAgent = false,
        taxYear = taxYear,
        changeTo = changeTo,
        itvcErrorHandler = itvcErrorHandler
      )
  }

  def submitAgent(incomeSourceId: String, taxYear: String, changeTo: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              id = incomeSourceId,
              isAgent = true,
              taxYear = taxYear,
              changeTo = changeTo,
              itvcErrorHandler = itvcErrorHandlerAgent
            )
        }
  }

  private def handleRequest(id: Option[String],
                            isAgent: Boolean,
                            taxYear: String,
                            changeTo: String,
                            itvcErrorHandler: ShowInternalServerError)
                           (implicit user: MtdItUser[_]): Future[Result] = {
    Future(
      (isEnabled(IncomeSources), TaxYear.getTaxYearStartYearEndYear(taxYear), getReportingMethod(changeTo), getIncomeSourceId(id)) match {
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
        case (_, _, _, Left(ex)) =>
          Logger("application")
            .error(s"[ConfirmReportingMethodSharedController][handleRequest]: $ex")
          itvcErrorHandler.showInternalServerError()
        case (_, Some(taxYears), Some(reportingMethod), Right(id)) =>
          Ok(
            confirmReportingMethod(
              form = ConfirmReportingMethodForm.form,
              incomeSourceId = id,
              isAgent = isAgent,
              taxYear = taxYear,
              taxYearEndYear = taxYears.endYear,
              taxYearStartYear = taxYears.startYear,
              reportingMethod = reportingMethod
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
                                  itvcErrorHandler: ShowInternalServerError)
                                 (implicit user: MtdItUser[_]): Future[Result] = {

    (isEnabled(IncomeSources), TaxYear.getTaxYearStartYearEndYear(taxYear), getReportingMethod(changeTo)) match {
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
      case (_, Some(taxYears), Some(reportingMethod)) =>
        ConfirmReportingMethodForm.form.bindFromRequest().fold(
          formWithErrors =>
            Future(
              BadRequest(
                confirmReportingMethod(
                  form = formWithErrors,
                  incomeSourceId = id,
                  taxYear = taxYear,
                  isAgent = isAgent,
                  taxYearStartYear = taxYears.startYear,
                  taxYearEndYear = taxYears.endYear,
                  reportingMethod = reportingMethod
                )
              )
            ),
          _ =>
            updateIncomeSourceService.updateTaxYearSpecific(
              nino = user.nino,
              incomeSourceId = id,
              taxYearSpecific = List(
                TaxYearSpecific(
                  taxYear = taxYears.endYear,
                  latencyIndicator = reportingMethod match {
                    case "annual" => true
                    case _ => false
                  }
                )
              )
            ) flatMap {
              case res: UpdateIncomeSourceResponseModel =>
                Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updated tax year specific reporting method : $res")
                getRedirectCall(
                  id = id,
                  isAgent = isAgent,
                  changeTo = changeTo,
                  taxYear = taxYear
                ) match {
                  case Right(call) => Future.successful(Redirect(call))
                  case Left(ex) =>
                    Logger("application").error(s"${if (isAgent) "[Agent]"}" + s" Failed to redirect to update success page, reason : $ex")
                    Future(itvcErrorHandler.showInternalServerError())
                }
              case err: UpdateIncomeSourceResponseError =>
                Logger("application").error(s"${if (isAgent) "[Agent]"}" + s" Failed to Update tax year specific reporting method : $err")
                Future(itvcErrorHandler.showInternalServerError())
            } recover {
              case ex: Exception =>
                Logger("application").error(s"${if (isAgent) "[Agent]"}" +
                  s"Error updating reporting method: ${ex.getMessage}")
                itvcErrorHandler.showInternalServerError()
            }
        )
    }
  }

  private def getReportingMethod(reportingMethod: String): Option[String] = {
    Set("annual", "quarterly")
      .find(_ == reportingMethod.toLowerCase)
  }

  private def getIncomeSourceId(incomeSourceId: Option[String])(implicit user: MtdItUser[_]): Either[Throwable, String] = {

    (maybeUkProperty, maybeForeignProperty, incomeSourceId) match {
      case (_, _, Some(id)) if isSoleTraderBusiness(id) => Right(id)
      case (_, Some(PropertyDetailsModel(Some(id), _, _, _, _, _, _)), None) => Right(id)
      case (Some(PropertyDetailsModel(Some(id), _, _, _, _, _, _)), _, None) => Right(id)
      case _ => Left(new Error(s"Could not find income source"))
    }
  }

  private def getRedirectCall(id: String,
                              isAgent: Boolean,
                              changeTo: String,
                              taxYear: String)
                             (implicit user: MtdItUser[_]): Either[Throwable, Call] = {

    lazy val redirectController = controllers.incomeSources.manage.routes.ManageObligationsController

    (isAgent,
      maybeUkProperty.exists(_.incomeSourceId.contains(id)),
      maybeForeignProperty.exists(_.incomeSourceId.contains(id)),
      isSoleTraderBusiness(id)
    ) match {
      case (false, false, false, true) =>
        Right(redirectController.showSelfEmployment(id, changeTo, taxYear))
      case (false, false, true, false) =>
        Right(redirectController.showForeignProperty(changeTo, taxYear))
      case (false, true, false, false) =>
        Right(redirectController.showUKProperty(changeTo, taxYear))
      case (true, false, false, true) =>
        Right(redirectController.showAgentSelfEmployment(id, changeTo, taxYear))
      case (true, false, true, false) =>
        Right(redirectController.showAgentForeignProperty(changeTo, taxYear))
      case (true, true, false, false) =>
        Right(redirectController.showAgentUKProperty(changeTo, taxYear))
      case _ =>
        Left(new Error(s"Could not find income source type for incomeSourceId: $id"))
    }
  }

  def maybeUkProperty(implicit user: MtdItUser[_]): Option[PropertyDetailsModel] = {
    user.incomeSources.properties.find(p => p.isUkProperty && !p.isCeased)
  }

  def maybeForeignProperty(implicit user: MtdItUser[_]): Option[PropertyDetailsModel] = {
    user.incomeSources.properties.find(p => p.isForeignProperty && !p.isCeased)
  }

  private def isSoleTraderBusiness(id: String)(implicit user: MtdItUser[_]): Boolean = {
    user.incomeSources.businesses.exists(b => b.incomeSourceId.contains(id) && !b.isCeased)
  }
}

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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.incomeSources.add.AddBusinessReportingMethodForm
import models.updateIncomeSource.{UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{BusinessReportingMethodService, IncomeSourceDetailsService}
import uk.gov.hmrc.http.{HeaderCarrier, InternalServerException}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.BusinessReportingMethod

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BusinessReportingMethodController @Inject()(val authenticate: AuthenticationPredicate,
                                                  val authorisedFunctions: FrontendAuthorisedFunctions,
                                                  val checkSessionTimeout: SessionTimeoutPredicate,
                                                  val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                  val retrieveBtaNavBar: NavBarPredicate,
                                                  val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                  val retrieveNino: NinoPredicate,
                                                  val view: BusinessReportingMethod,
                                                  val businessReportingMethodService: BusinessReportingMethodService,
                                                  val customNotFoundErrorView: CustomNotFoundError)
                                                 (implicit val appConfig: FrontendAppConfig,
                                                  mcc: MessagesControllerComponents,
                                                  val ec: ExecutionContext,
                                                  val itvcErrorHandler: ItvcErrorHandler,
                                                  val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                                 )
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private def handleRequest(isAgent: Boolean, incomeSourceId: String)
                           (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.BusinessReportingMethodController.submitAgent(incomeSourceId) else
      controllers.incomeSources.add.routes.BusinessReportingMethodController.submit(incomeSourceId)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val redirectUrl: Call = if (isAgent) routes.BusinessAddedController.showAgent(incomeSourceId) else routes.BusinessAddedController.show(incomeSourceId)

    if (incomeSourcesEnabled) {
      businessReportingMethodService.checkITSAStatusCurrentYear.flatMap {
        case true =>
          businessReportingMethodService.getBusinessReportingMethodDetails(incomeSourceId).map {
            case Some(viewModel) =>
              Ok(view(
                addBusinessReportingMethodForm = AddBusinessReportingMethodForm.form,
                businessReportingViewModel = viewModel,
                postAction = postAction,
                isAgent = isAgent)(user, messages))
            case None =>
              Redirect(redirectUrl)
          }.recover {
            case err: Exception =>
              Logger("application").error(s"${if (isAgent) "[Agent]"}" + s"Error getting BusinessReportingMethodDetails : $err")
              errorHandler.showInternalServerError()
          }

        case false => Future.successful(Redirect(redirectUrl))

      }.recover {
        case err: InternalServerException =>
          Logger("application").error(s"${if (isAgent) "[Agent]"}" + s"Error getting ITSA Status : $err")
          errorHandler.showInternalServerError()
      }


    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting BusinessReportingMethodController page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  private def handleSubmitRequest(isAgent: Boolean, incomeSourceId: String)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val redirectUrl: Call = if (isAgent) routes.BusinessAddedController.showAgent(incomeSourceId) else routes.BusinessAddedController.show(incomeSourceId)
    if (incomeSourcesEnabled) {
      AddBusinessReportingMethodForm.form.bindFromRequest().fold(
        hasErrors => {
          val updatedForm = AddBusinessReportingMethodForm.updateErrorMessagesWithValues(hasErrors)

          if (updatedForm.hasErrors) {
            businessReportingMethodService.getBusinessReportingMethodDetails(incomeSourceId).map {
              case Some(viewModel) =>
                BadRequest(view(
                  addBusinessReportingMethodForm = updatedForm,
                  businessReportingViewModel = viewModel,
                  postAction = controllers.incomeSources.add.routes.BusinessReportingMethodController.submit(incomeSourceId),
                  isAgent = isAgent))
              case None =>
                Redirect(redirectUrl)
            }.recover {
              case err: InternalServerException =>
                Logger("application").error(s"${if (isAgent) "[Agent]"}" + s"Error getting BusinessReportingMethodDetails : $err")
                errorHandler.showInternalServerError()
            }


          } else {
            businessReportingMethodService.updateIncomeSourceTaxYearSpecific(user.nino, incomeSourceId, updatedForm.data).map {
              case Some(res: UpdateIncomeSourceResponseModel) =>
                Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updated tax year specific reporting method : $res")
                Redirect(redirectUrl)
              case Some(err: UpdateIncomeSourceResponseError) =>
                Logger("application").error(s"${if (isAgent) "[Agent]"}" + s" Failed to Updated tax year specific reporting method : $err")
                errorHandler.showInternalServerError()
              case None =>
                Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updating tax year specific reporting method not required.")
                Redirect(redirectUrl)
            }

          }
        },
        valid => {
          businessReportingMethodService.updateIncomeSourceTaxYearSpecific(user.nino, incomeSourceId, valid).map {
            case Some(res: UpdateIncomeSourceResponseModel) =>
              Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updated tax year specific reporting method : $res")
              Redirect(redirectUrl)
            case Some(err: UpdateIncomeSourceResponseError) =>
              Logger("application").error(s"${if (isAgent) "[Agent]"}" + s" failed to Updated tax year specific reporting method : $err")
              errorHandler.showInternalServerError()
            case None =>
              Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updating tax year specific reporting method not required.")
              Redirect(redirectUrl)
          }
        }
      )
    } else {
      Future.successful(Ok(customNotFoundErrorView()))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"Error getting BusinessReportingMethodController page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }

  }

  def show(incomeSourceId: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user => handleRequest(isAgent = false, incomeSourceId = incomeSourceId)
  }

  def showAgent(incomeSourceId: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(isAgent = true, incomeSourceId = incomeSourceId)
        }
  }

  def submit(incomeSourceId: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(isAgent = false, incomeSourceId = incomeSourceId)
  }

  def submitAgent(incomeSourceId: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser => handleSubmitRequest(isAgent = true, incomeSourceId = incomeSourceId)
        }
  }

}

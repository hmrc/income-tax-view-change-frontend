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
import enums.AccountingMethod.fromApiField
import enums.IncomeSourceJourney.{BeforeSubmissionPage, ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.IncomeSourcesAccountingMethodForm
import models.incomeSourceDetails.{AddIncomeSourceData, BusinessDetailsModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyChecker}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.IncomeSourcesAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourcesAccountingMethodController @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                                        val view: IncomeSourcesAccountingMethod,
                                                        val customNotFoundErrorView: CustomNotFoundError,
                                                        val sessionService: SessionService,
                                                        val auth: AuthenticatorPredicate)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils with JourneyChecker {

  def handleUserActiveBusinessesCashOrAccruals(isAgent: Boolean,
                                               errorHandler: ShowInternalServerError,
                                               incomeSourceType: IncomeSourceType,
                                               cashOrAccrualsFlag: Option[String])
                                              (implicit user: MtdItUser[_],
                                               backUrl: String, postAction: Call): Future[Result] = {
    val cashOrAccrualsRecords = user.incomeSources.getBusinessCashOrAccruals()
    if (cashOrAccrualsRecords.distinct.size > 1) {
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"Error multiple values for business cashOrAccruals Field found")
    }
    cashOrAccrualsRecords.headOption match {
      case Some(cashOrAccrualsField) =>
        sessionService.setMongoKey(
          key = AddIncomeSourceData.incomeSourcesAccountingMethodField,
          value = fromApiField(cashOrAccrualsField).name,
          journeyType = JourneyType(Add, incomeSourceType)).flatMap {
          case Right(_) =>
            val successRedirectUrl = {
              if (isAgent) routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType).url
              else routes.IncomeSourceCheckDetailsController.show(incomeSourceType).url
            }
            Future.successful(Redirect(successRedirectUrl))
          case Left(ex) =>
            Future.failed(ex)
        }
      case None =>
        Future.successful(
          Ok(view(
            cashOrAccrualsFlag = cashOrAccrualsFlag,
            incomeSourcesType = incomeSourceType,
            form = IncomeSourcesAccountingMethodForm(incomeSourceType),
            postAction = postAction,
            isAgent = isAgent,
            backUrl = backUrl,
            btaNavPartial = user.btaNavPartial
          )))
    }
  }.recover {
    case ex =>
      Logger("application").error(s"[IncomeSourcesAccountingMethodController][handleUserActiveBusinessesCashOrAccruals] - ${ex.getMessage} - ${ex.getCause}")
      errorHandler.showInternalServerError()
  }

  private def loadIncomeSourceAccountingMethod(isAgent: Boolean,
                                               incomeSourceType: IncomeSourceType,
                                               cashOrAccrualsFlag: Option[String])
                                              (implicit user: MtdItUser[_],
                                               backUrl: String,
                                               postAction: Call
                                              ): Future[Result] = {
    Future.successful(Ok(view(
      cashOrAccrualsFlag = cashOrAccrualsFlag,
      incomeSourcesType = incomeSourceType,
      form = IncomeSourcesAccountingMethodForm(incomeSourceType),
      postAction = postAction,
      isAgent = isAgent,
      backUrl = backUrl,
      btaNavPartial = user.btaNavPartial
    )))
  }

  def handleRequest(isAgent: Boolean,
                    incomeSourceType: IncomeSourceType,
                    cashOrAccrualsFlag: Option[String] = None,
                    isChange: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    withSessionData(JourneyType(Add, incomeSourceType), journeyState = BeforeSubmissionPage) { _ =>

      val backUrl = getBackUrl(isAgent, isChange, incomeSourceType)

      val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

      {
        if (incomeSourceType == SelfEmployment) {
          handleUserActiveBusinessesCashOrAccruals(isAgent, errorHandler, incomeSourceType, cashOrAccrualsFlag)(
            user, backUrl, postAction(isAgent, incomeSourceType))
        } else {
          loadIncomeSourceAccountingMethod(isAgent, incomeSourceType, cashOrAccrualsFlag)(
            user, backUrl, postAction(isAgent, incomeSourceType))
        }
      }.recover {
        case ex: Exception =>
          Logger("application").error(s"${if (isAgent) "[Agent]"}" +
            s"Error getting BusinessEndDate page: - ${ex.getMessage} - ${ex.getCause}")
          errorHandler.showInternalServerError()
      }
    }

  private lazy val successCall: (Boolean, IncomeSourceType) => Call = { (isAgent, incomeSourceType) =>
    if (isAgent) routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType)
    else routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
  }

  private lazy val postAction: (Boolean, IncomeSourceType) => Call = { (isAgent, incomeSourceType) =>
    if (isAgent) routes.IncomeSourcesAccountingMethodController.submitAgent(incomeSourceType)
    else routes.IncomeSourcesAccountingMethodController.submit(incomeSourceType)
  }

  private def getBackUrl(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): String = {
    ((isAgent, isChange, incomeSourceType) match {
      case (false, false, SelfEmployment) => routes.AddBusinessAddressController.show(isChange)
      case (_,     false, SelfEmployment) => routes.AddBusinessAddressController.showAgent(isChange)
      case (_,     false, _)              => routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange, incomeSourceType)
      case (false, _,     _)              => routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
      case (_,     _,     _)              => routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType)
    }).url
  }

  def handleSubmitRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    IncomeSourcesAccountingMethodForm(incomeSourceType).bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(view(
        incomeSourcesType = incomeSourceType,
        form = hasErrors,
        postAction = postAction(isAgent, incomeSourceType),
        backUrl = getBackUrl(isAgent, isChange = false, incomeSourceType),
        isAgent = isAgent
      ))),
      validatedInput => {
        val accountingMethod = if (validatedInput.contains("cash")) "cash" else "accruals"
        sessionService.setMongoKey(
          AddIncomeSourceData.incomeSourcesAccountingMethodField,
          accountingMethod,
          JourneyType(Add, incomeSourceType)).flatMap {
          case Right(_) => Future.successful(Redirect(successCall(isAgent, incomeSourceType)))
          case Left(exception) => Future.failed(exception)
        }
      }.recover {
        case ex =>
          val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
          Logger("application").error(s"[IncomeSourcesAccountingMethodController][handleSubmitRequest] - ${ex.getMessage} - ${ex.getCause}")
          errorHandler.showInternalServerError()
      }
    )
  }

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate
      andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        handleRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType,
          isChange = false
        )
    }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType,
              isChange = false
            )
        }
  }

  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      handleSubmitRequest(isAgent = false, incomeSourceType)
  }

  def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      handleSubmitRequest(isAgent = true, incomeSourceType)
  }

  def changeIncomeSourcesAccountingMethod(incomeSourceType: IncomeSourceType): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate
      andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        sessionService.getMongoKeyTyped[String](AddIncomeSourceData.incomeSourcesAccountingMethodField, JourneyType(Add, incomeSourceType)).flatMap {
          case Right(cashOrAccrualsFlag) =>
            handleRequest(
              isAgent = false,
              incomeSourceType = incomeSourceType,
              cashOrAccrualsFlag = cashOrAccrualsFlag,
              isChange = true
            )
          case Left(exception) => Future.failed(exception)
        }.recover {
          case ex =>
            Logger("application").error(s"[IncomeSourcesAccountingMethodController][changeIncomeSourcesAccountingMethod] - ${ex.getMessage} - ${ex.getCause}")
            itvcErrorHandler.showInternalServerError()
        }
    }

  def changeIncomeSourcesAccountingMethodAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            sessionService.getMongoKeyTyped[String](AddIncomeSourceData.incomeSourcesAccountingMethodField, JourneyType(Add, incomeSourceType)).flatMap {
              case Right(cashOrAccrualsFlag) =>
                handleRequest(
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  cashOrAccrualsFlag = cashOrAccrualsFlag,
                  isChange = true
                )
              case Left(exception) => Future.failed(exception)
            }.recover {
              case ex =>
                Logger("application")
                  .error(s"[IncomeSourcesAccountingMethodController][changeIncomeSourcesAccountingMethodAgent] - ${ex.getMessage} - ${ex.getCause}")
                itvcErrorHandlerAgent.showInternalServerError()
            }
        }
  }
}

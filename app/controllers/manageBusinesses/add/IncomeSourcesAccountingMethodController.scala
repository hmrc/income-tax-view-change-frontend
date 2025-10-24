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

package controllers.manageBusinesses.add

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.AccountingMethod.fromApiField
import enums.BeforeSubmissionPage
import enums.IncomeSourceJourney._
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.manageBusinesses.add.IncomeSourcesAccountingMethodForm
import models.core.{CheckMode, Mode, NormalMode}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.errorPages.CustomNotFoundError
import views.html.manageBusinesses.add.IncomeSourcesAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourcesAccountingMethodController @Inject()(val authActions: AuthActions,
                                                        val view: IncomeSourcesAccountingMethod,
                                                        val customNotFoundErrorView: CustomNotFoundError,
                                                        val sessionService: SessionService,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses {

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  def handleUserActiveBusinessesCashOrAccruals(isAgent: Boolean,
                                               incomeSourceType: IncomeSourceType,
                                               cashOrAccrualsFlag: Option[String])
                                              (implicit user: MtdItUser[_],
                                               backUrl: String, postAction: Call): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>
      val cashOrAccrualsRecords = user.incomeSources.getBusinessCashOrAccruals()
      if (cashOrAccrualsRecords.distinct.size > 1) {
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          "Error multiple values for business cashOrAccruals Field found")
      }
      cashOrAccrualsRecords.headOption match {
        case Some(cashOrAccrualsField) =>
          sessionService.setMongoData(
            sessionData.copy(
              addIncomeSourceData =
                sessionData.addIncomeSourceData.map(
                  _.copy(
                    incomeSourcesAccountingMethod =
                      Some(fromApiField(cashOrAccrualsField).name)
                  )
                )
            )
          ) flatMap {
            case true => Future.successful(Redirect(successCall(isAgent, incomeSourceType)))
            case false => throw new Exception("Failed to set mongo data")
          }
        case None =>
          Future.successful(
            Ok(view(
              cashOrAccrualsFlag = cashOrAccrualsFlag,
              incomeSourceType = incomeSourceType,
              form = IncomeSourcesAccountingMethodForm(incomeSourceType),
              postAction = postAction,
              isAgent = isAgent,
              backUrl = backUrl,
              btaNavPartial = user.btaNavPartial
            )))
      }
    }
  }.recover {
    case ex =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      errorHandler(isAgent).showInternalServerError()
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
      incomeSourceType = incomeSourceType,
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
                    mode: Mode)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = BeforeSubmissionPage) { _ =>

      val backUrl = getBackUrl(isAgent, mode, incomeSourceType)

      {
        if (incomeSourceType == SelfEmployment) {
          handleUserActiveBusinessesCashOrAccruals(isAgent, incomeSourceType, cashOrAccrualsFlag)(
            user, backUrl, postAction(isAgent, incomeSourceType))
        } else {
          loadIncomeSourceAccountingMethod(isAgent, incomeSourceType, cashOrAccrualsFlag)(
            user, backUrl, postAction(isAgent, incomeSourceType))
        }
      }
    }.recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting BusinessEndDate page: - ${ex.getMessage} - ${ex.getCause}")
        errorHandler(isAgent).showInternalServerError()
    }


  def handleSubmitRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>
      IncomeSourcesAccountingMethodForm(incomeSourceType).bindFromRequest().fold(
        hasErrors => Future.successful(BadRequest(view(
          incomeSourceType = incomeSourceType,
          form = hasErrors,
          postAction = postAction(isAgent, incomeSourceType),
          backUrl = getBackUrl(isAgent, mode = NormalMode, incomeSourceType),
          isAgent = isAgent
        ))),
        validatedInput => {
          val accountingMethod = if (validatedInput.contains("cash")) "cash" else "accruals"
          sessionService.setMongoData(
            sessionData.copy(
              addIncomeSourceData =
                sessionData.addIncomeSourceData.map(
                  _.copy(
                    incomeSourcesAccountingMethod =
                      Some(accountingMethod)
                  )
                )
            )
          ) flatMap {
            case true => Future.successful(Redirect(successCall(isAgent, incomeSourceType)))
            case false => throw new Exception("Failed to set mongo data")
          }
        }
      )
    }
  }.recover {
    case ex =>
      Logger("application").error(s"${ex.getMessage} - ${ex.getCause}")
      errorHandler(isAgent).showInternalServerError()
  }

  private lazy val successCall: (Boolean, IncomeSourceType) => Call = { (isAgent, incomeSourceType) =>
    if (isAgent) routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType)
    else routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
  }

  private lazy val postAction: (Boolean, IncomeSourceType) => Call = { (isAgent, incomeSourceType) =>
    routes.IncomeSourcesAccountingMethodController.submit(incomeSourceType, isAgent)
  }

  private def getBackUrl(isAgent: Boolean, mode: Mode, incomeSourceType: IncomeSourceType): String = {
    ((isAgent, mode, incomeSourceType) match {
      case (false, NormalMode, SelfEmployment) => routes.AddBusinessAddressController.show(mode)
      case (_, NormalMode, SelfEmployment) => routes.AddBusinessAddressController.showAgent(mode)
      case (_, NormalMode, _) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, mode, incomeSourceType)
      case (false, _, _) => routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
      case (_, _, _) => routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType)
    }).url
  }

  def show(incomeSourceType: IncomeSourceType, isAgent: Boolean): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      handleRequest(
        isAgent,
        incomeSourceType = incomeSourceType,
        mode = NormalMode
      )
  }

  def submit(incomeSourceType: IncomeSourceType, isAgent: Boolean): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user =>
        handleSubmitRequest(
          isAgent,
          incomeSourceType
        )
    }

  def changeIncomeSourcesAccountingMethod(incomeSourceType: IncomeSourceType, isAgent: Boolean): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user =>
        withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), BeforeSubmissionPage) { sessionData =>
          val accountingMethodOpt = sessionData.addIncomeSourceData.flatMap(_.incomeSourcesAccountingMethod)
          handleRequest(
            isAgent,
            incomeSourceType = incomeSourceType,
            cashOrAccrualsFlag = accountingMethodOpt,
            mode = CheckMode
          )
        }
    }
}

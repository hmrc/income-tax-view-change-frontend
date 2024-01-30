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
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import enums.AccountingMethod.fromApiField
import enums.IncomeSourceJourney._
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.IncomeSourcesAccountingMethodForm
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.SessionService
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

  def show(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        handleRequest(isAgent, isChange, incomeSourceType)
    }

  def submit(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    auth.authenticatedAction(isAgent) {
      implicit user =>
        handleSubmitRequest(isAgent, isChange, incomeSourceType)
    }

  def handleRequest(isAgent: Boolean,
                    isChange: Boolean,
                    incomeSourceType: IncomeSourceType
                   )
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    withSessionData(JourneyType(Add, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>

      implicit val back: String = backUrl(isAgent, isChange, incomeSourceType)
      implicit val post: Call = postAction(isAgent, incomeSourceType)

      lazy val maybeCashOrAccrualsFlag = sessionData.addIncomeSourceData.flatMap(_.incomeSourcesAccountingMethod)

      (isChange, incomeSourceType) match {
        case (true, SelfEmployment) =>
          handleUserActiveBusinessesCashOrAccruals(isAgent, incomeSourceType, maybeCashOrAccrualsFlag)
        case (true, _) =>
          loadIncomeSourceAccountingMethod(isAgent, incomeSourceType, maybeCashOrAccrualsFlag)
        case (false, _) =>
          loadIncomeSourceAccountingMethod(isAgent, incomeSourceType, None)
      }
    }.recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting BusinessEndDate page: - ${ex.getMessage} - ${ex.getCause}")
        errorHandler(isAgent).showInternalServerError()
    }

  def handleSubmitRequest(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    IncomeSourcesAccountingMethodForm(incomeSourceType).bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(view(
        incomeSourcesType = incomeSourceType,
        form = hasErrors,
        postAction = postAction(isAgent, incomeSourceType),
        backUrl = backUrl(isAgent, isChange, incomeSourceType),
        isAgent = isAgent
      ))),
      validatedInput => {
        val accountingMethod = if (validatedInput.contains("cash")) "cash" else "accruals"
        sessionService.setMongoKey(AddIncomeSourceData.incomeSourcesAccountingMethodField,
          accountingMethod, JourneyType(Add, incomeSourceType)).flatMap {
          case Right(_) => Future.successful {
            Redirect(successCall(isAgent, incomeSourceType))
          }
          case Left(_) => Future.successful {
            errorHandler(isAgent).showInternalServerError()
          }
        }
      }
    )
  }.recover {
    case ex =>
      Logger("application").error(s"[IncomeSourcesAccountingMethodController][handleSubmitRequest] - ${ex.getMessage} - ${ex.getCause}")
      errorHandler(isAgent).showInternalServerError()
  }

  def handleUserActiveBusinessesCashOrAccruals(isAgent: Boolean,
                                               incomeSourceType: IncomeSourceType,
                                               cashOrAccrualsFlag: Option[String])
                                              (implicit user: MtdItUser[_],
                                               backUrl: String, postAction: Call): Future[Result] = {
    val cashOrAccrualsRecords = user.incomeSources.getBusinessCashOrAccruals()
    if (cashOrAccrualsRecords.distinct.size > 1) {
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        "Error multiple values for business cashOrAccruals Field found")
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
      incomeSourcesType = incomeSourceType,
      form = IncomeSourcesAccountingMethodForm(incomeSourceType),
      postAction = postAction,
      isAgent = isAgent,
      backUrl = backUrl,
      btaNavPartial = user.btaNavPartial
    )))
  }

  private lazy val successCall: (Boolean, IncomeSourceType) => Call = { (isAgent, incomeSourceType) =>
    if (isAgent) routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType)
    else routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
  }

  private lazy val postAction: (Boolean, IncomeSourceType) => Call = { (isAgent, incomeSourceType) =>
    routes.IncomeSourcesAccountingMethodController.submit(incomeSourceType, isAgent)
  }

  private lazy val backUrl: (Boolean, Boolean, IncomeSourceType) => String = (isAgent, isChange, incomeSourceType) =>
    ((isAgent, isChange, incomeSourceType) match {
      case (false, false, SelfEmployment) => routes.AddBusinessAddressController.show(isChange)
      case (_,     false, SelfEmployment) => routes.AddBusinessAddressController.showAgent(isChange)
      case (_,     false, _) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange, incomeSourceType)
      case (false, _,     _) => routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
      case (_,     _,     _) => routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType)
    }).url

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
}

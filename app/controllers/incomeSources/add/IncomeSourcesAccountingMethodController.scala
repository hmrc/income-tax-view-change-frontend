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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.IncomeSourcesAccountingMethodForm
import models.incomeSourceDetails.{AddIncomeSourceData, BusinessDetailsModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.IncomeSourcesAccountingMethod

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourcesAccountingMethodController @Inject()(val authenticate: AuthenticationPredicate,
                                                        val authorisedFunctions: FrontendAuthorisedFunctions,
                                                        val checkSessionTimeout: SessionTimeoutPredicate,
                                                        val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                        val retrieveBtaNavBar: NavBarPredicate,
                                                        val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                        val retrieveNino: NinoPredicate,
                                                        val view: IncomeSourcesAccountingMethod,
                                                        val customNotFoundErrorView: CustomNotFoundError,
                                                        val sessionService: SessionService)
                                                       (implicit val appConfig: FrontendAppConfig,
                                                        mcc: MessagesControllerComponents,
                                                        val ec: ExecutionContext,
                                                        val itvcErrorHandler: ItvcErrorHandler,
                                                        val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  def handleUserActiveBusinessesCashOrAccruals(isAgent: Boolean, errorHandler: ShowInternalServerError, incomeSourceType: IncomeSourceType, cashOrAccrualsFlag: Option[String])
                                              (implicit user: MtdItUser[_], backUrl: String, postAction: Call, messages: Messages): Future[Result] = {
    val userActiveBusinesses: List[BusinessDetailsModel] = user.incomeSources.businesses.filterNot(_.isCeased)

    if (userActiveBusinesses.flatMap(_.cashOrAccruals).distinct.size > 1) {
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"Error getting business cashOrAccruals Field")
    }

    val successRedirectUrl = if (isAgent) controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url else controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url

    userActiveBusinesses.map(_.cashOrAccruals).headOption match {
      case Some(cashOrAccrualsFieldMaybe) =>
        if (cashOrAccrualsFieldMaybe.isDefined) {
          val accountingMethodIsAccruals: String = if (cashOrAccrualsFieldMaybe.get) "accruals" else "cash"

          sessionService.setMongoKey(
            AddIncomeSourceData.incomeSourcesAccountingMethodField,
            accountingMethodIsAccruals,
            JourneyType(Add, incomeSourceType)).flatMap {
            case Right(_) => Future.successful(Redirect(successRedirectUrl))
            case Left(ex) => Future.failed(ex)
          }

        }.recover {
          case exception =>
            Logger("application").error(s"[IncomeSourcesAccountingMethodController][handleUserActiveBusinessesCashOrAccruals] ${exception.getMessage}")
            errorHandler.showInternalServerError()
        }
        else {
          Logger("application").error(s"${if (isAgent) "[Agent]"}" +
            s"Error getting business cashOrAccruals field")
          Future.successful(errorHandler.showInternalServerError())
        }
      case None =>
        Future.successful(Ok(view(
          cashOrAccrualsFlag = cashOrAccrualsFlag,
          incomeSourcesType = incomeSourceType,
          form = IncomeSourcesAccountingMethodForm(incomeSourceType),
          postAction = postAction,
          isAgent = isAgent,
          backUrl = backUrl,
          btaNavPartial = user.btaNavPartial
        )(user, messages)))
    }
  }

  private def loadIncomeSourceAccountingMethod(isAgent: Boolean, incomeSourceType: IncomeSourceType, cashOrAccrualsFlag: Option[String])
                                              (implicit user: MtdItUser[_], backUrl: String, postAction: Call, messages: Messages): Future[Result] = {
    Future.successful(Ok(view(
      cashOrAccrualsFlag = cashOrAccrualsFlag,
      incomeSourcesType = incomeSourceType,
      form = IncomeSourcesAccountingMethodForm(incomeSourceType),
      postAction = postAction,
      isAgent = isAgent,
      backUrl = backUrl,
      btaNavPartial = user.btaNavPartial
    )(user, messages)))
  }

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType, cashOrAccrualsFlag: Option[String] = None, backUrl: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.submitAgent(incomeSourceType) else
      controllers.incomeSources.add.routes.IncomeSourcesAccountingMethodController.submit(incomeSourceType)

    if (incomeSourcesEnabled) {
      if (incomeSourceType == SelfEmployment) {
        handleUserActiveBusinessesCashOrAccruals(isAgent, errorHandler, incomeSourceType, cashOrAccrualsFlag)(
          user, backUrl, postAction, messages)
      } else {
        loadIncomeSourceAccountingMethod(isAgent, incomeSourceType, cashOrAccrualsFlag)(
          user, backUrl, postAction, messages)
      }
    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting BusinessEndDate page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  private def actionIndividualSubmitRequest(incomeSourceType: IncomeSourceType): (Call, String, Call) = {
    incomeSourceType match {
      case SelfEmployment =>
        (routes.IncomeSourcesAccountingMethodController.submit(SelfEmployment),
          routes.AddBusinessAddressController.show(isChange = false).url,
          routes.CheckBusinessDetailsController.show())
      case UkProperty =>
        (routes.IncomeSourcesAccountingMethodController.submit(UkProperty),
          routes.AddIncomeSourceStartDateCheckController.show(isAgent = false, isChange = false, UkProperty).url,
          routes.CheckUKPropertyDetailsController.show())
      case _ =>
        (routes.IncomeSourcesAccountingMethodController.submit(ForeignProperty),
          routes.AddIncomeSourceStartDateCheckController.show(isAgent = false, isChange = false, ForeignProperty).url,
          routes.ForeignPropertyCheckDetailsController.show())
    }
  }

  private def actionAgentSubmitRequest(incomeSourceType: IncomeSourceType): (Call, String, Call) = {
    incomeSourceType match {
      case SelfEmployment =>
        (routes.IncomeSourcesAccountingMethodController.submitAgent(SelfEmployment),
          routes.AddBusinessAddressController.showAgent(isChange = false).url,
          routes.CheckBusinessDetailsController.showAgent())
      case UkProperty =>
        (routes.IncomeSourcesAccountingMethodController.submitAgent(UkProperty),
          routes.AddIncomeSourceStartDateCheckController.show(isAgent = true, isChange = false, UkProperty).url,
          routes.CheckUKPropertyDetailsController.showAgent())
      case _ =>
        (routes.IncomeSourcesAccountingMethodController.submitAgent(ForeignProperty),
          routes.AddIncomeSourceStartDateCheckController.show(isAgent = true, isChange = false, ForeignProperty).url,
          routes.ForeignPropertyCheckDetailsController.showAgent())
    }
  }

  def handleSubmitRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    val (postAction, backUrl, redirect) = if (isAgent)
      actionAgentSubmitRequest(incomeSourceType)
    else
      actionIndividualSubmitRequest(incomeSourceType)

    IncomeSourcesAccountingMethodForm(incomeSourceType).bindFromRequest().fold(
      hasErrors => Future.successful(BadRequest(view(
        incomeSourcesType = incomeSourceType,
        form = hasErrors,
        postAction = postAction,
        backUrl = backUrl,
        isAgent = isAgent
      ))),
      validatedInput => {
        val accountingMethod = if (validatedInput.contains("cash")) "cash" else "accruals"
        sessionService.setMongoKey(
          AddIncomeSourceData.incomeSourcesAccountingMethodField,
          accountingMethod,
          JourneyType(Add, incomeSourceType)).flatMap {
          case Right(result) => Future.successful(Redirect(redirect))
          case Left(exception) => Future.failed(exception)
        }
      }.recover {
        case exception =>
          val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
          Logger("application").error(s"[IncomeSourcesAccountingMethodController][handleSubmitRequest] ${exception.getMessage}")
          errorHandler.showInternalServerError()
      }
    )
  }

  def show(incomeSourceType: IncomeSourceType): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        val backUrl = incomeSourceType match {
          case SelfEmployment =>
            routes.AddBusinessAddressController.show(isChange = false).url
          case _ =>
            routes.AddIncomeSourceStartDateCheckController.show(isAgent = false, isChange = false, incomeSourceType).url
        }
        handleRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType,
          backUrl = backUrl
        )
    }

  def showAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            val backUrl = incomeSourceType match {
              case SelfEmployment =>
                routes.AddBusinessAddressController.showAgent(isChange = false).url
              case _ =>
                routes.AddIncomeSourceStartDateCheckController.show(isAgent = true, isChange = false, incomeSourceType).url
            }
            handleRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType,
              backUrl = backUrl
            )
        }
  }

  def submit(incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(isAgent = false, incomeSourceType)
  }

  def submitAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true, incomeSourceType)
        }
  }

  def changeIncomeSourcesAccountingMethod(incomeSourceType: IncomeSourceType): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate andThen retrieveNino
      andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        val backUrl = incomeSourceType match {
          case SelfEmployment =>
            routes.CheckBusinessDetailsController.show().url
          case UkProperty =>
            routes.CheckUKPropertyDetailsController.show().url
          case _ =>
            routes.ForeignPropertyCheckDetailsController.show().url
        }

        sessionService.getMongoKeyTyped[String](AddIncomeSourceData.incomeSourcesAccountingMethodField, JourneyType(Add, incomeSourceType)).flatMap {

          case Right(cashOrAccrualsFlag) =>
            handleRequest(
              isAgent = false,
              incomeSourceType = incomeSourceType,
              cashOrAccrualsFlag = cashOrAccrualsFlag,
              backUrl = backUrl
            )

          case Left(exception) => Future.failed(exception)

        }.recover {
          case exception =>
            Logger("application").error(s"[IncomeSourcesAccountingMethodController][changeIncomeSourcesAccountingMethod] ${exception.getMessage}")
            itvcErrorHandler.showInternalServerError()
        }
    }

  def changeIncomeSourcesAccountingMethodAgent(incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            val backUrl = incomeSourceType match {
              case SelfEmployment =>
                routes.CheckBusinessDetailsController.showAgent().url
              case UkProperty =>
                routes.CheckUKPropertyDetailsController.showAgent().url
              case _ =>
                routes.ForeignPropertyCheckDetailsController.showAgent().url
            }
            sessionService.getMongoKeyTyped[String](AddIncomeSourceData.incomeSourcesAccountingMethodField, JourneyType(Add, incomeSourceType)).flatMap {
              case Right(cashOrAccrualsFlag) =>
                handleRequest(
                  isAgent = true,
                  incomeSourceType = incomeSourceType,
                  cashOrAccrualsFlag = cashOrAccrualsFlag,
                  backUrl = backUrl
                )
              case Left(exception) => Future.failed(exception)
            }.recover {
              case exception =>
                Logger("application").error(s"[IncomeSourcesAccountingMethodController][changeIncomeSourcesAccountingMethodAgent] ${exception.getMessage}")
                itvcErrorHandlerAgent.showInternalServerError()
            }
        }
  }
}

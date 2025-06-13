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
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import enums.IncomeSourceJourney._
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.incomeSources.add.AddIncomeSourceStartDateFormProvider
import implicits.ImplicitDateFormatterImpl
import models.incomeSourceDetails.AddIncomeSourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, SessionService}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyChecker
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddIncomeSourceStartDate

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddIncomeSourceStartDateController @Inject()(val authActions: AuthActions,
                                                   val addIncomeSourceStartDate: AddIncomeSourceStartDate,
                                                   val customNotFoundErrorView: CustomNotFoundError,
                                                   val sessionService: SessionService,
                                                   val itvcErrorHandler: ItvcErrorHandler,
                                                   val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                   form: AddIncomeSourceStartDateFormProvider)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                    val dateFormatter: ImplicitDateFormatterImpl,
                                                   val dateService: DateService,
                                                    val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport with JourneyChecker {


  def show(isAgent: AffinityGroup,
           isChange: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent == Agent).async { implicit user =>

    handleShowRequest(
      incomeSourceType = incomeSourceType,
      isAgent = isAgent == Agent,
      isChange = isChange
    )
  }

  def submit(isAgent: Boolean,
             isChange: Boolean,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>

    handleSubmitRequest(
      incomeSourceType = incomeSourceType,
      isAgent = isAgent,
      isChange = isChange
    )
  }

  private def handleShowRequest(incomeSourceType: IncomeSourceType,
                                isAgent: Boolean,
                                isChange: Boolean)
                               (implicit user: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.startDateMessagesPrefix

    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = {
      incomeSourceType match {
        case SelfEmployment => BeforeSubmissionPage
        case _ => InitialPage
      }
    }) { sessionData =>
      if (!isChange && incomeSourceType.equals(UkProperty) || !isChange && incomeSourceType.equals(ForeignProperty)) {
        lazy val journeyType = IncomeSourceJourneyType(Add, incomeSourceType)
        sessionService.createSession(journeyType)
      }

      val dateStartedOpt = sessionData.addIncomeSourceData.flatMap(_.dateStarted)
      val filledForm = dateStartedOpt match {
        case Some(date) =>
          form(messagesPrefix).fill(date)
        case None => form(messagesPrefix)
      }

      Future.successful {
        Ok(
          addIncomeSourceStartDate(
            form = filledForm,
            isAgent = isAgent,
            messagesPrefix = messagesPrefix,
            backUrl = getBackUrl(incomeSourceType, isAgent, isChange),
            postAction = getPostAction(incomeSourceType, isAgent, isChange)
          )
        )
      }
    }.recover {
      case ex =>
        val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application").error(s"[AddIncomeSourceStartDateController]${ex.getMessage} - ${ex.getCause}")
        errorHandler.showInternalServerError()
    }
  }


  private def handleSubmitRequest(incomeSourceType: IncomeSourceType,
                                  isAgent: Boolean,
                                  isChange: Boolean)
                                 (implicit user: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.startDateMessagesPrefix

    withIncomeSourcesFS {
      form(messagesPrefix).bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(
            addIncomeSourceStartDate(
              isAgent = isAgent,
              form = formWithErrors,
              backUrl = getBackUrl(incomeSourceType, isAgent, isChange),
              postAction = getPostAction(incomeSourceType, isAgent, isChange),
              messagesPrefix = messagesPrefix
            )
          )),
        formData => handleValidFormData(formData, incomeSourceType, isAgent, isChange)
      )
    }
  }.recover {
    case ex =>
      Logger("application")
        .error(s"[${incomeSourceType.key}] ${ex.getMessage} - ${ex.getCause}")
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      errorHandler.showInternalServerError()
  }

  def handleValidFormData(formData: LocalDate, incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean)
                         (implicit user: MtdItUser[_]): Future[Result] = {
    withSessionDataAndOldIncomeSourceFS(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = {
      incomeSourceType match {
        case SelfEmployment => BeforeSubmissionPage
        case _ => InitialPage
      }
    }) { sessionData =>
      sessionService.setMongoData(
        sessionData.addIncomeSourceData match {
          case Some(_) =>
            sessionData.copy(
              addIncomeSourceData =
                sessionData.addIncomeSourceData.map(
                  _.copy(
                    dateStarted = Some(formData)
                  )
                )
            )
          case None =>
            sessionData.copy(
              addIncomeSourceData =
                Some(
                  AddIncomeSourceData(
                    dateStarted = Some(formData)
                  )
                )
            )
        }
      ) flatMap {
        case true => Future.successful(Redirect(getSuccessUrl(incomeSourceType, isAgent, isChange)))
        case false => Future.failed(new Exception("Mongo update call was not acknowledged"))
      }
    }
  }

  private def getPostAction(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): Call = {
    routes.AddIncomeSourceStartDateController.submit(isAgent, isChange, incomeSourceType)
  }

  private def getSuccessUrl(incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean): Call = {
    routes.AddIncomeSourceStartDateCheckController.show(isAgent, isChange, incomeSourceType)
  }

  private def getBackUrl(incomeSourceType: IncomeSourceType,
                         isAgent: Boolean,
                         isChange: Boolean): String = {

    ((isAgent, isChange, incomeSourceType) match {
      case (false, false, SelfEmployment) => routes.AddBusinessNameController.show(isChange)
      case (_, false, SelfEmployment) => routes.AddBusinessNameController.showAgent(isChange)
      case (false, false, _) => routes.AddIncomeSourceController.show()
      case (_, false, _) => routes.AddIncomeSourceController.showAgent()
      case (false, _, _) => routes.IncomeSourceCheckDetailsController.show(incomeSourceType)
      case (_, _, _) => routes.IncomeSourceCheckDetailsController.showAgent(incomeSourceType)
    }).url
  }
}

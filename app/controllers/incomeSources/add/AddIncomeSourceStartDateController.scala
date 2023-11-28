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
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Add, JourneyType}
import forms.incomeSources.add.{AddIncomeSourceStartDateForm => form}
import forms.models.DateFormElement
import implicits.ImplicitDateFormatterImpl
import models.incomeSourceDetails.AddIncomeSourceData.dateStartedField
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import utils.{IncomeSourcesUtils, JourneyChecker}
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddIncomeSourceStartDate

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddIncomeSourceStartDateController @Inject()(authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   checkSessionTimeout: SessionTimeoutPredicate,
                                                   val addIncomeSourceStartDate: AddIncomeSourceStartDate,
                                                   val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val customNotFoundErrorView: CustomNotFoundError,
                                                   incomeSourceDetailsService: IncomeSourceDetailsService,
                                                   val sessionService: SessionService)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   implicit val itvcErrorHandler: ItvcErrorHandler,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                   implicit val dateFormatter: ImplicitDateFormatterImpl,
                                                   implicit val dateService: DateService,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching with IncomeSourcesUtils with JourneyChecker {

  def show(isAgent: Boolean,
           isChange: Boolean,
           incomeSourceType: IncomeSourceType
          ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    handleShowRequest(
      incomeSourceType = incomeSourceType,
      isAgent = isAgent,
      isChange = isChange
    )
  }

  def submit(isAgent: Boolean,
             isChange: Boolean,
             incomeSourceType: IncomeSourceType
            ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

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

    withIncomeSourcesFSWithSessionCheck(JourneyType(Add, incomeSourceType)) {
      if (!isChange && incomeSourceType.equals(UkProperty) || !isChange && incomeSourceType.equals(ForeignProperty)) {
        lazy val journeyType = JourneyType(Add, incomeSourceType)
        sessionService.createSession(journeyType.toString).flatMap {
          case true => Future.successful(None)
          case false => throw new Exception("Unable to create session")
        }
      }

      getFilledForm(form(messagesPrefix), incomeSourceType, isChange).map { filledForm =>
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
        Logger("application").error(s"[AddIncomeSourceStartDateController][handleRequest][${incomeSourceType.key}] ${ex.getMessage}")
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
    case exception =>
      val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"[AddIncomeSourceStartDateController][handleSubmitRequest][${incomeSourceType.key}] ${exception.getMessage}")
      errorHandler.showInternalServerError()
  }

  def handleValidFormData(formData: DateFormElement, incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean)
                         (implicit user: MtdItUser[_]): Future[Result] = {
    val journeyType = JourneyType(Add, incomeSourceType)
    sessionService.setMongoKey(dateStartedField, formData.date.toString, journeyType).flatMap {
      case Right(result) if result => Future.successful {
        val successUrl = getSuccessUrl(incomeSourceType, isAgent, isChange)
        Redirect(successUrl)
      }
      case Right(_) => Future.failed(new Exception("Mongo update call was not acknowledged"))
      case Left(exception) => Future.failed(exception)
    }
  }

  private def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent)
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              authenticatedCodeBlock(mtdItUser)
            }
      }
    else
      (checkSessionTimeout andThen authenticate
        andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }

  private def getFilledForm(form: Form[DateFormElement],
                            incomeSourceType: IncomeSourceType,
                            isChange: Boolean)(implicit user: MtdItUser[_]): Future[Form[DateFormElement]] = {

    if (isChange) {
      getStartDate(incomeSourceType).flatMap {
        case Some(date) =>
          Future.successful(
            form.fill(
              DateFormElement(date)
            )
          )
        case None =>
          throw new Exception(s"Unable to retrieve start date from Mongo")
      }
    }
    else Future.successful(form)
  }

  private def getStartDate(incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Option[LocalDate]] = {
    val journeyType = JourneyType(Add, incomeSourceType)
    sessionService.getMongoKeyTyped[LocalDate](dateStartedField, journeyType).flatMap {
      case Right(dateOpt) => Future.successful(dateOpt)
      case Left(ex) => Future.failed(ex)
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
      case (false, false, SelfEmployment) => routes.AddBusinessNameController.show()
      case (_, false, SelfEmployment) => routes.AddBusinessNameController.showAgent()
      case (false, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.show(SelfEmployment)
      case (_, _, SelfEmployment) => routes.IncomeSourceCheckDetailsController.showAgent(SelfEmployment)
      case (false, false, _) => routes.AddIncomeSourceController.show()
      case (_, false, _) => routes.AddIncomeSourceController.showAgent()
      case (false, _, UkProperty) => routes.IncomeSourceCheckDetailsController.show(UkProperty)
      case (_, _, UkProperty) => routes.IncomeSourceCheckDetailsController.showAgent(UkProperty)
      case (false, _, _) => routes.IncomeSourceCheckDetailsController.show(ForeignProperty)
      case (_, _, _) => routes.IncomeSourceCheckDetailsController.showAgent(ForeignProperty)
    }).url
  }
}
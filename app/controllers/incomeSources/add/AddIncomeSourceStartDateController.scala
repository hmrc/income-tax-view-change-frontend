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
import forms.incomeSources.add.{AddIncomeSourceStartDateForm => form}
import forms.models.DateFormElement
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatterImpl
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.AddIncomeSourceStartDate

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class AddIncomeSourceStartDateController @Inject()(authenticate: AuthenticationPredicate,
                                                   val authorisedFunctions: AuthorisedFunctions,
                                                   checkSessionTimeout: SessionTimeoutPredicate,
                                                   retrieveNino: NinoPredicate,
                                                   val addIncomeSourceStartDate: AddIncomeSourceStartDate,
                                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                   val retrieveBtaNavBar: NavBarPredicate,
                                                   val customNotFoundErrorView: CustomNotFoundError,
                                                   incomeSourceDetailsService: IncomeSourceDetailsService)
                                                  (implicit val appConfig: FrontendAppConfig,
                                                   implicit val itvcErrorHandler: ItvcErrorHandler,
                                                   implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                   implicit val dateFormatter: ImplicitDateFormatterImpl,
                                                   implicit val dateService: DateService,
                                                   implicit override val mcc: MessagesControllerComponents,
                                                   val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  def showUKProperty(isAgent: Boolean, isUpdate: Boolean): Action[AnyContent] = handleRequest(UkProperty, isAgent, isUpdate)
  def showForeignProperty(isAgent: Boolean, isUpdate: Boolean): Action[AnyContent] = handleRequest(ForeignProperty, isAgent, isUpdate)
  def showSoleTraderBusiness(isAgent: Boolean, isUpdate: Boolean): Action[AnyContent] = handleRequest(SelfEmployment, isAgent, isUpdate)
  def submitUKProperty(isAgent: Boolean, isUpdate: Boolean): Action[AnyContent] = handleRequest(UkProperty, isAgent, isUpdate)
  def submitForeignProperty(isAgent: Boolean, isUpdate: Boolean): Action[AnyContent] = handleRequest(ForeignProperty, isAgent, isUpdate)
  def submitSoleTraderBusiness(isAgent: Boolean, isUpdate: Boolean): Action[AnyContent] = handleRequest(SelfEmployment, isAgent, isUpdate)

  private def handleRequest(incomeSourceType: IncomeSourceType,
                            isAgent: Boolean,
                            isUpdate: Boolean): Action[AnyContent] = {
    if (isAgent)
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
              implicit mtdItUser =>
                request.method match {
                  case "POST" => submit(incomeSourceType, isAgent, isUpdate)
                  case "GET" => show(incomeSourceType, isAgent, isUpdate)
                }
            }
      }
    else
      (checkSessionTimeout andThen authenticate andThen retrieveNino
        andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
        implicit user =>
          user.method match {
            case "POST" => submit(incomeSourceType, isAgent, isUpdate)
            case "GET" => show(incomeSourceType, isAgent, isUpdate)
          }
      }
  }

  private def show(incomeSourceType: IncomeSourceType,
                   isAgent: Boolean,
                   isUpdate: Boolean)
                  (implicit user: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = getMessagesPrefix(incomeSourceType)

    if(isEnabled(IncomeSources)) {
      getCalls(incomeSourceType, isAgent, isUpdate) match {
        case (backCall, postAction, _) =>
          Future {
            Ok(
              addIncomeSourceStartDate(
                form = getFilledForm(
                  form = form(messagesPrefix),
                  maybeStartDateKey = user.session.get(getStartDateKey(incomeSourceType)),
                  isUpdate = isUpdate
                ),
                isAgent = isAgent,
                backUrl = backCall.url,
                postAction = postAction,
                messagesPrefix = messagesPrefix
              )
            )
          }
      }
    } else {
      Future {
        Ok(
          customNotFoundErrorView()
        )
      }
    } recover {
      case ex: Exception =>
        Logger("application").error(s"[AddIncomeSourceStartDateController][handleRequest] - Error: ${ex.getMessage}")
        if (isAgent) itvcErrorHandlerAgent.showInternalServerError
        else itvcErrorHandler.showInternalServerError
    }
  }

  private def submit(incomeSourceType: IncomeSourceType,
                     isAgent: Boolean,
                     isUpdate: Boolean)
                    (implicit user: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = getMessagesPrefix(incomeSourceType)

    if(isEnabled(IncomeSources)) {
      getCalls(incomeSourceType, isAgent, isUpdate) match {
        case (backCall, postAction, redirectCall) =>
          form(messagesPrefix).bindFromRequest().fold(
            formWithErrors =>
              Future {
                BadRequest(
                  addIncomeSourceStartDate(
                    isAgent = isAgent,
                    form = formWithErrors,
                    backUrl = backCall.url,
                    postAction = postAction,
                    messagesPrefix = messagesPrefix
                  )
                )
              },
            formData =>
              Future.successful(
                Redirect(redirectCall)
                  .addingToSession(
                    (incomeSourceType match {
                      case SelfEmployment =>
                        SessionKeys.addBusinessStartDate
                      case UkProperty =>
                        SessionKeys.addUkPropertyStartDate
                      case ForeignProperty =>
                        SessionKeys.foreignPropertyStartDate
                    }) -> formData.date.toString
                  )
              )
          )
      }
    } else {
      Future {
        Ok(
          customNotFoundErrorView()
        )
      }
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateController][handleSubmitRequest] - Error: ${ex.getMessage}")
      if (isAgent) itvcErrorHandlerAgent.showInternalServerError
      else itvcErrorHandler.showInternalServerError
  }

  private def getCalls(incomeSourceType: IncomeSourceType, isAgent: Boolean, isUpdate: Boolean): (Call, Call, Call) = {

    incomeSourceType match {
      case SelfEmployment =>
        (
          (isAgent, isUpdate) match {
            case (false, false) => routes.AddBusinessNameController.show()
            case (false, true) => routes.CheckBusinessDetailsController.show()
            case (true, false) => routes.AddBusinessNameController.showAgent()
            case (true, true) => routes.CheckBusinessDetailsController.showAgent()
          },
          routes.AddIncomeSourceStartDateController.submitSoleTraderBusiness(isAgent = isAgent, isUpdate = isUpdate),
          routes.AddIncomeSourceStartDateCheckController.showSoleTraderBusiness(isAgent = isAgent, isUpdate = isUpdate)
        )
      case UkProperty =>
        (
          (isAgent, isUpdate) match {
            case (false, false) => routes.AddIncomeSourceController.show()
            case (false, true) => routes.CheckUKPropertyDetailsController.show()
            case (true, false) => routes.AddIncomeSourceController.showAgent()
            case (true, true) => routes.CheckUKPropertyDetailsController.showAgent()
          },
          routes.AddIncomeSourceStartDateController.submitUKProperty(isAgent = isAgent, isUpdate = isUpdate),
          routes.AddIncomeSourceStartDateCheckController.submitUKProperty(isAgent = isAgent, isUpdate = isUpdate)
        )

      case ForeignProperty =>
        (
          (isAgent, isUpdate) match {
            case (false, false) => routes.AddIncomeSourceController.show()
            case (false, true) => routes.ForeignPropertyCheckDetailsController.show()
            case (true, false) => routes.AddIncomeSourceController.showAgent()
            case (true, true) => routes.ForeignPropertyCheckDetailsController.showAgent()
          },
          routes.AddIncomeSourceStartDateController.submitForeignProperty(isAgent = isAgent, isUpdate = isUpdate),
          routes.AddIncomeSourceStartDateCheckController.submitForeignProperty(isAgent = isAgent, isUpdate = isUpdate)
        )
    }
  }

  private def getMessagesPrefix(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => SelfEmployment.addIncomeSourceStartDateMessagesPrefix
      case ForeignProperty => ForeignProperty.addIncomeSourceStartDateMessagesPrefix
      case UkProperty => UkProperty.addIncomeSourceStartDateMessagesPrefix
    }
  }

  def getStartDateKey(incomeSourceType: IncomeSourceType): String = {
    incomeSourceType match {
      case SelfEmployment => SessionKeys.businessStartDate
      case UkProperty => SessionKeys.addUkPropertyStartDate
      case ForeignProperty => SessionKeys.foreignPropertyStartDate
    }
  }

  private def getFilledForm(form: Form[DateFormElement],
                            maybeStartDateKey: Option[String],
                            isUpdate: Boolean): Form[DateFormElement] = {

    (maybeStartDateKey, isUpdate) match {
      case (Some(key), true) if Try(LocalDate.parse(key)).toOption.isDefined =>
        form.fill(
          DateFormElement(
            LocalDate.parse(key)
          )
        )
      case _ => form
    }
  }
}

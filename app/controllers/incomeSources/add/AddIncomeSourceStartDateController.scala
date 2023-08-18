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
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import forms.incomeSources.add.{AddIncomeSourceStartDateForm => form}
import forms.models.DateFormElement
import forms.utils.SessionKeys
import implicits.ImplicitDateFormatterImpl
import play.api.Logger
import play.api.data.Form
import play.api.http.HttpVerbs
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

  def handleRequest(incomeSourceKey: String,
                    isAgent: Boolean,
                    isChange: Boolean
                   ): Action[AnyContent] = authenticatedAction(isAgent) { implicit user =>

    user.method match {
      case HttpVerbs.GET => show(IncomeSourceType.get(incomeSourceKey), isAgent, isChange)
      case HttpVerbs.POST => submit(IncomeSourceType.get(incomeSourceKey), isAgent, isChange)
    }
  }

  private def show(incomeSourceType: IncomeSourceType,
                   isAgent: Boolean,
                   isUpdate: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.startDateMessagesPrefix

    val maybeStartDateSessionKey = user.session.get(incomeSourceType.startDateSessionKey)

    Future.successful(
      if (isEnabled(IncomeSources)) {
        getCalls(incomeSourceType, isAgent, isUpdate) match {
          case (backCall, postAction, _) =>
            Ok(
              addIncomeSourceStartDate(
                form = getFilledForm(form(messagesPrefix), maybeStartDateSessionKey, isUpdate),
                isAgent = isAgent,
                backUrl = backCall.url,
                postAction = postAction,
                messagesPrefix = messagesPrefix
              )
            )
        }
      } else Ok(customNotFoundErrorView())
    )
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateController][handleRequest] - Error: ${ex.getMessage}")
      getErrorHandler(isAgent).showInternalServerError()
  }


  private def submit(incomeSourceType: IncomeSourceType,
                     isAgent: Boolean,
                     isUpdate: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {

    val messagesPrefix = incomeSourceType.startDateMessagesPrefix

    Future.successful(
      if (isEnabled(IncomeSources)) {
        getCalls(incomeSourceType, isAgent, isUpdate) match {
          case (backCall, postAction, redirectCall) =>
            form(messagesPrefix).bindFromRequest().fold(
              formWithErrors =>
                BadRequest(
                  addIncomeSourceStartDate(
                    isAgent = isAgent,
                    form = formWithErrors,
                    backUrl = backCall.url,
                    postAction = postAction,
                    messagesPrefix = messagesPrefix
                  )
                ),
              formData =>
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
        }
      } else Ok(customNotFoundErrorView())
    )
  } recover {
    case ex: Exception =>
      Logger("application").error(s"[AddIncomeSourceStartDateController][handleSubmitRequest] - Error: ${ex.getMessage}")
      getErrorHandler(isAgent).showInternalServerError()
  }

  private def authenticatedAction(isAgent: Boolean)(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent) {
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              authenticatedCodeBlock(mtdItUser)
            }
      }
    } else {
      (checkSessionTimeout andThen authenticate andThen retrieveNino
        andThen retrieveIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
    }
  }

  private def getCalls(incomeSourceType: IncomeSourceType,
                       isAgent: Boolean,
                       isUpdate: Boolean): (Call, Call, Call) = {
    (
      (isAgent, isUpdate, incomeSourceType) match {
        case (false, false, SelfEmployment) => routes.AddBusinessNameController.show()
        case (false, true,  SelfEmployment) => routes.CheckBusinessDetailsController.show()
        case (true,  false, SelfEmployment) => routes.AddBusinessNameController.showAgent()
        case (true,  true,  SelfEmployment) => routes.CheckBusinessDetailsController.showAgent()
        case (false, false, _)              => routes.AddIncomeSourceController.show()
        case (true,  false, _)              => routes.AddIncomeSourceController.showAgent()
        case (false, true, UkProperty)      => routes.CheckUKPropertyDetailsController.show()
        case (true,  true, UkProperty)      => routes.CheckUKPropertyDetailsController.showAgent()
        case (false, true, ForeignProperty) => routes.ForeignPropertyCheckDetailsController.show()
        case (true,  true, ForeignProperty) => routes.ForeignPropertyCheckDetailsController.showAgent()
      },
      routes.AddIncomeSourceStartDateController.handleRequest(incomeSourceType.key, isAgent, isUpdate),
      routes.AddIncomeSourceStartDateCheckController.handleRequest(incomeSourceType.key, isAgent, isUpdate)
    )
  }

  private def getErrorHandler(isAgent: Boolean): ShowInternalServerError = {
    if (isAgent) itvcErrorHandlerAgent
    else itvcErrorHandler
  }

  private def getFilledForm(form: Form[DateFormElement],
                            startDateKey: Option[String],
                            isUpdate: Boolean): Form[DateFormElement] = {

    (startDateKey, isUpdate) match {
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

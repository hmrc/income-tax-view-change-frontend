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

package controllers.incomeSources.cease

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{ForeignProperty, IncomeSourceType, SelfEmployment, UkProperty}
import enums.JourneyType.{Cease, JourneyType}
import forms.incomeSources.cease.IncomeSourceEndDateForm
import forms.models.DateFormElement
import models.core.IncomeSourceIdHash.mkFromQueryString
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import models.incomeSourceDetails.CeaseIncomeSourceData
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services.{IncomeSourceDetailsService, SessionService}
import utils.{IncomeSourcesUtils, JourneyChecker}
import views.html.incomeSources.cease.IncomeSourceEndDate

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceEndDateController @Inject()(val authenticate: AuthenticationPredicate,
                                              val authorisedFunctions: FrontendAuthorisedFunctions,
                                              val checkSessionTimeout: SessionTimeoutPredicate,
                                              val incomeSourceEndDateForm: IncomeSourceEndDateForm,
                                              val incomeSourceDetailsService: IncomeSourceDetailsService,
                                              val retrieveBtaNavBar: NavBarPredicate,
                                              val retrieveNinoWithIncomeSources: IncomeSourceDetailsPredicate,
                                              val incomeSourceEndDate: IncomeSourceEndDate,
                                              val sessionService: SessionService)
                                             (implicit val appConfig: FrontendAppConfig,
                                              mcc: MessagesControllerComponents,
                                              val ec: ExecutionContext,
                                              val itvcErrorHandler: ItvcErrorHandler,
                                              val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils with JourneyChecker {

  private def getActions(isAgent: Boolean, incomeSourceType: IncomeSourceType, maybeIncomeSourceId: Option[IncomeSourceId], isChange: Boolean): Future[(Call, Call, Call)] = {

    val hashedId: Option[String] = maybeIncomeSourceId.map(m => m.toHash.hash)
    Future.successful(
      (incomeSourceType, isAgent, isChange) match {
        case (UkProperty, true, false) =>
          (routes.DeclarePropertyCeasedController.showAgent(incomeSourceType),
            routes.IncomeSourceEndDateController.submitAgent(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.showAgent(UkProperty))
        case (UkProperty, false, false) =>
          (routes.DeclarePropertyCeasedController.show(incomeSourceType),
            routes.IncomeSourceEndDateController.submit(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.show(UkProperty))
        case (UkProperty, true, true) =>
          (routes.DeclarePropertyCeasedController.showAgent(incomeSourceType),
            routes.IncomeSourceEndDateController.submitChangeAgent(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.showAgent(UkProperty))
        case (UkProperty, false, true) =>
          (routes.DeclarePropertyCeasedController.show(incomeSourceType),
            routes.IncomeSourceEndDateController.submitChange(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.show(UkProperty))
        case (ForeignProperty, true, false) =>
          (routes.DeclarePropertyCeasedController.showAgent(incomeSourceType),
            routes.IncomeSourceEndDateController.submitAgent(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.showAgent(ForeignProperty))
        case (ForeignProperty, false, false) =>
          (routes.DeclarePropertyCeasedController.show(incomeSourceType),
            routes.IncomeSourceEndDateController.submit(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.show(ForeignProperty))
        case (ForeignProperty, true, true) =>
          (routes.DeclarePropertyCeasedController.showAgent(incomeSourceType),
            routes.IncomeSourceEndDateController.submitChangeAgent(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.showAgent(ForeignProperty))
        case (ForeignProperty, false, true) =>
          (routes.DeclarePropertyCeasedController.show(incomeSourceType),
            routes.IncomeSourceEndDateController.submitChange(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.show(ForeignProperty))
        case (SelfEmployment, true, false) =>
          (routes.CeaseIncomeSourceController.showAgent(),
            routes.IncomeSourceEndDateController.submitAgent(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.showAgent(SelfEmployment))
        case (SelfEmployment, false, false) =>
          (routes.CeaseIncomeSourceController.show(),
            routes.IncomeSourceEndDateController.submit(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.show(SelfEmployment))
        case (SelfEmployment, true, true) =>
          (routes.CeaseIncomeSourceController.showAgent(),
            routes.IncomeSourceEndDateController.submitChangeAgent(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.showAgent(SelfEmployment))
        case (SelfEmployment, false, true) =>
          (routes.CeaseIncomeSourceController.show(),
            routes.IncomeSourceEndDateController.submitChange(id = hashedId, incomeSourceType = incomeSourceType),
            routes.CeaseCheckIncomeSourceDetailsController.show(SelfEmployment))
      })
  }

  def show(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate
      andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)
        handleRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType,
          id = incomeSourceIdHashMaybe,
          isChange = false
        )
    }

  def showAgent(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)
            handleRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType,
              id = incomeSourceIdHashMaybe,
              isChange = false
            )
        }
  }

  def showChange(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] =
    (checkSessionTimeout andThen authenticate
      andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
      implicit user =>
        val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)
        handleRequest(
          isAgent = false,
          incomeSourceType = incomeSourceType,
          id = incomeSourceIdHashMaybe,
          isChange = true
        )
    }

  def showChangeAgent(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)
            handleRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType,
              id = incomeSourceIdHashMaybe,
              isChange = true
            )
        }
  }

  def handleRequest(id: Option[IncomeSourceIdHash], isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], ec: ExecutionContext, messages: Messages): Future[Result] =  withSessionData(JourneyType(Cease, incomeSourceType)) { _ =>

    val incomeSourceIdMaybe: Option[IncomeSourceId] = id.flatMap(x => user.incomeSources.compareHashToQueryString(x))

    if(incomeSourceType == SelfEmployment && !isChange) {
      sessionService.createSession(JourneyType(Cease, incomeSourceType).toString)
    }

      getActions(isAgent, incomeSourceType, incomeSourceIdMaybe, isChange).flatMap {
        actions =>
          val (backAction: Call, postAction: Call, _) = actions
          (incomeSourceType, id) match {
            case (SelfEmployment, None) =>
              Future.failed(new Exception(s"Missing income source ID"))
            case _ =>
              getFilledForm(incomeSourceEndDateForm(incomeSourceType, incomeSourceIdMaybe.map(_.value)), incomeSourceType, isChange).flatMap {
                form: Form[DateFormElement] =>
                  Future.successful(Ok(
                    incomeSourceEndDate(
                      incomeSourceEndDateForm = form,
                      postAction = postAction,
                      isAgent = isAgent,
                      backUrl = backAction.url,
                      incomeSourceType = incomeSourceType
                    )(user, messages))
                  )
              }
          }
      }
  } recover {
    case ex: Exception =>
      val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"[IncomeSourceEndDateController][handleRequest]: Error getting IncomeSourceEndDate page: ${ex.getMessage}")
      errorHandler.showInternalServerError()
  }

  def submit(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)
      handleSubmitRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType,
        id = incomeSourceIdHashMaybe,
        isChange = false
      )
  }

  def submitAgent(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)
            handleSubmitRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType,
              id = incomeSourceIdHashMaybe,
              isChange = false
            )
        }
  }

  def submitChange(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] = (checkSessionTimeout andThen authenticate
    andThen retrieveNinoWithIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)
      handleSubmitRequest(
        isAgent = false,
        incomeSourceType = incomeSourceType,
        id = incomeSourceIdHashMaybe,
        isChange = true
      )
  }

  def submitChangeAgent(id: Option[String], incomeSourceType: IncomeSourceType): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)
            handleSubmitRequest(
              isAgent = true,
              incomeSourceType = incomeSourceType,
              id = incomeSourceIdHashMaybe,
              isChange = true
            )
        }
  }

  def handleSubmitRequest(id: Option[IncomeSourceIdHash], isAgent: Boolean, incomeSourceType: IncomeSourceType, isChange: Boolean)
                         (implicit user: MtdItUser[_], messages: Messages): Future[Result] = withIncomeSourcesFS {

    val incomeSourceIdMaybe: Option[IncomeSourceId] = id.flatMap(x => user.incomeSources.compareHashToQueryString(x))

    getActions(isAgent, incomeSourceType, incomeSourceIdMaybe, isChange).flatMap { actions =>
      val (backAction, postAction, redirectAction) = actions

      incomeSourceEndDateForm(incomeSourceType, incomeSourceIdMaybe.map(_.value)).bindFromRequest().fold(
        hasErrors => {
          Future.successful(BadRequest(incomeSourceEndDate(
            incomeSourceEndDateForm = hasErrors,
            postAction = postAction,
            backUrl = backAction.url,
            isAgent = isAgent,
            incomeSourceType = incomeSourceType
          )(user, messages)))
        },

        validatedInput =>
            (incomeSourceType, incomeSourceIdMaybe) match {
              case (SelfEmployment, Some(incomeSourceId)) =>
                val result = Redirect(redirectAction)
                sessionService.setMongoKey(
                  CeaseIncomeSourceData.dateCeasedField, validatedInput.date.toString, JourneyType(Cease, incomeSourceType)
                ).flatMap {
                  case Right(_) =>
                    sessionService.setMongoKey(
                      CeaseIncomeSourceData.incomeSourceIdField, incomeSourceId.value, JourneyType(Cease, incomeSourceType)
                    ).flatMap {
                      case Right(_) => Future.successful(result)
                      case Left(_) => Future.failed(new Error(s"Failed to set income source id in session storage. incomeSourceType: $incomeSourceType. incomeSourceType: $incomeSourceType"))
                    }

                  case Left(_) => Future.failed(new Error(s"Failed to set end date value in session storage. incomeSourceType: $incomeSourceType, incomeSourceType: $incomeSourceType"))
                }

              case _ =>
                val propertyEndDate = validatedInput.date.toString
                val result = Redirect(redirectAction)
                sessionService.setMongoKey(key = CeaseIncomeSourceData.dateCeasedField, value = propertyEndDate, journeyType = JourneyType(Cease, incomeSourceType)).flatMap {
                  case Right(_) => Future.successful(result)
                  case Left(exception) => Future.failed(exception)
                }
          }
      )
    }
  } recover {
    case ex: Exception =>
      val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"[IncomeSourceEndDateController][handleSubmitRequest]: Error getting IncomeSourceEndDate page: ${ex.getMessage}")
      errorHandler.showInternalServerError()
  }

  private def getFilledForm(form: Form[DateFormElement],
                            incomeSourceType: IncomeSourceType,
                            isChange: Boolean)(implicit user: MtdItUser[_]): Future[Form[DateFormElement]] = {

    if (isChange) {
      sessionService.getMongoKeyTyped[String](CeaseIncomeSourceData.dateCeasedField, JourneyType(Cease, incomeSourceType)).flatMap {
        case Right(Some(date)) =>
          Future.successful(
            form.fill(
              DateFormElement(
                LocalDate.parse(date)
              )
            ))
        case _ => Future.failed(new Exception(s"[IncomeSourceEndDateController][getFilledForm]: Error getting ${CeaseIncomeSourceData.dateCeasedField}:"))
      }
    } else {
      Future.successful(form)
    }
  }
}

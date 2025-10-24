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

package controllers.manageBusinesses.cease

import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.BeforeSubmissionPage
import enums.IncomeSourceJourney.{IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Cease, IncomeSourceJourneyType}
import forms.manageBusinesses.cease.CeaseIncomeSourceEndDateFormProvider

import models.core.IncomeSourceIdHash.mkFromQueryString
import models.core.{IncomeSourceId, IncomeSourceIdHash, Mode}
import models.incomeSourceDetails.CeaseIncomeSourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import views.html.manageBusinesses.cease.IncomeSourceEndDate

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceEndDateController @Inject()(val authActions: AuthActions,
                                              val incomeSourceEndDate: IncomeSourceEndDate,
                                              val sessionService: SessionService,
                                              form: CeaseIncomeSourceEndDateFormProvider,
                                              val itvcErrorHandler: ItvcErrorHandler,
                                              val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                             (implicit val appConfig: FrontendAppConfig,
                                              mcc: MessagesControllerComponents,
                                              val ec: ExecutionContext,
                                              dateService: DateService)
  extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses {

  private def getBackCall(isAgent: Boolean): Call = {
    if(isAgent) {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.showAgent()
    } else {
      controllers.manageBusinesses.routes.ManageYourBusinessesController.show()
    }
  }

  private def getPostAction(isAgent: Boolean, mode: Mode, maybeIncomeSourceId: Option[IncomeSourceId], incomeSourceType: IncomeSourceType
                           ): Call = {

    val hashedId: Option[String] = maybeIncomeSourceId.map(_.toHash.hash)

    routes.IncomeSourceEndDateController.submit(hashedId, incomeSourceType, isAgent, mode)

  }

  private def getRedirectCall(isAgent: Boolean, incomeSourceType: IncomeSourceType): Call = {
    if (isAgent) routes.CeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType)
    else routes.CeaseCheckIncomeSourceDetailsController.show(incomeSourceType)
  }

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  def show(id: Option[String], incomeSourceType: IncomeSourceType, isAgent: Boolean, mode: Mode):
  Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)

    handleRequest(
      isAgent = isAgent,
      incomeSourceType = incomeSourceType,
      id = incomeSourceIdHashMaybe,
      mode = mode
    )
  }

  def handleRequest(id: Option[IncomeSourceIdHash], isAgent: Boolean, mode: Mode, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] =
    withSessionData(IncomeSourceJourneyType(Cease, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>

      val hashCompareResult: Option[Either[Throwable, IncomeSourceId]] = id.map(x => user.incomeSources.compareHashToQueryString(x))

      hashCompareResult match {
        case Some(Left(exception: Exception)) => Future.failed(exception)
        case _ =>
          val incomeSourceIdMaybe: Option[IncomeSourceId] = IncomeSourceId.toOption(hashCompareResult)

          (incomeSourceType, id) match {
            case (SelfEmployment, None) =>
              Future.failed(new Exception(s"Missing income source ID for hash: <$id>"))

            case _ =>
              val dateStartedOpt = sessionData.ceaseIncomeSourceData.flatMap(_.endDate)
              val newForm = dateStartedOpt match {
                case Some(date) =>
                  form(incomeSourceType, incomeSourceIdMaybe.map(_.value)).fill(date)
                case None => form(incomeSourceType, incomeSourceIdMaybe.map(_.value))
              }
              Future.successful(Ok(
                incomeSourceEndDate(
                  form = newForm,
                  postAction = getPostAction(isAgent, mode, incomeSourceIdMaybe, incomeSourceType),
                  isAgent = isAgent,
                  backUrl = getBackCall(isAgent).url,
                  incomeSourceType = incomeSourceType
                )
              ))
          }
      }
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting IncomeSourceEndDate page: ${ex.getMessage} - ${ex.getCause}")
        errorHandler(isAgent).showInternalServerError()
    }

  def submit(id: Option[String], incomeSourceType: IncomeSourceType, isAgent: Boolean, mode: Mode):
  Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async { implicit user =>
    val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)

    handleSubmitRequest(
      isAgent = isAgent,
      incomeSourceType = incomeSourceType,
      id = incomeSourceIdHashMaybe,
      mode = mode
    )
  }

  private def handleValidatedInput(validatedInput: LocalDate,
                                   incomeSourceType: IncomeSourceType,
                                   incomeSourceIdMaybe: Option[IncomeSourceId],
                                   redirectAction: Call)(
                                    implicit headerCarrier: HeaderCarrier) = {
    (incomeSourceType, incomeSourceIdMaybe) match {
      case (SelfEmployment, Some(incomeSourceId)) =>
        val result = Redirect(redirectAction)
        val mongoSetValues = Map(
          CeaseIncomeSourceData.dateCeasedField -> validatedInput.toString,
          CeaseIncomeSourceData.incomeSourceIdField -> incomeSourceId.value
        )
        sessionService.setMultipleMongoData(mongoSetValues, IncomeSourceJourneyType(Cease, incomeSourceType)).flatMap {
          case Right(_) => Future.successful(result)
          case Left(_) => Future.failed(new Error(
            s"Failed to set data in session storage. incomeSourceType: $incomeSourceType."))
        }

      case _ =>
        val propertyEndDate = validatedInput.toString
        val result = Redirect(redirectAction)
        sessionService.setMongoKey(key = CeaseIncomeSourceData.dateCeasedField, value = propertyEndDate,
          incomeSources = IncomeSourceJourneyType(Cease, incomeSourceType)).flatMap {
          case Right(_) => Future.successful(result)
          case Left(exception) => Future.failed(exception)
        }
    }
  }

  def handleSubmitRequest(id: Option[IncomeSourceIdHash], isAgent: Boolean, incomeSourceType: IncomeSourceType, mode: Mode)
                         (implicit user: MtdItUser[_]): Future[Result] = {

    val hashCompareResult: Option[Either[Throwable, IncomeSourceId]] = id.map(x => user.incomeSources.compareHashToQueryString(x))

    hashCompareResult match {
      case Some(Left(exception: Exception)) => Future.failed(exception)
      case _ =>
        val incomeSourceIdMaybe: Option[IncomeSourceId] = IncomeSourceId.toOption(hashCompareResult)

        (incomeSourceType, id) match {
          case (SelfEmployment, None) =>
            Future.failed(new Exception(s"Missing income source ID for hash: <$id>"))
          case _ =>
            form(incomeSourceType, incomeSourceIdMaybe.map(_.value)).bindFromRequest().fold(
              hasErrors => {
                Future.successful(BadRequest(incomeSourceEndDate(
                  form = hasErrors,
                  postAction = getPostAction(isAgent, mode, incomeSourceIdMaybe, incomeSourceType),
                  backUrl = getBackCall(isAgent).url,
                  isAgent = isAgent,
                  incomeSourceType = incomeSourceType
                )))
              },
              validatedInput =>
                handleValidatedInput(
                  validatedInput,
                  incomeSourceType,
                  incomeSourceIdMaybe,
                  getRedirectCall(isAgent, incomeSourceType)
                )
            )
        }
    }
  } recover {
    case ex: Exception =>
      Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"Error getting IncomeSourceEndDate page: ${ex.getMessage} ${ex.getCause}")
      errorHandler(isAgent).showInternalServerError()
  }
}
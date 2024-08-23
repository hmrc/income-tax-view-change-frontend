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

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import enums.IncomeSourceJourney.{BeforeSubmissionPage, IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Cease, JourneyType}
import forms.incomeSources.cease.CeaseIncomeSourceEndDateFormProvider
import models.admin.IncomeSourcesNewJourney
import models.core.IncomeSourceIdHash.mkFromQueryString
import models.core.{IncomeSourceId, IncomeSourceIdHash}
import models.incomeSourceDetails.CeaseIncomeSourceData
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DateService, SessionService}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthenticatorPredicate, IncomeSourcesUtils, JourneyCheckerManageBusinesses}
import views.html.manageBusinesses.cease.IncomeSourceEndDate

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class IncomeSourceEndDateController @Inject()(val authorisedFunctions: FrontendAuthorisedFunctions,
                                              val incomeSourceEndDate: IncomeSourceEndDate,
                                              val sessionService: SessionService,
                                              form: CeaseIncomeSourceEndDateFormProvider,
                                              val auth: AuthenticatorPredicate)
                                             (implicit val appConfig: FrontendAppConfig,
                                              mcc: MessagesControllerComponents,
                                              val ec: ExecutionContext,
                                              implicit val dateService: DateService,
                                              val itvcErrorHandler: ItvcErrorHandler,
                                              val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils with JourneyCheckerManageBusinesses {

  private def getBackCall(isAgent: Boolean): Call = {
    controllers.manageBusinesses.routes.ManageYourBusinessesController.show(isAgent)
  }

  private def getPostAction(isAgent: Boolean, isChange: Boolean, maybeIncomeSourceId: Option[IncomeSourceId], incomeSourceType: IncomeSourceType
                           ): Call = {

    val hashedId: Option[String] = maybeIncomeSourceId.map(_.toHash.hash)

    routes.IncomeSourceEndDateController.submit(hashedId, incomeSourceType, isAgent, isChange)

  }

  private def getRedirectCall(isAgent: Boolean, incomeSourceType: IncomeSourceType): Call = {
    if (isAgent) routes.CeaseCheckIncomeSourceDetailsController.showAgent(incomeSourceType)
    else routes.CeaseCheckIncomeSourceDetailsController.show(incomeSourceType)
  }

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  def show(id: Option[String], incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean):
            Action[AnyContent] = auth.authenticatedAction(isAgent) { implicit user =>
            val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)

      handleRequest(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType,
        id = incomeSourceIdHashMaybe,
        isChange = isChange
      )
  }

  def handleRequest(id: Option[IncomeSourceIdHash], isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType)
                   (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] =
    withSessionData(JourneyType(Cease, incomeSourceType), journeyState = BeforeSubmissionPage) { sessionData =>

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
                  form(incomeSourceType, incomeSourceIdMaybe.map(_.value), isEnabled(IncomeSourcesNewJourney)).fill(date)
                case None => form(incomeSourceType, incomeSourceIdMaybe.map(_.value), isEnabled(IncomeSourcesNewJourney))
              }
              Future.successful(Ok(
                incomeSourceEndDate(
                  form = newForm,
                  postAction = getPostAction(isAgent, isChange, incomeSourceIdMaybe, incomeSourceType),
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

  def submit(id: Option[String], incomeSourceType: IncomeSourceType, isAgent: Boolean, isChange: Boolean):
              Action[AnyContent] = auth.authenticatedAction(isAgent) { implicit user =>
              val incomeSourceIdHashMaybe: Option[IncomeSourceIdHash] = id.flatMap(x => mkFromQueryString(x).toOption)

      handleSubmitRequest(
        isAgent = isAgent,
        incomeSourceType = incomeSourceType,
        id = incomeSourceIdHashMaybe,
        isChange = isChange
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
        sessionService.setMultipleMongoData(mongoSetValues, JourneyType(Cease, incomeSourceType)).flatMap {
          case Right(_) => Future.successful(result)
          case Left(_) => Future.failed(new Error(
            s"Failed to set data in session storage. incomeSourceType: $incomeSourceType."))
        }

      case _ =>
        val propertyEndDate = validatedInput.toString
        val result = Redirect(redirectAction)
        sessionService.setMongoKey(key = CeaseIncomeSourceData.dateCeasedField, value = propertyEndDate,
          journeyType = JourneyType(Cease, incomeSourceType)).flatMap {
          case Right(_) => Future.successful(result)
          case Left(exception) => Future.failed(exception)
        }
    }
  }

  def handleSubmitRequest(id: Option[IncomeSourceIdHash], isAgent: Boolean, incomeSourceType: IncomeSourceType, isChange: Boolean)
                         (implicit user: MtdItUser[_]): Future[Result] = withIncomeSourcesFS {

    val hashCompareResult: Option[Either[Throwable, IncomeSourceId]] = id.map(x => user.incomeSources.compareHashToQueryString(x))

    hashCompareResult match {
      case Some(Left(exception: Exception)) => Future.failed(exception)
      case _ =>
        val incomeSourceIdMaybe: Option[IncomeSourceId] = IncomeSourceId.toOption(hashCompareResult)

        (incomeSourceType, id) match {
          case (SelfEmployment, None) =>
            Future.failed(new Exception(s"Missing income source ID for hash: <$id>"))
          case _ =>
            form(incomeSourceType, incomeSourceIdMaybe.map(_.value), isEnabled(IncomeSourcesNewJourney)).bindFromRequest().fold(
              hasErrors => {
                Future.successful(BadRequest(incomeSourceEndDate(
                  form = hasErrors,
                  postAction = getPostAction(isAgent, isChange, incomeSourceIdMaybe, incomeSourceType),
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

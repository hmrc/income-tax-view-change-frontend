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

import audit.AuditingService
import auth.MtdItUser
import auth.authV2.AuthActions
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import enums.IncomeSourceJourney.{AfterSubmissionPage, IncomeSourceType, SelfEmployment}
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.incomeSources.add.IncomeSourceReportingMethodForm
import forms.manageBusinesses.add.IncomeSourceReportingFrequencyForm
import models.core.NormalMode
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import services.optIn.OptInService
import services.optout.OptOutService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.JourneyCheckerManageBusinesses
import viewUtils.ReportingFrequencyViewUtils
import views.html.errorPages.templates.ErrorTemplate
import views.html.manageBusinesses.add.IncomeSourceReportingFrequency

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceReportingFrequencyController @Inject()(val authActions: AuthActions,
                                                         val updateIncomeSourceService: UpdateIncomeSourceService,
                                                         val itsaStatusService: ITSAStatusService,
                                                         val calculationListService: CalculationListService,
                                                         val auditingService: AuditingService,
                                                         val view: IncomeSourceReportingFrequency,
                                                         val sessionService: SessionService,
                                                         val itvcErrorHandler: ItvcErrorHandler,
                                                         val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                         val errorTemplate: ErrorTemplate,
                                                         val optOutService: OptOutService,
                                                         val optInService: OptInService,
                                                         val reportingFrequencyViewUtils: ReportingFrequencyViewUtils)
                                                        (implicit val appConfig: FrontendAppConfig,
                                                         val dateService: DateService,
                                                         mcc: MessagesControllerComponents,
                                                         val ec: ExecutionContext
                                                        ) extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses {

  private lazy val errorHandler: Boolean => ShowInternalServerError = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  lazy val backUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    (isAgent, incomeSourceType.equals(SelfEmployment)) match {
      case (false, true) => routes.AddBusinessAddressController.show(NormalMode).url
      case (true, true) => routes.AddBusinessAddressController.showAgent(NormalMode).url
      case (_, _) => routes.AddIncomeSourceStartDateCheckController.show(isAgent, mode = NormalMode, incomeSourceType).url
    }

  lazy val errorRedirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showAgent(incomeSourceType).url
    else routes.IncomeSourceReportingMethodNotSavedController.show(incomeSourceType).url

  lazy val redirectUrl: (Boolean, IncomeSourceType) => String = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    if (isAgent) routes.IncomeSourceAddedController.showAgent(incomeSourceType).url
    else routes.IncomeSourceAddedController.show(incomeSourceType).url

  lazy val submitUrl: (Boolean, IncomeSourceType) => Call = (isAgent: Boolean, incomeSourceType: IncomeSourceType) =>
    controllers.manageBusinesses.add.routes.IncomeSourceReportingFrequencyController.submit(isAgent, incomeSourceType)

  def show(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      handleRequest(isAgent = isAgent, incomeSourceType)
  }

  def handleRequest(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    withNewIncomeSourcesFS {
      withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), journeyState = AfterSubmissionPage) { sessionData =>

        sessionData.addIncomeSourceData.flatMap(_.incomeSourceId) match {
          case Some(id) => handleIncomeSourceIdRetrievalSuccess(incomeSourceType, id, sessionData, isAgent = isAgent)
          case None =>
            val agentPrefix = if (isAgent) "[Agent]" else ""
            Logger("application").error(agentPrefix +
              s"Unable to retrieve incomeSourceId from session data for for $incomeSourceType")
            Future.successful {
              errorHandler(isAgent).showInternalServerError()
            }
        }
      }.recover {
        case ex: Exception =>
          Logger("application").error(
            "" +
              s"Unable to display IncomeSourceReportingMethod page for $incomeSourceType: ${ex.getMessage} ${ex.getCause}")
          errorHandler(isAgent).showInternalServerError()
      }
    }

  }


  private def handleIncomeSourceIdRetrievalSuccess(incomeSourceType: IncomeSourceType, id: String, sessionData: UIJourneySessionData, isAgent: Boolean)
                                                  (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    updateIncomeSourceAsAdded(sessionData).flatMap {
      case false => Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"Error retrieving data from session, IncomeSourceType: $incomeSourceType")
        Future.successful {
          errorHandler(isAgent).showInternalServerError()
        }
      case true =>
        itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear.flatMap {
          case true =>
            Future.successful(Ok(view(
              continueAction = submitUrl(isAgent, incomeSourceType),
              isAgent = isAgent,
              form = IncomeSourceReportingFrequencyForm(),
              incomeSourceType = incomeSourceType,
              taxDateService = dateService
            )))
          case false =>
            Future.successful {
              Redirect(redirectUrl(isAgent, incomeSourceType))
            }
        }
    }
  }

  private def updateIncomeSourceAsAdded(sessionData: UIJourneySessionData): Future[Boolean] = {
    val oldAddIncomeSourceSessionData = sessionData.addIncomeSourceData.getOrElse(AddIncomeSourceData())
    val updatedAddIncomeSourceSessionData = oldAddIncomeSourceSessionData.copy(incomeSourceAdded = Some(true))
    val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceSessionData))

    sessionService.setMongoData(uiJourneySessionData)
  }


  def submit(isAgent: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      handleSubmit(isAgent, incomeSourceType)
  }

  private def handleSubmit(isAgent: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), AfterSubmissionPage) { sessionData =>
      sessionData.addIncomeSourceData.flatMap(_.incomeSourceId) match {
        case Some(id) =>
          IncomeSourceReportingMethodForm.form.bindFromRequest().fold(
            invalid => handleInvalidForm(isAgent, incomeSourceType),
            valid => handleValidForm(isAgent)
          )
        case None =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix +
            s"Could not find an incomeSourceId in session data for $incomeSourceType")
          Future.successful {
            errorHandler(isAgent).showInternalServerError()
          }
      }
    }
  }

  private def handleInvalidForm(isAgent: Boolean, incomeSourceType: IncomeSourceType)
                               (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {


    IncomeSourceReportingFrequencyForm().bindFromRequest().fold(
      formWithError => {
        Future(
          BadRequest(view(
            continueAction = submitUrl(isAgent, incomeSourceType),
            isAgent = isAgent,
            form = formWithError,
            incomeSourceType = incomeSourceType,
            taxDateService = dateService
          ))
        )
      }, {
        _ =>
          Future.successful(
            Ok(view(
              continueAction = submitUrl(isAgent, incomeSourceType),
              isAgent = isAgent,
              form = IncomeSourceReportingFrequencyForm(),
              incomeSourceType = incomeSourceType,
              taxDateService = dateService
            ))
          )
      }
    )
  }

  private def handleValidForm(isAgent: Boolean): Future[Result] = {
    if (isAgent) {
      Future.successful(Redirect(controllers.routes.HomeController.showAgent()))
    } else {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    }
  }

}

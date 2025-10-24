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

import auth.MtdItUser
import auth.authV2.AuthActions
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import enums.AfterSubmissionPage
import enums.IncomeSourceJourney.IncomeSourceType
import enums.JourneyType.{Add, IncomeSourceJourneyType}
import forms.manageBusinesses.add.IncomeSourceReportingFrequencyForm
import models.admin.OptInOptOutContentUpdateR17
import models.incomeSourceDetails.{AddIncomeSourceData, UIJourneySessionData}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import services.manageBusinesses.IncomeSourceRFService
import services.optIn.OptInService
import services.optout.OptOutService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.{JourneyCheckerManageBusinesses, MtdConstants}
import viewUtils.ReportingFrequencyViewUtils
import views.html.errorPages.templates.ErrorTemplate
import views.html.manageBusinesses.add.IncomeSourceReportingFrequency

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class IncomeSourceReportingFrequencyController @Inject()(val authActions: AuthActions,
                                                         val updateIncomeSourceService: UpdateIncomeSourceService,
                                                         val view: IncomeSourceReportingFrequency,
                                                         val sessionService: SessionService,
                                                         val incomeSourceReportingFrequencyService: IncomeSourceRFService,
                                                         val errorTemplate: ErrorTemplate,
                                                         val optOutService: OptOutService,
                                                         val optInService: OptInService,
                                                         val reportingFrequencyViewUtils: ReportingFrequencyViewUtils)
                                                        (implicit val appConfig: FrontendAppConfig,
                                                         val dateService: DateService,
                                                         mcc: MessagesControllerComponents,
                                                         val ec: ExecutionContext
                                                        ) extends FrontendController(mcc) with I18nSupport with JourneyCheckerManageBusinesses with FeatureSwitching with MtdConstants {

  lazy val submitUrl: (Boolean, Boolean, IncomeSourceType) => Call = (isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType) =>
    controllers.manageBusinesses.add.routes.IncomeSourceReportingFrequencyController.submit(isAgent, isChange, incomeSourceType)

  def show(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user => handleGetRequest(isAgent, isChange, incomeSourceType)
    }

  def handleGetRequest(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType)(implicit user: MtdItUser[_]): Future[Result] = {
    incomeSourceReportingFrequencyService.redirectChecksForIncomeSourceRF(IncomeSourceJourneyType(Add, incomeSourceType),
      AfterSubmissionPage, incomeSourceType, dateService.getCurrentTaxYearEnd, isAgent, isChange) { sessionData =>
      handleIncomeSourceIdRetrievalSuccess(incomeSourceType, sessionData, isAgent, isChange, isEnabled(OptInOptOutContentUpdateR17))
    }
  }


  private def handleIncomeSourceIdRetrievalSuccess(incomeSourceType: IncomeSourceType,
                                                   sessionData: UIJourneySessionData,
                                                   isAgent: Boolean,
                                                   isChange: Boolean,
                                                   isR17ContentEnabled: Boolean
                                                  )
                                                  (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    updateIncomeSourceAsAdded(sessionData).flatMap {
      case false => Logger("application").error(s"${if (isAgent) "[Agent]"}" +
        s"Error retrieving data from session, IncomeSourceType: $incomeSourceType")
        Future.successful {
          incomeSourceReportingFrequencyService.errorHandler(isAgent).showInternalServerError()
        }
      case true =>
        val changeReportingFrequencyOption: Option[Boolean] = sessionData.addIncomeSourceData.flatMap(_.changeReportingFrequency)
        val filledOrEmptyForm: Form[IncomeSourceReportingFrequencyForm] = changeReportingFrequencyOption.fold(IncomeSourceReportingFrequencyForm(isR17ContentEnabled))(yesOrNo =>
          IncomeSourceReportingFrequencyForm(isR17ContentEnabled).fill(IncomeSourceReportingFrequencyForm(Some(yesOrNo.toString))))
        Future.successful(Ok(view(
          continueAction = submitUrl(isAgent, isChange, incomeSourceType),
          isAgent = isAgent,
          form = filledOrEmptyForm,
          incomeSourceType = incomeSourceType,
          taxDateService = dateService,
          isR17ContentEnabled = isR17ContentEnabled,
          mtdThreshold = getMtdThreshold
        )))
    }
  }

  private def updateIncomeSourceAsAdded(sessionData: UIJourneySessionData): Future[Boolean] = {
    val oldAddIncomeSourceSessionData = sessionData.addIncomeSourceData.getOrElse(AddIncomeSourceData())
    val updatedAddIncomeSourceSessionData = oldAddIncomeSourceSessionData.copy(incomeSourceAdded = Some(true))
    val uiJourneySessionData: UIJourneySessionData = sessionData.copy(addIncomeSourceData = Some(updatedAddIncomeSourceSessionData))

    sessionService.setMongoData(uiJourneySessionData)
  }

  def submit(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType): Action[AnyContent] =
    authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
      implicit user =>
        handleSubmit(isAgent, isChange, incomeSourceType, isEnabled(OptInOptOutContentUpdateR17))
    }

  private def handleSubmit(isAgent: Boolean, isChange: Boolean,
                           incomeSourceType: IncomeSourceType, isR17ContentEnabled: Boolean)(implicit user: MtdItUser[_]): Future[Result] = {
    withSessionData(IncomeSourceJourneyType(Add, incomeSourceType), AfterSubmissionPage) { sessionData: UIJourneySessionData =>
      sessionData.addIncomeSourceData.flatMap(_.incomeSourceId) match {
        case Some(_) => IncomeSourceReportingFrequencyForm(isR17ContentEnabled).bindFromRequest().fold(
          _ => handleInvalidForm(isAgent, isChange, incomeSourceType, isR17ContentEnabled),
          valid => handleValidForm(isAgent, isChange, valid, incomeSourceType, sessionData)
        )
        case None =>
          val agentPrefix = if (isAgent) "[Agent]" else ""
          Logger("application").error(agentPrefix +
            s"Could not find an incomeSourceId in session data for $incomeSourceType")
          Future.successful {
            incomeSourceReportingFrequencyService.errorHandler(isAgent).showInternalServerError()
          }
      }
    }
  }

  private def handleInvalidForm(isAgent: Boolean, isChange: Boolean, incomeSourceType: IncomeSourceType, isR17ContentEnabled: Boolean)
                               (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {


    IncomeSourceReportingFrequencyForm(isR17ContentEnabled).bindFromRequest().fold(
      formWithError => {
        Future(
          BadRequest(view(
            continueAction = submitUrl(isAgent, isChange, incomeSourceType),
            isAgent = isAgent,
            form = formWithError,
            incomeSourceType = incomeSourceType,
            taxDateService = dateService,
            isR17ContentEnabled = isR17ContentEnabled,
            mtdThreshold = getMtdThreshold
          ))
        )
      }, {
        _ =>
          Future.successful(
            Ok(view(
              continueAction = submitUrl(isAgent, isChange, incomeSourceType),
              isAgent = isAgent,
              form = IncomeSourceReportingFrequencyForm(isR17ContentEnabled),
              incomeSourceType = incomeSourceType,
              taxDateService = dateService,
              isR17ContentEnabled = isR17ContentEnabled,
              mtdThreshold = getMtdThreshold
            ))
          )
      }
    )
  }

  private def handleValidForm(isAgent: Boolean,
                              isChange: Boolean,
                              form: IncomeSourceReportingFrequencyForm,
                              incomeSourceType: IncomeSourceType,
                              sessionData: UIJourneySessionData
                             )
                             (implicit user: MtdItUser[_]): Future[Result] = {
    val yesOrNo = form.yesNo.exists(_.toBoolean)
    if (yesOrNo) {
      val updatedSessionData = sessionData.copy(
        addIncomeSourceData = sessionData.addIncomeSourceData.map(_.copy(changeReportingFrequency = Some(yesOrNo))))
      sessionService.setMongoData(updatedSessionData)
      Future.successful(Redirect(controllers.manageBusinesses.add.routes.ChooseTaxYearController.show(isAgent, false, incomeSourceType)))
    } else {
      sessionService.setMongoData(sessionData.copy(
        addIncomeSourceData = sessionData.addIncomeSourceData.map(_.copy(changeReportingFrequency = Some(yesOrNo))),
        incomeSourceReportingFrequencyData = None))
      Future.successful(Redirect(controllers.manageBusinesses.add.routes.IncomeSourceRFCheckDetailsController.show(isAgent, incomeSourceType)))
    }
  }
}

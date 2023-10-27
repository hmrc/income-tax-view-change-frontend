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
import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.ForeignProperty
import forms.incomeSources.add.AddForeignPropertyReportingMethodForm
import models.incomeSourceDetails.LatencyDetails
import models.incomeSourceDetails.viewmodels.ForeignPropertyReportingMethodViewModel
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponse, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services._
import utils.IncomeSourcesUtils
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.ForeignPropertyReportingMethod

import javax.inject.Inject
import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class ForeignPropertyReportingMethodController @Inject()(val authenticate: AuthenticationPredicate,
                                                         val authorisedFunctions: FrontendAuthorisedFunctions,
                                                         val checkSessionTimeout: SessionTimeoutPredicate,
                                                         val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                         val retrieveBtaNavBar: NavBarPredicate,
                                                         val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                         val retrieveNino: NinoPredicate,
                                                         val foreignPropertyReportingMethodView: ForeignPropertyReportingMethod,
                                                         val updateIncomeSourceService: UpdateIncomeSourceService,
                                                         val itsaStatusService: ITSAStatusService,
                                                         val dateService: DateService,
                                                         val calculationListService: CalculationListService,
                                                         val customNotFoundErrorView: CustomNotFoundError)
                                                        (implicit val appConfig: FrontendAppConfig,
                                                         override implicit val mcc: MessagesControllerComponents,
                                                         val ec: ExecutionContext,
                                                         val itvcErrorHandler: ItvcErrorHandler,
                                                         val itvcErrorHandlerAgent: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport with IncomeSourcesUtils {

  def show(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        id = id,
        isAgent = false,
        postAction = postAction(id),
        redirectCall = redirectCall(id)
      )
  }

  def showAgent(id: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(
              id = id,
              isAgent = true,
              postAction = postActionAgent(id),
              redirectCall = redirectCallAgent(id)
            )
        }
  }

  def submit(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(
        id = id,
        isAgent = false,
        postAction = postAction(id),
        redirectCall = redirectCall(id),
        errorCall = redirectErrorCall
      )
  }

  def submitAgent(id: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(
              id = id,
              isAgent = true,
              postAction = postActionAgent(id),
              redirectCall = redirectCallAgent(id),
              errorCall = redirectErrorCallAgent
            )
        }
  }

  private def handleRequest(id: String,
                            isAgent: Boolean,
                            postAction: Call,
                            redirectCall: Call)
                           (implicit user: MtdItUser[_]): Future[Result] = {
    withIncomeSourcesFS {
      (for {
        isMandatoryOrVoluntary <- itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear
        latencyDetailsMaybe <- Future(user.incomeSources.properties.find(
          propertyDetails => propertyDetails.incomeSourceId.contains(id) && propertyDetails.isForeignProperty
        ).flatMap(_.latencyDetails))
        viewModel <- getForeignPropertyReportingMethodDetails(latencyDetailsMaybe)
      } yield {
        (isMandatoryOrVoluntary, viewModel) match {

          case (_, Left(ex)) =>
            Logger("application")
              .error(s"[ForeignPropertyReportingMethodController][handleRequest]: Failed with error - $ex")
            Future.successful(Redirect(redirectCall))
          case (true, Right(viewModel)) =>
            Future.successful(Ok(foreignPropertyReportingMethodView(
              form = AddForeignPropertyReportingMethodForm.form,
              viewModel = viewModel,
              postAction = postAction,
              isAgent = isAgent
            )))
          case _ =>
            Logger("application")
              .error(s"[ForeignPropertyReportingMethodController][handleRequest]: second level not found error")
            Future(Ok(customNotFoundErrorView()))
        }
      }).flatten
    }
  }

  private def handleSubmitRequest(id: String,
                                  isAgent: Boolean,
                                  postAction: Call,
                                  redirectCall: Call,
                                  errorCall: Call)
                                 (implicit user: MtdItUser[_]): Future[Result] = {

    withIncomeSourcesFS {
      AddForeignPropertyReportingMethodForm.form.bindFromRequest().fold(
        formWithErrors => handleFormErrors(formWithErrors, id, isAgent),
        valid => handleFormData(valid, id, isAgent))
    }
  }

  private def handleFormErrors(errors: Form[AddForeignPropertyReportingMethodForm], id: String, isAgent: Boolean)
                              (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    val postAction = if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submitAgent(id) else
      controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submit(id)
    val redirectCall = if (isAgent) controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(id, ForeignProperty) else
      controllers.incomeSources.add.routes.IncomeSourceAddedController.show(id, ForeignProperty)

    for {
      latencyDetailsMaybe <- Future(user.incomeSources.properties
        .find(_.incomeSourceId.contains(id))
        .flatMap(_.latencyDetails))
      fPropertyReportingMethodViewModel <- getForeignPropertyReportingMethodDetails(latencyDetailsMaybe)
    } yield {
      fPropertyReportingMethodViewModel match {
        case Right(viewModel) =>
          BadRequest(foreignPropertyReportingMethodView(
            form = AddForeignPropertyReportingMethodForm.updateErrorMessagesWithValues(errors),
            viewModel = viewModel,
            postAction = postAction,
            isAgent = isAgent
          ))
        case Left(ex) =>
          Logger("application")
            .error(s"[ForeignPropertyReportingMethodController][handleRequest]: " +
              s"Failed to retrieve latency details - $ex")
          Redirect(redirectCall)
      }
    }
  }

  private def handleFormData(form: AddForeignPropertyReportingMethodForm, id: String, isAgent: Boolean)
                            (implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {

    val redirectUrl: Call = if (isAgent) routes.IncomeSourceAddedController.showAgent(id, ForeignProperty) else routes.IncomeSourceAddedController.show(id, ForeignProperty)

    if (form.reportingMethodIsChanged) {
      val newReportingMethods: Seq[TaxYearSpecific] = Seq(
        getSelectedReportingMethodValues(form.taxYear1ReportingMethod, form.newTaxYear1ReportingMethod, form.taxYear1),
        getSelectedReportingMethodValues(form.taxYear2ReportingMethod, form.newTaxYear2ReportingMethod, form.taxYear2)
      ).flatten

      updateReportingMethod(isAgent, id, newReportingMethods)

    } else {
      Future.successful(Redirect(redirectUrl))
    }
  }

  private def getSelectedReportingMethodValues(existingMethod: Option[String], newMethod: Option[String], taxYear: Option[String]): Option[TaxYearSpecific] = {
    (existingMethod, newMethod, taxYear) match {
      case (Some(existing), Some(newMethod), Some(taxYear)) if existing != newMethod =>
        Some(TaxYearSpecific(taxYear, isAnnualReporting(newMethod)))
      case _ =>
        None
    }
  }

  private def updateReportingMethod(isAgent: Boolean, id: String, newReportingMethods: Seq[TaxYearSpecific])
                                   (implicit user: MtdItUser[_]): Future[Result] = {

    val redirectUrl: Call = if (isAgent) routes.IncomeSourceAddedController.showAgent(id, incomeSourceType = ForeignProperty) else routes.IncomeSourceAddedController.show(id, incomeSourceType = ForeignProperty)
    val redirectErrorUrl: Call = if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showAgent(id = id, incomeSourceType = ForeignProperty) else
      routes.IncomeSourceReportingMethodNotSavedController.show(id = id, incomeSourceType = ForeignProperty)

    val futures = newReportingMethods.map(taxYearSpecific =>
      updateIncomeSourceService.updateTaxYearSpecific(user.nino, id, taxYearSpecific))

    val updateResults: Future[Seq[UpdateIncomeSourceResponse]] = Future.sequence(futures)

    updateResults.map { results =>
      manageReportingMethodUpdateResponses(results, redirectUrl) match {
        case Left(ex) => Logger("application").error(s"[ForeignPropertyReportingMethodController][updateReportingMethod]: " +
          s"Unable to update tax year specific reporting method: ${ex.getMessage}")
          Redirect(redirectErrorUrl)
        case Right(redirectCall) => redirectCall
      }
    }.recover {
      case ex: Exception =>
        Logger("application").error(s"[ForeignPropertyReportingMethodController][updateReportingMethod]: " +
          s"Error updating tax year specific reporting method: ${ex.getMessage}")
        Redirect(redirectErrorUrl)
    }
  }

  @tailrec
  private def manageReportingMethodUpdateResponses(results: Seq[UpdateIncomeSourceResponse], redirectUrl: Call): Either[Throwable, Result] = {
    results match {
      case head :: Nil =>
        head match {
          case UpdateIncomeSourceResponseModel(_) => Right(Redirect(redirectUrl))
          case UpdateIncomeSourceResponseError(status, reason) => Left(new Error(s"Error response received when updating tax year specific reporting method: status: $status, reason: $reason"))
        }
      case head :: tail =>
        head match {
          case UpdateIncomeSourceResponseModel(_) => manageReportingMethodUpdateResponses(tail, redirectUrl)
          case UpdateIncomeSourceResponseError(status, reason) => Left(new Error(s"Error response received when updating tax year specific reporting method: status: $status, reason: $reason"))
        }
      case _ => Left(new Error("No responses received when updating tax year specific reporting methods"))
    }
  }

  private def getForeignPropertyReportingMethodDetails(latencyDetailsMaybe: Option[LatencyDetails])
                                                      (implicit user: MtdItUser[_]): Future[Either[Throwable, ForeignPropertyReportingMethodViewModel]] = {

    val currentTaxYearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
    latencyDetailsMaybe match {
      case Some(latencyDetails) if Try(latencyDetails.taxYear1.toInt).toOption.isDefined =>
        latencyDetails match {
          case _ if Try(latencyDetails.taxYear2.toInt).toOption.isEmpty =>
            Future.successful(Left(new Error(s"Unable to convert taxYear2 to Int: ${latencyDetails.taxYear2}")))
          case _ if latencyDetails.taxYear2.toInt < currentTaxYearEnd =>
            Future.successful(Left(new Error("Current tax year not in scope of change period")))
          case LatencyDetails(_, tY1, tY1LatencyIndicator, tY2, tY2LatencyIndicator) =>
            calculationListService.isTaxYearCrystallised(tY1.toInt, isEnabled(TimeMachineAddYear)).flatMap {
              case Some(true) =>
                Future.successful(
                  Right(
                    ForeignPropertyReportingMethodViewModel(
                      taxYear2 = Some(tY2),
                      latencyIndicator2 = Some(tY2LatencyIndicator)
                    )
                  )
                )
              case _ =>
                Future.successful(
                  Right(
                    ForeignPropertyReportingMethodViewModel(
                      taxYear1 = Some(tY1),
                      latencyIndicator1 = Some(tY1LatencyIndicator),
                      taxYear2 = Some(tY2),
                      latencyIndicator2 = Some(tY2LatencyIndicator)
                    )
                  )
                )
            }
        }
      case Some(latencyDetails) =>
        Future(Left(new Error(s"Unable to convert taxYear1 to Int: ${latencyDetails.taxYear1}")))
      case None =>
        Future.successful(Left(new Error("Latency details are not provided")))
    }
  }

  private def isAnnualReporting(taxYearReportingMethod: String): Boolean = taxYearReportingMethod.toUpperCase().equals("A")

  private def postAction(id: String) = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submit(id)

  private def postActionAgent(id: String) = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submitAgent(id)

  private def redirectCall(id: String) = controllers.incomeSources.add.routes.IncomeSourceAddedController.show(id, incomeSourceType = ForeignProperty)

  private def redirectCallAgent(id: String) = controllers.incomeSources.add.routes.IncomeSourceAddedController.showAgent(id, incomeSourceType = ForeignProperty)

  val redirectErrorCall: Call = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.show(ForeignProperty)
  val redirectErrorCallAgent: Call = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showAgent(ForeignProperty)

}
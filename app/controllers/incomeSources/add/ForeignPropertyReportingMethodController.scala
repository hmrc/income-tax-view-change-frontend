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
    val redirectCall = if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyAddedController.showAgent(id) else
      controllers.incomeSources.add.routes.ForeignPropertyAddedController.show(id)

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

    val redirectUrl: Call = if (isAgent) routes.ForeignPropertyAddedController.showAgent(id) else routes.ForeignPropertyAddedController.show(id)

    if (form.reportingMethodIsChanged) {
      val taxYearSpecific1Opt = for {
        newReportingMethod <- form.newTaxYear1ReportingMethod
        taxYear <- form.taxYear1
      } yield TaxYearSpecific(taxYear, isAnnualReporting(newReportingMethod))

      val taxYearSpecific2Opt = for {
        newReportingMethod <- form.newTaxYear2ReportingMethod
        taxYear <- form.taxYear2
      } yield TaxYearSpecific(taxYear, isAnnualReporting(newReportingMethod))

      updateReportingMethod(isAgent, id, taxYearSpecific1Opt, taxYearSpecific2Opt)

    } else {
      Future.successful(Redirect(redirectUrl))
    }
  }

  private def updateReportingMethod(isAgent: Boolean, id: String, taxYearSpecific1Opt: Option[TaxYearSpecific], taxYearSpecific2Opt: Option[TaxYearSpecific])
                                   (implicit user: MtdItUser[_]): Future[Result] = {

    val redirectUrl: Call = if (isAgent) routes.ForeignPropertyAddedController.showAgent(id) else routes.ForeignPropertyAddedController.show(id)
    val redirectErrorUrl: Call = if (isAgent) routes.IncomeSourceReportingMethodNotSavedController.showForeignPropertyAgent() else
      routes.IncomeSourceReportingMethodNotSavedController.showForeignProperty()

    val futures = Seq(
      taxYearSpecific1Opt.map(taxYearSpecific => updateIncomeSourceService.updateTaxYearSpecific(user.nino, id, taxYearSpecific)),
      taxYearSpecific2Opt.map(taxYearSpecific => updateIncomeSourceService.updateTaxYearSpecific(user.nino, id, taxYearSpecific))
    ).flatten

    val updateResults: Future[Seq[UpdateIncomeSourceResponse]] = Future.sequence(futures)

    updateResults.map { results =>
      val responseCount = results.length

      responseCount match {
        case 0 =>
          Logger("application").error("[UKPropertyReportingMethodController][updateReportingMethod]: " +
            "No responses received when updating tax year specific reporting methods")
          Redirect(redirectErrorUrl)
        case 1 =>
          val result = results.head
          result match {
            case _: UpdateIncomeSourceResponseModel =>
              Logger("application").info(s"[UKPropertyReportingMethodController][updateReportingMethod]: " +
                s"Updated tax year specific reporting method: $result")
              Redirect(redirectUrl)
            case _: UpdateIncomeSourceResponseError =>
              Logger("application").info(s"[UKPropertyReportingMethodController][updateReportingMethod]: " +
                s"Error response received when updating tax year specific reporting method: $result")
              Redirect(redirectErrorUrl)
            case _ =>
              Logger("application").info(s"[UKPropertyReportingMethodController][updateReportingMethod]: " +
                s"Unexpected response received when updating tax year specific reporting method: $result")
              Redirect(redirectErrorUrl)
          }
        case 2 =>
          val (result1, result2) = (results.head, results(1))
          (result1, result2) match {
            case (_: UpdateIncomeSourceResponseError, _: UpdateIncomeSourceResponseError) =>
              Logger("application").info(s"[UKPropertyReportingMethodController][updateReportingMethod]: " +
                s"Errors received when updating tax year specific reporting methods: $result1\n$result2")
              Redirect(redirectErrorUrl)
            case (_: UpdateIncomeSourceResponseModel, _: UpdateIncomeSourceResponseError) =>
              Logger("application").info(s"[UKPropertyReportingMethodController][updateReportingMethod]: " +
                s"Updated tax year specific reporting method: $result1")
              Logger("application").info(s"[UKPropertyReportingMethodController][updateReportingMethod]: " +
                s"Error received when updating tax year specific reporting method: $result2")
              //TODO: redirect to a new error page based on 1 success, 1 error
              Redirect(redirectErrorUrl)
            case (_: UpdateIncomeSourceResponseError, _: UpdateIncomeSourceResponseModel) =>
              Logger("application").info(s"[UKPropertyReportingMethodController][updateReportingMethod]: " +
                s"Error received when updating tax year specific reporting method: $result2")
              Logger("application").info(s"[UKPropertyReportingMethodController][updateReportingMethod]: " +
                s"Updated tax year specific reporting method: $result1")
              //TODO: redirect to a new error page based on 1 success, 1 error
              Redirect(redirectErrorUrl)
            case (_: UpdateIncomeSourceResponseModel, _: UpdateIncomeSourceResponseModel) =>
              Logger("application").info(s"[UKPropertyReportingMethodController][updateReportingMethod]: " +
                s"Updated tax year specific reporting methods: $result1\n$result2")
              Redirect(redirectUrl)
          }
        case _ =>
          Logger("application").error("[UKPropertyReportingMethodController][updateReportingMethod]: " +
            "Unexpected response received when updating tax year specific reporting methods")
          Redirect(redirectErrorUrl)
      }
    }.recover {
      case ex: Exception =>
        Logger("application").error(s"[UKPropertyReportingMethodController][updateReportingMethod]: " +
          s"Error updating tax year specific reporting method: ${ex.getMessage}")
        Redirect(redirectErrorUrl)
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
            calculationListService.isTaxYearCrystallised(tY1.toInt).flatMap {
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

  private def redirectCall(id: String) = controllers.incomeSources.add.routes.ForeignPropertyAddedController.show(id)

  private def redirectCallAgent(id: String) = controllers.incomeSources.add.routes.ForeignPropertyAddedController.showAgent(id)

  val redirectErrorCall: Call = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showForeignProperty()
  val redirectErrorCallAgent: Call = controllers.incomeSources.add.routes.IncomeSourceNotAddedController.showForeignPropertyAgent()

}
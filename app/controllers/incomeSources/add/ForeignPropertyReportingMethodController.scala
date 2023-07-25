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
import config.featureswitch.{FeatureSwitching, IncomeSources, TimeMachineAddYear}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler, ShowInternalServerError}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.incomeSources.add.{AddBusinessReportingMethodForm, AddForeignPropertyReportingMethodForm}
import models.incomeSourceDetails.{LatencyDetails, PropertyDetailsModel}
import models.incomeSourceDetails.viewmodels.{BusinessReportingMethodViewModel, ForeignPropertyReportingMethodViewModel}
import models.updateIncomeSource.{TaxYearSpecific, UpdateIncomeSourceResponseError, UpdateIncomeSourceResponseModel}
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc._
import services._
import uk.gov.hmrc.http.HeaderCarrier
import views.html.errorPages.CustomNotFoundError
import views.html.incomeSources.add.{BusinessReportingMethod, ForeignPropertyReportingMethod}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
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
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

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
    (for {
      isMandatoryOrVoluntary <- itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear
      latencyDetailsMaybe <- Future(user.incomeSources.properties.find(
        propertyDetails => propertyDetails.incomeSourceId.contains(id) && propertyDetails.isForeignProperty
      ).flatMap(_.latencyDetails))
      viewModel <- getForeignPropertyReportingMethodDetails(latencyDetailsMaybe)
    } yield {
      (isEnabled(IncomeSources), isMandatoryOrVoluntary, viewModel) match {
        case (false, _, _) =>
          Logger("application")
            .error(s"[ForeignPropertyReportingMethodController][handleRequest]: not found error")
          Future(Ok(customNotFoundErrorView()))
        case (_, _, Left(ex)) =>
          Logger("application")
            .error(s"[ForeignPropertyReportingMethodController][handleRequest]: Failed with error - $ex")
          Future.successful(Redirect(redirectCall))
        case (_, true, Right(viewModel)) =>
          Future(Ok(foreignPropertyReportingMethodView(
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

    private def handleSubmitRequest(id: String,
                                    isAgent: Boolean,
                                    postAction: Call,
                                    redirectCall: Call,
                                    errorCall: Call)
                                   (implicit user: MtdItUser[_]): Future[Result] = {

      if (isEnabled(IncomeSources)) {
        AddForeignPropertyReportingMethodForm.form.bindFromRequest().fold(
          formWithErrors => {
            for {
              latencyDetailsMaybe <- Future(user.incomeSources.properties
                .find(_.incomeSourceId.contains(id))
                .flatMap(_.latencyDetails))
              fPropertyReportingMethodViewModel <- getForeignPropertyReportingMethodDetails(latencyDetailsMaybe)
            } yield {
              fPropertyReportingMethodViewModel match {
                case Right(viewModel) =>
                  BadRequest(foreignPropertyReportingMethodView(
                    form = AddForeignPropertyReportingMethodForm.updateErrorMessagesWithValues(formWithErrors),
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
          },
          valid => {

            if (valid.reportingMethodIsChanged) {

              val updatedReportingMethods = List(
                getTaxYearSpecific(valid.taxYear1, valid.taxYear1ReportingMethod),
                getTaxYearSpecific(valid.taxYear2, valid.taxYear2ReportingMethod)
              ).flatten

              updateIncomeSourceService.updateTaxYearSpecific(
                nino = user.nino,
                incomeSourceId = id,
                taxYearSpecific = updatedReportingMethods
              ).map {
                case res: UpdateIncomeSourceResponseModel =>
                  Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updated tax year specific reporting method : $res")
                  Redirect(redirectCall)
                case err: UpdateIncomeSourceResponseError =>
                  Logger("application").error(s"${if (isAgent) "[Agent]"}" + s" Failed to Updated tax year specific reporting method : $err")
                  Redirect(errorCall)
              }
            } else {
              Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updating the tax year specific reporting method not required.")
              Future.successful(Redirect(redirectCall))
            }
          })
      } else Future(Ok(customNotFoundErrorView()))
    }

  private def getForeignPropertyReportingMethodDetails(latencyDetailsMaybe: Option[LatencyDetails])
                                                      (implicit user: MtdItUser[_]): Future[Either[Throwable, ForeignPropertyReportingMethodViewModel]] = {

    val currentTaxYearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
    latencyDetailsMaybe match {
      case Some(latencyDetails) if Try(latencyDetails.taxYear1.toInt).toOption.isDefined =>
        calculationListService.isTaxYearCrystallised(latencyDetails.taxYear1.toInt).flatMap { isTaxYear1Crystallised =>
          (latencyDetails, isTaxYear1Crystallised) match {
            case _ if Try(latencyDetails.taxYear2.toInt).toOption.isEmpty =>
              Future.successful( Left( new Error(s"Unable to convert taxYear2 to Int: ${latencyDetails.taxYear2}") ) )
            case _ if latencyDetails.taxYear2.toInt < currentTaxYearEnd =>
              Future.successful( Left( new Error("Current tax year not in scope of change period") ) )
            case (_, Some(true)) =>
              Future.successful(
                Right(
                  ForeignPropertyReportingMethodViewModel(
                    taxYear2 = Some(latencyDetails.taxYear2),
                  latencyIndicator2 = Some(latencyDetails.latencyIndicator2)
                )
                )
              )
            case _ =>
              Future.successful(
                Right(ForeignPropertyReportingMethodViewModel(
                taxYear1 = Some(latencyDetails.taxYear1),
                latencyIndicator1 = Some(latencyDetails.latencyIndicator1),
                taxYear2 = Some(latencyDetails.taxYear2),
                latencyIndicator2 = Some(latencyDetails.taxYear2)
              )))
          }
        }
      case Some(latencyDetails) =>
        Future(Left(new Error(s"Unable to convert taxYear1 to Int: ${latencyDetails.taxYear1}")))
      case None =>
        Future.successful(Left(new Error("Latency details are not provided")))
    }
  }

  private def isAnnualReporting(taxYearReportingMethod: String): Boolean = taxYearReportingMethod.toUpperCase().equals("A")

  private def getTaxYearSpecific(taxYearMaybe: Option[String], reportingMethod: Option[String]): Option[TaxYearSpecific] = for {
    taxYear <- taxYearMaybe
    taxYearReportingMethod <- reportingMethod
  } yield {
    TaxYearSpecific(
      taxYear = taxYear,
      latencyIndicator = isAnnualReporting(taxYearReportingMethod)
    )
  }

  private def postAction(id: String) = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submit(id)
  private def postActionAgent(id: String) = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submitAgent(id)

  private def redirectCall(id: String) = controllers.incomeSources.add.routes.ForeignPropertyAddedObligationsController.show(id)
  private def redirectCallAgent(id: String) = controllers.incomeSources.add.routes.ForeignPropertyAddedObligationsController.showAgent(id)

  val redirectErrorCall: Call = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodErrorController.show()
  val redirectErrorCallAgent: Call = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodErrorController.showAgent()

}
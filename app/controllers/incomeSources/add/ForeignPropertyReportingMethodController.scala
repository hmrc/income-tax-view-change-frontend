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

  private def annualQuarterlyToBoolean(method: Option[String]): Option[Boolean] = method match {
    case Some("A") => Some(true)
    case Some("Q") => Some(false)
    case _ => None
  }

  private def getUKPropertyReportingMethodDetails(incomeSourceId: String)
                                                 (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext)
  : Future[Option[ForeignPropertyReportingMethodViewModel]] = {
    val latencyDetails: Option[LatencyDetails] = user.incomeSources.properties
      .filter(_.isUkProperty).find(_.incomeSourceId.getOrElse("").equals(incomeSourceId)).flatMap(_.latencyDetails)

    latencyDetails match {
      case Some(x) =>
        val currentTaxYearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
        x match {
          case LatencyDetails(_, _, _, taxYear2, _) if taxYear2.toInt < currentTaxYearEnd => Future.successful(None)
          case LatencyDetails(_, taxYear1, taxYear1LatencyIndicator, taxYear2, taxYear2LatencyIndicator) =>
            calculationListService.isTaxYearCrystallised(taxYear1.toInt).flatMap {
              case Some(true) =>
                Future.successful(Some(ForeignPropertyReportingMethodViewModel(None, None, Some(taxYear2), Some(taxYear2LatencyIndicator))))
              case _ =>
                Future.successful(Some(ForeignPropertyReportingMethodViewModel(Some(taxYear1),
                  Some(taxYear1LatencyIndicator), Some(taxYear2), Some(taxYear2LatencyIndicator))))
            }
        }
      case None =>
        Logger("application").info(s"[UKPropertyReportingMethodController][getUKPropertyReportingMethodDetails] latency details not available")
        Future.successful(None)
    }

  }

  private def handleRequest(isAgent: Boolean, id: String)
                           (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {

    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val postAction: Call = if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submitAgent(id) else
      controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submit(id)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val redirectUrl: Call = if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyAddedController.showAgent(id) else
      controllers.incomeSources.add.routes.ForeignPropertyAddedController.show(id)

    if (incomeSourcesEnabled) {
      itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear.flatMap {
        case true =>
          getUKPropertyReportingMethodDetails(id).map {
            case Some(viewModel) =>
              Ok(foreignPropertyReportingMethodView(
                form = AddForeignPropertyReportingMethodForm.form,
                viewModel = viewModel,
                postAction = postAction,
                isAgent = isAgent)(user, messages))
            case None =>
              Redirect(redirectUrl)
          }

        case false => Future.successful(Redirect(redirectUrl))
      }

    } else {
      Future.successful(Ok(customNotFoundErrorView()(user, messages)))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"${if (isAgent) "[Agent]"}" +
          s"Error getting ForeignPropertyReportingMethodController page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }
  }

  private def handleSubmitRequest(isAgent: Boolean, id: String)(implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    val incomeSourcesEnabled: Boolean = isEnabled(IncomeSources)
    val errorHandler: ShowInternalServerError = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
    val redirectUrl: Call = if (isAgent) routes.ForeignPropertyAddedController.showAgent(id) else routes.ForeignPropertyAddedController.show(id)
    val redirectErrorUrl: Call = if (isAgent) routes.ForeignPropertyReportingMethodErrorController.showAgent() else routes.ForeignPropertyReportingMethodErrorController.show()
    val submitUrl: Call = if (isAgent) controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submitAgent(id) else
      controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submit(id)

    if (incomeSourcesEnabled) {
      AddForeignPropertyReportingMethodForm.form.bindFromRequest().fold(
        hasErrors => {
          val updatedForm = AddForeignPropertyReportingMethodForm.updateErrorMessagesWithValues(hasErrors)
          getUKPropertyReportingMethodDetails(id).map {
            case Some(viewModel) =>
              BadRequest(foreignPropertyReportingMethodView(
                form = updatedForm,
                viewModel = viewModel,
                postAction = submitUrl,
                isAgent = isAgent))
            case None => Redirect(redirectUrl)
          }
        },
        valid => {
          val taxYear1ReportingMethod = valid.taxYear1ReportingMethod
          val taxYear2ReportingMethod = valid.taxYear2ReportingMethod
          val newTaxYear1ReportingMethod = valid.newTaxYear1ReportingMethod
          val newTaxYear2ReportingMethod = valid.newTaxYear2ReportingMethod

          if (taxYear1ReportingMethod != newTaxYear1ReportingMethod || taxYear2ReportingMethod != newTaxYear2ReportingMethod) {
            val taxYearSpecific1 = newTaxYear1ReportingMethod match {
              case Some(s) => Some(TaxYearSpecific(valid.taxYear1.get, annualQuarterlyToBoolean(Some(s)).get))
              case _ => None
            }
            val taxYearSpecific2 = newTaxYear2ReportingMethod match {
              case Some(s) => Some(TaxYearSpecific(valid.taxYear2.get, annualQuarterlyToBoolean(Some(s)).get))
              case _ => None
            }
            updateIncomeSourceService.updateTaxYearSpecific(user.nino, id, List(taxYearSpecific1, taxYearSpecific2).flatten).map {
              case res: UpdateIncomeSourceResponseModel =>
                Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updated tax year specific reporting method : $res")
                Redirect(redirectUrl)
              case err: UpdateIncomeSourceResponseError =>
                Logger("application").error(s"${if (isAgent) "[Agent]"}" + s" Failed to Updated tax year specific reporting method : $err")
                Redirect(redirectErrorUrl)
            }
          } else {
            Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updating the tax year specific reporting method not required.")
            Future(Redirect(redirectUrl))
          }
        })
    } else {
      Future.successful(Ok(customNotFoundErrorView()))
    } recover {
      case ex: Exception =>
        Logger("application").error(s"Error getting UKPropertyReportingMethodController page: ${ex.getMessage}")
        errorHandler.showInternalServerError()
    }

  }

  def show(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>

      println(s"\nSHOW: ALL user properties ON PAGE LOAD: ${user.incomeSources.properties}\n")

      println(s"\nSHOW: UK PROPERTIES ON PAGE LOAD: ${user.incomeSources.properties.find(_.isUkProperty)}\n")

      println(s"\nmaybeLatencyDetails: ${user.incomeSources.properties.filter(_.isUkProperty).find(_.incomeSourceId.getOrElse("").equals(id))}\n")

      handleRequest(isAgent = false, id = id)
  }

  def showAgent(id: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser =>
            handleRequest(isAgent = true, id = id)
        }
  }

  def submit(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleSubmitRequest(isAgent = false, id = id)
  }

  def submitAgent(id: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
          implicit mtdItUser => handleSubmitRequest(isAgent = true, id = id)
        }
  }


//  def show(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
//    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
//    implicit user =>
//
//      println(s"\nSHOW: ALL user properties ON PAGE LOAD: ${user.incomeSources.properties}\n")
//
//      println(s"\nSHOW: FOREIGN PROPERTIES ON PAGE LOAD: ${user.incomeSources.properties.find(_.isForeignProperty)}\n")
//
//      println(s"\nmaybeLatencyDetails: ${user.incomeSources.properties.filter(_.isForeignProperty).find(_.incomeSourceId.getOrElse("").equals(id))}\n")
//
//      handleRequest(
//        id = id,
//        isAgent = false,
//        postAction = postAction(id),
//        redirectCall = redirectCall(id)
//      )
//  }
//
//  def showAgent(id: String): Action[AnyContent] = Authenticated.async {
//    implicit request =>
//      implicit user =>
//        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
//          implicit mtdItUser =>
//            handleRequest(
//              id = id,
//              isAgent = true,
//              postAction = postActionAgent(id),
//              redirectCall = redirectCallAgent(id)
//            )
//        }
//  }
//
//  def submit(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
//    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
//    implicit user =>
//      println(s"HANDLE SUBMIT REQUEST: user properties: ${user.incomeSources.properties}")
//
//      handleSubmitRequest(
//        id = id,
//        isAgent = false,
//        postAction = postAction(id),
//        redirectCall = redirectCall(id),
//        errorCall = redirectErrorCall
//      )
//  }
//
//  def submitAgent(id: String): Action[AnyContent] = Authenticated.async {
//    implicit request =>
//      implicit user =>
//        getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
//          implicit mtdItUser =>
//            handleSubmitRequest(
//              id = id,
//              isAgent = true,
//              postAction = postActionAgent(id),
//              redirectCall = redirectCallAgent(id),
//              errorCall = redirectErrorCallAgent
//            )
//        }
//  }
//
//  private def handleRequest(id: String,
//                            isAgent: Boolean,
//                            postAction: Call,
//                            redirectCall: Call)
//                           (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext, messages: Messages): Future[Result] = {
//
//    (for {
//      isMandatoryOrVoluntary <- itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear
//      latencyDetailsMaybe <- Future(user.incomeSources.properties.find(
//        propertyDetails => propertyDetails.incomeSourceId.contains(id) && propertyDetails.isForeignProperty
//      ).flatMap(_.latencyDetails))
//      viewModel <- getForeignPropertyReportingMethodDetails(latencyDetailsMaybe)
//    } yield {
//      (isEnabled(IncomeSources), isMandatoryOrVoluntary, viewModel) match {
//        case (false, _, _) =>
//          Logger("application")
//            .error(s"[ForeignPropertyReportingMethodController][handleRequest]: not found error")
//          Future(Ok(customNotFoundErrorView()(user, messages)))
//        case (_, _, Left(ex)) =>
//          Logger("application")
//            .error(s"[ForeignPropertyReportingMethodController][handleRequest]: Failed with error - $ex")
//          Future.successful(Redirect(redirectCall))
//        case (_, true, Right(viewModel)) =>
//          Future(Ok(foreignPropertyReportingMethodView(
//            form = AddForeignPropertyReportingMethodForm.form,
//            viewModel = viewModel,
//            postAction = postAction,
//            isAgent = isAgent
//          )(user, messages)))
//        case _ =>
//          Logger("application")
//            .error(s"[ForeignPropertyReportingMethodController][handleRequest]: second level not found error")
//          Future(Ok(customNotFoundErrorView()(user, messages)))
//      }
//    }).flatten
//  }
//
//    private def handleSubmitRequest(id: String,
//                                    isAgent: Boolean,
//                                    postAction: Call,
//                                    redirectCall: Call,
//                                    errorCall: Call)
//                                   (implicit user: MtdItUser[_]): Future[Result] = {
//
//      if (isEnabled(IncomeSources)) {
//        AddForeignPropertyReportingMethodForm.form.bindFromRequest().fold(
//          formWithErrors => {
//            for {
//              latencyDetailsMaybe <- Future(user.incomeSources.properties
//                .filter(_.isForeignProperty)
//                .find(_.incomeSourceId.getOrElse("").equals(id))
//                .flatMap(_.latencyDetails))
//              fPropertyReportingMethodViewModel <- getForeignPropertyReportingMethodDetails(latencyDetailsMaybe)
//            } yield {
//              fPropertyReportingMethodViewModel match {
//                case Right(viewModel) =>
//                  BadRequest(foreignPropertyReportingMethodView(
//                    form = AddForeignPropertyReportingMethodForm.updateErrorMessagesWithValues(formWithErrors),
//                    viewModel = viewModel,
//                    postAction = postAction,
//                    isAgent = isAgent
//                  ))
//                case Left(ex) =>
//                  Logger("application")
//                    .error(s"[ForeignPropertyReportingMethodController][handleRequest]: " +
//                      s"Failed to retrieve latency details - $ex")
//                  Redirect(redirectCall)
//              }
//            }
//          },
//          valid => {
//
//            if (valid.reportingMethodIsChanged) {
//
//              val updatedReportingMethods = List(
//                getTaxYearSpecific(valid.taxYear1, valid.taxYear1ReportingMethod),
//                getTaxYearSpecific(valid.taxYear2, valid.taxYear2ReportingMethod)
//              ).flatten
//
//              updateIncomeSourceService.updateTaxYearSpecific(
//                nino = user.nino,
//                incomeSourceId = id,
//                taxYearSpecific = updatedReportingMethods
//              ).map {
//                case res: UpdateIncomeSourceResponseModel =>
//                  Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updated tax year specific reporting method : $res")
//                  Redirect(redirectCall)
//                case err: UpdateIncomeSourceResponseError =>
//                  Logger("application").error(s"${if (isAgent) "[Agent]"}" + s" Failed to Updated tax year specific reporting method : $err")
//                  Redirect(errorCall)
//              }
//            } else {
//              Logger("application").info(s"${if (isAgent) "[Agent]"}" + s" Updating the tax year specific reporting method not required.")
//              Future.successful(Redirect(redirectCall))
//            }
//          })
//      } else Future(Ok(customNotFoundErrorView()))
//    }
//
//  private def getForeignPropertyReportingMethodDetails(latencyDetailsMaybe: Option[LatencyDetails])
//                                                      (implicit user: MtdItUser[_]): Future[Either[Throwable, ForeignPropertyReportingMethodViewModel]] = {
//
//    println(s"\n TRYING TO GET REPORTING METHODS\n")
//    println(s"\n SENDING IN: $latencyDetailsMaybe\n")
//
//
//    val currentTaxYearEnd = dateService.getCurrentTaxYearEnd(isEnabled(TimeMachineAddYear))
//    latencyDetailsMaybe match {
//      case Some(latencyDetails) if Try(latencyDetails.taxYear1.toInt).toOption.isDefined =>
//        calculationListService.isTaxYearCrystallised(latencyDetails.taxYear1.toInt).flatMap { isTaxYear1Crystallised =>
//          (latencyDetails, isTaxYear1Crystallised) match {
//            case _ if Try(latencyDetails.taxYear2.toInt).toOption.isEmpty =>
//              Future.successful( Left( new Error(s"Unable to convert taxYear2 to Int: ${latencyDetails.taxYear2}") ) )
//            case _ if latencyDetails.taxYear2.toInt < currentTaxYearEnd =>
//              Future.successful( Left( new Error("Current tax year not in scope of change period") ) )
//            case (_, Some(true)) =>
//              Future.successful(
//                Right(
//                  ForeignPropertyReportingMethodViewModel(
//                    taxYear2 = Some(latencyDetails.taxYear2),
//                  latencyIndicator2 = Some(latencyDetails.latencyIndicator2)
//                )
//                )
//              )
//            case _ =>
//              Future.successful(
//                Right(ForeignPropertyReportingMethodViewModel(
//                taxYear1 = Some(latencyDetails.taxYear1),
//                latencyIndicator1 = Some(latencyDetails.latencyIndicator1),
//                taxYear2 = Some(latencyDetails.taxYear2),
//                latencyIndicator2 = Some(latencyDetails.taxYear2)
//              )))
//          }
//        }
//      case Some(latencyDetails) =>
//        Future(Left(new Error(s"Unable to convert taxYear1 to Int: ${latencyDetails.taxYear1}")))
//      case None =>
//        Future.successful(Left(new Error("Latency details are not provided")))
//    }
//  }
//
//  private def isAnnualReporting(taxYearReportingMethod: String): Boolean = taxYearReportingMethod.toUpperCase().equals("A")
//
//  private def getTaxYearSpecific(taxYearMaybe: Option[String], reportingMethod: Option[String]): Option[TaxYearSpecific] = for {
//    taxYear <- taxYearMaybe
//    taxYearReportingMethod <- reportingMethod
//  } yield {
//    TaxYearSpecific(
//      taxYear = taxYear,
//      latencyIndicator = isAnnualReporting(taxYearReportingMethod)
//    )
//  }
//
//  private def postAction(id: String) = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submit(id)
//  private def postActionAgent(id: String) = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodController.submitAgent(id)
//
//  private def redirectCall(id: String) = controllers.incomeSources.add.routes.ForeignPropertyAddedController.show(id)
//  private def redirectCallAgent(id: String) = controllers.incomeSources.add.routes.ForeignPropertyAddedController.showAgent(id)
//
//  val redirectErrorCall: Call = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodErrorController.show()
//  val redirectErrorCallAgent: Call = controllers.incomeSources.add.routes.ForeignPropertyReportingMethodErrorController.showAgent()

}
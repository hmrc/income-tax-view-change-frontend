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

package controllers.incomeSources.manage

import audit.AuditingService
import audit.models.ObligationsAuditModel
import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney._
import enums.JourneyType.{JourneyType, Manage}
import models.core.TaxYearId
import models.core.TaxYearId.mkTaxYearId
import models.incomeSourceDetails.{ManageIncomeSourceData, PropertyDetailsModel}
import play.api.Logger
import play.api.mvc._
import services.{IncomeSourceDetailsService, NextUpdatesService, SessionService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.incomeSources.manage.ManageObligations

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageObligationsController @Inject()(val checkSessionTimeout: SessionTimeoutPredicate,
                                            val authenticate: AuthenticationPredicate,
                                            val authorisedFunctions: AuthorisedFunctions,
                                            val retrieveNino: NinoPredicate,
                                            val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                            implicit val itvcErrorHandler: ItvcErrorHandler,
                                            implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                            val incomeSourceDetailsService: IncomeSourceDetailsService,
                                            val retrieveBtaNavBar: NavBarPredicate,
                                            val obligationsView: ManageObligations,
                                            val auditingService: AuditingService,
                                            val sessionService: SessionService,
                                            nextUpdatesService: NextUpdatesService)
                                           (implicit val ec: ExecutionContext,
                                            implicit override val mcc: MessagesControllerComponents,
                                            val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching with IncomeSourcesUtils {


  def showSelfEmployment(changeTo: String, taxYearString: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      withIncomeSourcesFS {
        {
          for {
            taxYearIdE <- Future {
              mkTaxYearId(taxYearString)
            }
            res <- sessionService
              .getMongoKey(ManageIncomeSourceData.incomeSourceIdField, JourneyType(Manage, SelfEmployment))
          } yield (res, taxYearIdE) match {
            case (Right(incomeSourceIdMayBe), Right(taxYearId)) =>
              handleRequest(
                mode = SelfEmployment,
                isAgent = false,
                taxYearId,
                changeTo,
                incomeSourceIdMayBe
              )
            case (Left(exception), _) => Future.failed(exception)
            case (_, Left(exception)) => Future.failed(exception)
          }
        }.flatten
      }.recover {
        case exception =>
          Logger("application").error(s"[ManageObligationsController][showSelfEmployment] ${exception.getMessage}")
          showInternalServerError(isAgent = false)
      }
  }

  def showAgentSelfEmployment(changeTo: String, taxYearString: String): Action[AnyContent] = authenticatedAction(isAgent = true) {
    implicit user =>
      withIncomeSourcesFS {
        sessionService.getMongoKey(ManageIncomeSourceData.incomeSourceIdField, JourneyType(Manage, SelfEmployment)).flatMap {
          case Right(incomeSourceIdMayBe) =>
            handleRequest(
              mode = SelfEmployment,
              isAgent = true,
              mkTaxYearId(taxYearString).toOption.get,
              changeTo,
              incomeSourceIdMayBe
            )
          case Left(exception) => Future.failed(exception)
        }
      }.recover {
        case exception =>
          Logger("application").error(s"[ManageObligationsController][showAgentSelfEmployment] ${exception.getMessage}")
          showInternalServerError(isAgent = true)
      }
  }

  def showUKProperty(changeTo: String, taxYearString: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      mkTaxYearId(taxYearString).toOption.map {
        taxYear =>
          handleRequest(
            mode = UkProperty,
            isAgent = false,
            taxYear,
            changeTo,
            None
          )
      }.getOrElse(Future.failed(throw new Error("Error : Boom")))
  }

  def showAgentUKProperty(changeTo: String, taxYearString: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              mode = UkProperty,
              isAgent = true,
              mkTaxYearId(taxYearString).toOption.get,
              changeTo,
              None
            )
        }
  }

  def showForeignProperty(changeTo: String, taxYearString: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        mode = ForeignProperty,
        isAgent = false,
        mkTaxYearId(taxYearString).toOption.get,
        changeTo,
        None
      )
  }

  def showAgentForeignProperty(changeTo: String, taxYearString: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              mode = ForeignProperty,
              isAgent = true,
              mkTaxYearId(taxYearString).toOption.get,
              changeTo,
              None
            )
        }
  }

  def handleRequest(mode: IncomeSourceType, isAgent: Boolean, taxYearId: TaxYearId, changeTo: String, incomeSourceId: Option[String])(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    if (isDisabled(IncomeSources)) {
      if (isAgent) Future.successful(Redirect(controllers.routes.HomeController.showAgent))
      else Future.successful(Redirect(controllers.routes.HomeController.show()))
    }
    else {
      val postUrl: Call = if (isAgent) controllers.incomeSources.manage.routes.ManageObligationsController.agentSubmit() else controllers.incomeSources.manage.routes.ManageObligationsController.submit()
      val addedBusinessName: String = getBusinessName(mode, incomeSourceId)

      if (changeTo == "annual" || changeTo == "quarterly") {
        getIncomeSourceId(mode, incomeSourceId, isAgent = isAgent) match {
          case Left(error) =>
            showError(isAgent, {
              error.getMessage
            })
          case Right(value) =>
            nextUpdatesService.getObligationsViewModel(value, showPreviousTaxYears = false) map { viewModel =>
              auditingService.extendedAudit(ObligationsAuditModel(
                incomeSourceType = mode,
                obligations = viewModel,
                businessName = addedBusinessName,
                changeTo,
                taxYearId
              ))
              Ok(obligationsView(viewModel, addedBusinessName, taxYearId, changeTo, isAgent, postUrl))
            }
        }
      }
      else {
        showError(isAgent, "invalid changeTo mode provided")
      }


    }
  }

  def showError(isAgent: Boolean, message: String)(implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {
    Logger("application").error(
      s"${if (isAgent) "[Agent]"}[ManageObligationsController][handleRequest] - $message")
    if (isAgent) Future.successful(itvcErrorHandlerAgent.showInternalServerError())
    else Future.successful(itvcErrorHandler.showInternalServerError())
  }

  def getBusinessName(mode: IncomeSourceType, incomeSourceId: Option[String])(implicit user: MtdItUser[_]): String = {
    (mode, incomeSourceId) match {
      case (SelfEmployment, Some(incomeSourceId)) =>
        val businessDetailsParams = for {
          addedBusiness <- user.incomeSources.businesses.find(x => x.incomeSourceId.contains(incomeSourceId))
          businessName <- addedBusiness.tradingName
        } yield (addedBusiness, businessName)
        businessDetailsParams match {
          case Some((_, name)) => name
          case None => "Not Found"
        }
      case (UkProperty, _) => "UK property"
      case (ForeignProperty, _) => "Foreign property"
      case _ => "Not Found"
    }
  }

  def getIncomeSourceId(incomeSourceType: IncomeSourceType, id: Option[String], isAgent: Boolean)(implicit user: MtdItUser[_]): Either[Throwable, String] = {
    (incomeSourceType, id) match {
      case (SelfEmployment, Some(id)) => Right(id)
      case _ =>
        Seq(UkProperty, ForeignProperty).find(_ == incomeSourceType)
          .map(_ => incomeSourceDetailsService.getActiveUkOrForeignPropertyBusinessFromUserIncomeSources(incomeSourceType == UkProperty))
          .getOrElse(Left(new Error("No id supplied for Self Employment business")))
        match {
          case Right(property: PropertyDetailsModel) => Right(property.incomeSourceId)
          case Left(error: Error) => Left(error)
          case _ => Left(new Error(s"Unknown error. IncomeSourceType: $incomeSourceType"))
        }
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      Future.successful(Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(false)))
  }

  def agentSubmit: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            Future.successful(Redirect(controllers.incomeSources.manage.routes.ManageIncomeSourceController.show(true)))
        }
  }

  private def authenticatedAction(isAgent: Boolean
                                 )(authenticatedCodeBlock: MtdItUser[_] => Future[Result]): Action[AnyContent] = {
    if (isAgent)
      Authenticated.async {
        implicit request =>
          implicit user =>
            getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap { implicit mtdItUser =>
              authenticatedCodeBlock(mtdItUser)
            }
      }
    else
      (checkSessionTimeout andThen authenticate andThen retrieveNino
        andThen retrieveIncomeSources andThen retrieveBtaNavBar).async { implicit user =>
        authenticatedCodeBlock(user)
      }
  }
}
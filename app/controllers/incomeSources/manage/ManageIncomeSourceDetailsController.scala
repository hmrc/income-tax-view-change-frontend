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

import auth.MtdItUser
import cats.data.EitherT
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import enums.IncomeSourceJourney.{PropertyBusiness, SelfEmployment}
import models.incomeSourceDetails.viewmodels.{ManageBusinessDetailsViewModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, LatencyDetails, PropertyDetailsModel}
import play.api.Logger
import play.api.mvc._
import services.{CalculationListService, DateService, ITSAStatusService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import utils.IncomeSourcesUtils
import views.html.incomeSources.manage.ManageSelfEmployment

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageIncomeSourceDetailsController @Inject()(val view: ManageSelfEmployment,
                                                    val checkSessionTimeout: SessionTimeoutPredicate,
                                                    val authenticate: AuthenticationPredicate,
                                                    val authorisedFunctions: AuthorisedFunctions,
                                                    val retrieveNino: NinoPredicate,
                                                    val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                                    val itvcErrorHandler: ItvcErrorHandler,
                                                    implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                                    val incomeSourceDetailsService: IncomeSourceDetailsService,
                                                    val itsaStatusService: ITSAStatusService,
                                                    val dateService: DateService,
                                                    val retrieveBtaNavBar: NavBarPredicate,
                                                    val calculationListService: CalculationListService)
                                                   (implicit val ec: ExecutionContext,
                                                    implicit override val mcc: MessagesControllerComponents,
                                                    val appConfig: FrontendAppConfig)
  extends ClientConfirmedController with FeatureSwitching with IncomeSourcesUtils {


  def showUkProperty: Action[AnyContent] = Action(Ok)

  def showUkPropertyAgent: Action[AnyContent] = Action(Ok)

  def showForeignProperty: Action[AnyContent] = Action(Ok)

  def showForeignPropertyAgent: Action[AnyContent] = Action(Ok)

  def submitUkProperty: Action[AnyContent] = Action(Ok)

  def submitUkPropertyAgent: Action[AnyContent] = Action(Ok)

  def submitForeignProperty: Action[AnyContent] = Action(Ok)

  def submitForeignPropertyAgent: Action[AnyContent] = Action(Ok)

  def submitSoleTraderBusiness: Action[AnyContent] = Action(Ok)

  def submitSoleTraderBusinessAgent: Action[AnyContent] = Action(Ok)

  def showSoleTraderBusiness(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show().url,
        id = id
      )
  }

  def showSoleTraderBusinessAgent(id: String): Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleRequest(
              sources = mtdItUser.incomeSources,
              isAgent = true,
              backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.showAgent().url,
              id = id
            )
        }
  }

  def getCrystallisationInformation(latencyDetails: Option[LatencyDetails])
                                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, String, List[Boolean]] = {
    EitherT {
      latencyDetails match {
        case Some(x) =>
          for {
            i <- calculationListService.isTaxYearCrystallised(x.taxYear1.toInt)
            j <- calculationListService.isTaxYearCrystallised(x.taxYear2.toInt)
          } yield {
            Right(List(i.get, j.get))
          }
        case _ =>
          Future.successful(Left("No data ready"))
      }
    }
  }

  def variableViewModelSEBusiness(incomeSource: BusinessDetailsModel, itsaStatus: Boolean, crystallisationTaxYear1: Option[Boolean],
                                  crystallisationTaxYear2: Option[Boolean]): ManageBusinessDetailsViewModel = {
    ManageBusinessDetailsViewModel(
      incomeSourceId = incomeSource.incomeSourceId,
      tradingName = incomeSource.tradingName,
      tradingStartDate = incomeSource.tradingStartDate,
      address = incomeSource.address,
      businessAccountingMethod = incomeSource.cashOrAccruals,
      itsaHasMandatedOrVoluntaryStatusCurrentYear = itsaStatus,
      taxYearOneCrystallised = crystallisationTaxYear1,
      taxYearTwoCrystallised = crystallisationTaxYear2,
      latencyDetails = incomeSource.latencyDetails,
      incomeSourceType = SelfEmployment
    )
  }

  def getManageIncomeSourceViewModel(sources: IncomeSourceDetailsModel, id: String, isAgent: Boolean)
                                    (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, ManageBusinessDetailsViewModel]] = {

    val desiredIncomeSourceMaybe: Option[BusinessDetailsModel] = sources.businesses
      .filterNot(_.isCeased)
      .find(e => e.incomeSourceId == id)

    if (desiredIncomeSourceMaybe.isDefined) {
      itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear.flatMap {
        case true =>
          getCrystallisationInformation(desiredIncomeSourceMaybe.get.latencyDetails).value.flatMap {
            case Left(x) => Future(Right(variableViewModelSEBusiness(
              incomeSource = desiredIncomeSourceMaybe.get,
              itsaStatus = true,
              crystallisationTaxYear1 = None,
              crystallisationTaxYear2 = None)))
            case Right(crystallisationData: List[Boolean]) =>
              Future(Right(
                variableViewModelSEBusiness(
                  incomeSource = desiredIncomeSourceMaybe.get,
                  itsaStatus = true,
                  crystallisationTaxYear1 = Option(crystallisationData.head),
                  crystallisationTaxYear2 = Option(crystallisationData(1)))
              ))
          }
        case false =>
          Future(Right(
            variableViewModelSEBusiness(
              incomeSource = desiredIncomeSourceMaybe.get,
              itsaStatus = true,
              crystallisationTaxYear1 = None,
              crystallisationTaxYear2 = None)
          ))
      }
    } else {
      Future(Left(
        new Error("Unable to find income source")
      ))
    }
  }


  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String, id: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    withIncomeSourcesFS {
      for {
        value <- getManageIncomeSourceViewModel(sources = sources, id = id, isAgent = isAgent)
      } yield {
        value match {
          case Right(viewModel) =>
            Ok(view(viewModel = viewModel,
              isAgent = isAgent,
              backUrl = backUrl
            ))
          case Left(error) =>
            Logger("application").error(s"[ManageIncomeSourceDetailsController][extractIncomeSource] unable to find income source: $error. isAgent = $isAgent")
            if (isAgent) {
              itvcErrorHandlerAgent.showInternalServerError()
            } else {
              itvcErrorHandler.showInternalServerError()
            }
        }
      }
    }
  }
}
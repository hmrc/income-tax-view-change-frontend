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
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import exceptions.MissingFieldException
import models.incomeSourceDetails.viewmodels.{ViewBusinessDetailsViewModel, ViewLatencyDetailsViewModel}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel, LatencyDetails}
import play.api.mvc._
import services.{CalculationListService, DateService, ITSAStatusService, IncomeSourceDetailsService}
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.http.HeaderCarrier
import views.html.incomeSources.manage.BusinessManageDetails

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ManageSelfEmploymentController @Inject()(val view: BusinessManageDetails,
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
                                               implicit override val mcc: MessagesControllerComponents, val appConfig: FrontendAppConfig) extends ClientConfirmedController
  with FeatureSwitching {

  def show(id: String): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest(
        sources = user.incomeSources,
        isAgent = false,
        backUrl = controllers.incomeSources.manage.routes.ManageIncomeSourceController.show().url,
        id = id
      )
  }

  def showAgent(id: String): Action[AnyContent] = Authenticated.async {
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

  def getCrystallisationInformation(incomeSource: Option[BusinessDetailsModel])
                                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): EitherT[Future, String, List[Boolean]] = {
    EitherT {
      incomeSource match {
        case Some(x) if x.latencyDetails.isDefined =>
          for {
            i <- calculationListService.isTaxYearCrystallised(x.latencyDetails.get.taxYear1.toInt)
            j <- calculationListService.isTaxYearCrystallised(x.latencyDetails.get.taxYear2.toInt)
          } yield
            Right(List(i.get, j.get))
        case _ =>
          Future.successful(Left("No data ready"))
      }
    }
  }

  def getViewIncomeSourceChosenViewModel(sources: IncomeSourceDetailsModel, id: String)
                                        (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Either[Throwable, ViewBusinessDetailsViewModel]] = {

    val desiredIncomeSourceMaybe: Option[BusinessDetailsModel] = sources.businesses
      //.filterNot(_.isCeased)
      //.filter(e => e.incomeSourceId.isDefined && e.incomeSourceId.get == id)
      .headOption

    //val itsaStatus: Future[Boolean] =itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear
    //println("LLLLLLLL" + itsaStatus)
    val latencyDetails: Option[LatencyDetails] = desiredIncomeSourceMaybe.map(_.latencyDetails).flatten

    getCrystallisationInformation(desiredIncomeSourceMaybe).value.flatMap {
      case Left(x) =>
        for {
          i <- itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear
          //if desiredIncomeSourceMaybe.isDefined
          //if desiredIncomeSourceMaybe.get.incomeSourceId.isDefined
        } yield {
          Right(ViewBusinessDetailsViewModel(
            incomeSourceId = desiredIncomeSourceMaybe.get.incomeSourceId.get,
            tradingName = desiredIncomeSourceMaybe.get.tradingName,
            tradingStartDate = desiredIncomeSourceMaybe.get.tradingStartDate,
            address = desiredIncomeSourceMaybe.get.address,
            businessAccountingMethod = desiredIncomeSourceMaybe.get.cashOrAccruals,
            itsaHasMandatedOrVoluntaryStatusCurrentYear = Option(i),
            taxYearOneCrystallised = None,
            taxYearTwoCrystallised = None,
            latencyDetails = None)
          )
        }
      case Right(crystallisationData: List[Boolean]) =>
        for {
          i <- itsaStatusService.hasMandatedOrVoluntaryStatusCurrentYear
          if desiredIncomeSourceMaybe.isDefined
          if desiredIncomeSourceMaybe.get.incomeSourceId.isDefined
        } yield {
          Right(ViewBusinessDetailsViewModel(
            incomeSourceId = desiredIncomeSourceMaybe.get.incomeSourceId.get,
            tradingName = desiredIncomeSourceMaybe.get.tradingName,
            tradingStartDate = desiredIncomeSourceMaybe.get.tradingStartDate,
            address = desiredIncomeSourceMaybe.get.address,
            businessAccountingMethod = desiredIncomeSourceMaybe.get.cashOrAccruals,
            itsaHasMandatedOrVoluntaryStatusCurrentYear = Option(i),
            taxYearOneCrystallised = Option(crystallisationData.head),
            taxYearTwoCrystallised = Option(crystallisationData(1)),
            latencyDetails = Option(ViewLatencyDetailsViewModel(
              latencyEndDate = latencyDetails.get.latencyEndDate,
              taxYear1 = latencyDetails.get.taxYear1.toInt,
              latencyIndicator1 = latencyDetails.get.latencyIndicator1,
              taxYear2 = latencyDetails.get.taxYear2.toInt,
              latencyIndicator2 = latencyDetails.get.latencyIndicator2
             )
             )
           )
          )
        }
    }
  }

  def handleRequest(sources: IncomeSourceDetailsModel, isAgent: Boolean, backUrl: String, id: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = {

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(controllers.routes.HomeController.show()))
    } else {
      println(s"Here is my incomeSource: ${sources.businesses.head.latencyDetails}")
      for {
        value <- getViewIncomeSourceChosenViewModel(sources = sources, id = id)
      } yield {
        value match {
          case Right(v) =>
            Ok(view(viewModel = v,
              isAgent = isAgent,
              backUrl = backUrl
            ))
          case Left(ex) =>
            itvcErrorHandler.showInternalServerError()
        }
      }

    }
  }
}
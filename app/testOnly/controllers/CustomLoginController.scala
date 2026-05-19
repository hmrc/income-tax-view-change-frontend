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

package testOnly.controllers

import common.config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import common.config.featureswitch.FeatureSwitching
import common.controllers.BaseController
import common.services.{DateServiceInterface, ITSAStatusService}
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.CalculationListService
import testOnly.TestOnlyAppConfig
import testOnly.connectors.{ClearITSAStatusCacheConnector, CustomAuthConnector, DynamicStubConnector}
import testOnly.models.*
import testOnly.services.{DynamicStubService, OptOutCustomDataService}
import testOnly.utils.UserRepository
import testOnly.views.html.LoginPage
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthExchange, SessionBuilder}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomLoginController @Inject()(implicit val appConfig: FrontendAppConfig,
                                      val testOnlyAppConfig: TestOnlyAppConfig,
                                      val mcc: MessagesControllerComponents,
                                      val executionContext: ExecutionContext,
                                      userRepository: UserRepository,
                                      loginPage: LoginPage,
                                      val dynamicStubConnector: DynamicStubConnector,
                                      val optOutCustomDataService: OptOutCustomDataService,
                                      val customAuthConnector: CustomAuthConnector,
                                      val clearITSAStatusCacheConnector: ClearITSAStatusCacheConnector,
                                      val calculationListService: CalculationListService,
                                      val dynamicStubService: DynamicStubService,
                                      val ITSAStatusService: ITSAStatusService,
                                      val itvcErrorHandler: ItvcErrorHandler,
                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                      dateService: DateServiceInterface
                                     ) extends BaseController with I18nSupport with FeatureSwitching {

  private final val customIncomeSourceUsers         = Seq("TR000001A", "AS000000A", "AS000001A")
  private final val customReportingObligationsUsers = Seq("OP000001A", "OP000002A", "OP000003A", "NE000000A", "NE000001A", "NE000002A", "HP000000A")
  private final val latentBusinessUser              = "AS000002A"
  private final val recentActivityUser              = "HP000000A"

  val showLogin: Action[AnyContent] = Action.async { implicit request =>
    userRepository.findAll().map(userRecords =>
      Ok(loginPage(routes.CustomLoginController.postLogin(), userRecords, customReportingObligationsUsers, customIncomeSourceUsers, latentBusinessUser))
    )
  }

  val postLogin: Action[AnyContent] = Action.async { implicit request =>
    PostedUser.form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(s"Invalid form submission: $formWithErrors")),
      (postedUser: PostedUser) => {
        userRepository.findUser(postedUser.nino).flatMap(
          user =>
            customAuthConnector.login(user.nino, postedUser.isAgent, postedUser.isSupporting).flatMap {
              case (authExchange, _) =>
                val (bearer, auth) = (authExchange.bearerToken, authExchange.sessionAuthorityUri)
                val redirectURL = if (postedUser.isAgent)
                  s"report-quarterly/income-and-expenses/view/test-only/stub-client/nino/${user.nino}/utr/" + user.utr
                else {
                  val origin = if (postedUser.usePTANavBar) "PTA" else "BTA"
                  s"report-quarterly/income-and-expenses/view?origin=$origin"
                }
                val homePage = s"${appConfig.itvcFrontendEnvironment}/$redirectURL"

                updateEffectiveDateOfPayment().failed.foreach(ex => {
                  Logger("application").error("Failed to update effectiveDateOfPayment", ex)
                })

                updateEstimatedRepaymentDate().failed.foreach(ex => {
                  Logger("application").error("Failed to update estimatedRepaymentDate", ex)
                })

                user.category match {
                  case "Income Sources" if(customIncomeSourceUsers.contains(user.nino)) => overwriteDataForIncomeSources(user, postedUser, bearer, auth, homePage)
                  case "Income Sources" if(user.nino == latentBusinessUser) => overwriteDataforLatentBusinesses(user, postedUser, bearer, auth, homePage)
                  case "Misc" if (user.nino == recentActivityUser) => overwriteDataForReportingObligations(user.nino, postedUser, bearer, auth, homePage)
                  case _ if(customReportingObligationsUsers.contains(user.nino)) => overwriteDataForReportingObligations(user.nino, postedUser, bearer, auth, homePage)
                  case _ => Future.successful(successRedirect(bearer, auth, homePage))
                }
            }
        )
      }
    )
  }

  private def overwriteDataForIncomeSources(user: UserRecord, postedUser: PostedUser, bearer: String, auth: String, homePage: String)(implicit headerCarrier: HeaderCarrier, request: Request[_]) = {
    val incomeSourcesUser = IncomeSourcesUser(
      activeSoleTrader = postedUser.activeSoleTrader,
      activeUkProperty = postedUser.activeUkProperty,
      activeForeignProperty = postedUser.activeForeignProperty,
      ceasedBusiness = postedUser.ceasedBusiness
    )

    updateTestDataForIncomeSourcesUser(user.mtditid, incomeSourcesUser).map {
      _ => successRedirect(bearer, auth, homePage)
    }.recover {
      case ex =>
        val errorHandler = if (postedUser.isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application")
          .error(s"Unexpected response, status: - ${ex.getMessage} - ${ex.getCause} - ")
        errorHandler.showInternalServerError()
    }
  }

  private def overwriteDataForReportingObligations(nino: String, postedUser: PostedUser, bearer: String, auth: String, homePage: String)(implicit headerCarrier: HeaderCarrier, request: Request[_]) = {
    updateTestDataForOptOut(
      nino = nino,
      crystallisationStatus = postedUser.cyMinusOneCrystallisationStatus.get,
      cyMinusOneItsaStatus = postedUser.cyMinusOneItsaStatus.get,
      cyItsaStatus = postedUser.cyItsaStatus.get,
      cyPlusOneItsaStatus = postedUser.cyPlusOneItsaStatus.get
    ).map {
      _ =>
        if (nino == recentActivityUser) {
          updateTestDataForRecentActivityUser(nino)
        }
        successRedirect(bearer, auth, homePage)
    }.recover {
      case ex =>
        val errorHandler = if (postedUser.isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application")
          .error(s"Unexpected response, status: - ${ex.getMessage} - ${ex.getCause} - ")
        errorHandler.showInternalServerError()
    }
  }

  private def overwriteDataforLatentBusinesses(user: UserRecord, postedUser: PostedUser, bearer: String, auth: String, homePage: String)(implicit headerCarrier: HeaderCarrier, request: Request[_]) = {
    val latentBusinessUser = LatentBusinessUser(
      latencyIndicator1 = postedUser.latentBusinessYear1.getOrElse("Annual"),
      latencyIndicator2 = postedUser.latentBusinessYear2.getOrElse("Annual")
    )

    updateTestDataForLatentBusinessUser(user.mtditid, latentBusinessUser).map {
      _ => successRedirect(bearer, auth, homePage)
    }.recover {
      case ex =>
        val errorHandler = if (postedUser.isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application")
          .error(s"Unexpected response, status: - ${ex.getMessage} - ${ex.getCause} - ")
        errorHandler.showInternalServerError()
    }
  }

  private def successRedirect(bearer: String, auth: String, homePage: String): Result = {
    Redirect(homePage)
      .withSession(
        SessionBuilder.buildGGSession(AuthExchange(bearerToken = bearer,
          sessionAuthorityUri = auth)))
  }

  private def updateEstimatedRepaymentDate()(implicit headerCarrier: HeaderCarrier) = {
    dynamicStubService.overwriteEstimatedRepaymentDate()
  }

  private def updateTestDataForIncomeSourcesUser(mtdid: String, incomeSourcesUser: IncomeSourcesUser)(implicit headerCarrier: HeaderCarrier) = {
    dynamicStubService.overwriteBusinessData(mtdid, incomeSourcesUser)
  }

  private def updateTestDataForRecentActivityUser(nino: String)(implicit headerCarrier: HeaderCarrier) = {
    dynamicStubService.overwriteObligationsData(nino)
  }

  private def updateTestDataForLatentBusinessUser(mtdid: String, latentBusinessUser: LatentBusinessUser)(implicit headerCarrier: HeaderCarrier) = {
    dynamicStubService.overwriteLatentBusinessData(mtdid, latentBusinessUser)
  }

  private def updateTestDataForOptOut(nino: String, crystallisationStatus: String, cyMinusOneItsaStatus: String,
                                      cyItsaStatus: String, cyPlusOneItsaStatus: String)(implicit hc: HeaderCarrier): Future[Unit] = {

    val ninoObj = Nino(nino)
    val taxYear: TaxYear = TaxYear(
      dateService.getCurrentTaxYearStart.getYear,
      dateService.getCurrentTaxYearEnd
    )

    val crystallisationStatusResult: Future[Unit] = optOutCustomDataService.uploadCalculationListData(nino = ninoObj, taxYear = taxYear.addYears(-1), status = crystallisationStatus)
    val itsaStatusCyMinusOneResult: Future[Unit] = optOutCustomDataService.uploadITSAStatusData(nino = ninoObj, taxYear = taxYear.addYears(-1), status = cyMinusOneItsaStatus)
    val itsaStatusCyResult: Future[Unit] = optOutCustomDataService.uploadITSAStatusData(nino = ninoObj, taxYear = taxYear, status = cyItsaStatus)
    val itsaStatusCyPlusOneResult: Future[Unit] = optOutCustomDataService.uploadITSAStatusData(nino = ninoObj, taxYear = taxYear.addYears(1), status = cyPlusOneItsaStatus)
    val combinedItsaStatusFutureYear = optOutCustomDataService.stubITSAStatusFutureYearData(nino, taxYear, cyMinusOneItsaStatus, cyItsaStatus, cyPlusOneItsaStatus)

    for {
      _ <- clearITSAStatusCacheConnector.overwriteItsaStatus(nino)
      _ <- crystallisationStatusResult
      _ <- itsaStatusCyMinusOneResult
      _ <- itsaStatusCyResult
      _ <- itsaStatusCyPlusOneResult
      _ <- combinedItsaStatusFutureYear
    } yield ()
  }

  private def updateEffectiveDateOfPayment()(implicit headerCarrier: HeaderCarrier) = {
    dynamicStubService.overwriteEffectiveDateOfPaymentUrl()
  }

}

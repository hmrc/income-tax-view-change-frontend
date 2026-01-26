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

import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.BaseController
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{CalculationListService, DateServiceInterface, ITSAStatusService}
import testOnly.TestOnlyAppConfig
import testOnly.connectors.{CustomAuthConnector, DynamicStubConnector}
import testOnly.models._
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
                                      val calculationListService: CalculationListService,
                                      val dynamicStubService: DynamicStubService,
                                      val ITSAStatusService: ITSAStatusService,
                                      val itvcErrorHandler: ItvcErrorHandler,
                                      val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                      dateService: DateServiceInterface
                                     ) extends BaseController with I18nSupport with FeatureSwitching {

  // Logging page functionality
  val showLogin: Action[AnyContent] = Action.async { implicit request =>
    userRepository.findAll().map(userRecords =>
      Ok(loginPage(routes.CustomLoginController.postLogin(), userRecords, testOnlyAppConfig.optOutUserPrefixes))
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

                if (postedUser.isOptOutWhitelisted(testOnlyAppConfig.optOutUserPrefixes) && user.nino != "OP000009A") {
                  updateTestDataForOptOut(
                    nino = user.nino,
                    crystallisationStatus = postedUser.cyMinusOneCrystallisationStatus.get,
                    cyMinusOneItsaStatus = postedUser.cyMinusOneItsaStatus.get,
                    cyItsaStatus = postedUser.cyItsaStatus.get,
                    cyPlusOneItsaStatus = postedUser.cyPlusOneItsaStatus.get
                  ).map {
                    _ => successRedirect(bearer, auth, homePage)
                  }.recover {
                    case ex =>
                      val errorHandler = if (postedUser.isAgent) itvcErrorHandlerAgent else itvcErrorHandler
                      Logger("application")
                        .error(s"Unexpected response, status: - ${ex.getMessage} - ${ex.getCause} - ")
                      errorHandler.showInternalServerError()
                  }
                } else {
                  Future.successful(successRedirect(bearer, auth, homePage))
                }

              case code =>
                Future.successful(InternalServerError("something went wrong.." + code))
            }
        )
      }
    )
  }

  private def successRedirect(bearer: String, auth: String, homePage: String): Result = {
    Redirect(homePage)
      .withSession(
        SessionBuilder.buildGGSession(AuthExchange(bearerToken = bearer,
          sessionAuthorityUri = auth)))
  }

  private def updateTestDataForOptOut(nino: String, crystallisationStatus: String, cyMinusOneItsaStatus: String,
                                      cyItsaStatus: String, cyPlusOneItsaStatus: String)(implicit hc: HeaderCarrier): Future[Unit] = {

    // TODO: maybe make crystallisationStatus and itsaStatus value classes, using Scala Request Binders or Scala Actions composition perhaps

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
      _ <- crystallisationStatusResult
      _ <- itsaStatusCyMinusOneResult
      _ <- itsaStatusCyResult
      _ <- itsaStatusCyPlusOneResult
      _ <- combinedItsaStatusFutureYear
    } yield ()

  }

}

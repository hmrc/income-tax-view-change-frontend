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

import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.BaseController
import models.core.{CrystallisationStatus, ItsaStatusCyMinusOne}
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}
import services.{CalculationListService, DateServiceInterface, ITSAStatusService}
import testOnly.connectors.{CustomAuthConnector, DynamicStubConnector}
import testOnly.models.{Nino, PostedUser}
import testOnly.utils.UserRepository
import testOnly.views.html.LoginPage
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.AuthRedirects
import utils.{AuthExchange, SessionBuilder}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomLoginController @Inject()(implicit val appConfig: FrontendAppConfig,
                                      override val config: Configuration,
                                      override val env: Environment,
                                      implicit val mcc: MessagesControllerComponents,
                                      implicit val executionContext: ExecutionContext,
                                      userRepository: UserRepository,
                                      loginPage: LoginPage,
                                      val dynamicStubConnector: DynamicStubConnector,
                                      val customAuthConnector: CustomAuthConnector,
                                      val calculationListService: CalculationListService,
                                      val ITSAStatusService: ITSAStatusService,
                                      val itvcErrorHandler: ItvcErrorHandler,
                                      implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                      dateService: DateServiceInterface
                                     ) extends BaseController with AuthRedirects with I18nSupport {

  // Logging page functionality
  val showLogin: Action[AnyContent] = Action.async { implicit request =>
    userRepository.findAll().map(userRecords =>
      Ok(loginPage(routes.CustomLoginController.postLogin, userRecords))
    )
  }

  val postLogin: Action[AnyContent] = Action.async { implicit request =>
    PostedUser.form.bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(s"Invalid form submission: $formWithErrors")),
      (postedUser: PostedUser) => {

        userRepository.findUser(postedUser.nino).flatMap(
          user =>
            customAuthConnector.login(Nino(user.nino), postedUser.isAgent).flatMap {
              case (authExchange, _) =>
                val (bearer, auth) = (authExchange.bearerToken, authExchange.sessionAuthorityUri)
                val redirectURL = if (postedUser.isAgent)
                  s"report-quarterly/income-and-expenses/view/test-only/stub-client/nino/${user.nino}/utr/" + user.utr
                else
                  "report-quarterly/income-and-expenses/view?origin=BTA"
                val homePage = s"${appConfig.itvcFrontendEnvironment}/$redirectURL"

                updateTestDataForOptOut(user.nino, CrystallisationStatus(appConfig)(postedUser.cyMinusOneCrystallisationStatus),
                  ItsaStatusCyMinusOne(appConfig)(postedUser.cyMinusOneItsaStatus), postedUser.cyItsaStatus, postedUser.cyPlusOneItsaStatus).map{
                  case Left(ex) =>
                    val errorHandler = if (postedUser.isAgent) itvcErrorHandlerAgent else itvcErrorHandler
                    Logger("application")
                      .error(s"[CustomLoginController][postLogin] - Unexpected response, status: - ${ex.getMessage} - ${ex.getCause} - ")
                    errorHandler.showInternalServerError()
                  case Right(_) =>
                    Redirect(homePage)
                      .withSession(
                        SessionBuilder.buildGGSession(AuthExchange(bearerToken = bearer,
                          sessionAuthorityUri = auth)))
                }

              case code =>
                Future.successful(InternalServerError("something went wrong.." + code))
            }
        )
      }
    )
  }

  val showCss: Action[AnyContent] = Action.async { implicit request =>
    dynamicStubConnector.showLogin("hmrc-frontend/assets/hmrc-frontend-5.19.0.min.css").map(
      response => response.status match {
        case OK => Ok(response.body).as("text/css")
        case _ => InternalServerError(response.body)
      }
    )
  }

  private def updateTestDataForOptOut(nino: String, crystallisationStatus: CrystallisationStatus, cyMinusOneItsaStatus: ItsaStatusCyMinusOne,
                                      cyItsaStatus: String, cyPlusOneItsaStatus: String)(implicit hc: HeaderCarrier)
  : Future[Either[Throwable, Unit]] = {

    // TODO: maybe make crystallisationStatus and itsaStatus value classes, using Scala Request Binders or Scala Actions composition perhaps

    println(cyItsaStatus + cyPlusOneItsaStatus)


    val ninoObj = models.core.Nino(nino)
    val crystallisationStatusResult = crystallisationStatus.uploadData(nino = ninoObj)
    val itsaStatusCyMinusOneResult = cyMinusOneItsaStatus.uploadData(nino = ninoObj)

    for {
      result1 <- crystallisationStatusResult
      result2 <- itsaStatusCyMinusOneResult
    } yield {
      val aggregatedResult: Either[Throwable, Unit] = for {
        _ <- result1
        _ <- result2
      } yield ()

      aggregatedResult match {
        case Left(ex: Throwable) => Left(ex)
        case Right(_) => Right(Logger("application").info("[CustomLoginController][updateTestDataForOptOut] - Data was updated successfully"))
      }

    }



//    crystallisationStatus.uploadData(nino = models.core.Nino(nino)).map {
//      case Left(ex: Throwable) =>
//        Left(ex)
//      case Right(_) => Right(Logger("application").info("[CustomLoginController][updateTestDataForOptOut] - Data was updated successfully"))
//    }
  }

}

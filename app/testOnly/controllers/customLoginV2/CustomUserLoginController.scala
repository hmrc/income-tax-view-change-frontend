/*
 * Copyright 2026 HM Revenue & Customs
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

package testOnly.controllers.customLoginV2

import controllers.BaseController
import models.incomeSourceDetails.TaxYear
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.DateServiceInterface
import testOnly.TestOnlyAppConfig
import testOnly.connectors.CustomAuthConnector
import testOnly.forms.customLoginV2.CustomUserCreateForm
import testOnly.models.{CreateCustomUserModel, Nino}
import testOnly.services.{DynamicStubService, OptOutCustomDataService}
import testOnly.views.html.customLoginV2.CustomUserLoginPage
import uk.gov.hmrc.http.HeaderCarrier
import utils.{AuthExchange, SessionBuilder}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CustomUserLoginController @Inject()(implicit mcc: MessagesControllerComponents,
                                          appConfig: TestOnlyAppConfig,
                                          val executionContext: ExecutionContext,
                                          customUserLoginPage: CustomUserLoginPage,
                                          dynamicStubService: DynamicStubService,
                                          dateService: DateServiceInterface,
                                          val customAuthConnector: CustomAuthConnector,
                                          optOutCustomDataService: OptOutCustomDataService) extends BaseController with I18nSupport {

  val postAction: Call = routes.CustomUserLoginController.createCustomUser()

  def show(userCode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    Future.successful(Ok(customUserLoginPage(userCode = userCode, postAction = postAction, createCustomUserForm = CustomUserCreateForm.form)))
  }

  def createCustomUser(userCode: Option[String]): Action[AnyContent] = Action.async { implicit request =>
    CustomUserCreateForm.form.bindFromRequest().fold(
      formWithErrors => Future.successful(BadRequest(customUserLoginPage(postAction = postAction, createCustomUserForm = formWithErrors, userCode = userCode))),
      formData =>
        val previousTaxYear = TaxYear(dateService.getCurrentTaxYearStart.getYear, dateService.getCurrentTaxYearEnd).addYears(-1).formatAsShortYearRange
        for {
          customUserData    <- dynamicStubService.createCustomUser(CreateCustomUserModel(formData.userCode, previousTaxYear))
          _ <- updateTestDataForOptOut(customUserData.nino, customUserData.crystallisationStatus, customUserData.cyMinusOneItsaStatus, customUserData.cyItsaStatus, customUserData.cyPlusOneItsaStatus)
          (authExchange, _) <- customAuthConnector.login(customUserData.nino, customUserData.isAgent, customUserData.isSupportingAgent)
          redirectURL       = if (customUserData.isAgent) s"report-quarterly/income-and-expenses/view/test-only/stub-client/nino/${customUserData.nino}/utr/" + customUserData.utr else s"report-quarterly/income-and-expenses/view?origin=BTA"
          (bearer, auth)    = (authExchange.bearerToken, authExchange.sessionAuthorityUri)
          homePage          = s"${appConfig.itvcFrontendEnvironment}/$redirectURL"
        } yield {
          successRedirect(bearer, auth, homePage)
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

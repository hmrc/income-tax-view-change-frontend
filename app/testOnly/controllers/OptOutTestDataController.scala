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

import config.featureswitch.{FeatureSwitching, TimeMachineAddYear}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import models.calculationList.CalculationListResponseModel
import models.core.Nino
import models.itsaStatus.ITSAStatusResponseModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import services.{CalculationListService, DateServiceInterface}
import testOnly.models.ItsaStatusCyMinusOne
import testOnly.services.DynamicStubService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AuthenticatorPredicate

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class OptOutTestDataController @Inject()(
                                          val auth: AuthenticatorPredicate,
                                          val appConfig: FrontendAppConfig,
                                          val calculationListService: CalculationListService,
                                          val dynamicStubService: DynamicStubService,
                                          implicit val dateService: DateServiceInterface,
                                          implicit val mcc: MessagesControllerComponents,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler
                                        )
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def retrieveData(nino: String)(implicit hc: HeaderCarrier, dateService: DateServiceInterface, request: Request[_]): Future[Result] = {

    val cyMinusOneTaxYearRange = dateService.getCurrentTaxYearMinusOneRange(isEnabled(TimeMachineAddYear))

    val crystallisationStatusResult: Future[CalculationListResponseModel] = calculationListService.getCalculationList(nino = Nino(nino), taxYearRange = cyMinusOneTaxYearRange)
    val itsaStatusCyMinusOneResult: Future[ITSAStatusResponseModel] = dynamicStubService.getITSAStatusDetail(ItsaStatusCyMinusOne(appConfig)(""), nino)


    val combinedResults: Future[(CalculationListResponseModel, ITSAStatusResponseModel)] = for {
      crystallisationStatusResponse <- crystallisationStatusResult
      itsaStatusResponse <- itsaStatusCyMinusOneResult
    } yield (crystallisationStatusResponse, itsaStatusResponse)

    combinedResults.map { seqResult =>
      Ok(s"Crystallisation Status:    ${Json.toJson(seqResult._1)}\n" +
         s"ITSA Status:               ${Json.toJson(seqResult._2)}")
    }.recover {
      case ex: Throwable =>
        val errorHandler = if (false) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application")
          .error(s"[OptOutTestDataController][retrieveData] - Could not retrieve Opt Out user Data, status: - ${ex.getMessage} - ${ex.getCause} - ")
        errorHandler.showInternalServerError()
    }

  }

  val show: Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      retrieveData(
        nino = user.nino
      )
  }

}

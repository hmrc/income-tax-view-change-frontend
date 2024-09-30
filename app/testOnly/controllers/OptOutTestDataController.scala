/*
 * Copyright 2024 HM Revenue & Customs
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

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import models.calculationList.CalculationListResponseModel
import models.core.Nino
import models.incomeSourceDetails.TaxYear
import models.itsaStatus.ITSAStatusResponseModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import services.{CalculationListService, DateServiceInterface}
import testOnly.services.DynamicStubService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.AuthenticatorPredicate

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OptOutTestDataController @Inject()(
                                          val auth: AuthenticatorPredicate,
                                          val appConfig: FrontendAppConfig,
                                          val calculationListService: CalculationListService,
                                          val dynamicStubService: DynamicStubService,
                                          implicit val dateService: DateServiceInterface,
                                          implicit val mcc: MessagesControllerComponents,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          implicit val executionContext: ExecutionContext
                                        )
  extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  private def retrieveData(nino: String, isAgent: Boolean)
                          (implicit hc: HeaderCarrier, request: MtdItUser[_]): Future[Result] = {

    val taxYear: TaxYear = TaxYear(
      dateService.getCurrentTaxYearStart.getYear,
      dateService.getCurrentTaxYearEnd
    )
    val taxYearPlusOne = taxYear.addYears(1)
    val taxYearMinusOne = taxYear.addYears(-1)

    def combinedResults: Future[(CalculationListResponseModel, ITSAStatusResponseModel, ITSAStatusResponseModel, ITSAStatusResponseModel, List[ITSAStatusResponseModel])] = for {
      crystallisationStatusResponseCyMinusOne <- cyMinusOneCrystallisationStatusResult(nino, taxYear.addYears(-1))
      itsaStatusResponseCyMinusOne <- dynamicStubService.getITSAStatusDetail(taxYearMinusOne, nino)
      itsaStatusResponseCy <- dynamicStubService.getITSAStatusDetail(taxYear, nino)
      itsaStatusResponseCyPlusOne <- dynamicStubService.getITSAStatusDetail(taxYearPlusOne, nino)
      itsaStatusFutureYearResponseCyMinusOne <- dynamicStubService.getITSAStatusDetail(taxYearMinusOne, nino, futureYears = true)
    } yield (
      crystallisationStatusResponseCyMinusOne,
      itsaStatusResponseCyMinusOne.head,
      itsaStatusResponseCy.head,
      itsaStatusResponseCyPlusOne.head,
      itsaStatusFutureYearResponseCyMinusOne)

    combinedResults.map { seqResult =>
      Ok(
        s"Crystallisation Status:               ${Json.toJson(seqResult._1)}\n" +
          s"ITSA Status CY-1:                   ${Json.toJson(seqResult._2)}\n" +
          s"ITSA Status CY:                     ${Json.toJson(seqResult._3)}\n" +
          s"ITSA Status CY+1:                   ${Json.toJson(seqResult._4)}\n" +
          s"ITSA Status Future Year from CY-1:  ${Json.toJson(seqResult._5)}\n")

    }.recover {
      case ex: Throwable =>
        val errorHandler = if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler
        Logger("application").error(s"${if (isAgent) "Agent" else "Individual"} - Could not retrieve Opt Out user Data, status: - ${ex.getMessage} - ${ex.getCause} - ")
        errorHandler.showInternalServerError()
    }

  }

  private def cyMinusOneCrystallisationStatusResult(nino: String, taxYear: TaxYear)
                                                   (implicit hc: HeaderCarrier): Future[CalculationListResponseModel] = {

    if (taxYear.endYear >= 2024) {
      calculationListService.getCalculationList(nino = Nino(nino), taxYearRange = taxYear.formatTaxYearRange)
    } else {
      calculationListService.getLegacyCalculationList(nino = Nino(nino), taxYearEnd = taxYear.endYear.toString)
    }
  }

  val show: Action[AnyContent] = auth.authenticatedAction(isAgent = false) {
    implicit user =>
      retrieveData(
        nino = user.nino,
        isAgent = false
      )
  }

  def showAgent(): Action[AnyContent] = auth.authenticatedAction(isAgent = true) {
    implicit mtdItUser =>
      retrieveData(
        nino = mtdItUser.nino,
        isAgent = true
      )
  }

}

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

package controllers.newHomePage

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{FrontendAppConfig, ItvcErrorHandler}
import models.admin.MortgageEvidence
import models.incomeSourceDetails.TaxYear
import models.liabilitycalculation.{LiabilityCalculationError, LiabilityCalculationResponse}
import models.newHomePage.ProofOfYourIncomeCardViewModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.*
import services.{CalculationService, DateServiceInterface, ITSAStatusService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.partials.newHome.overview.ProofOfYourIncomeView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProofOfYourIncomeController @Inject()(val authActions: AuthActions,
                                            val view: ProofOfYourIncomeView,
                                            val ITSAStatusService: ITSAStatusService,
                                            calcService: CalculationService,
                                            itvcErrorHandler: ItvcErrorHandler,
                                            implicit val dateService: DateServiceInterface)
                                           (implicit val ec: ExecutionContext,
                                            mcc: MessagesControllerComponents,
                                            val appConfig: FrontendAppConfig) extends FrontendController(mcc) with I18nSupport with FeatureSwitching {

  def show(origin: Option[String] = None): Action[AnyContent] = authActions.asMTDIndividual().async {
    implicit user =>
      if (isEnabled(MortgageEvidence)) {
        handleRequest(controllers.routes.HomeController.show(origin).url, false)
      } else {
        Future.successful(Redirect(controllers.newHomePage.routes.HandleYourTasksController.show()))
      }
  }

  def showAgent(): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient().async {
    implicit mtdItUser =>
      if (isEnabled(MortgageEvidence)) {
        handleRequest(controllers.routes.HomeController.showAgent().url, true)
      } else {
        Future.successful(Redirect(controllers.newHomePage.routes.HandleYourTasksController.showAgent()))
      }
  }

  def handleRequest(backUrl: String, isAgent: Boolean)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {

    if (user.saUtr.isDefined) {
      val resultFuture: Future[Seq[ProofOfYourIncomeCardViewModel]] = for {
        itsaStatusDetails <- ITSAStatusService.getITSAStatusDetail(TaxYear(2021, 2022), true, false)
        proofOfYourIncomeViewModels <- Future.sequence(assembleProofOfYourIncomeModel(itsaStatusDetails.map(_.taxYear), user.saUtr.get))
        finalModel = addLegacyData(proofOfYourIncomeViewModels, user.saUtr.get)
      } yield finalModel

      resultFuture.map { viewModels =>
        val sortedModels = viewModels.sortBy(_.taxYearStart).reverse
        Ok(view(Some(backUrl), isAgent, sortedModels))
      }.recover {
        case e: Exception =>
          itvcErrorHandler.showInternalServerError()
      }
    }
  else
  {
    Future.successful(itvcErrorHandler.showInternalServerError())
  }
}


  private def assembleProofOfYourIncomeModel (taxYears: List[String], saUtr: String)
                   (implicit user: MtdItUser[_], hc: HeaderCarrier, ec: ExecutionContext): Seq[Future[ProofOfYourIncomeCardViewModel]] = {
    taxYears.map { taxYear =>
      val taxYearFormatted = taxYear.split("-")(0).toInt
      calcService.getLiabilityCalculationDetail(user.mtditid, user.nino, taxYearFormatted).flatMap {
        case calcResponse: LiabilityCalculationResponse =>
          Future.successful(ProofOfYourIncomeCardViewModel(taxYearFormatted, calcResponse.metadata.calculationType,
            dateService.getCurrentTaxYearStart.getYear, false, saUtr))
        case calcError: LiabilityCalculationError if calcError.status == NO_CONTENT =>
          Logger("application").info(s"No data for tax year: ${taxYearFormatted}")
          Future.failed(new Exception("NO_CONTENT"))
        case _ =>
          Logger("application").error("Unexpected error has occurred while retrieving calculation data.")
          Future.failed(new Exception("CALC_ERROR"))
      }
    }
  }

  private def addLegacyData(model: Seq[ProofOfYourIncomeCardViewModel], saUtr: String): Seq[ProofOfYourIncomeCardViewModel] = {
    val earliestYear = model.filter(_.calculationTypeIsValid).map(_.taxYearStart).min
    val legacyYearStart = earliestYear - 1

    model ++ Seq(ProofOfYourIncomeCardViewModel(legacyYearStart, "", dateService.getCurrentTaxYearStart.getYear, true, saUtr))
  }
}

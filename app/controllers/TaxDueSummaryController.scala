/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.predicates._
import implicits.ImplicitDateFormatter
import javax.inject.{Inject, Singleton}
import models.calculation._
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.CalculationService
import uk.gov.hmrc.play.language.LanguageUtils

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class TaxDueSummaryController @Inject()(checkSessionTimeout: SessionTimeoutPredicate,
                                        authenticate: AuthenticationPredicate,
                                        retrieveNino: NinoPredicate,
                                        retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                        calculationService: CalculationService,
                                        itvcErrorHandler: ItvcErrorHandler)
                                       (implicit val appConfig: FrontendAppConfig,
                                        val languageUtils: LanguageUtils,
                                        mcc: MessagesControllerComponents,
                                        val executionContext: ExecutionContext) extends BaseController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  val action: ActionBuilder[MtdItUser, AnyContent] = checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources


  def showTaxDueSummary(taxYear: Int): Action[AnyContent] = {

    action.async {
      implicit user => {
        calculationService.getCalculationDetail(user.nino, taxYear).flatMap {
          case calcDisplayModel: CalcDisplayModel =>
            Future.successful(Ok(views.html.taxCalcBreakdown(calcDisplayModel, taxYear, backUrl(taxYear))))

          case CalcDisplayNoDataFound =>
            Logger.warn(s"[TaxDueController][showTaxDueSummary[$taxYear]] No tax due data could be retrieved. Not found")
            Future.successful(itvcErrorHandler.showInternalServerError())

          case CalcDisplayError =>
            Logger.error(s"[TaxDueController][showTaxDueSummary[$taxYear]] No tax due data could be retrieved. Downstream error")
            Future.successful(itvcErrorHandler.showInternalServerError())
        }
      }
    }
  }

  def backUrl(taxYear: Int): String = controllers.routes.TaxYearOverviewController.renderTaxYearOverviewPage(taxYear).url

}

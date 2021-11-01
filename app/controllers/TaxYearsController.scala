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

import config.featureswitch.{FeatureSwitching, ITSASubmissionIntegration}
import config.{FrontendAppConfig, ItvcErrorHandler, ItvcHeaderCarrierForPartialsConverter}
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NinoPredicate, SessionTimeoutPredicate}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import views.html.TaxYears

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxYearsController @Inject() (taxYearsView: TaxYears)
                                   (implicit val appConfig: FrontendAppConfig,
                                   mcc: MessagesControllerComponents,
                                   implicit val executionContext: ExecutionContext,
                                   val checkSessionTimeout: SessionTimeoutPredicate,
                                   val authenticate: AuthenticationPredicate,
                                   val retrieveNino: NinoPredicate,
                                   val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                   val itvcHeaderCarrierForPartialsConverter: ItvcHeaderCarrierForPartialsConverter,
                                   val itvcErrorHandler: ItvcErrorHandler
                                  ) extends BaseController with I18nSupport with FeatureSwitching {

  val viewTaxYears: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino andThen retrieveIncomeSources).async {
    implicit user =>
      user.incomeSources.orderedTaxYearsByAccountingPeriods match {
        case taxYears if taxYears.nonEmpty =>
          Future.successful(Ok(taxYearsView(taxYears = taxYears, backUrl = backUrl, utr = user.saUtr, isEnabled(ITSASubmissionIntegration))))
        case _ => Future.successful(itvcErrorHandler.showInternalServerError)
      }
  }

  lazy val backUrl: String = controllers.routes.HomeController.home().url

}

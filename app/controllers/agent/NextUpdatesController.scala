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

package controllers.agent

import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch.FeatureSwitching
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import implicits.{ImplicitDateFormatter, ImplicitDateFormatterImpl}
import models.reportDeadlines.ObligationsModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{IncomeSourceDetailsService, ReportDeadlinesService}
import uk.gov.hmrc.http.NotFoundException
import uk.gov.hmrc.play.language.LanguageUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NextUpdatesController @Inject()(val incomeSourceDetailsService: IncomeSourceDetailsService,
                                      val reportDeadlinesService: ReportDeadlinesService,
                                      val authorisedFunctions: FrontendAuthorisedFunctions)
                                     (implicit val appConfig: FrontendAppConfig,
                                      val languageUtils: LanguageUtils,
                                      mcc: MessagesControllerComponents,
                                      implicit val ec: ExecutionContext,
                                      dateFormatter: ImplicitDateFormatterImpl,
                                      val itvcErrorHandler: ItvcErrorHandler)
  extends ClientConfirmedController with ImplicitDateFormatter with FeatureSwitching with I18nSupport {

  private def view(obligationsModel: ObligationsModel, backUrl: String)
                  (implicit request: Request[_], user: MtdItUser[_]): Html = {
    views.html.agent.nextUpdates(
      currentObligations = obligationsModel,
      implicitDateFormatter = dateFormatter,
      backUrl = backUrl
    )
  }

  def getNextUpdates: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
			getMtdItUserWithIncomeSources(incomeSourceDetailsService).flatMap {
				mtdItUser =>
					reportDeadlinesService.getReportDeadlines()(implicitly, mtdItUser).map {
						case nextUpdates: ObligationsModel if nextUpdates.obligations.nonEmpty => Ok(view(nextUpdates, backUrl)(implicitly, mtdItUser))
						case _ => itvcErrorHandler.showInternalServerError()
					}
			}
  }

  lazy val backUrl: String = controllers.agent.routes.HomeController.show().url

}

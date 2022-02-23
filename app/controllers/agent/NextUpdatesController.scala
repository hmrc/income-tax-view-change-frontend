/*
 * Copyright 2022 HM Revenue & Customs
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
import config.{AgentItvcErrorHandler, FrontendAppConfig}
import controllers.agent.predicates.ClientConfirmedController
import models.nextUpdates.ObligationsModel
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{IncomeSourceDetailsService, NextUpdatesService}
import uk.gov.hmrc.play.language.LanguageUtils

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class NextUpdatesController @Inject()(agentNextUpdates: views.html.NextUpdates,
                                      incomeSourceDetailsService: IncomeSourceDetailsService,
                                      nextUpdatesService: NextUpdatesService,
                                      implicit val appConfig: FrontendAppConfig,
                                      val authorisedFunctions: FrontendAuthorisedFunctions)
                                     (implicit val languageUtils: LanguageUtils,
                                      mcc: MessagesControllerComponents,
                                      val ec: ExecutionContext,
                                      itvcErrorHandler: AgentItvcErrorHandler)
  extends ClientConfirmedController with FeatureSwitching with I18nSupport {

  private def view(obligationsModel: ObligationsModel, backUrl: String, isAgent: Boolean)
                  (implicit user: MtdItUser[_]): Html = {
    agentNextUpdates(
      currentObligations = obligationsModel,
      backUrl = backUrl,
      isAgent = isAgent
    )
  }

  def getNextUpdates: Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      getMtdItUserWithIncomeSources(incomeSourceDetailsService, useCache = false).flatMap {
        mtdItUser =>
          nextUpdatesService.getNextUpdates()(implicitly, mtdItUser).map {
            case nextUpdates: ObligationsModel if nextUpdates.obligations.nonEmpty => Ok(view(nextUpdates, backUrl, isAgent = true)(mtdItUser))
            case _ => itvcErrorHandler.showInternalServerError()
          }
      }
  }

  lazy val backUrl: String = controllers.agent.routes.HomeController.show().url

}

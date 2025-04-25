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

package controllers.optIn

import auth.MtdItUser
import auth.authV2.AuthActions
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import models.incomeSourceDetails.TaxYear
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.optIn.OptInService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.optIn.BeforeYouStart

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class BeforeYouStartController @Inject()(authActions: AuthActions,
                                         val beforeYouStart: BeforeYouStart,
                                         val optInService: OptInService,
                                         val itvcErrorHandler: ItvcErrorHandler,
                                         val itvcErrorHandlerAgent: AgentItvcErrorHandler)
                                        (implicit val appConfig: FrontendAppConfig,
                                         val ec: ExecutionContext,
                                         mcc: MessagesControllerComponents
                                        )
  extends FrontendController(mcc) with FeatureSwitching with I18nSupport {

  private val errorHandler = (isAgent: Boolean) => if (isAgent) itvcErrorHandlerAgent else itvcErrorHandler

  private def withRecover(isAgent: Boolean)(code: => Future[Result])(implicit mtdItUser: MtdItUser[_]): Future[Result] = {
    code.recover {
      case ex: Exception =>
        Logger("application").error(s"request failed :: $ex")
        errorHandler(isAgent).showInternalServerError()
    }
  }

  private def startButtonUrl(isAgent: Boolean, availableYears: Seq[TaxYear]) = {
    availableYears match {
      case Seq(_) =>
        controllers.optIn.routes.SingleTaxYearOptInWarningController.show(isAgent)
      case _ => controllers.optIn.routes.ChooseYearController.show(isAgent)
    }
  }

  def show(isAgent: Boolean = false): Action[AnyContent] = authActions.asMTDIndividualOrAgentWithClient(isAgent).async {
    implicit user =>
      withRecover(isAgent) {
        optInService.availableOptInTaxYear().flatMap { availableYears =>
          Future.successful(Ok(beforeYouStart(isAgent, startButtonUrl(isAgent, availableYears).url)))
        }
      }
  }
}
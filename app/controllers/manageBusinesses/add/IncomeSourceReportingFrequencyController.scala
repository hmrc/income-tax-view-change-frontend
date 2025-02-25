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

//
//package controllers.manageBusinesses.add
//
//import auth.MtdItUser
//import auth.authV2.AuthActions
//import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
//import enums.IncomeSourceJourney.IncomeSourceType
//import forms.manageBusinesses.add.IncomeSourceReportingFrequencyForm
//import forms.optOut.ConfirmOptOutSingleTaxYearForm
//import play.api.i18n.I18nSupport
//import play.api.mvc._
//import uk.gov.hmrc.http.HeaderCarrier
//import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
//import utils.IncomeSourcesUtils
//import views.html.ReportingFrequencyView
//import views.html.manageBusinesses.add.{IncomeSourceReportingFrequency, IncomeSourceReportingMethodNotSaved}
//
//import javax.inject.{Inject, Singleton}
//import scala.concurrent.{ExecutionContext, Future}
//
//@Singleton
//class IncomeSourceReportingFrequencyController @Inject()(val authActions: AuthActions,
//                                                         val view: IncomeSourceReportingFrequency,
//                                                         val itvcAgentErrorHandler: AgentItvcErrorHandler,
//                                                         val itvcErrorHandler: ItvcErrorHandler)
//                                                        (implicit val ec: ExecutionContext,
//                                                               val mcc: MessagesControllerComponents,
//                                                              val appConfig: FrontendAppConfig) extends FrontendController(mcc)
//  with I18nSupport with IncomeSourcesUtils {
//  //Delete this controller
//  def handleRequest(isAgent: Boolean)
//                   (implicit user: MtdItUser[_], hc: HeaderCarrier): Future[Result] = withIncomeSourcesFS {
//
//    val action: Call =
//      if (isAgent)
//        controllers.manageBusinesses.add.routes.IncomeSourceReportingFrequencyController.showAgent()
//      else
//        controllers.manageBusinesses.add.routes.IncomeSourceReportingFrequencyController.show()
//
//    Future.successful(Ok(view(continueAction = action, isAgent = isAgent, form = IncomeSourceReportingFrequencyForm())))
//  }
//
//
//  def show(): Action[AnyContent] = authActions.asMTDIndividual.async {
//    implicit user =>
//      handleRequest(
//        isAgent = false
//      )
//  }
//
//  def showAgent(): Action[AnyContent] = authActions.asMTDAgentWithConfirmedClient.async {
//    implicit mtdItUser =>
//      handleRequest(
//        isAgent = true
//      )
//  }
//}

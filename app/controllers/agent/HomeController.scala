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

import audit.AuditingService
import audit.models.HomeAudit
import auth.{FrontendAuthorisedFunctions, MtdItUser}
import config.featureswitch._
import config.{FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import models.financialDetails.{FinancialDetailsErrorModel, FinancialDetailsModel, FinancialDetailsResponseModel}
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, _}
import play.twirl.api.Html
import services._
import uk.gov.hmrc.http.InternalServerException
import views.html.agent.Home

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class HomeController @Inject()(home: Home,
                               nextUpdatesService: NextUpdatesService,
                               financialDetailsService: FinancialDetailsService,
                               incomeSourceDetailsService: IncomeSourceDetailsService,
                               auditingService: AuditingService,
                               val authorisedFunctions: FrontendAuthorisedFunctions)
                              (implicit mcc: MessagesControllerComponents,
                               implicit val appConfig: FrontendAppConfig,
                               val itvcErrorHandler: ItvcErrorHandler,
                               implicit val ec: ExecutionContext) extends ClientConfirmedController with I18nSupport with FeatureSwitching {


  private def view(nextPaymentOrOverdue: Option[Either[(LocalDate, Boolean), Int]],
                   nextUpdateOrOverdue: Either[(LocalDate, Boolean), Int],
                   overduePaymentExists: Boolean,
                   dunningLockExists: Boolean,
                   currentTaxYear: Int)
                  (implicit request: Request[_], user: MtdItUser[_]): Html = {
    home(
      nextPaymentOrOverdue = nextPaymentOrOverdue,
      nextUpdateOrOverdue = nextUpdateOrOverdue,
      overduePaymentExists = overduePaymentExists,
      paymentHistoryEnabled = isEnabled(PaymentHistory),
      ITSASubmissionIntegrationEnabled = isEnabled(ITSASubmissionIntegration),
      dunningLockExists = dunningLockExists,
      currentTaxYear = currentTaxYear
    )
  }

  def show(): Action[AnyContent] = Authenticated.async { implicit request =>
    implicit user =>
      for {
        mtdItUser <- getMtdItUserWithIncomeSources(incomeSourceDetailsService)
        dueObligationDetails <- nextUpdatesService.getObligationDueDates()(implicitly, implicitly, mtdItUser)
        unpaidFinancialDetails <- financialDetailsService.getAllUnpaidFinancialDetails(mtdItUser, implicitly, implicitly)
        _ = if(unpaidFinancialDetails.exists(fds => fds.isInstanceOf[FinancialDetailsErrorModel]
          && fds.asInstanceOf[FinancialDetailsErrorModel].code != NOT_FOUND))
          throw new InternalServerException("[FinancialDetailsService][getChargeDueDates] - Failed to retrieve successful financial details")
        dueChargesDetails = financialDetailsService.getChargeDueDates(unpaidFinancialDetails)
        dunningLockExistsValue = dunningLockExists(unpaidFinancialDetails)
      } yield {
        if (isEnabled(TxmEventsApproved)) {
          auditingService.extendedAudit(HomeAudit(
            mtdItUser, dueChargesDetails, dueObligationDetails
          ))
        }
        Ok(view(dueChargesDetails, dueObligationDetails, overduePaymentExists(dueChargesDetails), dunningLockExistsValue,
          currentTaxYear = mtdItUser.incomeSources.getCurrentTaxEndYear)(implicitly, mtdItUser))
      }
  }

  private def dunningLockExists(financialDetailsResponseModel: List[FinancialDetailsResponseModel]): Boolean = {
    financialDetailsResponseModel.collectFirst {
      case fdm: FinancialDetailsModel if fdm.dunningLockExists => true
    }.isDefined
  }

  private def overduePaymentExists(nextPaymentOrOverdue: Option[Either[(LocalDate, Boolean), Int]]): Boolean = {
    nextPaymentOrOverdue match {
      case Some(_@Left((_, true))) => true
      case Some(_@Right(overdueCount)) if overdueCount > 0 => true
      case _ => false
    }
  }

}

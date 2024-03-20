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

//package services
//
//import audit.AuditingService
//import audit.models.HomeAudit
//import auth.MtdItUser
//import config.featureswitch._
//import controllers.HomeController
//import models.homePage.{HomeControllerViewModel, PaymentCreditAndRefundHistoryTileViewModel}
//import models.nextUpdates.NextUpdatesTileViewModel
//import play.api.mvc.Result
//import play.api.mvc._
//
//import java.time.LocalDate
//import javax.inject.Inject
//import scala.concurrent.{ExecutionContext, Future}
//
//class HomeService @Inject()(val dateService: DateServiceInterface,
//                            val financialDetailsService: FinancialDetailsService,
//                            homeController: HomeController,
//                            auditingService: AuditingService
//                           ) (implicit val ec: ExecutionContext) extends FeatureSwitching {
//
//    def getHomeControllerViewModel: Future[HomeControllerViewModel] = {
//
//      def buildHomePage(nextUpdatesDueDates: Seq[LocalDate], isAgent: Boolean)
//                               (implicit user: MtdItUser[_]): Future[Result] =
//      for {
//        unpaidCharges <- financialDetailsService.getAllUnpaidFinancialDetails(isEnabled(CodingOut))
//        paymentsDue = homeController.getDueDates(unpaidCharges)
//        dunningLockExists = homeController.hasDunningLock(unpaidCharges)
//        outstandingChargesModel <- homeController.getOutstandingChargesModel(unpaidCharges)
//        outstandingChargeDueDates = homeController.getRelevantDates(outstandingChargesModel)
//        overDuePaymentsCount = homeController.calculateOverduePaymentsCount(paymentsDue, outstandingChargesModel)
//        paymentsDueMerged = homeController.mergePaymentsDue(paymentsDue, outstandingChargeDueDates)
//      } yield {
//
//        val nextUpdatesTileViewModel = NextUpdatesTileViewModel(nextUpdatesDueDates, dateService.getCurrentDate)
//
//        auditingService.extendedAudit(HomeAudit(user, paymentsDueMerged, overDuePaymentsCount, nextUpdatesTileViewModel))
//
//        val paymentCreditAndRefundHistoryTileViewModel =
//          PaymentCreditAndRefundHistoryTileViewModel(unpaidCharges, isEnabled(CreditsRefundsRepay), isEnabled(PaymentHistoryRefunds))
//
//        Ok(homeController.view(
//          isAgent = isAgent,
//          dunningLockExists = dunningLockExists,
//          nextPaymentDueDate = paymentsDueMerged,
//          currentTaxYear = dateService.getCurrentTaxYearEnd,
//          overDuePaymentsCount = Some(overDuePaymentsCount),
//          nextUpdatesTileViewModel = nextUpdatesTileViewModel,
//          displayCeaseAnIncome = user.incomeSources.hasOngoingBusinessOrPropertyIncome,
//          paymentCreditAndRefundHistoryTileViewModel = paymentCreditAndRefundHistoryTileViewModel
//        ))
//      }
//
//}

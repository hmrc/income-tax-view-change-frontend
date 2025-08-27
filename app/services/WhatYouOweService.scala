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

package services

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.FeatureSwitching
import connectors.{FinancialDetailsConnector, OutstandingChargesConnector}
import models.financialDetails.ChargeItem.isAKnownTypeOfCharge
import models.financialDetails._
import models.incomeSourceDetails.TaxYear
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.HeaderCarrier
import models.admin._
import models.financialDetails.{ChargeItem, WhatYouOweViewModel}
import models.core.Nino
import models.nextPayments.viewmodels.WYOClaimToAdjustViewModel
import play.api.Logger

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYouOweService @Inject()(val financialDetailsService: FinancialDetailsService,
                                  val claimToAdjustService: ClaimToAdjustService,
                                  val selfServeTimeToPayService: SelfServeTimeToPayService,
                                  val financialDetailsConnector: FinancialDetailsConnector,
                                  val outstandingChargesConnector: OutstandingChargesConnector,
                                  implicit val dateService: DateServiceInterface)
                                 (implicit ec: ExecutionContext, implicit val appConfig: FrontendAppConfig)
  extends TransactionUtils with FeatureSwitching {

  implicit lazy val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  def getWhatYouOweChargesList(isFilterCodedOutPoasEnabled: Boolean,
                               isPenaltiesEnabled: Boolean,
                               remainingToPayByChargeOrInterestWhenChargeIsPaidOrNot: PartialFunction[ChargeItem, ChargeItem])
                              (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[WhatYouOweChargesList] = {
    {
      for {
        unpaidChanges <- financialDetailsService.getAllUnpaidFinancialDetails()
      } yield getWhatYouOweChargesList(unpaidChanges, isFilterCodedOutPoasEnabled, isPenaltiesEnabled, remainingToPayByChargeOrInterestWhenChargeIsPaidOrNot)
    }.flatten
  }

  def getWhatYouOweChargesList(unpaidCharges: List[FinancialDetailsResponseModel],
                               isFilterCodedOutPoasEnabled: Boolean,
                               isPenaltiesEnabled: Boolean,
                               remainingToPayByChargeOrInterestWhenChargeIsPaidOrNot: PartialFunction[ChargeItem, ChargeItem])
                              (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[WhatYouOweChargesList] = {

    unpaidCharges match {
      case financialDetails: List[FinancialDetailsResponseModel] if financialDetails.exists(_.isInstanceOf[FinancialDetailsErrorModel]) =>
        throw new Exception("Error response while getting Unpaid financial details")
      case financialDetails =>
        val financialDetailsModelList = financialDetails.asInstanceOf[List[FinancialDetailsModel]]
        val balanceDetails = financialDetailsModelList.headOption
          .map(_.balanceDetails).getOrElse(BalanceDetails(0.00, 0.00, 0.00, None, None, None, None, None))

        val codingOutDetails: Option[CodingOutDetails] = financialDetailsModelList.map(_.codingDetails).maxByOption(_.size).getOrElse(List()).find(
          _.taxYearReturn.contains(dateService.getCurrentTaxYear.startYear.toString)).flatMap(_.toCodingOutDetails)

        val whatYouOweChargesList = WhatYouOweChargesList(
          balanceDetails = balanceDetails,
          chargesList = getFilteredChargesList(financialDetailsModelList,
            isFilterCodedOutPoasEnabled,
            isPenaltiesEnabled,
            remainingToPayByChargeOrInterestWhenChargeIsPaidOrNot),
          codedOutDetails = codingOutDetails)

        {
          for {
            clientUtr <- mtdUser.saUtr
            yearOfMigrationAsString <- mtdUser.incomeSources.yearOfMigration
          } yield
            callOutstandingCharges(dateService.getCurrentTaxYear, yearOfMigrationAsString, clientUtr).map {
              case Some(outstandingChargesModel) => whatYouOweChargesList.copy(outstandingChargesModel = Some(outstandingChargesModel))
              case _ => whatYouOweChargesList
            }
        }.getOrElse {
          Future.successful(whatYouOweChargesList)
        }
    }
  }

  private def callOutstandingCharges(currentTaxYear: TaxYear, yearOfMigration: String, utr: String)
                                    (implicit headerCarrier: HeaderCarrier): Future[Option[OutstandingChargesModel]] = {
    // Move this comparison on the type level: compare TaxYear with TaxYear
    if (yearOfMigration.toInt >= currentTaxYear.startYear) {
      val saPreviousYear = (yearOfMigration.toInt - 1).toString
      outstandingChargesConnector.getOutstandingCharges("utr", utr, saPreviousYear) map {
        case outstandingChargesModel: OutstandingChargesModel =>
          Some(outstandingChargesModel)
        case outstandingChargesErrorModel: OutstandingChargesErrorModel if outstandingChargesErrorModel.code == NOT_FOUND =>
          None
        case _ =>
          throw new Exception("Error response while getting outstanding charges")
      }
    } else {
      Future.successful(None)
    }
  }

  def getFilteredChargesList(financialDetailsList: List[FinancialDetailsModel],
                             isFilterCodedOutPoasEnabled: Boolean,
                             isPenaltiesEnabled: Boolean,
                             remainingToPayByChargeOrInterestWhenChargeIsPaidOrNot: PartialFunction[ChargeItem, ChargeItem]): List[ChargeItem] = {

    def getChargeItem(financialDetails: List[FinancialDetail]): DocumentDetail => Option[ChargeItem] =
      getChargeItemOpt(financialDetails)

    financialDetailsList
      .flatMap(financialDetails => {
        financialDetails
          .getAllDocumentDetailsWithDueDates()
          .flatMap(dd => getChargeItem(financialDetails.financialDetails)(dd.documentDetail))
      })
      .filter(isAKnownTypeOfCharge)
      .filterNot(_.codedOutStatus.contains(Accepted))
      .filterNot(_.isPenalty && !isPenaltiesEnabled)
      .collect(remainingToPayByChargeOrInterestWhenChargeIsPaidOrNot)
      .filter(_.notCodedOutPoa(isFilterCodedOutPoasEnabled))
      .sortBy(_.dueDate.get)
  }

  def createWhatYouOweViewModel(backUrl: String,
                                origin: Option[String],
                                creditAndRefundUrl: String,
                                taxYearSummaryUrl: Int => String,
                                adjustPoaUrl: String,
                                chargeSummaryUrl: (Int, String, Boolean, Option[String]) => String,
                                paymentHandOffUrl: Long => String)
                               (implicit user: MtdItUser[_], headerCarrier: HeaderCarrier): Future[Option[WhatYouOweViewModel]] = {
    for {
      whatYouOweChargesList        <- getWhatYouOweChargesList(isEnabled(FilterCodedOutPoas), isEnabled(PenaltiesAndAppeals), mainChargeIsNotPaidFilter)
      ctaViewModel                 <- claimToAdjustViewModel(Nino(user.nino))
      lpp2Url                       = getSecondLatePaymentPenaltyLink(whatYouOweChargesList.chargesList, user.isAgent())
      hasOverdueCharges             = whatYouOweChargesList.chargesList.exists(_.isOverdue()(dateService))
      hasAccruingInterestRARCharges = whatYouOweChargesList.chargesList.exists(_.isNotPaidAndNotOverduePoaReconciliationDebit()(dateService))
      startUrl                     <- selfServeTimeToPayService.startSelfServeTimeToPayJourney(isEnabled(YourSelfAssessmentCharges))
    } yield (startUrl, lpp2Url) match {
      case (Left(ex), _) =>
        Logger("application").error(s"Unable to retrieve selfServeTimeToPayStartUrl: ${ex.getMessage} - ${ex.getCause}")
        None
      case (_, None) =>
        Logger("application").error("No chargeReference supplied with second late payment penalty. Hand-off url could not be formulated")
        None
      case (Right(startUrl), Some(lpp2Url)) =>
        Some(WhatYouOweViewModel(
          currentDate = dateService.getCurrentDate,
          hasOverdueOrAccruingInterestCharges = hasOverdueCharges || hasAccruingInterestRARCharges,
          whatYouOweChargesList = whatYouOweChargesList,
          hasLpiWithDunningLock = whatYouOweChargesList.hasLpiWithDunningLock,
          currentTaxYear = dateService.getCurrentTaxYearEnd,
          backUrl = backUrl,
          utr = user.saUtr,
          dunningLock = whatYouOweChargesList.hasDunningLock,
          creditAndRefundUrl = creditAndRefundUrl,
          creditAndRefundEnabled = isEnabled(CreditsRefundsRepay)(user),
          taxYearSummaryUrl = taxYearSummaryUrl,
          claimToAdjustViewModel = ctaViewModel,
          lpp2Url = lpp2Url,
          adjustPoaUrl = adjustPoaUrl,
          chargeSummaryUrl = chargeSummaryUrl,
          paymentHandOffUrl = paymentHandOffUrl,
          selfServeTimeToPayEnabled  = isEnabled(SelfServeTimeToPayR17)(user),
          selfServeTimeToPayStartUrl = startUrl
        ))
    }
  }

  private def mainChargeIsNotPaidFilter: PartialFunction[ChargeItem, ChargeItem] = {
    case x if x.remainingToPayByChargeOrInterest > 0 => x
  }

  private def getSecondLatePaymentPenaltyLink(chargeItems: List[ChargeItem], isAgent: Boolean): Option[String] = {

    val LPP2 = chargeItems.find(_.transactionType == SecondLatePaymentPenalty)

    LPP2 match {
      case Some(charge) => charge.chargeReference match {
        case Some(value) if isAgent => Some(appConfig.incomeTaxPenaltiesFrontendLPP2CalculationAgent(value))
        case Some(value)            => Some(appConfig.incomeTaxPenaltiesFrontendLPP2Calculation(value))
        case None                   => None
      }
      case None => Some("")
    }
  }

  private def claimToAdjustViewModel(nino: Nino)(implicit hc: HeaderCarrier, user: MtdItUser[_]): Future[WYOClaimToAdjustViewModel] = {
    claimToAdjustService.getPoaTaxYearForEntryPoint(nino).flatMap {
      case Right(value) => Future.successful(WYOClaimToAdjustViewModel(value))
      case Left(ex: Throwable) => Future.failed(ex)
    }
  }

}

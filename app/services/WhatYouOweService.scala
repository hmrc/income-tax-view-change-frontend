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
import connectors.FinancialDetailsConnector
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYouOweService @Inject()(val financialDetailsService: FinancialDetailsService,
                                  val financialDetailsConnector: FinancialDetailsConnector,
                                  implicit val dateService: DateServiceInterface)
                                 (implicit ec: ExecutionContext, implicit val appConfig: FrontendAppConfig) {

  implicit lazy val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  val validChargeTypeCondition: DocumentDetail => Boolean = documentDetail => {
    (documentDetail.documentText, documentDetail.documentDescription) match {
      case (Some(documentText), _) if documentText.contains("Class 2 National Insurance") => true
      case (_, Some("ITSA- POA 1") | Some("ITSA - POA 2") | Some("TRM New Charge") | Some("TRM Amend Charge")) => true
      case (_, _) => false
    }
  }

  def getCreditCharges()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[List[DocumentDetail]] = {
    financialDetailsService.getAllCreditFinancialDetails.map {
      case financialDetails if financialDetails.exists(_.isInstanceOf[FinancialDetailsErrorModel]) =>
        throw new Exception("[WhatYouOweService][getCreditCharges] Error response while getting Unpaid financial details")
      case financialDetails: List[FinancialDetailsResponseModel] =>
        val financialDetailsModelList = financialDetails.asInstanceOf[List[FinancialDetailsModel]]
        financialDetailsModelList.flatMap(_.documentDetails)
    }
  }

  def getWhatYouOweChargesList(isCodingOutEnabled: Boolean, isMFACreditsEnabled: Boolean, isTimeMachineEnabled: Boolean)(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[WhatYouOweChargesList] = {
    {
      for {
        unpaidChanges <- financialDetailsService.getAllUnpaidFinancialDetails(isCodingOutEnabled)
      } yield getWhatYouOweChargesList(unpaidChanges, isCodingOutEnabled, isMFACreditsEnabled, isTimeMachineEnabled)
    }.flatten
  }

  def getWhatYouOweChargesList(unpaidCharges: List[FinancialDetailsResponseModel], isCodingOutEnabled: Boolean, isMFACreditsEnabled: Boolean, isTimeMachineEnabled: Boolean)
                              (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[WhatYouOweChargesList] = {

    unpaidCharges match {
      case financialDetails: List[FinancialDetailsResponseModel] if financialDetails.exists(_.isInstanceOf[FinancialDetailsErrorModel]) =>
        throw new Exception("[WhatYouOweService][getWhatYouOweChargesList] Error response while getting Unpaid financial details")
      case financialDetails =>
        val financialDetailsModelList = financialDetails.asInstanceOf[List[FinancialDetailsModel]]
        val balanceDetails = financialDetailsModelList.headOption
          .map(_.balanceDetails).getOrElse(BalanceDetails(0.00, 0.00, 0.00, None, None, None, None))
        val codedOutDocumentDetail = if (isCodingOutEnabled) {
          financialDetailsModelList.flatMap(fdm =>
            fdm.documentDetails.find(dd => dd.isPayeSelfAssessment
              && dd.taxYear == (dateService.getCurrentTaxYearEnd - 1))
          ).headOption
        } else None

        val whatYouOweChargesList = WhatYouOweChargesList(
          balanceDetails = balanceDetails,
          chargesList = getFilteredChargesList(financialDetailsModelList, isMFACreditsEnabled, isCodingOutEnabled),
          codedOutDocumentDetail = codedOutDocumentDetail)

        callOutstandingCharges(mtdUser.saUtr, mtdUser.incomeSources.yearOfMigration, dateService.getCurrentTaxYearEnd).map {
          case Some(outstandingChargesModel) => whatYouOweChargesList.copy(outstandingChargesModel = Some(outstandingChargesModel))
          case _ => whatYouOweChargesList
        }
    }
  }

  private def callOutstandingCharges(saUtr: Option[String], yearOfMigration: Option[String], currentTaxYear: Int)
                                    (implicit headerCarrier: HeaderCarrier): Future[Option[OutstandingChargesModel]] = {
    if (saUtr.isDefined && yearOfMigration.isDefined && yearOfMigration.get.toInt >= currentTaxYear - 1) {
      val saPreviousYear = yearOfMigration.get.toInt - 1
      financialDetailsConnector.getOutstandingCharges("utr", saUtr.get, saPreviousYear.toString) map {
        case outstandingChargesModel: OutstandingChargesModel => Some(outstandingChargesModel)
        case outstandingChargesErrorModel: OutstandingChargesErrorModel if outstandingChargesErrorModel.code == 404 => None
        case _ => throw new Exception("[WhatYouOweService][callOutstandingCharges] Error response while getting outstanding charges")
      }
    } else {
      Future.successful(None)
    }
  }

  private def getFilteredChargesList(financialDetailsList: List[FinancialDetailsModel], isMFACreditsEnabled: Boolean, isCodingOutEnabled: Boolean): List[DocumentDetailWithDueDate] = {
    val documentDetailsWithDueDates = financialDetailsList.flatMap(financialDetails =>
      financialDetails.getAllDocumentDetailsWithDueDates(isCodingOutEnabled))
      .filter(documentDetailWithDueDate => whatYouOwePageDataExists(documentDetailWithDueDate)
        && validChargeTypeCondition(documentDetailWithDueDate.documentDetail)
        && !documentDetailWithDueDate.documentDetail.isPayeSelfAssessment
        && documentDetailWithDueDate.documentDetail.checkIfEitherChargeOrLpiHasRemainingToPay)
      .sortBy(_.dueDate.get)
    if (!isMFACreditsEnabled) filterMFADebits(documentDetailsWithDueDates) else documentDetailsWithDueDates
  }

  private def whatYouOwePageDataExists(documentDetailWithDueDate: DocumentDetailWithDueDate): Boolean = {
    documentDetailWithDueDate.documentDetail.documentDescription.isDefined && documentDetailWithDueDate.dueDate.isDefined
  }

  private def filterMFADebits(documentDetailsWithDueDate: List[DocumentDetailWithDueDate]): List[DocumentDetailWithDueDate] = {
    documentDetailsWithDueDate.filterNot(documentDetailWithDueDate => documentDetailWithDueDate.isMFADebit)
  }
}

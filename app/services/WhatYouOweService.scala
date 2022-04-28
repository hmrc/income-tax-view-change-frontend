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

package services

import auth.MtdItUser
import config.FrontendAppConfig
import config.featureswitch.{CodingOut, FeatureSwitching}
import connectors.IncomeTaxViewChangeConnector
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class WhatYouOweService @Inject()(val financialDetailsService: FinancialDetailsService,
                                  val incomeTaxViewChangeConnector: IncomeTaxViewChangeConnector,
                                  dateService: DateService)
                                 (implicit ec: ExecutionContext, implicit val appConfig: FrontendAppConfig) extends FeatureSwitching {

  implicit lazy val localDateOrdering: Ordering[LocalDate] = Ordering.by(_.toEpochDay)

  val validChargeTypeCondition: String => Boolean = documentDescription => {
    documentDescription == "ITSA- POA 1" ||
      documentDescription == "ITSA - POA 2" ||
      documentDescription == "TRM New Charge" ||
      documentDescription == "TRM Amend Charge"
  }

  def getCreditCharges()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[List[DocumentDetail]]= {
    financialDetailsService.getAllCreditFinancialDetails.map {
      case financialDetails if financialDetails.exists(_.isInstanceOf[FinancialDetailsErrorModel]) =>
        throw new Exception("[WhatYouOweService][getCreditCharges] Error response while getting Unpaid financial details")
      case financialDetails: List[FinancialDetailsResponseModel] =>
        val financialDetailsModelList = financialDetails.asInstanceOf[List[FinancialDetailsModel]]
        financialDetailsModelList.flatMap(_.documentDetails)
    }
  }

  def getWhatYouOweChargesList()(implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[WhatYouOweChargesList] = {
    financialDetailsService.getAllUnpaidFinancialDetails flatMap {
      case financialDetails if financialDetails.exists(_.isInstanceOf[FinancialDetailsErrorModel]) =>
        throw new Exception("[WhatYouOweService][getWhatYouOweChargesList] Error response while getting Unpaid financial details")
      case financialDetails =>
        val financialDetailsModelList = financialDetails.asInstanceOf[List[FinancialDetailsModel]]
        val balanceDetails = financialDetailsModelList.headOption
          .map(_.balanceDetails).getOrElse(BalanceDetails(0.00, 0.00, 0.00, None, None, None,None))
        val codedOutDocumentDetail: Option[DocumentDetailWithCodingDetails] = if (isEnabled(CodingOut)) {
          financialDetailsModelList.flatMap(fdm =>
            fdm.documentDetails.find(dd => dd.isPayeSelfAssessment
              && dd.taxYear.toInt == (dateService.getCurrentTaxYearEnd(dateService.getCurrentDate) - 1)) flatMap fdm.getDocumentDetailWithCodingDetails
          ).headOption
        } else None

        val whatYouOweChargesList = WhatYouOweChargesList(
          balanceDetails = balanceDetails,
          chargesList = getFilteredChargesList(financialDetailsModelList),
          codedOutDocumentDetail = codedOutDocumentDetail)

        callOutstandingCharges(mtdUser.saUtr, mtdUser.incomeSources.yearOfMigration, dateService.getCurrentTaxYearEnd(dateService.getCurrentDate)).map {
          case Some(outstandingChargesModel) => whatYouOweChargesList.copy(outstandingChargesModel = Some(outstandingChargesModel))
          case _ => whatYouOweChargesList
        }
    }
  }

  private def callOutstandingCharges(saUtr: Option[String], yearOfMigration: Option[String], currentTaxYear: Int)
                                    (implicit headerCarrier: HeaderCarrier): Future[Option[OutstandingChargesModel]] = {
    if (saUtr.isDefined && yearOfMigration.isDefined && yearOfMigration.get.toInt >= currentTaxYear - 1) {
      val saPreviousYear = yearOfMigration.get.toInt - 1
      incomeTaxViewChangeConnector.getOutstandingCharges("utr", saUtr.get, saPreviousYear.toString) map {
        case outstandingChargesModel: OutstandingChargesModel => Some(outstandingChargesModel)
        case outstandingChargesErrorModel: OutstandingChargesErrorModel if outstandingChargesErrorModel.code == 404 => None
        case _ => throw new Exception("[WhatYouOweService][callOutstandingCharges] Error response while getting outstanding charges")
      }
    } else {
      Future.successful(None)
    }
  }

  private def whatYouOwePageDataExists(documentDetailWithDueDate: DocumentDetailWithDueDate): Boolean = {
    documentDetailWithDueDate.documentDetail.documentDescription.isDefined && documentDetailWithDueDate.dueDate.isDefined
  }

  private def getFilteredChargesList(financialDetailsList: List[FinancialDetailsModel]): List[DocumentDetailWithDueDate] = {
    financialDetailsList.flatMap(financialDetails =>
      financialDetails.getAllDocumentDetailsWithDueDates(isEnabled(CodingOut))
        .filter(documentDetailWithDueDate => whatYouOwePageDataExists(documentDetailWithDueDate)
          && validChargeTypeCondition(documentDetailWithDueDate.documentDetail.documentDescription.get)
          && !documentDetailWithDueDate.documentDetail.isPayeSelfAssessment
          && documentDetailWithDueDate.documentDetail.checkIfEitherChargeOrLpiHasRemainingToPay)).sortBy(_.dueDate.get)
  }
}

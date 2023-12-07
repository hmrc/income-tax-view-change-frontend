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

package connectors

import auth.MtdItUser
import config.FrontendAppConfig
import models.chargeHistory.{ChargeHistoryResponseModel, ChargesHistoryErrorModel, ChargesHistoryModel}
import models.core.Nino
import models.financialDetails._
import models.outstandingCharges.{OutstandingChargesErrorModel, OutstandingChargesModel, OutstandingChargesResponseModel}
import models.paymentAllocationCharges.{
  FinancialDetailsWithDocumentDetailsErrorModel,
  FinancialDetailsWithDocumentDetailsModel, FinancialDetailsWithDocumentDetailsResponse
}
import models.paymentAllocations.{PaymentAllocations, PaymentAllocationsError, PaymentAllocationsResponse}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDetailsConnector @Inject()(val http: HttpClient,
                                          val appConfig: FrontendAppConfig
                                         )(implicit val ec: ExecutionContext) extends RawResponseReads {

  def getChargesUrl(nino: String, from: String, to: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/financial-details/charges/from/$from/to/$to"
  }

  def getOutstandingChargesUrl(idType: String, idNumber: String, taxYear: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/out-standing-charges/$idType/$idNumber/$taxYear"
  }

  def getChargeHistoryUrl(mtdBsa: String, docNumber: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/charge-history/$mtdBsa/docId/$docNumber"
  }

  def getPaymentsUrl(nino: String, from: String, to: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/financial-details/payments/from/$from/to/$to"
  }

  def getFinancialDetailsByDocumentIdUrl(nino: String, documentNumber: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/financial-details/charges/documentId/$documentNumber"
  }

  def getPaymentAllocationsUrl(nino: String, paymentLot: String, paymentLotItem: String): String = {
    s"${appConfig.itvcProtectedService}/income-tax-view-change/$nino/payment-allocations/$paymentLot/$paymentLotItem"
  }

  def getPaymentAllocations(nino: Nino, paymentLot: String, paymentLotItem: String)
                           (implicit headerCarrier: HeaderCarrier): Future[PaymentAllocationsResponse] = {

    val url = getPaymentAllocationsUrl(nino.value, paymentLot, paymentLotItem)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[PaymentAllocations].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - Json Validation Error: $invalid")
              PaymentAllocationsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Payment Allocations Data Response")
            },
            valid => valid
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - Status: ${response.status}, body: ${response.body}")
          }
          PaymentAllocationsError(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getPaymentAllocations] - Unexpected failure, ${ex.getMessage}", ex)
        PaymentAllocationsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

  // TODO: MFA Credits
  def getFinancialDetails(taxYear: Int, nino: String)
                         (implicit headerCarrier: HeaderCarrier): Future[FinancialDetailsResponseModel] = {

    val dateFrom: String = (taxYear - 1).toString + "-04-06"
    val dateTo: String = taxYear.toString + "-04-05"

    val url = getChargesUrl(nino, dateFrom, dateTo)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[FinancialDetailsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - Json Validation Error: $invalid")
              FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing FinancialDetails Data Response")
            },
            valid => valid
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - Status: ${response.status}, body: ${response.body}")
          }
          FinancialDetailsErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getFinancialDetails] - Unexpected failure, ${ex.getMessage}", ex)
        FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

  def getOutstandingCharges(idType: String, idNumber: String, taxYear: String)
                           (implicit headerCarrier: HeaderCarrier): Future[OutstandingChargesResponseModel] = {


    val url = getOutstandingChargesUrl(idType, idNumber, s"$taxYear-04-05")

    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[OutstandingChargesModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - Json Validation Error: $invalid")
              OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing OutstandingCharges Data Response")
            },
            valid => valid
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - Status: ${response.status}, body: ${response.body}")
          }
          OutstandingChargesErrorModel(response.status, response.body)
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getOutstandingCharges] - Unexpected failure, ${ex.getMessage}", ex)
        OutstandingChargesErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

  def getChargeHistory(mtdBsa: String, docNumber: String)
                      (implicit headerCarrier: HeaderCarrier): Future[ChargeHistoryResponseModel] = {
    val url = getChargeHistoryUrl(mtdBsa, docNumber)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getChargeHistory] - GET $url")

    http.GET[HttpResponse](url) map { response =>
      response.status match {
        case OK =>
          Logger("application").debug(s"[IncomeTaxViewChangeConnector][getChargeHistory] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[ChargesHistoryModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getChargeHistory] - Json Validation Error: $invalid")
              ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing ChargeHistory Data Response")
            },
            valid => valid
          )
        case status =>
          if (status == 404 || status == 403) {
            Logger("application").info(s"[IncomeTaxViewChangeConnector][getChargeHistory] - No charge history found for $docNumber - Status: ${response.status}, body: ${response.body}")
            ChargesHistoryModel("", "", "", None)
          } else {
            if (status >= 500) {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getChargeHistory] - Status: ${response.status}, body: ${response.body}")
            } else {
              Logger("application").warn(s"[IncomeTaxViewChangeConnector][getChargeHistory] - Status: ${response.status}, body: ${response.body}")
            }
            ChargesHistoryErrorModel(response.status, response.body)
          }
      }
    } recover {
      case ex =>
        Logger("application").error(s"[IncomeTaxViewChangeConnector][getChargeHistory] - Unexpected failure, ${ex.getMessage}", ex)
        ChargesHistoryErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
    }

  }

  def getPayments(taxYear: Int)
                 (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[PaymentsResponse] = {
    val dateFrom: String = s"${taxYear - 1}-04-06"
    val dateTo: String = s"$taxYear-04-05"

    val url: String = getPaymentsUrl(mtdUser.nino, dateFrom, dateTo)
    Logger("application").debug(s"[IncomeTaxViewChangeConnector][getPayments] - GET $url")

    http.GET[HttpResponse](url)(httpReads, headerCarrier, implicitly) map { response =>
      response.status match {
        case OK =>
          Logger("application").info(s"[IncomeTaxViewChangeConnector][getPayments] - Status: ${response.status}, json: ${response.json}")
          response.json.validate[Seq[Payment]].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getPayments] - Json validation error: $invalid")
              PaymentsError(response.status, "Json validation error")
            },
            valid => Payments(valid)
          )
        case status =>
          if (status >= 500) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getPayments] - Status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").info(s"[IncomeTaxViewChangeConnector][getPayments] - Status ${response.status}, body: ${response.body}")
          }
          PaymentsError(status, response.body)
      }
    }
  }

  // CUTOVER_CREDITS
  def getFinancialDetailsByDocumentId(nino: Nino, documentNumber: String)
                                     (implicit headerCarrier: HeaderCarrier): Future[FinancialDetailsWithDocumentDetailsResponse] = {
    http.GET[HttpResponse](getFinancialDetailsByDocumentIdUrl(nino.value, documentNumber))(
      httpReads,
      headerCarrier.withExtraHeaders("Accept" -> "application/vnd.hmrc.2.0+json"),
      ec
    ) map { response =>
      response.status match {
        case OK =>
          response.json.validate[FinancialDetailsWithDocumentDetailsModel].fold(
            invalid => {
              Logger("application").error(s"[IncomeTaxViewChangeConnector][getFinancialDetailsByDocumentId] - Json validation error parsing calculation response, error $invalid")
              FinancialDetailsWithDocumentDetailsErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation response")
            },
            valid => valid
          )
        case status =>
          if (status >= INTERNAL_SERVER_ERROR) {
            Logger("application").error(s"[IncomeTaxViewChangeConnector][getFinancialDetailsByDocumentId] - Response status: ${response.status}, body: ${response.body}")
          } else {
            Logger("application").warn(s"[IncomeTaxViewChangeConnector][getFinancialDetailsByDocumentId] - Response status: ${response.status}, body: ${response.body}")
          }
          FinancialDetailsWithDocumentDetailsErrorModel(response.status, response.body)
      }
    }
  }
}
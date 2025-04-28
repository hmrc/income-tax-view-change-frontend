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
import models.core.ResponseModel.{ResponseModel, UnexpectedError}
import models.core.{CorrelationId, Nino}
import models.creditsandrefunds.CreditsModel
import models.financialDetails._
import models.incomeSourceDetails.{TaxYear, TaxYearRange}
import models.paymentAllocationCharges.{FinancialDetailsWithDocumentDetailsErrorModel, FinancialDetailsWithDocumentDetailsModel, FinancialDetailsWithDocumentDetailsResponse}
import models.paymentAllocations.{PaymentAllocations, PaymentAllocationsError, PaymentAllocationsResponse}
import play.api.Logger
import play.api.http.Status
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import utils.Headers.checkAndAddTestHeader

import java.time.format.DateTimeFormatter
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDetailsConnector @Inject()(
                                           httpV2: HttpClientV2,
                                           appConfig: FrontendAppConfig
                                         )(implicit val ec: ExecutionContext) extends RawResponseReads {

  private[connectors] val baseUrl = s"${appConfig.itvcProtectedService}/income-tax-view-change"

  private[connectors] def getChargesUrl(nino: String, from: String, to: String): String =
    baseUrl + s"/$nino/financial-details/charges/from/$from/to/$to"

  private[connectors] def getCreditAndRefundUrl(nino: String, from: String, to: String): String =
    baseUrl + s"/$nino/financial-details/credits/from/$from/to/$to"

  private[connectors] def getPaymentsUrl(nino: String, from: String, to: String): String =
    baseUrl + s"/$nino/financial-details/payments/from/$from/to/$to"

  private[connectors] def getFinancialDetailsByDocumentIdUrl(nino: String, documentNumber: String): String =
    baseUrl + s"/$nino/financial-details/charges/documentId/$documentNumber"

  private[connectors] def getPaymentAllocationsUrl(nino: String, paymentLot: String, paymentLotItem: String): String =
    baseUrl + s"/$nino/payment-allocations/$paymentLot/$paymentLotItem"

  def getPaymentAllocations(
                             nino: Nino,
                             paymentLot: String,
                             paymentLotItem: String
                           )(implicit headerCarrier: HeaderCarrier): Future[PaymentAllocationsResponse] = {

    val url = getPaymentAllocationsUrl(nino.value, paymentLot, paymentLotItem)
    Logger("application").debug(s"GET $url")

    httpV2
      .get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Logger("application").debug(s"Status: ${response.status}, json: ${response.json}")
            response.json.validate[PaymentAllocations].fold(
              invalid => {
                Logger("application").error(s"Json Validation Error: $invalid")
                PaymentAllocationsError(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing Payment Allocations Data Response")
              },
              valid => valid
            )
          case status if status >= 500 =>
            Logger("application").error(s"Status: ${response.status}, body: ${response.body}")
            PaymentAllocationsError(response.status, response.body)
          case _ =>
            Logger("application").warn(s"Status: ${response.status}, body: ${response.body}")
            PaymentAllocationsError(response.status, response.body)
        }
      }.recover {
        case ex =>
          Logger("application").error(s"Unexpected failure, ${ex.getMessage}", ex)
          PaymentAllocationsError(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
      }
  }

  def getCreditsAndRefund(taxYear: TaxYear, nino: String)
                         (implicit headerCarrier: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[ResponseModel[CreditsModel]] = {

    val url = getCreditAndRefundUrl(nino, taxYear.financialYearStartString, taxYear.financialYearEndString)
    Logger("application").debug(s"GET $url")

    val hc = checkAndAddTestHeader(mtdItUser.path, headerCarrier, appConfig.poaAdjustmentOverrides(), "afterPoaAmountAdjusted")

    val correlationId =
      CorrelationId.fromHeaderCarrier(hc).getOrElse(CorrelationId())

    httpV2
      .get(url"$url")
      .setHeader(correlationId.asHeader())
      .execute[ResponseModel[CreditsModel]]
      .recover {
        case e =>
          Logger("application").error(e.getMessage)
          Left(UnexpectedError)
      }
  }

  def getCreditsAndRefund(taxYearFrom: TaxYear, taxYearTo: TaxYear, nino: String)
                         (implicit headerCarrier: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[ResponseModel[CreditsModel]] = {

    val url = getCreditAndRefundUrl(nino, taxYearFrom.financialYearStartString, taxYearTo.financialYearEndString)
    Logger("application").debug(s"GET $url")

    val hc = checkAndAddTestHeader(mtdItUser.path, headerCarrier, appConfig.poaAdjustmentOverrides(), "afterPoaAmountAdjusted")

    val correlationId =
      CorrelationId.fromHeaderCarrier(hc).getOrElse(CorrelationId())

    httpV2
      .get(url"$url")
      .setHeader(correlationId.asHeader())
      .execute[ResponseModel[CreditsModel]]
      .recover {
        case e =>
          Logger("application").error(e.getMessage)
          Left(UnexpectedError)
      }
  }

  // TODO: MFA Credits
  def getFinancialDetails(taxYear: Int, nino: String)
                         (implicit headerCarrier: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[FinancialDetailsResponseModel] = {

    val dateFrom: String = (taxYear - 1).toString + "-04-06"
    val dateTo: String = taxYear.toString + "-04-05"

    val url = getChargesUrl(nino, dateFrom, dateTo)
    Logger("application").debug(s"GET $url")

    val hc: HeaderCarrier = checkAndAddTestHeader(mtdItUser.path, headerCarrier, appConfig.poaAdjustmentOverrides(), "afterPoaAmountAdjusted")

    httpV2
      .get(url"$url")
      .setHeader(hc.extraHeaders: _*)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Logger("application").info(s"Status: ${response.status}, json: ${response.json}")
            response.json.validate[FinancialDetailsModel].fold(
              invalid => {
                Logger("application").error(s"Json Validation Error: $invalid ")
                FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing FinancialDetails Data Response")
              },
              valid => valid
            )
          case status if status >= 500 =>
            Logger("application").error(s"Status: ${response.status}, body: ${response.body}")
            FinancialDetailsErrorModel(response.status, response.body)
          case _ =>
            Logger("application").warn(s"Status: ${response.status}, body: ${response.body}")
            FinancialDetailsErrorModel(response.status, response.body)
        }
      }.recover {
        case ex =>
          Logger("application").error(s"Unexpected failure, ${ex.getMessage}", ex)
          FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
      }
  }

  def getFinancialDetailsByTaxYearRange(taxYearRange: TaxYearRange, nino: String)
                         (implicit headerCarrier: HeaderCarrier, mtdItUser: MtdItUser[_]): Future[FinancialDetailsResponseModel] = {

    val url = getChargesUrl(nino, taxYearRange.startYear.financialYearStartString, taxYearRange.endYear.financialYearEndString)
    Logger("application").debug(s"GET $url")

    val hc: HeaderCarrier = checkAndAddTestHeader(mtdItUser.path, headerCarrier, appConfig.poaAdjustmentOverrides(), "afterPoaAmountAdjusted")

    httpV2
      .get(url"$url")
      .setHeader(hc.extraHeaders: _*)
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Logger("application").debug(s"Status: ${response.status}, json: ${response.json}")
            response.json.validate[FinancialDetailsModel].fold(
              invalid => {
                Logger("application").error(s"Json Validation Error: $invalid")
                FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, "Json Validation Error. Parsing FinancialDetails Data Response")
              },
              valid => valid
            )
          case status if status >= 500 =>
            Logger("application").error(s"Status: ${response.status}, body: ${response.body}")
            FinancialDetailsErrorModel(response.status, response.body)
          case _ =>
            Logger("application").warn(s"Status: ${response.status}, body: ${response.body}")
            FinancialDetailsErrorModel(response.status, response.body)
        }
      }.recover {
        case ex =>
          Logger("application").error(s"Unexpected failure, ${ex.getMessage}", ex)
          FinancialDetailsErrorModel(Status.INTERNAL_SERVER_ERROR, s"Unexpected failure, ${ex.getMessage}")
      }
  }

  def getPayments(taxYear: TaxYear)
                 (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[PaymentsResponse] = {

    val url: String = getPaymentsUrl(mtdUser.nino, taxYear.toFinancialYearStart.toString, taxYear.toFinancialYearEnd.toString)
    Logger("application").debug(s"GET $url")

    httpV2
      .get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Logger("application").debug(s"Status: ${response.status}, json: ${response.json}")
            response.json.validate[Seq[Payment]].fold(
              invalid => {
                Logger("application").error(s"Json validation error: $invalid")
                PaymentsError(response.status, "Json validation error")
              },
              valid => Payments(valid)
            )
          case status if status >= 500 =>
            Logger("application").error(s"Status: ${response.status}, body: ${response.body}")
            PaymentsError(status, response.body)
          case status =>
            Logger("application").warn(s"Status ${response.status}, body: ${response.body}")
            PaymentsError(status, response.body)
        }
      }
  }

  def getPayments(taxYearFrom: TaxYear, taxYearTo: TaxYear)
                 (implicit headerCarrier: HeaderCarrier, mtdUser: MtdItUser[_]): Future[PaymentsResponse] = {

    val url: String = getPaymentsUrl(mtdUser.nino, taxYearFrom.financialYearStartString, taxYearTo.financialYearEndString)
    Logger("application").debug(s"GET $url")

    httpV2
      .get(url"$url")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            Logger("application").info(s"Status: ${response.status}, json: ${response.json}")
            response.json.validate[Seq[Payment]].fold(
              invalid => {
                Logger("application").error(s"Json validation error: $invalid")
                PaymentsError(response.status, "Json validation error")
              },
              valid => Payments(valid)
            )
          case status if status >= 500 =>
            Logger("application").error(s"Status: ${response.status}, body: ${response.body}")
            PaymentsError(status, response.body)
          case status =>
            Logger("application").warn(s"Status ${response.status}, body: ${response.body}")
            PaymentsError(status, response.body)
        }
      }
  }

  /*
  {"taxpayerDetails":{"idType":"NINO","idNumber":"AA888888A","regimeType":"ITSA"},
  "balanceDetails":{"balanceDueWithin30Days":-99999999999.99,"nxtPymntDateChrgsDueIn30Days":"1920-02-29","balanceNotDuein30Days":-99999999999.99,"nextPaymntDateBalnceNotDue":"1920-02-29","overDueAmount":-99999999999.99,"earlistPymntDateOverDue":"1920-02-29","totalBalance":-99999999999.99},"codingDetails":[],
  "documentDetails":[{"taxYear":9999,"transactionId":"601111111111","documentDate":"2019-01-31","documentText":"documentText","documentDescription":"Payment","originalAmount":-10000,"outstandingAmount":-9000,"paymentLot":"081203010066","paymentLotItem":"000001","effectiveDateOfPayment":"2019-01-31"}],
  "financialDetails":[{"taxYear":"2018","transactionId":"601111111111","chargeReference":"XM002610011594","originalAmount":-10000,"outstandingAmount":-9000,"clearedAmount":-1000,"items":[{"id":"081203010066-000001","subItem":"001","dueDate":"2019-01-31","amount":-10000,"outgoingPaymentMethod":"outgoing Payment","paymentReference":"GF235688","paymentAmount":-10000,"paymentMethod":"Payment","clearingSAPDocument":"3350000253"}]}]}
   */
  def getFinancialDetailsByDocumentId(
                                       nino: Nino,
                                       documentNumber: String
                                     )(implicit headerCarrier: HeaderCarrier): Future[FinancialDetailsWithDocumentDetailsResponse] = {
    val url = getFinancialDetailsByDocumentIdUrl(nino.value, documentNumber)
    println(s"Here is error: AA")
    httpV2
      .get(url"$url")
      .setHeader("Accept" -> "application/vnd.hmrc.2.0+json")
      .execute[HttpResponse]
      .map { response =>
        response.status match {
          case OK =>
            println(s"Here is error: CC => ${response.json}")
            response.json.validate[FinancialDetailsWithDocumentDetailsModel].fold(
              invalid => {
                Logger("application").error(s"Json validation error parsing calculation response, error $invalid")
                FinancialDetailsWithDocumentDetailsErrorModel(INTERNAL_SERVER_ERROR, "Json validation error parsing calculation response")
              },
              valid => valid
            )
          case status if status >= INTERNAL_SERVER_ERROR =>
            println(s"Here is error: BB")
            Logger("application").error(s"Response status: ${response.status}, body: ${response.body}")
            FinancialDetailsWithDocumentDetailsErrorModel(response.status, response.body)
          case ex =>
            println(s"Here is error: $ex")
            Logger("application").warn(s"Response status: ${response.status}, body: ${response.body}")
            FinancialDetailsWithDocumentDetailsErrorModel(response.status, response.body)
        }
      }
  }
}
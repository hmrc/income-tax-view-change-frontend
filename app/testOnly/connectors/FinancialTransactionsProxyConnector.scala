/*
 * Copyright 2020 HM Revenue & Customs
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

package testOnly.connectors

import javax.inject.{Inject, Singleton}

import connectors.RawResponseReads
import testOnly.TestOnlyAppConfig
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialTransactionsProxyConnector @Inject()(val appConfig: TestOnlyAppConfig,
                                                    val http: HttpClient) extends RawResponseReads {

  def getFinancialData(regime: String,
                       mtditid: String,
                       onlyOpenItems: Option[String],
                       dateFrom: Option[String],
                       dateTo: Option[String],
                       includeLocks: Option[String],
                       calculateAccruedInterest: Option[String],
                       customerPaymentInfo: Option[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[HttpResponse] = {

      lazy val url = s"${appConfig.ftUrl}/financial-transactions/$regime/$mtditid"

      val queryParams: Seq[(String, String)] = Seq(
        onlyOpenItems.map(("onlyOpenItems", _)),
        dateFrom.map(("dateFrom", _)),
        dateTo.map(("dateTo", _)),
        includeLocks.map(("includeLocks", _)),
        calculateAccruedInterest.map(("calculateAccruedInterest", _)),
        customerPaymentInfo.map(("customerPaymentInformation", _))
      ).flatten

      http.GET(url, queryParams)
  }
}

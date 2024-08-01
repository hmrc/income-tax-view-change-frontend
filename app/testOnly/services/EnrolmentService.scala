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

package testOnly.services

import config.FrontendAppConfig
import play.api.Logging
import play.api.libs.json.JsError
import testOnly.models.{EnrolmentsResponse, UserRecord}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EnrolmentService @Inject()(http: HttpClient, appConfig: FrontendAppConfig)(implicit ec: ExecutionContext) extends Logging {

  def fetchEnrolments(groupId: String)(implicit hc: HeaderCarrier): Future[Either[String, EnrolmentsResponse]] = {
    val url = s"${appConfig.enrolmentStoreProxyUrl}/enrolment-store-proxy/enrolment-store/groups/${groupId.replace("testGroupId-", "")}/enrolments?type=delegated"
    http.GET[HttpResponse](url).map { response =>
      response.json.validate[EnrolmentsResponse].asEither.left.map { errors =>
        logger.error("Failed to parse enrolments response: " + JsError.toJson(errors).toString)
        "Failed to parse enrolments response"
      }
    }
  }

  def filterUserRecords(userRecords: List[UserRecord], agentMTDITIDs: List[String]): List[(String, String, String)] = {
    userRecords.filter(ur => agentMTDITIDs.contains(ur.mtditid)).map { userRecord =>
      (userRecord.utr, userRecord.nino, userRecord.mtditid)
    }
  }
}


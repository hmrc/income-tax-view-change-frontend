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

package testOnly.utils

import play.api.Play
import testOnly.models.{UserCredentials, UserRecord}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object FileUtil {

  def getFileFromPath(path: String): Either[Throwable, String] = {
    Try {
      val resource = Play.getClass.getResourceAsStream(path)
      scala.io.Source.fromInputStream(resource).mkString("")
    }.toEither
  }

  def getUserCredentials(nino: String, userRepository: UserRepository)(implicit executor: ExecutionContext): Future[UserCredentials] = {
    userRepository.findUser(nino).map {
      case record: UserRecord =>
        UserCredentials(credId = UserCredentials.credId,
          affinityGroup = "Individual",
          confidenceLevel = if(record.description.contains("IV Uplift required")) 50 else 250,
          credentialStrength = "strong",
          Role = "User",
          enrolmentData = EnrolmentValues(record.mtditid, record.utr),
          delegatedEnrolmentData = None
        )
    }
  }

}

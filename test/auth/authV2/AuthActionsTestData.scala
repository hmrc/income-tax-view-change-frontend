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

package auth.authV2

import auth.MtdItUserOptionNino
import models.incomeSourceDetails.IncomeSourceDetailsModel
import play.api.mvc.Request
import uk.gov.hmrc.auth.core.AffinityGroup.Individual
import uk.gov.hmrc.auth.core.retrieve.{Credentials, Name}
import uk.gov.hmrc.auth.core.{ConfidenceLevel, Enrolment, EnrolmentIdentifier, Enrolments}

object AuthActionsTestData {

  val nino              = "AA111111A"
  val saUtr             = "123456789"
  val mtdId             = "abcde"
  val arn               = "12345"
  val userName = Name(Some("Issac Newton"), None)

  val mtdEnrolment             = Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", mtdId)), "Activated", None)
  val ninoEnrolment             = Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", nino)), "Activated", None)
  val saEnrolment               = Enrolment("IR-SA", Seq(EnrolmentIdentifier("UTR", saUtr)), "Activated", None)
  val credentials               = Credentials("foo", "bar")
  val defaultIncomeSourcesData  = IncomeSourceDetailsModel(nino, saUtr, Some("2012"), Nil, Nil)
  val confidenceLevel: ConfidenceLevel = ConfidenceLevel.L250

  def mtdIdAgentPredicate(mtdId: String) = Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", mtdId).withDelegatedAuthRule("mtd-it-auth")
  def mtdIdIndividualPredicate(mtdId: String) = Enrolment("HMRC-MTD-IT").withIdentifier("MTDITID", mtdId)


  def getAllEnrolmentsIndividual(hasNino: Boolean, hasSA: Boolean) = {
    var enrolmentList = List(mtdEnrolment)
    if(hasNino) enrolmentList :+= ninoEnrolment
    if(hasSA) enrolmentList :+= saEnrolment
    Enrolments(
      enrolmentList.toSet,
    )
  }
  def getMtdItUserOptionNinoForAuthorise(hasAllRelevantFields: Boolean)
                                        (implicit request: Request[_]) = MtdItUserOptionNino(
    mtdId,
    if(hasAllRelevantFields) Some(nino) else None,
    if(hasAllRelevantFields) Some(userName) else None,
    None,
    if(hasAllRelevantFields) Some(saUtr) else None,
    Some(credentials.providerId),
    Some(Individual)
  )

}

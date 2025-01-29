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

package services

import models.homePage.PenaltiesAndAppealsTileViewModel

import javax.inject.Inject
import scala.util.Random

class PenaltyDetailsService @Inject()() {

  private val annualPenaltyThreshold = 2

  private val quarterlyPenaltyThreshold = 4

  val dummySubmissionFrequency: String =
    Random.shuffle(Seq("Annual", "Quarterly")).head

  val dummyPenaltyPoints: Int =
    Random.shuffle(Seq(annualPenaltyThreshold, quarterlyPenaltyThreshold)).head

  def getPenaltyPenaltiesAndAppealsTileViewModel(penaltiesAndAppealsIsEnabled: Boolean): PenaltiesAndAppealsTileViewModel = {

    val penaltiesTagMessageKey: Option[String] = (dummySubmissionFrequency, dummyPenaltyPoints) match {
      case ("Annual",    points) if points >= annualPenaltyThreshold    => Some("home.penaltiesAndAppeals.twoPenaltiesTag")
      case ("Quarterly", points) if points >= quarterlyPenaltyThreshold => Some("home.penaltiesAndAppeals.fourPenaltiesTag")
      case _                                                            => None
    }

    PenaltiesAndAppealsTileViewModel(penaltiesAndAppealsIsEnabled, penaltiesTagMessageKey)
  }
}

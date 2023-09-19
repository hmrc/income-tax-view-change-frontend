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

package controllers.actions

import auth.MtdItUser
import enums.IncomeSourceJourney.SelfEmployment
import forms.utils.SessionKeys
import play.api.mvc.Results._
import play.api.mvc._
import services.SessionService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait IncomeSourcesPage {
  def path: String
  val requiredData: Set[Option[String]]
}

case object AddIncomeSource extends IncomeSourcesPage {
  override def path: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
  override val requiredData: Set[Option[String]] = Set.empty
}

case object BusinessName extends IncomeSourcesPage {
  override def path: String = controllers.incomeSources.add.routes.AddBusinessNameController.show().url
  override val requiredData: Set[Option[String]] = Set.empty
}

case object BusinessStartDate extends IncomeSourcesPage {
  override def path: String = {
    controllers.incomeSources.add.routes.AddIncomeSourceStartDateController.show(isAgent = false, isChange = false, SelfEmployment).url
  }
  override val requiredData: Set[Option[String]] = Set(Some(SessionKeys.businessName))
}

@Singleton
class CheckIncomeSourcesData @Inject()(sessionService: SessionService)(implicit ec: ExecutionContext)
  extends ActionRefiner[MtdItUser, MtdItUser] {

  val addJourney = Set(AddIncomeSource, BusinessName, BusinessStartDate)

  override def refine[A](request: MtdItUser[A]): Future[Either[Result, MtdItUser[A]]] = {
    val requestPath = request.uri

    val currentPageOption = addJourney.find(page => requestPath.contains(page.path))
    // This is not the best check but decided to leave it given time constraints

    println(Console.CYAN + s"currentPageOption: $currentPageOption" + Console.WHITE)

    currentPageOption match {
      case Some(currentPage) =>
        val requiredDataKeys = currentPage.requiredData.collect { case Some(key) => key }.toList

        println(Console.CYAN + s"requiredDataKeys: $requiredDataKeys" + Console.WHITE)

        // Check if all requiredDataKeys have corresponding values in the session
        sessionService.get(requiredDataKeys)(request, ec).map {
          case Right(dataList) =>
            println(Console.CYAN + s"Retrieved Data: $dataList" + Console.WHITE)

            // Check if dataList contains all required data (you can adjust this condition as needed)
            if (requiredDataKeys.forall(key => dataList.get(key).exists(_.isDefined))) {
              println(Console.GREEN + "All required data is present." + Console.WHITE)
              // Continue the action chain
              Right(request)
            } else {
              println(Console.RED + "Some required data is missing." + Console.WHITE)
              // Handle the case where some required data is missing
              Left(Redirect(controllers.routes.JourneyRestartController.show(false)))
            }

          case Left(_) =>
            println(Console.RED + "Error retrieving data from the session." + Console.WHITE)
            // Handle the case where there was an error retrieving the data
            Left(Redirect(controllers.routes.JourneyRestartController.show(false)))
        }

      case None =>
        // Debugging: Print a message when currentPage is not found
        println(Console.RED + s"Current path not found in addJourney for request path: $requestPath" + Console.WHITE)
        // Handle the case where currentPage is not found
        Future.successful(Left(Redirect(controllers.routes.JourneyRestartController.show(false))))
    }
  }


  override protected def executionContext: ExecutionContext = ec
}

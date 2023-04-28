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

package controllers

import models.core.{AccountingPeriodModel, CessationModel}
import models.incomeSourceDetails.PropertyDetailsModel

import java.time.{LocalDate, Month}
import auth.MtdItUser
import config.featureswitch.FeatureSwitching
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates.{AuthenticationPredicate, IncomeSourceDetailsPredicate, NavBarPredicate, NinoPredicate, SessionTimeoutPredicate}
import models.incomeSourceDetails.{BusinessDetailsModel, IncomeSourceDetailsModel}
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.CheckIncomeSources
import views.html.notMigrated.NotMigratedUser

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckIncomeSourcesController @Inject()(val checkIncomeSources: CheckIncomeSources,
                                             val notMigrated: NotMigratedUser,
                                             val checkSessionTimeout: SessionTimeoutPredicate,
                                             val authenticate: AuthenticationPredicate,
                                             val authorisedFunctions: AuthorisedFunctions,
                                             val retrieveNino: NinoPredicate,
                                             val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                             val itvcErrorHandler: ItvcErrorHandler,
                                             implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                             val incomeSourceDetailsService: IncomeSourceDetailsService,
                                             val retrieveBtaNavBar: NavBarPredicate)
                                            (implicit val ec: ExecutionContext,
                                             mcc: MessagesControllerComponents,
                                             val appConfig: FrontendAppConfig) extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  //TODO: Tie controller to FS
  // TODO: if income sources feature switch is off and this page is loaded, then redirected to home page.
  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      handleRequest()
  }

  def handleRequest()(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    Future {
      Ok(checkIncomeSources(businessesAndPropertyIncome))
    }
  }

  val business1 = BusinessDetailsModel(
    incomeSourceId = Some("XA00001234"),
    accountingPeriod = Some(AccountingPeriodModel(start = LocalDate.of(2017, Month.JUNE, 1), end = LocalDate.of(2018, Month.MAY, 30))),
    tradingName = Some("Big Company Ltd"),
    firstAccountingPeriodEndDate = Some(LocalDate.of(2018, Month.APRIL, 5)),
    tradingStartDate = Some(LocalDate.of(2018, 4, 5)),
    cessation = Some(CessationModel(Some(LocalDate.of(2022, 1, 2)), None))
  )

  val business2 = BusinessDetailsModel(
    incomeSourceId = Some("XA00001235"),
    accountingPeriod = Some(AccountingPeriodModel(start = LocalDate.of(2019, Month.MAY, 1), end = LocalDate.of(2018, Month.MAY, 30))),
    tradingName = Some("Small Company Ltd"),
    firstAccountingPeriodEndDate = None,
    tradingStartDate = Some(LocalDate.of(2020, 4, 5)),
    cessation = None
  )

  val propertyDetails = PropertyDetailsModel(
    incomeSourceId = Some("1234"),
    accountingPeriod = Some(AccountingPeriodModel(LocalDate.of(2017, 4, 6), LocalDate.of(2018, 4, 5))),
    firstAccountingPeriodEndDate = None,
    incomeSourceType = Some("uk-property"),
    tradingStartDate = Some(LocalDate.of(2020, 1, 5))
  )

  val businessesAndPropertyIncome: IncomeSourceDetailsModel = IncomeSourceDetailsModel(
    mtdbsa = "XIAT0000000000A",
    yearOfMigration = Some("2018"),
    businesses = List(business1, business2),
    property = Some(propertyDetails)
  )
}

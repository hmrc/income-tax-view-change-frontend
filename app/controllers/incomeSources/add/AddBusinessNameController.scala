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

package controllers.incomeSources.add

import auth.MtdItUser
import config.featureswitch.{FeatureSwitching, IncomeSources}
import config.{AgentItvcErrorHandler, FrontendAppConfig, ItvcErrorHandler}
import controllers.agent.predicates.ClientConfirmedController
import controllers.predicates._
import forms.BusinessNameForm
import forms.utils.SessionKeys
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.IncomeSourceDetailsService
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import views.html.incomeSources.add.AddBusinessName

import java.net.URI
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddBusinessNameController @Inject()(authenticate: AuthenticationPredicate,
                                          val authorisedFunctions: AuthorisedFunctions,
                                          checkSessionTimeout: SessionTimeoutPredicate,
                                          retrieveNino: NinoPredicate,
                                          val addBusinessView: AddBusinessName,
                                          val retrieveIncomeSources: IncomeSourceDetailsPredicate,
                                          val retrieveBtaNavBar: NavBarPredicate,
                                          val itvcErrorHandler: ItvcErrorHandler,
                                          incomeSourceDetailsService: IncomeSourceDetailsService)
                                         (implicit val appConfig: FrontendAppConfig,
                                          implicit val itvcErrorHandlerAgent: AgentItvcErrorHandler,
                                          implicit override val mcc: MessagesControllerComponents,
                                          val ec: ExecutionContext)
  extends ClientConfirmedController with I18nSupport with FeatureSwitching {

  lazy val backUrl: String = controllers.incomeSources.add.routes.AddIncomeSourceController.show().url
  lazy val backUrlAgent: String = controllers.incomeSources.add.routes.AddIncomeSourceController.showAgent().url

  lazy val checkDetailsBackUrl: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show().url
  lazy val checkDetailsBackUrlAgent: String = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent().url

  lazy val submitAction: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submit()
  lazy val submitActionAgent: Call = controllers.incomeSources.add.routes.AddBusinessNameController.submitAgent()
  lazy val redirect: Call = controllers.incomeSources.add.routes.AddBusinessStartDateController.show()
  lazy val redirectAgent: Call = controllers.incomeSources.add.routes.AddBusinessStartDateController.showAgent()

  lazy val checkDetailsRedirect: Call = controllers.incomeSources.add.routes.CheckBusinessDetailsController.show()
  lazy val checkDetailsRedirectAgent: Call = controllers.incomeSources.add.routes.CheckBusinessDetailsController.showAgent()


  lazy val addBusinessNameUrl: String = controllers.incomeSources.add.routes.AddBusinessNameController.show().url
  lazy val agentAddBusinessAddressUrl: String = controllers.incomeSources.add.routes.AddBusinessAddressController.showAgent().url

  lazy val changeBusinessNameUrl: String = controllers.incomeSources.add.routes.BusinessAccountingMethodController.show().url
  lazy val agentChangeBusinessNameUrl: String = controllers.incomeSources.add.routes.BusinessAccountingMethodController.showAgent().url


  def show(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      val formMode = getModeFromURL(user)
      handleRequest(
        isAgent = false,
        backUrl = backUrl,
        formMode = formMode
      )
  }

  def showAgent(): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              val formMode = getModeFromURL(request)
              handleRequest(
                isAgent = true,
                backUrl = backUrlAgent,
                formMode = formMode
              )
          }
    }

  def handleRequest(isAgent: Boolean, backUrl: String, formMode: String)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {

    if (isDisabled(IncomeSources)) {
      Future.successful(Redirect(
        if (isAgent) controllers.routes.HomeController.showAgent
        else controllers.routes.HomeController.show()
      ))
    } else {
      Future {
        val filledForm = user.session.get(SessionKeys.businessName) match {
          case Some(name) => BusinessNameForm.form.fill(BusinessNameForm(name))
          case None => BusinessNameForm.form
        }
        if (isAgent) {
          Ok(addBusinessView(filledForm, isAgent, submitActionAgent, backUrl, formMode))
        } else {
          Ok(addBusinessView(filledForm, isAgent, submitAction, backUrl, formMode))
        }
      }
    }
  }

  def submit: Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit request =>
      handleSubmitRequest(isAgent = false)
  }

  def submitAgent: Action[AnyContent] = Authenticated.async {
    implicit request =>
      implicit user =>
        getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
          implicit mtdItUser =>
            handleSubmitRequest(isAgent = true)
        }
  }

  def handleSubmitRequest(isAgent: Boolean)(implicit user: MtdItUser[_], ec: ExecutionContext): Future[Result] = {
    val (backUrlLocal, submitActionLocal, redirectLocal) = {
      if (isAgent)
        (backUrlAgent, submitActionAgent, redirectAgent)
      else
        (backUrl, submitAction, redirect)
    }
    val formMode = getModeFromURL(user)

    BusinessNameForm.form.bindFromRequest().fold(
      formWithErrors => {
        Future {
          Ok(addBusinessView(formWithErrors, isAgent, submitActionLocal, backUrlLocal, formMode))
        }
      },
      formData => {
        val updatedRedirect = user.session.get(SessionKeys.addBusinessAddressLine1) match {
          case Some(_) => if(isAgent)(checkDetailsRedirectAgent) else (checkDetailsRedirect)
          case None => redirectLocal
        }

        Future.successful {
          Redirect(updatedRedirect)
            .addingToSession(SessionKeys.businessName -> formData.name)
        }
      }
    )
  }

  private def getModeFromURL(request: RequestHeader): String = {
    val urlPath = request.uri

    println("LOOOOOOOOK HERE")
    println(s"urlPath: $urlPath")
    println(addBusinessNameUrl)

    urlPath match {
      case path if path == addBusinessNameUrl => "add"
      case _ => "update"
    }
  }

  def changeBusinessName(): Action[AnyContent] = (checkSessionTimeout andThen authenticate andThen retrieveNino
    andThen retrieveIncomeSources andThen retrieveBtaNavBar).async {
    implicit user =>
      val formMode = getModeFromURL(user)
      handleRequest(
        isAgent = false,
        backUrl = checkDetailsBackUrl,
        formMode = formMode
      )
  }

  def changeBusinessNameAgent(): Action[AnyContent] =
    Authenticated.async {
      implicit request =>
        implicit user =>
          getMtdItUserWithIncomeSources(incomeSourceDetailsService) flatMap {
            implicit mtdItUser =>
              val formMode = getModeFromURL(request)
              handleRequest(
                isAgent = true,
                backUrl = checkDetailsBackUrlAgent,
                formMode = formMode
              )
          }
    }
}

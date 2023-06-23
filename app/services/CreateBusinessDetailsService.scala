package services

import models.incomeSourceDetails.viewmodels.CheckBusinessDetailsViewModel

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}


@Singleton
class CreateBusinessDetailsService @Inject()() {

  def createBusinessDetails(viewModel: CheckBusinessDetailsViewModel)(implicit ec: ExecutionContext): Future[Either[Throwable, CheckBusinessDetailsViewModel]] = {
    ???
  }

}
case class SuccessResponse()

case class FailureResponse()
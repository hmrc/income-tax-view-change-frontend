
package models.clientData

import org.checkerframework.checker.units.qual.A
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core.retrieve.Name


case class ClientDataModel (clientMTDID: String,
                            clientName: Option[Name],
                            clientNino: String,
                            clientUTR: String)(implicit request: Request[A]) extends WrappedRequest[A](request) {


  object ClientDataModel {
    implicit val formats: OFormat[ClientDataModel] = Json.format[ClientDataModel]
  }

}

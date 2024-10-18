
package models.clientData

import org.checkerframework.checker.units.qual.A
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.{Request, WrappedRequest}


case class ClientDataModel (clientMTDID: String,
                            clientFirstName: String,
                            clientLastName: String,
                            clientUTR: String)(implicit request: Request[A]) extends WrappedRequest[A](request) {


  object ClientDataModel {
    implicit val formats: OFormat[ClientDataModel] = Json.format[ClientDataModel]
  }

}

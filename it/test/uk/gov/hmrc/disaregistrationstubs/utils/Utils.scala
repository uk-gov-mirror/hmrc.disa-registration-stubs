package uk.gov.hmrc.disaregistrationstubs.utils

import play.api.libs.json.{JsArray, JsObject}

trait Utils {

  def stripFields(json: JsArray, fields: String*): JsArray =
    JsArray(json.value.map { item =>
      fields.foldLeft(item.as[JsObject])(_ - _)
    })

}

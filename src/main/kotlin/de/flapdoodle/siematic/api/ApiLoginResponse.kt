package de.flapdoodle.siematic.api

import kotlinx.serialization.Serializable

@Serializable
class ApiLoginResponse() {
  var jsonrpc: String = ""
  var id: String = ""
  var result: Token = Token("")
}
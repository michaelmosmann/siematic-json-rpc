package de.flapdoodle.siematic.api

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

/*
  {
    "method": "PlcProgram.Browse",
    "params": {
      "mode": "children",
      "var": "\"dbMode\".system[2].heat"
    },
    "jsonrpc": "2.0",
    "id": "dbMode"
  }
 */
@Serializable
class MethodCall(
  var method: String = "",
  var params: ModeVar = ModeVar("", ""),
  @EncodeDefault
  var jsonrpc: String = "2.0",
  val id: String = ""
) {

  companion object {
    fun read(id: String, varName: String): MethodCall {
      return MethodCall(
        method = "PlcProgram.Read",
        params = ModeVar(mode = "simple", varName = varName),
        id = id
      )
    }

    fun browse(id: String, varName: String): MethodCall {
      return MethodCall(
        method = "PlcProgram.Browse",
        params = ModeVar(mode = "children", varName = varName),
        id = id
      )
    }
  }
}
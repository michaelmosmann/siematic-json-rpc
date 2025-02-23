package de.flapdoodle.siematic.api

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
class ApiLogin(
  @EncodeDefault
  var method: String = "Api.Login",
  @EncodeDefault
  var jsonrpc: String = "2.0",
  @EncodeDefault
  var id: String = "d4an6dnm",
  var params: User = User("", "")
) {

  companion object {
    fun loginWith(user: String, password: String): ApiLogin {
      return ApiLogin(params = User(user, password))
    }
  }
}
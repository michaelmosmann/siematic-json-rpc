package de.flapdoodle.siematic.api

import kotlinx.serialization.Serializable

@Serializable
class User(var user: String, var password: String)
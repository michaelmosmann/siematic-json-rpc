package de.flapdoodle.siematic

class PropertyCollector {
  val root: Property.Root = Property.Root()

  fun add(name: String) {
    root.addTree(parts(name))
  }

  fun value(name: String, value: Any) {
    root.addValue(parts(name), value)
  }

  private fun parts(name: String): List<String> {
    return name.split('.')
  }

  fun render(): String {
    val sb = StringBuilder()
    sb.append("\n")
    root.render(sb)
    return sb.toString()
  }


  sealed class Property() {
    abstract fun addTree(name: List<String>): Tree
    abstract fun addValue(name: List<String>, value: Any)

    abstract class TreeLike(
      open var childs: Map<String, Tree> = linkedMapOf(),
      open var values: Map<String, Any> = linkedMapOf()
    ) : Property() {
      override fun addTree(name: List<String>): Tree {
        return findOrAddTree(name)
      }

      private fun findOrAddTree(name: List<String>): Tree {
        val (name, rest) = nameAndRest(name)

        var current = childs[name]
        if (current == null) {
          current = Tree(name)
          childs = childs + (name to current)
        }

        return if (rest.isEmpty()) {
          current
        } else {
          current.addTree(rest)
        }
      }

      override fun addValue(name: List<String>, value: Any) {
        val tree = findOrAddTree(name.dropLast(1))
        tree.addValue(name.last(), value)
      }

      fun addValue(name: String, value: Any) {
        require(!values.containsKey(name)) { "Value $name already exists in $this" }
        values = values + (name to value)
      }
    }

    data class Root(
      override var childs: Map<String, Tree> = linkedMapOf(),
      override var values: Map<String, Any> = linkedMapOf()
    ) : TreeLike(childs, values) {
      fun render(sb: StringBuilder) {
        values.forEach { (name, value) ->
          sb.append(name).append(" = ").append(value).append("\n")
        }
        childs.forEach { (name, tree) ->
          tree.render(sb, 0)
        }
      }
    }

    data class Tree(
      val name: String,
      override var childs: Map<String, Tree> = linkedMapOf(),
      override var values: Map<String, Any> = linkedMapOf()
    ) : TreeLike(childs, values) {
      fun render(sb: StringBuilder, level: Int) {
        val prefix = "  ".repeat(level)
        sb.append(prefix).append(name).append("\n")
        values.forEach { (name, value) ->
          sb.append(prefix).append("  ").append(name).append(" = ").append(value).append("\n")
        }
        childs.forEach { (name, tree) ->
          tree.render(sb, level + 1)
        }
      }
    }

    companion object {
      fun nameAndRest(name: List<String>): Pair<String, List<String>> {
        return if (name.size > 1) {
          name.first() to name.drop(1)
        } else {
          name.first() to emptyList()
        }
      }
    }
  }
}
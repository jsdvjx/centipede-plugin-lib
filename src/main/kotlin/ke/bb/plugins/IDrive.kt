package ke.bb.plugins

data class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    fun click(drive: IDrive) {
        val left = (left..right).random()
        val top = (top..bottom).random()
        return drive.tap(left, top)
    }
}

data class Node(
    val text: String,
    val description: String,
    val rect: Rect,
    val className: String,
    val clickable: Boolean,
    val checkable: Boolean,
    val checked: Boolean,
    val enabled: Boolean,
    val scrollable: Boolean,
    val focusable: Boolean,
    val focused: Boolean,
    val selected: Boolean,
    val password: Boolean,
    val visibleToUser: Boolean,
    val longClickable: Boolean,
    val packageName: String,
    val index: Int,
    val resourceId: String,
)


class NodeQueryBuilder(private val nodes: List<Node>) {

    private val conditions = mutableListOf<(Node) -> Boolean>()

    fun text(value: String) = apply { conditions.add { it.text.contains(value) } }
    fun description(value: String) = apply { conditions.add { it.description.contains(value) } }

    //    fun rect(value: Rect) = apply { conditions.add { it.rect == value } }
    fun className(value: String) = apply { conditions.add { it.className == value } }
    fun clickable(value: Boolean) = apply { conditions.add { it.clickable == value } }
    fun checkable(value: Boolean) = apply { conditions.add { it.checkable == value } }
    fun checked(value: Boolean) = apply { conditions.add { it.checked == value } }
    fun enabled(value: Boolean) = apply { conditions.add { it.enabled == value } }
    fun scrollable(value: Boolean) = apply { conditions.add { it.scrollable == value } }
    fun focusable(value: Boolean) = apply { conditions.add { it.focusable == value } }
    fun focused(value: Boolean) = apply { conditions.add { it.focused == value } }
    fun selected(value: Boolean) = apply { conditions.add { it.selected == value } }
    fun password(value: Boolean) = apply { conditions.add { it.password == value } }
    fun visibleToUser(value: Boolean) = apply { conditions.add { it.visibleToUser == value } }
    fun longClickable(value: Boolean) = apply { conditions.add { it.longClickable == value } }
    fun packageName(value: String) = apply { conditions.add { it.packageName == value } }
    fun index(value: Int) = apply { conditions.add { it.index == value } }
    fun resourceId(value: String) = apply { conditions.add { it.resourceId == value } }
    fun condition(query: (Node) -> Boolean) = apply { conditions.add(query) }

    fun build(): List<Node> {
        return nodes.filter { node -> conditions.all { condition -> condition(node) } }
    }
}

data class ProcessInfo(
    val uid: Int,
    val pid: Int,
    val packageName: String
) {
    companion object {
        fun from(str: String): ProcessInfo {
            val (uid, _, last) = str.split("UID ").last().split(" ")
            val (pidStr, packageName) = last.split("/").first().split(":")
            return ProcessInfo(uid.toInt(), pidStr.toInt(), packageName)
        }
    }
}

interface IDrive {

    fun running(): List<ProcessInfo>
    fun startAppEntry(packageName: String): Boolean
    fun startAppActivity(activity: String): Boolean
    fun tap(x: Int, y: Int): Unit
    fun screenSize(): Pair<Int, Int>
    fun swipe(x: Int, y: Int, x1: Int, y1: Int, duration: Long)
    fun topActivity(): String
    fun ip(): String
    fun tcpIp(port: Int)
    fun id(): String
    fun queryNodes(): List<Node>
    fun queryNodes(text: String, justInclude: Boolean = true): List<Node>
    fun queryNode(text: String, justInclude: Boolean = true): Node?
    fun installedApps(): List<String>
    fun screenOn(): Boolean
    fun screenLocked(): Boolean
    fun unlock()
    fun installApp(path: String)
    fun installAppRemote(path: String)
    fun shell(cmd: String): String
    fun push(local: String, remote: String)
}
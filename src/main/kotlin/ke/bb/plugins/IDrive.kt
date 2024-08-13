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
    val packageName: String
)
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
    fun shell(cmd: String): String
}
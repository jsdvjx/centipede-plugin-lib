package ke.bb.plugins.ke.bb.plugins

import ke.bb.plugins.IDrive
import ke.bb.plugins.PluginBridge

enum class PluginType {
    CENTI, PEDE
}

/*
class PluginBridge(
    private val runtime: NodeRuntime,
    private val file: File
) {
    private var lastRunning = 0L

    enum class PluginType {
        CENTI, PEDE
    }

    private var source = ""
    private var name = ""

    private var modifyAt = 0L
    lateinit var instance: V8ValueObject

    lateinit var version: String
    lateinit var description: String
    lateinit var type: PluginType
    var cooldown: Int = 60
    var priority: Int = Int.MAX_VALUE

    private fun load() {
        modifyAt = file.lastModified()
        source = file.readText()

        name = resolvePluginName()
        instance = if (runtime.globalObject.getObject<HashMap<String, Any>>(name) == null) {
            runtime.getExecutor(file.readText()).executeVoid()
            runtime.globalObject.getProperty(name)
        } else {
            runtime.globalObject.delete(name)
            runtime.getExecutor(file.readText()).executeVoid()
            runtime.globalObject.getProperty(name)
        }
        version = instance.getString("version")
        description = instance.getString("description")
        priority = instance.getInteger("priority")
        type = PluginType.valueOf(instance.getString("type"))
        cooldown = instance.getInteger("cooldown")
    }

    init {
        load()
    }

    fun reload() {
        if (file.lastModified() > modifyAt) {
            load()
        }
    }

    private fun resolvePluginName(): String {
        val regex = Regex("var ([a-zA-Z]+) = ")
        return regex.find(source)?.groupValues?.get(1) ?: throw Exception("Plugin name not found")
    }

    fun run(ctx: MutableMap<String, Any>, drive: IDrive) {
        instance.invokeVoid("run", ctx, drive)

    }

    fun activate(ctx: MutableMap<String, Any>, drive: IDrive): Boolean {
        if (System.currentTimeMillis() / 1000 - lastRunning < cooldown) {
            return false
        }
        return instance.invokeBoolean("activate", ctx, drive).apply {
            lastRunning = System.currentTimeMillis() / 1000
        }
    }
}
 */
interface IPlugin {
    fun activate(ctx: MutableMap<String, Any>, drive: IDrive): Boolean
    fun run(ctx: MutableMap<String, Any>, drive: IDrive)
    fun update()
    var name: String
    var version: String
    var description: String
    var type: PluginType
    var cooldown: Int
    var priority: Int
}
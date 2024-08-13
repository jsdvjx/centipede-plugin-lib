package ke.bb.listener

import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.interop.converters.JavetProxyConverter
import com.caoccao.javet.values.reference.V8ValueObject
import java.io.File

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
        lastRunning = System.currentTimeMillis() / 1000
        instance.invokeVoid("run", ctx, drive)

    }

    fun activate(ctx: MutableMap<String, Any>, drive: IDrive): Boolean {
        if (System.currentTimeMillis() / 1000 - lastRunning < cooldown) {
            return false
        }
        return instance.invokeBoolean("activate", ctx, drive)
    }
}

class PluginExecutor(
    private val drives: Map<String, List<IDrive>>,
    private val pluginSourceDir: String,
    private val localPath: String
) {
    private val runtime = V8Host.getNodeInstance().createV8Runtime<NodeRuntime>().apply {
        converter = JavetProxyConverter()
    }

    private fun load(): List<PluginBridge> {
        File(pluginSourceDir).run {
            return (if (isDirectory && exists()) {
                (listFiles() ?: emptyArray()).map {
                    PluginBridge(runtime, it)
                }
            } else emptyList())
                .sortedBy { it.priority }
                .filter { it.type == PluginBridge.PluginType.CENTI }
        }
    }

    private val plugins = load()
    private val contexts = drives.keys.associateWith {
        mutableMapOf<String, Any>().apply {
            put("globalData", mutableMapOf<String, Any>())
            put("id", drives[it]!!.first().id())
            put("ip", drives[it]!!.first().ip())
            val (w, h) = drives[it]!!.first().screenSize()
            put("screenSize", mapOf("width" to w, "height" to h))
            put("screenOn", drives[it]!!.first().screenOn())
            put("screenLocked", drives[it]!!.first().screenLocked())
            put("localPath", localPath)
        }
    }

    private fun fillContext(ctx: MutableMap<String, Any>, drive: IDrive) {
        ctx["topActivity"] = drive.topActivity()
        ctx["screenOn"] = drive.screenOn()
        ctx["screenLocked"] = drive.screenLocked()
    }

    fun run() {
        drives.forEach { (t, u) ->
            plugins.forEach {
                it.reload()
            }
            fillContext(contexts[t]!!, u.first())
            plugins.firstOrNull {
                it.activate(contexts[t]!!, u.first())
            }?.run(contexts[t]!!, u.first())
        }
    }
}
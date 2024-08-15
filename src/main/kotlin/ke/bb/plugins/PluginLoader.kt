package ke.bb.plugins

import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.interop.converters.JavetProxyConverter
import com.caoccao.javet.values.reference.V8ValueObject
import ke.bb.plugins.ke.bb.plugins.IPlugin
import ke.bb.plugins.ke.bb.plugins.PluginType
import java.io.File


class PluginBridge(
    private val runtime: NodeRuntime,
    private val file: File
) : IPlugin {


    private var source = ""
    override var name = ""

    private var modifyAt = 0L
    lateinit var instance: V8ValueObject

    override lateinit var version: String
    override lateinit var description: String
    override lateinit var type: PluginType
    override var cooldown: Int = 60
    override var priority: Int = Int.MAX_VALUE

    fun load() {
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

    override fun update() {
        if (file.lastModified() > modifyAt) {
            load()
        }
    }

    private fun resolvePluginName(): String {
        val regex = Regex("var ([a-zA-Z]+) = ")
        return regex.find(source)?.groupValues?.get(1) ?: throw Exception("Plugin name not found")
    }

    override fun run(ctx: MutableMap<String, Any>, drive: IDrive) {
        instance.invokeVoid("run", ctx, drive)

    }

    override fun activate(ctx: MutableMap<String, Any>, drive: IDrive): Boolean {
        return instance.invokeBoolean("activate", ctx, drive)
    }
}

class PluginExecutor(
    private val drives: MutableMap<String, List<IDrive>>,
    private val syncDir: String,
    private val type: PluginType = PluginType.CENTI

) {
    fun setDrives(drives: MutableMap<String, List<IDrive>>) {
        this.drives.clear()
        this.drives.putAll(drives)
    }
    private val pluginSourceDir: String = "$syncDir/scripts"
    private val localPath: String = "$syncDir/apks"
    private val runtime = V8Host.getNodeInstance().createV8Runtime<NodeRuntime>().apply {
        converter = JavetProxyConverter()
    }

    private fun load(): List<IPlugin> {
        File(pluginSourceDir).run {
            return (if (isDirectory && exists()) {
                (listFiles() ?: emptyArray()).map {
                    PluginBridge(runtime, it)
                }
            } else emptyList())
                .sortedBy { it.priority }
                .filter { it.type == type }
        }
    }

    private var plugins = load()
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

    fun installPlugin(plugin: IPlugin) {
        plugins = (plugins + (plugin)).sortedBy { it.priority }
    }

    fun pluginInfo(): List<Map<String, Any>> {
        return plugins.map {
            mapOf(
                "version" to it.version,
                "description" to it.description,
                "type" to it.type,
                "cooldown" to it.cooldown,
                "priority" to it.priority
            )
        }
    }

    private val cooldowns = plugins.associateWith { 0L }.toMutableMap()

    fun run() {
        drives.forEach { (t, u) ->
            plugins.forEach {
                it.update()
            }
            fillContext(contexts[t]!!, u.first())
            val plugin = plugins.firstOrNull {
                val now = System.currentTimeMillis() / 1000
                val last = cooldowns[it] ?: 0L
                (now - last > it.cooldown && it.activate(contexts[t]!!, u.first()))
            }
            plugin?.run(contexts[t]!!, u.first())
            if (plugin != null) {
                cooldowns[plugin] = System.currentTimeMillis() / 1000
            }
        }
    }
}
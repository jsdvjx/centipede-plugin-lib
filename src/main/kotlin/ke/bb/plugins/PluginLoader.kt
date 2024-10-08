package ke.bb.plugins

import com.caoccao.javet.interop.NodeRuntime
import com.caoccao.javet.interop.V8Host
import com.caoccao.javet.interop.converters.JavetProxyConverter
import com.caoccao.javet.values.reference.V8ValueObject
import ke.bb.plugins.ke.bb.plugins.IPlugin
import ke.bb.plugins.ke.bb.plugins.PluginType
import java.io.File
import java.util.logging.Logger


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
    private val log = Logger.getLogger(this::class.simpleName)
    fun setDrives(drives: MutableMap<String, List<IDrive>>) {
        this.drives.clear()
        this.drives.putAll(drives)
    }

    private val pluginSourceDir: String = "$syncDir/scripts"
    private val localPath: String = "$syncDir/apks"
//    private val runtime = V8Host.getNodeInstance().createV8Runtime<NodeRuntime>().apply {
//        converter = JavetProxyConverter()
//    }

    private fun load(): List<IPlugin> {
//        file(pluginsourcedir).run {
//            return (if (isdirectory && exists()) {
//                (listfiles() ?: emptyarray()).map {
//                    pluginbridge(runtime, it)
//                }
//            } else emptylist())
//                .sortedby { it.priority }
//                .filter { it.type == type }
//        }
        return emptyList<IPlugin>()
    }

    private var plugins = load()
    private val contexts = drives.keys.associateWith {
        initContext(drives[it]!!.first())
    }.toMutableMap()

    fun setContext(id: String, key: String, data: Any): Boolean {
        if (contexts[id] == null) {
            return false
        }
        contexts[id]?.set(key, data)
        return true
    }

    fun resetContext(drive: IDrive) {
        val id = drive.id()
        if (contexts[id] == null) {
            contexts[id] = initContext(drive)
        }
    }

    private fun initContext(instance: IDrive): MutableMap<String, Any> {
        return mutableMapOf<String, Any>().apply {
            put("globalData", mutableMapOf<String, Any>())
            put("id", instance.id())
            put("ip", instance.ip())
            val (w, h) = instance.screenSize()
            put("screenSize", mapOf("width" to w, "height" to h))
            put("screenOn", instance.screenOn())
            put("screenLocked", instance.screenLocked())
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

    private fun pick(list: List<IDrive>): IDrive? {
        return list.firstOrNull {
            it.ready()
        }
    }

    fun run() {
        drives.forEach { (t, u) ->
            plugins.forEach {
                it.update()
            }
            pick(u)?.let { drive ->
                fillContext(contexts[t]!!, drive)
                val plugin = plugins.firstOrNull {
                    val now = System.currentTimeMillis() / 1000
                    val last = cooldowns[it] ?: 0L
                    (now - last > it.cooldown && it.activate(contexts[t]!!, drive))
                }
                plugin?.run(contexts[t]!!, drive)
                if (plugin != null) {
                    cooldowns[plugin] = System.currentTimeMillis() / 1000
                }
            } ?: log.warning("No available drive for $t")
        }
    }
}
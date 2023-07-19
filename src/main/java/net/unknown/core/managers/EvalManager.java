package net.unknown.core.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.unknown.UnknownNetworkCore;
import net.unknown.core.define.DefinedTextColor;
import net.unknown.core.util.MinecraftAdapter;
import net.unknown.core.util.NewMessageUtil;
import net.unknown.shared.SharedConstants;
import net.unknown.survival.chat.ChatManager;
import net.unknown.survival.chat.CustomChannels;
import net.unknown.survival.data.PlayerData;
import net.unknown.survival.managers.FlightManager;
import net.unknown.survival.queue.ItemGiveQueue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.mozilla.javascript.*;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class EvalManager {
    private static final Logger LOGGER = Logger.getLogger("UNC/EvalManager");

    private static final File ROOT_FOLDER = new File(SharedConstants.DATA_FOLDER, "eval");
    private static final Map<String, Object> GLOBAL_STORAGE = new HashMap<>();
    private static final ContextFactory RHINO_CONTEXT_FACTORY = new ContextFactory();
    private static final Context RHINO_CONTEXT = RHINO_CONTEXT_FACTORY.enterContext();
    private static ScriptableObject GLOBAL_SCOPE = RHINO_CONTEXT.initStandardObjects();
    private static final File COMPILED_SCRIPTS_FOLDER = new File(ROOT_FOLDER, "compiled-scripts");
    private static final Map<String, String> COMPILED_SCRIPTS_SOURCES = new HashMap<>();
    private static final Map<String, Script> COMPILED_SCRIPTS = new HashMap<>();
    private static final File DEFINED_FUNCTIONS_FOLDER = new File(ROOT_FOLDER, "defined-functions");
    private static final Map<String, String> DEFINED_FUNCTIONS_SOURCES = new HashMap<>();
    private static final Map<String, Function> DEFINED_FUNCTIONS = new HashMap<>();

    static {
        initGlobalScope();
    }

    public static void initGlobalScope() {
        // Bukkit
        importClassGlobally(Bukkit.class);
        importClassGlobally(Material.class);
        importClassGlobally(EntityType.class);
        importClassGlobally(ChatColor.class);

        // Unknown Network
        importClassGlobally(UnknownNetworkCore.class);
        importClassGlobally(RunnableManager.class);
        importClassGlobally(NewMessageUtil.class);
        importClassGlobally(PlayerData.class);
        importClassGlobally(MinecraftAdapter.class);
        importClassGlobally(ItemGiveQueue.class);
        importClassGlobally(ChatManager.class);
        importClassGlobally(CustomChannels.class);
        importClassGlobally(FlightManager.class);

        // Adventure
        importClassGlobally(Component.class);
        importClassGlobally(TextDecoration.class);
        importClassGlobally(TextColor.class);
        importClassGlobally(DefinedTextColor.class);
        importClassGlobally(NamedTextColor.class);
        importClassGlobally(Style.class);

        putProperty("plugin", UnknownNetworkCore.getInstance());
        putProperty("Storage", GLOBAL_STORAGE);
    }

    public static void load() {
        loadDefinedFunctions();
        loadCompiledScripts();
    }

    public static void reload() {
        GLOBAL_SCOPE = RHINO_CONTEXT.initStandardObjects();
        initGlobalScope();
        load();
    }

    public static void loadCompiledScripts() {
        COMPILED_SCRIPTS_SOURCES.clear();
        COMPILED_SCRIPTS.clear();

        if (COMPILED_SCRIPTS_FOLDER.exists() || COMPILED_SCRIPTS_FOLDER.mkdirs()) {
            File[] files = COMPILED_SCRIPTS_FOLDER.listFiles(file -> file.isFile() && file.getName().endsWith(".js"));
            if (files != null) {
                for (File scriptFile : files) {
                    try {
                        String fileName = scriptFile.getName();
                        String id = scriptFile.getName().substring(0, fileName.lastIndexOf("."));
                        String script = String.join("\n", Files.readAllLines(scriptFile.toPath()));
                        COMPILED_SCRIPTS_SOURCES.put(id, script);
                        COMPILED_SCRIPTS.put(id, compile(id, script));
                    } catch(Exception e) {
                        e.printStackTrace();
                        LOGGER.warning("Failed to load script for file " + scriptFile.getName());
                    }
                }
            }
        }
    }

    public static void addCompiledScript(String id, String script) {
        if (COMPILED_SCRIPTS_SOURCES.containsKey(id)) throw new IllegalArgumentException("Script with id " + id + " already exists");

        COMPILED_SCRIPTS_SOURCES.put(id, script);
        COMPILED_SCRIPTS.put(id, compile(id, script));
    }

    public static boolean removeCompiledScript(String id) {
        if (COMPILED_SCRIPTS.containsKey(id) && COMPILED_SCRIPTS_SOURCES.containsKey(id)) {
            File scriptFile = new File(COMPILED_SCRIPTS_FOLDER, id + ".js");
            COMPILED_SCRIPTS.remove(id);
            COMPILED_SCRIPTS_SOURCES.remove(id);
            return scriptFile.exists() && scriptFile.delete();
        }
        return false;
    }

    public static void saveCompiledScripts() {
        if (!COMPILED_SCRIPTS_FOLDER.exists() && COMPILED_SCRIPTS_FOLDER.mkdirs()) {
            COMPILED_SCRIPTS_SOURCES.forEach((id, script) -> {
                File scriptFile = new File(COMPILED_SCRIPTS_FOLDER, id + ".js");
                try {
                    if (scriptFile.exists() || scriptFile.createNewFile()) {
                        Files.write(scriptFile.toPath(), script.getBytes(), StandardOpenOption.WRITE);
                    } else {
                        LOGGER.warning("Failed to save script for file " + scriptFile.getName() + " (bad permission?)");
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    LOGGER.warning("Failed to save script for file " + scriptFile.getName());
                }
            });
        }
    }

    public static void loadDefinedFunctions() {
        DEFINED_FUNCTIONS_SOURCES.clear();
        DEFINED_FUNCTIONS.clear();

        if (DEFINED_FUNCTIONS_FOLDER.exists() || DEFINED_FUNCTIONS_FOLDER.mkdirs()) {
            File[] files = DEFINED_FUNCTIONS_FOLDER.listFiles(file -> file.isFile() && file.getName().endsWith(".js"));
            if (files != null) {
                for (File scriptFile : files) {
                    try {
                        String fileName = scriptFile.getName();
                        String id = scriptFile.getName().substring(0, fileName.lastIndexOf("."));
                        String script = String.join("\n", Files.readAllLines(scriptFile.toPath()));
                        DEFINED_FUNCTIONS_SOURCES.put(id, script);
                        DEFINED_FUNCTIONS.put(id, compileFunction(id, script));
                    } catch(Exception e) {
                        e.printStackTrace();
                        LOGGER.warning("Failed to load function for file " + scriptFile.getName());
                    }
                }
            }
        }

        defineFunctions();
    }

    private static void defineFunctions() {
        DEFINED_FUNCTIONS_SOURCES.forEach(EvalManager::defineFunction);
    }

    private static void defineFunction(String id, String script) {
        getRhinoContext().evaluateString(GLOBAL_SCOPE, script, id, 1, null);
    }

    public static void addDefinedFunction(String id, String script) {
        if (DEFINED_FUNCTIONS_SOURCES.containsKey(id)) throw new IllegalArgumentException("Function with id " + id + " already exists");

        DEFINED_FUNCTIONS_SOURCES.put(id, script);
        DEFINED_FUNCTIONS.put(id, compileFunction(id, script));
        saveDefinedFunctions();
        defineFunction(id, script);
    }

    public static boolean removeDefinedFunction(String id) {
        if (DEFINED_FUNCTIONS.containsKey(id) && DEFINED_FUNCTIONS_SOURCES.containsKey(id)) {
            File definedFunctionFile = new File(DEFINED_FUNCTIONS_FOLDER, id + ".js");
            DEFINED_FUNCTIONS.remove(id);
            DEFINED_FUNCTIONS_SOURCES.remove(id);
            boolean result = definedFunctionFile.exists() && definedFunctionFile.delete();
            if (result) {
                reload(); // for re-define functions
                return true;
            }
        }
        return false;
    }

    public static void saveDefinedFunctions() {
        if (!DEFINED_FUNCTIONS_FOLDER.exists() && DEFINED_FUNCTIONS_FOLDER.mkdirs()) {
            DEFINED_FUNCTIONS_SOURCES.forEach((id, script) -> {
                File definedFunctionFile = new File(DEFINED_FUNCTIONS_FOLDER, id + ".js");
                try {
                    if (definedFunctionFile.exists() || definedFunctionFile.createNewFile()) {
                        Files.write(definedFunctionFile.toPath(), script.getBytes(), StandardOpenOption.WRITE);
                    } else {
                        LOGGER.warning("Failed to save function for file " + definedFunctionFile.getName() + " (bad permission?)");
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                    LOGGER.warning("Failed to save function for file " + definedFunctionFile.getName());
                }
            });
        }
    }

    public static Map<String, Object> getGlobalStorage() {
        return GLOBAL_STORAGE;
    }

    public static ContextFactory getRhinoContextFactory() {
        return RHINO_CONTEXT_FACTORY;
    }

    public static Context getRhinoContext() {
        return RHINO_CONTEXT;
    }

    /**
     * グローバルスコープを取得します。
     *
     * @return グローバルスコープのコピー
     */
    public static ScriptableObject getGlobalScope() {
        return getRhinoContext().initStandardObjects(GLOBAL_SCOPE, false);
    }

    public static void importClassGlobally(Class<?> clazz) {
        importClassGlobally(clazz.getSimpleName(), clazz);
    }
    
    public static void importClassGlobally(String name, Class<?> clazz) {
        putProperty(name, new NativeJavaClass(GLOBAL_SCOPE, clazz));
    }

    public static void unImportClassGlobally(Class<?> clazz) {
        unImportClassGlobally(clazz.getSimpleName(), clazz);
    }
    
    public static void unImportClassGlobally(String name, Class<?> clazz) {
        if (ScriptableObject.hasProperty(GLOBAL_SCOPE, name) && ScriptableObject.getProperty(GLOBAL_SCOPE, name) instanceof NativeJavaClass nClass && nClass.getClassObject().equals(clazz)) {
            removeProperty(name);
        }
    }

    public static void putProperty(String name, Object value) {
        if (ScriptableObject.hasProperty(GLOBAL_SCOPE, name)) throw new IllegalArgumentException("Property name " + name + " already taken! remove Unregister");
        ScriptableObject.putProperty(GLOBAL_SCOPE, name, value);
    }

    public static void removeProperty(String name) {
        ScriptableObject.deleteProperty(GLOBAL_SCOPE, name);
    }

    public static Script compile(String sourceName, String script) {
        return RHINO_CONTEXT.compileString(script, sourceName, 1, null);
    }

    public static Function compileFunction(String sourceName, String script) {
        return RHINO_CONTEXT.compileFunction(GLOBAL_SCOPE, script, sourceName, 1, null);
    }

    public static Object exec(Script script, @Nullable Scriptable scope) {
        return script.exec(getRhinoContext(), scope == null ? GLOBAL_SCOPE : scope);
    }

    public static Object execFromString(String script, @Nullable Scriptable scope) {
        return execFromString("EvalManager#execFromString", script, scope);
    }

    public static Object execFromString(String sourceName, String script, @Nullable Scriptable scope) {
        return exec(compile(sourceName, script), scope);
    }
}

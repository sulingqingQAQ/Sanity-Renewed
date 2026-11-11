package guivnf.sanity_renewed.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import guivnf.sanity_renewed.SanityMod;
import guivnf.sanity_renewed.platform.ConfigPlatform;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public abstract class DimensionConfig
{
    public static final Map<String, Map<ResourceLocation, Object>> configToDimStored = new HashMap<>();
    public static final Map<ResourceLocation, Map<Integer, ConfigItemCategory>> idToItemCat = new HashMap<>();
    public static final Map<ResourceLocation, Map<Integer, ConfigBrokenBlockCategory>> idToBrokenBlockCat = new HashMap<>();

    /**
     * Rejects a dimension file whose {@code [sanity.link]} part has an unknown key, a value of the wrong type or a value
     * outside its range (these values are stored raw and never pass through {@code defineInRange}).
     */
    private static void checkLinkOverrides(String origin, Config config)
    {
        Object section = config.get("sanity.link");
        if (section == null)
            return;
        if (!(section instanceof Config link))
            throw new LinkConfigSpec.LinkConfigException(origin + ": sanity.link must be a table");
        for (var entry : link.entrySet())
        {
            Object value = entry.getValue();
            LinkConfigSpec.checkValue(entry.getKey(), value, origin);
        }
    }

    private static void unloadDimension(String file, String dim, Config config)
    {
        ResourceLocation name = ResourceLocation.parse(dim);
        checkLinkOverrides(file + " (" + dim + ")", config);
        for (Map.Entry<String, Map<ResourceLocation, Object>> entry : configToDimStored.entrySet())
        {
            String key = entry.getKey();
            Map<ResourceLocation, Object> value = entry.getValue();
            if (config.contains(key))
            {
                switch (key)
                {
                    case "sanity.passive.blocks" -> value.put(name, ConfigManager.processPassiveBlocks(config.get(key)));
                    case "sanity.active.items" -> value.put(name, ConfigManager.processItems(config.get(key)));
                    case "sanity.active.item_categories" ->
                    {
                        List<ConfigItemCategory> list = ConfigManager.processItemCats(config.get(key));
                        value.put(name, list);
                        idToItemCat.put(name, ConfigManager.getMapFromItemCats(list));
                    }
                    case "sanity.active.broken_blocks" -> value.put(name, ConfigManager.processBrokenBlocks(config.get(key)));
                    case "sanity.active.broken_block_categories" ->
                    {
                        List<ConfigBrokenBlockCategory> list = ConfigManager.processBrokenBlockCats(config.get(key));
                        value.put(name, list);
                        idToBrokenBlockCat.put(name, ConfigManager.getMapFromBrokenBlockCats(list));
                    }
                    default -> value.put(name, config.get(key));
                }
            }
        }
    }

    private static void createExampleConfig(Path dimPath)
    {
        Path example = Paths.get(dimPath.toString(), "example.toml");
        String lines =
                """
                # Dimensions to apply this config to, e.g. minecraft:overworld, minecraft:the_nether, minecraft:the_end
                applied_dimensions = [ "minecraft:example_dimension1", "someothermodid:example_dimension2" ]

                # Anything that is not present in this config will default to whatever is set in default.toml
                [sanity]
                \t[sanity.passive]
                \t\t# Now that's what I call hardcore
                \t\tmonster = -100.0
                """;
        try
        {

            Files.writeString(example, lines, StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            SanityMod.LOGGER.warn("unable to create example dimension config, refer to github to see one");
        }
    }

    public static void init()
    {
        clear();

        for (Map.Entry<String, ConfigManager.ProxyValueEntry<?>> entry : ConfigManager.proxies.entrySet())
        {
            configToDimStored.put(entry.getKey(), new HashMap<>());
        }

        Path dimPath = Paths.get(ConfigPlatform.getConfigDir().toString(), SanityMod.MOD_ID + File.separator + "dimension");
        try
        {
            Files.createDirectories(dimPath);
        }
        catch (IOException e)
        {
            SanityMod.LOGGER.warn("unable to create dimension config directory: " + dimPath);
            return;
        }

        try (Stream<Path> paths = Files.walk(dimPath, 1))
        {
            paths.forEach(path ->
            {
                // Compare the file name, not the full path: the generated example.toml must never be applied.
                if (path.toString().endsWith(".toml") && !path.getFileName().toString().equals("example.toml"))
                {
                    FileConfig config = FileConfig.of(path);
                    config.load();
                    List<String> dims = config.get("applied_dimensions");
                    if (dims != null && dims.size() > 0)
                    {
                        SanityMod.LOGGER.info("Applying dimension config " + path.getFileName());
                        for (String dim : dims)
                        {
                            unloadDimension(path.getFileName().toString(), dim, config);
                        }
                    }
                }
            });
        }
        catch (LinkConfigSpec.LinkConfigException e)
        {
            throw e; // a rejected [sanity.link] value must not be swallowed by the generic handler below
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        createExampleConfig(dimPath);
    }

    public static void clear()
    {
        configToDimStored.clear();
        idToItemCat.clear();
        idToBrokenBlockCat.clear();
    }

    /** Everything {@link #init()} replaces, so that a rejected reload can put the previous state back. */
    public static final class Snapshot
    {
        private final Map<String, Map<ResourceLocation, Object>> stored = copyNested(configToDimStored);
        private final Map<ResourceLocation, Map<Integer, ConfigItemCategory>> itemCats = copyNested(idToItemCat);
        private final Map<ResourceLocation, Map<Integer, ConfigBrokenBlockCategory>> brokenBlockCats = copyNested(idToBrokenBlockCat);
    }

    public static Snapshot snapshot()
    {
        return new Snapshot();
    }

    public static void restore(Snapshot snapshot)
    {
        clear();
        configToDimStored.putAll(snapshot.stored);
        idToItemCat.putAll(snapshot.itemCats);
        idToBrokenBlockCat.putAll(snapshot.brokenBlockCats);
    }

    private static <A, B, C> Map<A, Map<B, C>> copyNested(Map<A, Map<B, C>> source)
    {
        Map<A, Map<B, C>> copy = new HashMap<>();
        for (Map.Entry<A, Map<B, C>> entry : source.entrySet())
            copy.put(entry.getKey(), new HashMap<>(entry.getValue()));
        return copy;
    }
}
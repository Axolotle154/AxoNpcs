package org.axostudio.axonpcs.manager;

import org.axostudio.axonpcs.AxoNPCsPlugin;
import org.axostudio.axonpcs.api.model.NPCSkin;
import org.axostudio.axonpcs.api.model.NPCSkinMode;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkinManager {
    private static final Pattern ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"([a-fA-F0-9]{32})\"");
    private static final Pattern VALUE_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"");

    private final AxoNPCsPlugin plugin;
    private final File directory;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public SkinManager(AxoNPCsPlugin plugin) {
        this.plugin = plugin;
        this.directory = new File(plugin.getDataFolder(), "skins");
    }

    public void init() {
        if (!directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("Could not create skins directory");
        }
    }

    public CompletableFuture<Optional<NPCSkin>> resolve(String input) {
        init();
        if (input == null || input.isBlank() || input.equalsIgnoreCase("@none")) {
            return CompletableFuture.completedFuture(Optional.of(NPCSkin.none()));
        }
        if (input.equalsIgnoreCase("@mirror")) {
            return CompletableFuture.completedFuture(Optional.of(NPCSkin.mirror()));
        }
        String[] inline = input.split(":", 2);
        if (inline.length == 2 && inline[0].length() > 80) {
            return CompletableFuture.completedFuture(Optional.of(NPCSkin.texture("inline", inline[0], inline[1])));
        }
        File cached = new File(directory, input.toLowerCase(Locale.ROOT) + ".yml");
        if (cached.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(cached);
            String value = yaml.getString("value", "");
            if (!value.isBlank()) {
                return CompletableFuture.completedFuture(Optional.of(NPCSkin.texture(input, value, yaml.getString("signature", ""))));
            }
        }

        return CompletableFuture.supplyAsync(() -> fetch(input), runnable -> plugin.getSchedulerUtil().runAsync(runnable));
    }

    private Optional<NPCSkin> fetch(String name) {
        try {
            HttpRequest profileRequest = HttpRequest.newBuilder()
                    .timeout(Duration.ofSeconds(8))
                    .uri(URI.create("https://api.mojang.com/users/profiles/minecraft/" + name))
                    .GET()
                    .build();
            HttpResponse<String> profileResponse = client.send(profileRequest, HttpResponse.BodyHandlers.ofString());
            if (profileResponse.statusCode() != 200) {
                return Optional.empty();
            }
            Matcher idMatcher = ID_PATTERN.matcher(profileResponse.body());
            if (!idMatcher.find()) {
                return Optional.empty();
            }
            String uuid = idMatcher.group(1);
            HttpRequest textureRequest = HttpRequest.newBuilder()
                    .timeout(Duration.ofSeconds(8))
                    .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid + "?unsigned=false"))
                    .GET()
                    .build();
            HttpResponse<String> textureResponse = client.send(textureRequest, HttpResponse.BodyHandlers.ofString());
            if (textureResponse.statusCode() != 200) {
                return Optional.empty();
            }
            Matcher valueMatcher = VALUE_PATTERN.matcher(textureResponse.body());
            Matcher signatureMatcher = SIGNATURE_PATTERN.matcher(textureResponse.body());
            if (!valueMatcher.find()) {
                return Optional.empty();
            }
            String value = valueMatcher.group(1);
            String signature = signatureMatcher.find() ? signatureMatcher.group(1) : "";
            NPCSkin skin = new NPCSkin(NPCSkinMode.TEXTURE, name, value, signature);
            cache(name, skin);
            return Optional.of(skin);
        } catch (IOException | InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private void cache(String name, NPCSkin skin) {
        File cached = new File(directory, name.toLowerCase(Locale.ROOT) + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("source", name);
        yaml.set("value", skin.value());
        yaml.set("signature", skin.signature());
        try {
            yaml.save(cached);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not cache skin " + name + ": " + exception.getMessage());
        }
    }
}

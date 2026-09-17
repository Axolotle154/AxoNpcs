package org.axostudio.axonpcs.render.skin

import org.axostudio.axonpcs.api.model.NPCSkin
import org.axostudio.axonpcs.api.model.NPCSkinMode
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Locale
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

internal data class MineSkinReference(val id: String, val legacy: Boolean)

internal fun parseMineSkinReference(raw: String): MineSkinReference? {
    val input = raw.trim()
    val token = if (input.startsWith("mineskin:", ignoreCase = true)) {
        input.substringAfter(':').trim()
    } else {
        val uri = try {
            URI.create(input)
        } catch (_: IllegalArgumentException) {
            return null
        }
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        val isMineSkinHost = host == "mineskin.org" || host.endsWith(".mineskin.org") ||
                host == "minesk.in" || host.endsWith(".minesk.in")
        if (!isMineSkinHost) return null
        uri.path.split('/').lastOrNull { it.isNotBlank() } ?: return null
    }

    if (token.matches(Regex("^[0-9]+$"))) {
        return MineSkinReference(token, legacy = true)
    }

    val compactUuid = token.replace("-", "").lowercase(Locale.ROOT)
    if (compactUuid.matches(Regex("^[0-9a-f]{8}$")) || compactUuid.matches(Regex("^[0-9a-f]{32}$"))) {
        return MineSkinReference(compactUuid, legacy = false)
    }
    return null
}

class SkinService(private val plugin: Plugin) {
    private val skinDir = File(plugin.dataFolder, "skins")
    private val memoryCache = ConcurrentHashMap<String, NPCSkin>()
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val valuePattern = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"")
    private val sigPattern = Pattern.compile("\"signature\"\\s*:\\s*\"([^\"]+)\"")

    init {
        if (!skinDir.exists()) skinDir.mkdirs()
    }

    fun getCached(input: String): NPCSkin? {
        val key = input.lowercase(Locale.ROOT)
        memoryCache[key]?.let { return it }

        val file = File(skinDir, "$key.yml")
        if (file.exists()) {
            val config = YamlConfiguration.loadConfiguration(file)
            val value = config.getString("value", "") ?: ""
            val signature = config.getString("signature", "") ?: ""
            if (value.isNotBlank()) {
                val skin = NPCSkin.texture(key, value, signature)
                memoryCache[key] = skin
                return skin
            }
        }
        return null
    }

    fun resolve(input: String?): CompletableFuture<NPCSkin> {
        if (input.isNullOrBlank() || input.equals("@none", ignoreCase = true)) {
            return CompletableFuture.completedFuture(NPCSkin.none())
        }
        if (input.equals("@mirror", ignoreCase = true)) {
            return CompletableFuture.completedFuture(NPCSkin.mirror())
        }

        val mineSkinReference = parseMineSkinReference(input)
        if (mineSkinReference != null) {
            return resolveMineSkin(input, mineSkinReference)
        }

        val cached = getCached(input)
        if (cached != null) {
            return CompletableFuture.completedFuture(cached)
        }

        if (input.startsWith("http://", ignoreCase = true) || input.startsWith("https://", ignoreCase = true)) {
            val future = CompletableFuture<NPCSkin>()
            future.completeExceptionally(IllegalArgumentException("URL is not a recognized or valid MineSkin link (e.g. https://minesk.in/<id>): $input"))
            return future
        }

        // Async HTTP lookup
        return CompletableFuture.supplyAsync {
            fetchSkinFromApi(input) ?: NPCSkin.named(input)
        }.thenApply { skin ->
            if (skin.mode() == NPCSkinMode.TEXTURE) {
                memoryCache[input.lowercase(Locale.ROOT)] = skin
                saveSkinToFile(input.lowercase(Locale.ROOT), skin.value(), skin.signature())
            }
            skin
        }
    }

    private fun resolveMineSkin(source: String, reference: MineSkinReference): CompletableFuture<NPCSkin> {
        val cacheKey = "mineskin_${reference.id.lowercase(Locale.ROOT)}"
        getCached(cacheKey)?.let {
            return CompletableFuture.completedFuture(NPCSkin.texture(source, it.value(), it.signature()))
        }

        return CompletableFuture.supplyAsync {
            fetchMineSkin(reference)
                ?: throw CompletionException(IllegalArgumentException("MineSkin did not return signed texture data."))
        }.thenApply { skin ->
            memoryCache[cacheKey] = skin
            saveSkinToFile(cacheKey, skin.value(), skin.signature())
            NPCSkin.texture(source, skin.value(), skin.signature())
        }
    }

    fun applyMirrorSkin(viewer: Player, defaultSkin: NPCSkin): NPCSkin {
        return try {
            val craftPlayer = viewer.javaClass.getMethod("getProfile").invoke(viewer)
            val properties = craftPlayer.javaClass.getMethod("getProperties").invoke(craftPlayer)
            val textures = properties.javaClass.getMethod("get", Any::class.java).invoke(properties, "textures") as Collection<*>
            val property = textures.firstOrNull() ?: return defaultSkin

            val value = property.javaClass.getMethod("value").invoke(property) as String
            val signature = property.javaClass.getMethod("signature").invoke(property) as? String ?: ""
            NPCSkin.texture("mirror:${viewer.name}", value, signature)
        } catch (t: Throwable) {
            defaultSkin
        }
    }

    private fun fetchSkinFromApi(nameOrUuid: String): NPCSkin? {
        return try {
            // Try Ashcon API
            val request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.ashcon.app/mojang/v2/user/$nameOrUuid"))
                .header("User-Agent", "AxoNPCs")
                .timeout(Duration.ofSeconds(4))
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val body = response.body()
                val valMatcher = valuePattern.matcher(body)
                val sigMatcher = sigPattern.matcher(body)
                if (valMatcher.find() && sigMatcher.find()) {
                    return NPCSkin.texture(nameOrUuid, valMatcher.group(1), sigMatcher.group(1))
                }
            }
            null
        } catch (ignored: Exception) {
            null
        }
    }

    private fun fetchMineSkin(reference: MineSkinReference): NPCSkin? {
        val endpoints = if (reference.legacy) {
            listOf(
                "https://api.mineskin.org/get/id/${reference.id}",
                "https://api.mineskin.org/v2/skins/${reference.id}"
            )
        } else {
            listOf(
                "https://api.mineskin.org/v2/skins/${reference.id}",
                "https://api.mineskin.org/get/id/${reference.id}"
            )
        }

        var lastError: Exception? = null
        for (endpoint in endpoints) {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("User-Agent", "AxoNPCs/${plugin.pluginMeta.version}")
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .GET()
                    .build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) {
                    val valueMatcher = valuePattern.matcher(response.body())
                    val signatureMatcher = sigPattern.matcher(response.body())
                    if (valueMatcher.find() && signatureMatcher.find()) {
                        return NPCSkin.texture("mineskin:${reference.id}", valueMatcher.group(1), signatureMatcher.group(1))
                    }
                } else if (response.statusCode() != 404) {
                    throw IllegalArgumentException("MineSkin request failed with HTTP ${response.statusCode()}.")
                }
            } catch (e: IllegalArgumentException) {
                throw e
            } catch (e: Exception) {
                lastError = e
            }
        }

        if (lastError != null) {
            throw IllegalArgumentException("Could not contact MineSkin: ${lastError.message}", lastError)
        }
        return null
    }

    private fun saveSkinToFile(name: String, value: String, signature: String) {
        val file = File(skinDir, "$name.yml")
        val config = YamlConfiguration()
        config.set("value", value)
        config.set("signature", signature)
        try {
            config.save(file)
        } catch (ignored: Exception) {
        }
    }
}

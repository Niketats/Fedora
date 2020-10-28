package us.potatoboy.fedora;

import com.google.gson.*;
import net.devtech.arrp.api.RRPCallback;
import net.devtech.arrp.api.RRPPreGenEntrypoint;
import net.devtech.arrp.api.RuntimeResourcePack;
import net.devtech.arrp.json.blockstate.JState;
import net.devtech.arrp.json.lang.JLang;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import java.util.logging.Logger;

public class HatLoader implements RRPPreGenEntrypoint {
    private static final Logger LOGGER = Logger.getLogger("fedora");
    private static final FileFilter POSSIBLE_HAT = (file) -> {
        boolean isJson = file.isFile() && file.getName().endsWith(".json");
        return isJson;
    };
    private static final FileFilter POSSIBLE_TEXTURE = (file) -> {
        boolean isPng = file.isFile() && file.getName().endsWith(".png");
        return isPng;
    };
    private File hatsFolder;
    private File modelsFolder;
    private File textFolder;
    public static final RuntimeResourcePack RESOURCE_PACK = RuntimeResourcePack.create("fedora:hats");

    @Override
    public void pregen() {
        hatsFolder = new File(FabricLoader.getInstance().getGameDirectory(), "hats");
        modelsFolder = new File(hatsFolder, "models");
        textFolder = new File(hatsFolder, "textures");


        if (!hatsFolder.isDirectory()) {
            LOGGER.info("Not hats installed, downloading hats from server");
            downloadHatsFromServer();
        }

        File[] modelFiles = this.modelsFolder.listFiles(POSSIBLE_HAT);
        File[] textureFiles = this.textFolder.listFiles(POSSIBLE_TEXTURE);

        if (modelFiles != null) {
            HashMap<String, JLang> lang = new HashMap<>();

            for (File file : modelFiles) {
                String hatName = FilenameUtils.removeExtension(file.getName());
                String creator = "Unknown";
                Hat.Rarity rarity = Hat.Rarity.COMMON;

                try {
                    JsonObject jsonObject = new JsonObject();

                    try {
                        JsonParser parser = new JsonParser();
                        JsonElement jsonElement = parser.parse(new FileReader(file));
                        jsonObject = jsonElement.getAsJsonObject();

                    } catch (JsonIOException | JsonSyntaxException | FileNotFoundException e) {
                        LOGGER.severe("Failed to parse json for hat: " + file.getPath());
                    }

                    RESOURCE_PACK.addAsset(new Identifier("fedora", "models/block/" + hatName + ".json"), jsonObject.toString().getBytes());
                    RESOURCE_PACK.addBlockState(JState.state(JState.variant(JState.model("fedora:block/" + hatName))), new Identifier("fedora:" + hatName));

                    try {
                        creator = jsonObject.get("credit").getAsString();
                    } catch (Exception e) {
                        LOGGER.severe("Failed to read creator for hat: " + file.getPath());
                    }

                    try {
                        jsonObject.get("lang").getAsJsonObject().entrySet().forEach(stringJsonElementEntry -> {
                            lang.putIfAbsent(stringJsonElementEntry.getKey(), JLang.lang());
                            lang.get(stringJsonElementEntry.getKey()).translate("fedora.hat." + hatName, stringJsonElementEntry.getValue().getAsString());
                        });
                    } catch (Exception e) {
                        LOGGER.severe("Failed to read lang for hat: " + file.getPath());
                    }

                    try {
                        rarity = Hat.Rarity.valueOf(jsonObject.get("rarity").getAsString());
                    } catch (Exception e) {
                        LOGGER.severe("Failed to read rarity for hat: " + file.getPath());
                    }

                } catch (IllegalStateException e) {
                    LOGGER.severe("Something went wrong loading hat: " + file.getPath());
                }

                lang.forEach((id, jLang) -> {
                    RESOURCE_PACK.addLang(new Identifier("fedora", id), jLang);
                });

                HatManager.registerHat(hatName, creator, true, rarity);
            }
        }

        if (textureFiles != null) {
            for (File file : textureFiles) {
                try {
                    String textureName = FilenameUtils.removeExtension(file.getName());

                    BufferedImage in = ImageIO.read(file);

                    RESOURCE_PACK.addTexture(new Identifier("fedora", "block/" + textureName), in);
                } catch (IOException e) {
                    LOGGER.severe("Something went wrong loading texture: " + file.getPath());
                }
            }
        }

        RRPCallback.EVENT.register(a -> a.add(RESOURCE_PACK));
    }

    private static JsonObject readJsonUrl(String urlString) throws Exception {
        URL url = new URL(urlString);
        InputStreamReader reader = new InputStreamReader(url.openStream());

        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(reader);
        return jsonElement.getAsJsonObject();
    }

    private void downloadHatsFromServer() {
        try {
            JsonObject jsonObject = readJsonUrl("https://raw.githubusercontent.com/PotatoPresident/Fedora/master/hats/hats.json");

            JsonArray models = jsonObject.get("models").getAsJsonArray();
            JsonArray textures = jsonObject.get("textures").getAsJsonArray();

            modelsFolder.mkdir();
            for (JsonElement model : models) {
                File modelFile = new File(modelsFolder, model.getAsString() + ".json");
                FileUtils.copyURLToFile(new URL("https://raw.githubusercontent.com/PotatoPresident/Fedora/master/hats/models/" + model.getAsString() + ".json"), modelFile, 10000, 10000);
            }

            textFolder.mkdir();
            for (JsonElement texture : textures) {
                File textureFile = new File(textFolder, texture.getAsString() + ".png");
                FileUtils.copyURLToFile(new URL("https://raw.githubusercontent.com/PotatoPresident/Fedora/master/hats/textures/" + texture.getAsString() + ".png"), textureFile, 10000, 10000);
            }
        } catch (Exception e) {
            LOGGER.info("Failed to download hats from server");
            e.printStackTrace();
        }
    }
}

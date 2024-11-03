package mods.tesseract.dragonflyjs;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.Configuration;
import net.tclproject.mysteriumlib.asm.common.CustomLoadingPlugin;
import net.tclproject.mysteriumlib.asm.common.FirstClassTransformer;

import java.io.File;

@Mod(modid = "djs", acceptedMinecraftVersions = "[1.7.10]")
public class DragonflyJS extends CustomLoadingPlugin {
    public static String greeting;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) throws NoSuchMethodException {
        syncConfig(e.getSuggestedConfigurationFile());
    }

    public static void syncConfig(File f) {
        Configuration cfg = new Configuration(f);
        greeting = cfg.getString("greeting", Configuration.CATEGORY_GENERAL, "Hello World", "How shall I greet?");
        if (cfg.hasChanged()) {
            cfg.save();
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{FirstClassTransformer.class.getName()};
    }

    @Override
    public void registerFixes() {
        registerClassWithFixes("mods.tesseract.dragonflyjs.fix.Fixes");
    }
}
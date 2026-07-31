package keystrokesmod.keystroke;

import keystrokesmod.client.command.FakePlayerCommand;
import keystrokesmod.client.main.Raven;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(modid = "keystrokesmod", name = "KeystrokesMod", version = "KMV5", acceptedMinecraftVersions = "[1.7.10]")

public class KeyStrokeMod {
    private static KeyStroke keyStroke;
    private static final KeyStrokeRenderer keyStrokeRenderer = new KeyStrokeRenderer();
    private static boolean isKeyStrokeConfigGuiToggled;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new KeyStrokeCommand());
        ClientCommandHandler.instance.registerCommand(new FakePlayerCommand());
        MinecraftForge.EVENT_BUS.register(new KeyStrokeRenderer());
        MinecraftForge.EVENT_BUS.register(this);
        Raven.init();
        Raven.clientConfig.applyKeyStrokeSettingsFromConfigFile();
    }

    public static KeyStroke getKeyStroke() {
        return keyStroke;
    }

    public static KeyStrokeRenderer getKeyStrokeRenderer() {
        return keyStrokeRenderer;
    }

    public static void toggleKeyStrokeConfigGui() {
        isKeyStrokeConfigGuiToggled = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (isKeyStrokeConfigGuiToggled) {
            isKeyStrokeConfigGuiToggled = false;
            Minecraft.getMinecraft().displayGuiScreen(new KeyStrokeConfigGui());
        }
    }
}

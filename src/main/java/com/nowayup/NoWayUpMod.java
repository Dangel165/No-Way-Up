package com.nowayup;

import com.mojang.logging.LogUtils;
import com.nowayup.event.NoWayUpEvents;
import com.nowayup.network.NoWayUpNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(NoWayUpMod.MOD_ID)
public class NoWayUpMod {
    public static final String MOD_ID = "nowayup";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NoWayUpMod() {
        NoWayUpNetwork.register();
        MinecraftForge.EVENT_BUS.register(new NoWayUpEvents());
    }
}

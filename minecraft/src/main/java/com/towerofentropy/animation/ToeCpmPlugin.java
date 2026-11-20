package com.towerofentropy.animation;

import com.tom.cpm.api.CPMPlugin;
import com.tom.cpm.api.ICPMPlugin;
import com.tom.cpm.api.IClientAPI;
import com.tom.cpm.api.ICommonAPI;

@CPMPlugin
public final class ToeCpmPlugin implements ICPMPlugin {
    public static ICommonAPI common;
    public static IClientAPI client;
    @Override public void initCommon(ICommonAPI api) { common = api; }
    @Override public void initClient(IClientAPI api) { client=api; }
    @Override public String getOwnerModId() { return "towerofentropy"; }
}

package com.yesclient.module.modules.visuals;

import java.util.List;

import com.yesclient.module.Category;
import com.yesclient.module.Module;
import com.yesclient.module.ModuleMode;
import com.yesclient.module.settings.NumberSetting;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.shader.Shader;
import net.minecraft.client.shader.ShaderGroup;
import net.minecraft.client.shader.ShaderUniform;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MotionBlurModule extends Module {
    private static final Logger LOGGER = LogManager.getLogger("YesClient/MotionBlur");
    private static final ResourceLocation PHOSPHOR_SHADER = new ResourceLocation("shaders/post/phosphor.json");

    private final NumberSetting level = new NumberSetting("Level", "Motion blur strength", 5, 1, 10);
    private ShaderGroup ownedShaderGroup;
    private World previousWorld;
    private boolean shaderLoadFailed;

    public MotionBlurModule() {
        super("Motion Blur", "Blend recent rendered frames while the camera and world move", Category.VISUALS,
                ModuleMode.BOTH);
        addSetting(level);
    }

    @Override
    protected void onEnable() {
        ownedShaderGroup = null;
        previousWorld = null;
        shaderLoadFailed = false;
    }

    @Override
    protected void onDisable() {
        stopOwnedShader();
        previousWorld = null;
    }

    @Override
    public void onTick() {
        if (mc == null || mc.entityRenderer == null) return;
        if (mc.thePlayer == null || mc.theWorld == null) {
            stopOwnedShader();
            previousWorld = null;
            return;
        }

        if (!OpenGlHelper.shadersSupported || shaderLoadFailed) return;

        if (mc.currentScreen != null) {
            ShaderGroup activeGroup = mc.entityRenderer.getShaderGroup();
            if (activeGroup == ownedShaderGroup && activeGroup != null) applyStrength(activeGroup);
            else ownedShaderGroup = null;
            return;
        }

        if (previousWorld != null && previousWorld != mc.theWorld
                && mc.entityRenderer.getShaderGroup() == ownedShaderGroup) {
            mc.entityRenderer.stopUseShader();
            ownedShaderGroup = null;
        }
        previousWorld = mc.theWorld;

        ShaderGroup activeGroup = mc.entityRenderer.getShaderGroup();
        if (activeGroup == null) {
            try {
                mc.entityRenderer.loadShader(PHOSPHOR_SHADER);
                activeGroup = mc.entityRenderer.getShaderGroup();
                if (activeGroup == null) return;
                ownedShaderGroup = activeGroup;
            } catch (Throwable error) {
                shaderLoadFailed = true;
                LOGGER.error("Could not load the motion blur post-processing shader", error);
                return;
            }
        } else if (activeGroup != ownedShaderGroup) {
            ownedShaderGroup = null;
            return;
        }

        applyStrength(activeGroup);
    }

    private void applyStrength(ShaderGroup group) {
        try {
            float normalized = (level.getFloatValue() - 1.0F) / 9.0F;
            float retention = 0.64F + normalized * 0.315F;
            List<Shader> shaders = ReflectionHelper.getPrivateValue(ShaderGroup.class, group,
                    "listShaders", "field_148031_d", "d");
            for (Shader shader : shaders) {
                ShaderUniform uniform = shader.getShaderManager().getShaderUniform("Phosphor");
                if (uniform != null) uniform.set(retention, retention, retention);
            }
        } catch (RuntimeException error) {
            shaderLoadFailed = true;
            stopOwnedShader();
            LOGGER.error("Could not update the motion blur strength", error);
        }
    }

    private void stopOwnedShader() {
        if (mc != null && mc.entityRenderer != null && ownedShaderGroup != null
                && mc.entityRenderer.getShaderGroup() == ownedShaderGroup) {
            mc.entityRenderer.stopUseShader();
        }
        ownedShaderGroup = null;
    }
}

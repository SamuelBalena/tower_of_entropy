package com.towerofentropy.combat;

import com.mojang.logging.LogUtils;
import com.towerofentropy.client.PlayerRendererR3;
import com.towerofentropy.client.ClientWallVisual;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "towerofentropy", value = Dist.CLIENT)
public final class ExpansionRuntimeTest {
    private static final int EMPTY_HAND_SLOT = 8;
    private static final BlockPos DIRT = new BlockPos(1, 101, 1);
    private static final Set<String> combat = new HashSet<>(), mining = new HashSet<>(), misses = new HashSet<>();
    private static final int[][] DIRECTIONS = {{0,1},{1,1},{1,0},{1,-1},{0,-1},{-1,-1},{-1,0},{-1,1}};
    private static boolean opening, leftWall, landed;
    private static volatile int targetId = -1;
    private static int title, age, wallTicks, dirs, sprintDirs, fixtureWait;
    private static boolean wallRearmed, releasedOnExit, reattachedAfterExhaustion;
    private static int missClicks;
    private static boolean walked, stayedSprintingOnReverse;
    private static double minSprintSpeed = Double.POSITIVE_INFINITY;
    private static float largestYawError;
    private static double lastY, fastestSlide, fastestFall;
    private static volatile float targetHealth;
    private static volatile boolean dirtBroken;
    private static final Set<String> heldMine = new HashSet<>();
    private static double jumpBase, groundHeight, verticalHeight, awayHeight, wallBase, wallZ, awayDistance;
    private static boolean retriedWallJump;
    private static int wallJumpActivations;
    private static boolean lastWallJump, badWallPose;
    private static boolean toolsComplete, weaponsComplete;
    private static final boolean EIGHT = Boolean.getBoolean("toe.eight.direction.test");
    private static final Set<Integer> jumpDirections = new HashSet<>();
    private static final double[] distances = new double[4], heights = new double[4], serverKick = new double[4];
    private static int measuredScenario = -1;
    private static volatile int serverScenario = -1;
    private static final int[] serverLaunches = new int[4];
    private static double boostedPeakSpeed;
    private static boolean originalSlide, originalHang, originalClimb, backwardSprintJump;
    private static double launchX, launchY, launchZ;
    private static boolean seenAir, measuredLanding, reversedAir;
    private static float lastAirYaw, reversalStartYaw, maxYawStep, rawAirYawStep;
    private static int rawAirSamples;
    private static final boolean PROTOTYPES = Boolean.getBoolean("toe.wall.prototype.test");
    private static final String[] PROFILES = {"original", "experimental", "misto"};
    private static final Set<String> prototypeSlides = new HashSet<>(), prototypeHangs = new HashSet<>(),
            prototypeClimbs = new HashSet<>(), prototypeCaptures = new HashSet<>();
    private static boolean switchedDuringClimb;
    private static float climbTimeBeforeSwitch;
    private static int prototypeClimbStart;
    private static final Set<String> groundClips = new HashSet<>(), verticalClips = new HashSet<>(),
            awayClips = new HashSet<>();
    private static boolean wallLedgeJump, ledgeClimbClip, verticalRecontact, isolatedVerticalStarted, isolatedWallRemoved;
    private static final Set<String> isolatedVerticalClips = new HashSet<>();
    private static boolean jumpRequested, caught, climbed, ledgeClimbRequested, caughtAfterWall, blockedCatch, leftMining;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !Boolean.getBoolean("toe.expansion.test")) return;
        var mc = Minecraft.getInstance();
        if (!opening && mc.screen instanceof TitleScreen && mc.getOverlay() == null && ++title > 60) {
            opening = true;
            mc.options.pauseOnLostFocus = false;
            mc.createWorldOpenFlows().loadLevel(mc.screen, "Global Survival one");
        }
        if (mc.player == null || mc.level == null) return;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        var player = mc.player;
        if (age == 28 && (mc.level.getEntity(targetId) == null || PlayerRendererR3.drawCalls < 10 || !mc.player.getMainHandItem().isEmpty())
                && ++fixtureWait < 600) return;
        age++;
        if (age == 5) {
            A12RuntimeTest.preview(mc);
            mc.options.setCameraType(CameraRuntimeTest.ENABLED || Boolean.getBoolean("toe.c0012.movement") ? CameraType.THIRD_PERSON_BACK : CameraType.THIRD_PERSON_FRONT);
        }
        if (age == 15) A3RuntimeTest.server(p -> {
            try { A3RuntimeTest.fixture(p, ""); }
            catch (Exception ex) { throw new IllegalStateException(ex); }
            var serverLevel = p.serverLevel();
            p.setInvulnerable(true);
            // Clearing old support blocks can drop items into the first empty hotbar slot.
            p.getInventory().selected = EMPTY_HAND_SLOT;
            p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(EMPTY_HAND_SLOT));
            p.inventoryMenu.broadcastFullState();
            serverLevel.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
            serverLevel.setDayTime(18000);
            for (int y = 100; y < 114; y++)
                serverLevel.setBlockAndUpdate(new BlockPos(0, y, 1), Blocks.AIR.defaultBlockState());
            serverLevel.setBlockAndUpdate(DIRT, Blocks.DIRT.defaultBlockState());
            var zombie = new Zombie(EntityType.ZOMBIE, serverLevel);
            zombie.setPos(.5, 100, 1.9);
            zombie.setNoAi(true);
            zombie.setPersistenceRequired();
            serverLevel.addFreshEntity(zombie);
            targetId = zombie.getId();
        });
        if (Boolean.getBoolean("toe.c001.stability.qa") && age >= 29) {
            if (age == 29) {
                age = GlobalPunchRuntimeTest.END;
                A3RuntimeTest.server(p -> {
                    var originalTarget = p.serverLevel().getEntity(targetId);
                    if (originalTarget != null) originalTarget.discard();
                });
            }
            if (age > GlobalPunchRuntimeTest.END && age <= CameraRuntimeTest.END) CameraRuntimeTest.tick(mc, age);
            if (age >= CameraRuntimeTest.END) CameraRefinementRuntimeTest.tick(mc, age - CameraRuntimeTest.END);
            if (age == CameraRuntimeTest.END + CameraRefinementRuntimeTest.STABILITY_LENGTH) mc.stop();
            return;
        }
        if (Boolean.getBoolean("toe.w0011.only") && age >= 29) {
            if (age == 29) age = AirSwordCameraRuntimeTest.START;
            String current = PlayerRendererR3.controller() == null ? "" : PlayerRendererR3.controller().getAnimationName();
            if (AirSwordCameraRuntimeTest.tick(mc, age, current)) mc.stop();
            return;
        }
        if (Boolean.getBoolean("toe.w0011.restart")) {
            if (age == 55) AirSwordCameraRuntimeTest.restart(mc);
            return;
        }
        if (Boolean.getBoolean("toe.c0012.only") && age >= 29) {
            if (age == 29) {
                age = SwordZoomRuntimeTest.START;
                A3RuntimeTest.server(p -> {
                    var entity = p.serverLevel().getEntity(targetId);
                    if (entity != null) entity.discard();
                });
            }
            String current = PlayerRendererR3.controller() == null ? "" : PlayerRendererR3.controller().getAnimationName();
            if (SwordZoomRuntimeTest.tick(mc, age, current)) mc.stop();
            return;
        }
        if (Boolean.getBoolean("toe.c0012.restart")) {
            if (age == 55) SwordZoomRuntimeTest.restart(mc);
            return;
        }
        if ((age == 45 || age == 68) && mc.level.getEntity(targetId) != null) {
            mc.gameMode.attack(player, mc.level.getEntity(targetId));
            player.swing(InteractionHand.MAIN_HAND);
        }
        String clip = PlayerRendererR3.controller() == null ? "" : PlayerRendererR3.controller().getAnimationName();
        if (age == 49 || age == 72 || age == 105 || age == 150 || age == 170) {
            LogUtils.getLogger().info("TOE_PUNCH_TRACE age={} clip={} selected={} fallback={} draws={} screen={} item={}",
                    age, clip, com.towerofentropy.client.VisualContext.selected(), PlayerRendererR3.fallbackReason,
                    PlayerRendererR3.drawCalls, mc.screen, player.getMainHandItem());
        }
        if (age > 45 && age < 80 && clip.startsWith("UAL_punch_")) combat.add(clip);
        if (age == 85) A3RuntimeTest.server(p -> {
            if (p.serverLevel().getEntity(targetId) instanceof Zombie zombie) {
                targetHealth = zombie.getHealth();
                zombie.discard();
            }
        });
        if (age == 100) mc.gameMode.startDestroyBlock(DIRT, Direction.NORTH);
        if (age >= 100 && age < 130 && !mc.level.getBlockState(DIRT).isAir()
                && mc.gameMode.continueDestroyBlock(DIRT, Direction.NORTH)) player.swing(InteractionHand.MAIN_HAND);
        if (age > 100 && age < 135 && clip.startsWith("UAL_punch_")) mining.add(clip);
        if (age == 137) A3RuntimeTest.server(p -> dirtBroken = p.serverLevel().getBlockState(DIRT).isAir());

        if (age >= 140 && age < 185) {
            player.setXRot(-90);
            if (age == 145 || age == 166) {
                if (mc.hitResult != null && mc.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS)
                    missClicks++;
                KeyMapping.click(mc.options.keyAttack.getKey());
            }
            if (clip.startsWith("UAL_punch_")) misses.add(clip);
        }
        if (age == 185) player.setXRot(0);
        if (age >= 190 && age < 510) {
            int phase = (age - 190) % 40;
            if (phase == 0) A3RuntimeTest.server(p -> {
                for (int x = -10; x <= 10; x++) for (int z = -10; z <= 10; z++)
                    p.serverLevel().setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
                p.teleportTo(p.serverLevel(), .5, 100, .5, 0, 0);
            });
            int[] d = DIRECTIONS[(age - 190) / 40];
            player.setYRot(0);
            boolean move = phase >= 12 && phase < 38;
            mc.options.keySprint.setDown(move && phase < 26);
            mc.options.keyUp.setDown(move && d[1] > 0);
            mc.options.keyDown.setDown(move && d[1] < 0);
            mc.options.keyLeft.setDown(move && d[0] > 0);
            mc.options.keyRight.setDown(move && d[0] < 0);
            if (phase == 35 && player.position().subtract(.5, 100, .5).horizontalDistanceSqr() > .4) {
                dirs++;
                LogUtils.getLogger().info("TOE_DIRECTION {} sprint={} clip={} yaw={}", (age - 190) / 40,
                        player.isSprinting(), clip, PlayerRendererR3.lastYaw);
                if (player.isSprinting() && clip.equals("UAL_sprint")) {
                    sprintDirs++;
                    minSprintSpeed = Math.min(minSprintSpeed, player.position().subtract(player.xo, player.yo, player.zo).horizontalDistance());
                }
                float target = (float) Math.toDegrees(Math.atan2(-d[0], d[1]));
                largestYawError = Math.max(largestYawError, Math.abs(Mth.wrapDegrees(target - PlayerRendererR3.lastYaw)));
            }
        }
        if (age == 530) A3RuntimeTest.server(p -> {
            for (int y = 100; y < 114; y++)
                p.serverLevel().setBlockAndUpdate(new BlockPos(0, y, 1), Blocks.STONE.defaultBlockState());
            p.teleportTo(p.serverLevel(), .5, 108, .68, 180, 0);
        });
        if (age > 530 && age < 615) {
            boolean slide = ClientWallVisual.active();
            double dy = lastY - player.getY();
            if (age % 5 == 0 && !player.onGround())
                LogUtils.getLogger().info("TOE_WALL_TRACE {} {} {} {}", age, slide, player.getY(), dy);
            if (slide) {
                reattachedAfterExhaustion |= leftWall;
                if (wallTicks > 0 && dy < 1) fastestSlide = Math.max(fastestSlide, dy);
                wallTicks++;
            } else if (wallTicks > 0) {
                if (!player.onGround()) {
                    leftWall = true;
                    fastestFall = Math.max(fastestFall, dy);
                }
                landed |= player.onGround();
            }
        }
        lastY = player.getY();
        if (age == 620) A3RuntimeTest.server(p ->
                p.teleportTo(p.serverLevel(), .5, 108, .68, 180, 0));
        if (age > 620 && age < 639) wallRearmed |= ClientWallVisual.active();
        if (age == 639) A3RuntimeTest.server(p -> {
            for (int y = 100; y < 114; y++)
                p.serverLevel().setBlockAndUpdate(new BlockPos(0, y, 1), Blocks.AIR.defaultBlockState());
        });
        if (age > 639 && age < 660 && wallRearmed && !player.onGround() && !ClientWallVisual.active())
            releasedOnExit = true;
        if (age == 670) A3RuntimeTest.server(p -> p.teleportTo(p.serverLevel(), .5, 100, .5, 0, 0));
        if (age >= 680 && age < 724) {
            player.setYRot(0);
            mc.options.keyUp.setDown(age < 710);
            mc.options.keyDown.setDown(age >= 710 && age < 723);
            mc.options.keySprint.setDown(age >= 698 && age < 710);
            if (age == 695) {
                walked = clip.equals("UAL_walk");
                if (Boolean.getBoolean("toe.w002.walk.trace"))
                    LogUtils.getLogger().info("TOE_W002_WALK_TRACE clip={} speed={} ground={} sprint={} screen={} held={}",
                            clip, player.getDeltaMovement().horizontalDistance(), player.onGround(), player.isSprinting(),
                            mc.screen, mc.options.keyUp.isDown());
            }
            if (age >= 710 && age <= 720) {
                if (age == 710) stayedSprintingOnReverse = player.isSprinting();
                else stayedSprintingOnReverse &= player.isSprinting();
            }
            if (age == 721) stayedSprintingOnReverse &= clip.equals("UAL_sprint")
                    && player.getZ() - player.zo < -.24
                    && Math.abs(Mth.wrapDegrees(PlayerRendererR3.lastYaw - 180)) < 15;
        }
        if (age >= 730 && Boolean.getBoolean("toe.parkour.test")) parkour(mc, clip);
        if (PROTOTYPES && age >= 1340) compareProfiles(mc, clip);
        if (EIGHT) {
            originalSlide |= ClientWallVisual.active() && clip.equals("UAL_wall_slide");
            originalHang |= "ledge_hang".equals(ClientWallVisual.clip()) && clip.equals("UAL_ledge_hang");
            originalClimb |= "ledge_climb".equals(ClientWallVisual.clip()) && clip.equals("UAL_ledge_climb");
            if (age >= 1340) directionJumps(mc, clip);
        }
        if (GlobalPunchRuntimeTest.ENABLED && age >= 2310) GlobalPunchRuntimeTest.tick(mc, age, clip);
        if (CameraRuntimeTest.ENABLED && !Boolean.getBoolean("toe.c0012.qa") && age > GlobalPunchRuntimeTest.END) CameraRuntimeTest.tick(mc, age);
        if (CameraRefinementRuntimeTest.ENABLED && age >= CameraRuntimeTest.END) CameraRefinementRuntimeTest.tick(mc, age - CameraRuntimeTest.END);
        if (ToolPolicyRuntimeTest.ENABLED && !toolsComplete) toolsComplete = ToolPolicyRuntimeTest.tick(mc, age, clip);
        boolean toolPolicyDone = Boolean.getBoolean("toe.c0012.qa") ? SwordZoomRuntimeTest.tick(mc, age, clip) : toolsComplete;
        if (WeaponBlockRuntimeTest.ENABLED && !weaponsComplete) weaponsComplete = WeaponBlockRuntimeTest.tick(mc, age, clip);
        boolean weaponDone = AirSwordCameraRuntimeTest.ENABLED ? AirSwordCameraRuntimeTest.tick(mc, age, clip) : weaponsComplete;
        if (weaponDone || !WeaponBlockRuntimeTest.ENABLED && (toolPolicyDone || !ToolPolicyRuntimeTest.ENABLED && age == (CameraRefinementRuntimeTest.ENABLED ? CameraRuntimeTest.END + CameraRefinementRuntimeTest.LENGTH : CameraRuntimeTest.ENABLED ? CameraRuntimeTest.END : GlobalPunchRuntimeTest.ENABLED ? GlobalPunchRuntimeTest.END : EIGHT ? 2310 : PROTOTYPES ? 1630 : Boolean.getBoolean("toe.wall.research.test") ? 1340 : Boolean.getBoolean("toe.parkour.test") ? 1240 : 730))) {
            check("combat", combat.size() == 2 && targetHealth > 0 && targetHealth < 20, combat + " health=" + targetHealth);
            check("mining", !mining.isEmpty() && dirtBroken, mining + " broken=" + dirtBroken);
            check("miss", missClicks == 2 && misses.size() == 2, misses + " emptyClicks=" + missClicks);
            check("eight_direction_sprint", dirs == 8 && sprintDirs == 8 && minSprintSpeed > .24 && largestYawError < 10,
                    dirs + "/8 sprint=" + sprintDirs + "/8 speed=" + minSprintSpeed + " yaw=" + largestYawError);
            check("walk_and_reverse", walked && stayedSprintingOnReverse,
                    "walk=" + walked + " keptSprint=" + stayedSprintingOnReverse);
            check("wall", wallTicks >= 30 && wallTicks <= 40 && leftWall && landed && !reattachedAfterExhaustion && fastestSlide < .15 && fastestFall > .2,
                    "ticks=" + wallTicks + " released=" + leftWall + " landed=" + landed
                    + " slideSpeed=" + fastestSlide + " fallSpeed=" + fastestFall);
            check("wall_reset_and_exit", wallRearmed && releasedOnExit, "rearmed=" + wallRearmed + " released=" + releasedOnExit);
            check("renderer", PlayerRendererR3.errors == 0, "errors=" + PlayerRendererR3.errors);
            if (Boolean.getBoolean("toe.parkour.test")) {
                check("held_mine", heldMine.size() == 2 && leftMining, heldMine + " ended=" + leftMining);
                check("ground_jump", height(groundHeight), "height=" + groundHeight);
                check("vertical_wall_jump", height(verticalHeight), "height=" + verticalHeight);
                check("wall_jump_lockout", retriedWallJump && wallJumpActivations == 1,
                        "retried=" + retriedWallJump + " activations=" + wallJumpActivations);
                check("away_wall_jump", height(awayHeight) && awayDistance > .5, "height=" + awayHeight + " away=" + awayDistance);
                check("catch_and_climb", caught && climbed, "caught=" + caught + " requested=" + ledgeClimbRequested + " climbed=" + climbed);
                check("wall_to_ledge", caughtAfterWall, "caught=" + caughtAfterWall);
                check("blocked_ledge", !blockedCatch, "caught=" + blockedCatch);
                if (Boolean.getBoolean("toe.wall.research.test")) {
                    check("ground_jump_clips", jumpCycle(groundClips), groundClips.toString());
                    check("vertical_jump_clips", jumpCycle(isolatedVerticalClips) && verticalRecontact && !badWallPose,
                            "landing run=" + isolatedVerticalClips + " recontact=" + verticalRecontact);
                    check("away_jump_clips", jumpCycle(awayClips) && !badWallPose, awayClips.toString());
                    check("ledge_clips", wallLedgeJump && caughtAfterWall && ledgeClimbClip,
                            "jump=" + wallLedgeJump + " hang=" + caughtAfterWall + " climb=" + ledgeClimbClip);
                }
            }
            if (PROTOTYPES) {
                check("profile_slide", prototypeSlides.size() == 3, prototypeSlides.toString());
                check("profile_hang", prototypeHangs.size() == 3, prototypeHangs.toString());
                check("profile_climb", prototypeClimbs.size() == 3 && switchedDuringClimb,
                        prototypeClimbs + " phase preserved=" + switchedDuringClimb);
            }
            if (EIGHT) {
                check("jump_eight_directions", jumpDirections.size()==8, jumpDirections.toString());
                check("backward_sprint_jump", backwardSprintJump, "sprint backwards without turning camera");
                if (Boolean.getBoolean("toe.c0012.movement"))
                    check("air_reversal_raw_clamp", rawAirSamples > 0 && rawAirYawStep <= 35.001f,
                            "samples=" + rawAirSamples + " maxTickYaw=" + rawAirYawStep);
                check("air_reversal", reversedAir && maxYawStep <= 36, "reverse="+reversedAir+" maxYawStep="+maxYawStep);
                check("jump_distances", distances[0]<.05 && distances[1]>1 && distances[3]>distances[2]+.12,
                        java.util.Arrays.toString(distances));
                check("jump_heights", java.util.Arrays.stream(heights).allMatch(n->Math.abs(n-2.50)<.045),
                        java.util.Arrays.toString(heights));
                check("server_sprint_kick", Math.abs(serverKick[2]-.20)<.035 && Math.abs(serverKick[3]-.23)<.035
                        && Math.abs((serverKick[3]-serverKick[2])-.03)<.012, java.util.Arrays.toString(serverKick));
                check("one_takeoff_only", java.util.Arrays.stream(serverLaunches).allMatch(n->n==1) && boostedPeakSpeed<.65,
                        java.util.Arrays.toString(serverLaunches)+" peak="+boostedPeakSpeed);
                check("wall_jump_no_bonus", Math.abs(awayDistance-3.6487)<.22, "away="+awayDistance);
                check("original_wall_clips", originalSlide && originalHang && originalClimb,
                        "slide="+originalSlide+" hang="+originalHang+" climb="+originalClimb);
            }
            mc.stop();
        }
    }

    private static boolean jumpCycle(Set<String> clips) {
        return clips.containsAll(Set.of("UAL_jump_start", "UAL_jump_loop", "UAL_jump_land"));
    }

    private static boolean height(double value) {
        return value > 2.25 && value < 2.75;
    }

    private static void parkour(Minecraft mc, String clip) {
        var p = mc.player;
        if (age == 740) setup(.5, false, 0);
        if (age == 758) jumpBase = p.getY();
        mc.options.keyJump.setDown(age == 759);
        if (age >= 759 && age < 790) {
            groundHeight = Math.max(groundHeight, p.getY() - jumpBase);
            groundClips.add(clip);
        }

        if (PROTOTYPES && age == 800) profile(mc, "experimental");
        if (PROTOTYPES && age == 915) profile(mc, "original");
        if (age == 800 || age == 860 || age == 1080) {
            jumpRequested = false;
            setup(.68, true, age == 1080 ? 109 : 115);
        }
        boolean vertical = age >= 805 && age < 850;
        boolean away = age >= 865 && age < 910;
        boolean wallLedge = age >= 1085 && age < 1140;
        if (vertical || away || wallLedge) {
            boolean inWallJump = ClientWallVisual.jumping();
            if (vertical && inWallJump && !lastWallJump) wallJumpActivations++;
            lastWallJump = inWallJump;
            // The sampled renderer frame may predate a packet processed on this client tick.
            if (inWallJump && java.util.Objects.toString(com.towerofentropy.client.VisualContext.selected(), "").startsWith("wall_slide"))
                badWallPose = true;
            if (jumpRequested) {
                if (vertical) {
                    verticalRecontact |= clip.startsWith("UAL_wall_slide") && !inWallJump
                            && verticalClips.contains("UAL_jump_loop");
                    verticalClips.add(clip);
                }
                if (away) awayClips.add(clip);
                if (wallLedge && clip.startsWith("UAL_jump_")) wallLedgeJump = true;
            }
            if (vertical && age > 819 && jumpRequested && !retriedWallJump && ClientWallVisual.active()) {
                retriedWallJump = true;
                com.towerofentropy.animation.ActionNetwork.requestParkour(0, 1);
            }
            if (!jumpRequested && ClientWallVisual.active()) {
                wallBase = p.getY();
                wallZ = p.getZ();
                mc.options.keyUp.setDown(!away);
                mc.options.keyDown.setDown(away);
                mc.options.keyJump.setDown(true);
                jumpRequested = true;
            } else {
                mc.options.keyUp.setDown(false);
                mc.options.keyDown.setDown(false);
            }
            if (jumpRequested) {
                var rise = p.getY() - wallBase;
                if (vertical) verticalHeight = Math.max(verticalHeight, rise);
                if (away) {
                    awayHeight = Math.max(awayHeight, rise);
                    awayDistance = Math.max(awayDistance, wallZ - p.getZ());
                }
                if (wallLedge && clip.equals("UAL_ledge_hang")) caughtAfterWall = true;
            }
        }
        if (age == 915 || age == 1010) setup(.68, false, age == 915 ? 103 : 104);
        if (age == 936 || age == 1031) mc.options.keyJump.setDown(true);
        if (age > 936 && age < 968 && clip.equals("UAL_ledge_hang")) caught = true;
        if (age > 942 && age < 968 && !ledgeClimbRequested && "ledge_hang".equals(ClientWallVisual.clip())) {
            ledgeClimbRequested = true;
            mc.options.keyJump.setDown(true);
        }
        if (age > 942 && age < 991 && clip.equals("UAL_ledge_climb")) ledgeClimbClip = true;
        if (age > 973 && age < 1000 && p.onGround() && p.getY() >= 103 && p.getZ() > 1) climbed = true;
        if (age > 1031 && age < 1068 && clip.equals("UAL_ledge_hang")) blockedCatch = true;

        if (age == 1150) A3RuntimeTest.server(server -> {
            server.getInventory().selected = EMPTY_HAND_SLOT;
            server.connection.send(new net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket(EMPTY_HAND_SLOT));
            server.inventoryMenu.broadcastFullState();
            for (int y = 100; y < 115; y++) server.serverLevel().setBlockAndUpdate(new BlockPos(0, y, 1), Blocks.AIR.defaultBlockState());
            server.serverLevel().setBlockAndUpdate(new BlockPos(0, 101, 2), Blocks.OBSIDIAN.defaultBlockState());
            server.teleportTo(server.serverLevel(), .5, 100, .5, 0, 0);
        });
        if (age == 1170) mc.mouseHandler.grabMouse();
        if (age >= 1170 && age < 1220) {
            p.setYRot(0);
            p.setXRot(0);
            if (age == 1170) KeyMapping.click(mc.options.keyAttack.getKey());
            mc.options.keyAttack.setDown(true);
            if (age % 6 == 0) LogUtils.getLogger().info("TOE_HELD_MINE {} active={} hit={} clip={} item={}",
                    age, mc.gameMode.isDestroying(), mc.hitResult, clip, p.getMainHandItem());
            if (clip.startsWith("UAL_punch_")) heldMine.add(clip);
        }
        if (age == 1220) mc.options.keyAttack.setDown(false);
        if (age > 1227 && age < 1240) leftMining |= !clip.startsWith("UAL_punch_");
        if (age == 1250) setup(.68, true, 115);
        if (age >= 1255 && age < 1300) {
            if (!isolatedVerticalStarted && ClientWallVisual.active()) {
                isolatedVerticalStarted = true;
                mc.options.keyJump.setDown(true);
            }
            if (ClientWallVisual.jumping() && !isolatedWallRemoved) {
                isolatedWallRemoved = true;
                A3RuntimeTest.server(server -> {
                    for (int y = 100; y < 115; y++) server.serverLevel().setBlockAndUpdate(
                            new BlockPos(0, y, 1), Blocks.AIR.defaultBlockState());
                });
            }
        }
        if (isolatedWallRemoved && age < 1340) isolatedVerticalClips.add(clip);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public static void takeoff(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        int i = serverScenario;
        if (EIGHT && i >= 0 && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            serverKick[i] = player.getDeltaMovement().horizontalDistance();
            serverLaunches[i]++;
        }
    }

    private static void directionJumps(Minecraft mc, String clip) {
        var p=mc.player;
        if (age==1341) A3RuntimeTest.server(server->{
            var level=server.serverLevel();
            level.setDayTime(6000);
            level.setBlockAndUpdate(new BlockPos(0,101,2),Blocks.AIR.defaultBlockState());
            for(int x=-14;x<=14;x++) for(int z=-14;z<=24;z++)
                level.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.STONE.defaultBlockState());
        });
        if(age<1350 || age>=2302) return;
        int scenario=(age-1350)/68, phase=(age-1350)%68;
        if(scenario>=14)return;
        if(phase==0){
            setup(.5,false,0);
            if(scenario>=9 && scenario<=12) A3RuntimeTest.server(server->{
                var bonus=(net.minecraftforge.common.ForgeConfigSpec.DoubleValue)
                        com.towerofentropy.movement.SprintJump.SPEC.getValues().get("sprintJumpBonus");
                bonus.set(scenario==12?.15:0.0);
                serverScenario=scenario-9;
            });
            measuredScenario=scenario>=9&&scenario<=12?scenario-9:-1;
            seenAir=false;
            measuredLanding=false;
            lastAirYaw=Float.NaN;
            mc.options.hideGui=true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        int[] direction=scenario<8?DIRECTIONS[scenario]:(scenario==8&&phase>=34)||scenario==13?DIRECTIONS[4]:DIRECTIONS[0];
        boolean move=phase>=12&&phase<61&&scenario!=9;
        mc.options.keyUp.setDown(move&&direction[1]>0);
        mc.options.keyDown.setDown(move&&direction[1]<0);
        mc.options.keyLeft.setDown(move&&direction[0]>0);
        mc.options.keyRight.setDown(move&&direction[0]<0);
        mc.options.keySprint.setDown(move&&scenario>=11);
        mc.options.keyJump.setDown(phase==31);
        if(phase==31){
            launchX=p.getX();launchY=p.getY();launchZ=p.getZ();
            reversalStartYaw=PlayerRendererR3.lastYaw;
        }
        if(phase>31 && !p.onGround()){
            seenAir=true;
            if(measuredScenario==3) boostedPeakSpeed=Math.max(boostedPeakSpeed,Math.hypot(p.getX()-p.xo,p.getZ()-p.zo));
            if(measuredScenario>=0) heights[measuredScenario]=Math.max(heights[measuredScenario],p.getY()-launchY);
            if(scenario<8&&phase==40&&clip.startsWith("UAL_jump_")){
                float expected=(float)Math.toDegrees(Math.atan2(-direction[0],direction[1]));
                if(Math.abs(Mth.wrapDegrees(expected-PlayerRendererR3.lastYaw))<12) jumpDirections.add(scenario);
                capture(mc,"jump-direction-"+scenario);
            }
            if(scenario==13&&phase==38) backwardSprintJump=p.isSprinting() && p.getZ()-p.zo<-.24
                    && clip.startsWith("UAL_jump_") && Math.abs(Mth.wrapDegrees(PlayerRendererR3.lastYaw-180))<12;
            if(scenario==8&&clip.startsWith("UAL_jump_")){
                if (Boolean.getBoolean("toe.c0012.movement") && !p.onGround()) sampleRawAirYaw();
                float yaw=PlayerRendererR3.lastYaw;
                if(Float.isFinite(lastAirYaw)) maxYawStep=Math.max(maxYawStep,Math.abs(Mth.wrapDegrees(yaw-lastAirYaw)));
                lastAirYaw=yaw;
                reversedAir|=p.getZ()-p.zo<-.025&&Math.abs(Mth.wrapDegrees(yaw-reversalStartYaw))>100;
            }
        }
        if(seenAir&&p.onGround()&&!measuredLanding){
            measuredLanding=true;
            if(measuredScenario>=0) distances[measuredScenario]=Math.hypot(p.getX()-launchX,p.getZ()-launchZ);
            serverScenario=-1;
        }
    }

    private static void compareProfiles(Minecraft mc, String clip) {
        if (age == 1348) {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            A3RuntimeTest.server(server -> server.serverLevel().setDayTime(6000));
            A3RuntimeTest.sideCamera = true;
        }
        if (age == 1350) setup(.68, true, 115);
        for (int i = 0; i < PROFILES.length; i++) {
            int start = 1356 + 8*i;
            if (age == start) profile(mc, PROFILES[i]);
            if (age >= start+3 && age < start+8
                    && clip.equals(i == 1 ? "UAL_wall_slide_push" : "UAL_wall_slide")) {
                prototypeSlides.add(PROFILES[i]);
                capture(mc, PROFILES[i]+"-slide");
            }
        }
        if (age < 1400 || age >= 1610) return;
        int index = (age - 1400)/70, phase = (age - 1400)%70;
        String profile = PROFILES[index];
        if (phase == 0) {
            setup(.68, false, 103);
            profile(mc, profile);
            prototypeClimbStart = -1;
        }
        if (phase == 17) mc.options.keyJump.setDown(true);
        if (phase > 17 && phase < 38 && clip.equals("UAL_ledge_hang")) {
            prototypeHangs.add(profile);
            capture(mc, profile+"-hang");
            if (prototypeClimbStart < 0) {
                prototypeClimbStart=age;
                mc.options.keyJump.setDown(true);
            }
        }
        if (phase > 19 && phase < 58 && prototypeClimbStart >= 0) {
            String expected = index == 0 ? "UAL_ledge_climb" : "UAL_ledge_climb_ual2";
            int elapsed = age-prototypeClimbStart;
            if (clip.equals(expected) && elapsed >= 6) {
                prototypeClimbs.add(profile);
                capture(mc, profile+"-climb");
                if (index == 2 && elapsed == 8) {
                    climbTimeBeforeSwitch=PlayerRendererR3.controller().getAnimationTime();
                    profile(mc, "original");
                }
            }
            if (index == 2 && elapsed >= 10 && clip.equals("UAL_ledge_climb")) {
                switchedDuringClimb |= PlayerRendererR3.controller().getAnimationTime() > climbTimeBeforeSwitch;
            }
        }
    }

    private static void sampleRawAirYaw() {
        try {
            var type = com.towerofentropy.client.HorizontalLocomotion.class;
            var current = type.getDeclaredField("yaw");
            var previous = type.getDeclaredField("previousYaw");
            current.setAccessible(true);
            previous.setAccessible(true);
            rawAirYawStep = Math.max(rawAirYawStep, Math.abs(Mth.wrapDegrees(current.getFloat(null) - previous.getFloat(null))));
            rawAirSamples++;
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("C001.2 yaw probe", failure);
        }
    }

    private static void profile(Minecraft mc, String name) {
        mc.player.connection.sendCommand("toe animprofile "+name);
    }

    private static void capture(Minecraft mc, String name) {
        if (!prototypeCaptures.add(name)) return;
        var directory = java.nio.file.Path.of(System.getProperty("toe.a3.output")).toFile();
        directory.mkdirs();
        net.minecraft.client.Screenshot.grab(directory, name+".png", mc.getMainRenderTarget(), message -> {});
    }

    private static void setup(double z, boolean airborne, int top) {
        A3RuntimeTest.server(p -> {
            var level = p.serverLevel();
            for (int y = 100; y < 115; y++) level.setBlockAndUpdate(new BlockPos(0, y, 1),
                    (y < top ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            // Ceiling over the landing area of the obstructed test, above the side wall.
            level.setBlockAndUpdate(new BlockPos(0, 105, 1),
                    (top == 104 ? Blocks.STONE : top > 105 ? Blocks.STONE : Blocks.AIR).defaultBlockState());
            p.teleportTo(level, .5, airborne ? 106 : 100, z, 0, 0);
            p.setDeltaMovement(0, 0, 0);
        });
    }

    private static void check(String name, boolean pass, String detail) {
        LogUtils.getLogger().info("TOE_EXPANSION {} {} {}", name, pass, detail);
    }
}

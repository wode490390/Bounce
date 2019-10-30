package cn.wode490390.nukkit.bounce;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.entity.EntityDamageEvent.DamageCause;
import cn.nukkit.event.player.PlayerToggleSneakEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.PlayerActionPacket;
import cn.nukkit.network.protocol.ProtocolInfo;
import cn.nukkit.plugin.PluginBase;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import java.util.Set;

public class Bounce extends PluginBase implements Listener {

    private final Set<Long> ready = new LongArraySet();
    private final Set<Long> jumping = new LongArraySet();
    private double power = 1.572;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        String node = "bounce-power";
        try {
            this.power = this.getConfig().getDouble(node, this.power);
        } catch (Exception e) {
            this.logConfigException(node, e);
        }

        this.getServer().getPluginManager().registerEvents(this, this);

        try {
            new MetricsLite(this);
        } catch (Throwable ignore) {

        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (event.getCause() == DamageCause.FALL && this.jumping.remove(entity.getId())) {
            event.setCancelled();
        }
    }

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("bounce.use")) {
            long id = player.getId();
            if (event.isSneaking()) {
                this.ready.add(id);
            } else {
                this.ready.remove(id);
            }
        }
    }

    @EventHandler
    public void onDataPacketReceive(DataPacketReceiveEvent event) {
        DataPacket packet = event.getPacket();
        if (packet.pid() == ProtocolInfo.PLAYER_ACTION_PACKET && ((PlayerActionPacket) packet).action == PlayerActionPacket.ACTION_JUMP) {
            Player player = event.getPlayer();
            long id = player.getId();
            if (this.ready.contains(id) && player.isOnGround()) {
                player.setMotion(new Vector3(
                        -Math.sin(Math.toRadians(player.yaw)) * Math.cos(Math.toRadians(player.pitch)) * this.power,
                        -Math.sin(Math.toRadians(player.pitch)) * this.power,
                        Math.cos(Math.toRadians(player.yaw)) * Math.cos(Math.toRadians(player.pitch)) * this.power));
                this.jumping.add(id);
            }
        }
    }

    private void logConfigException(String node, Throwable t) {
        this.getLogger().alert("An error occurred while reading the configuration '" + node + "'. Use the default value.", t);
    }
}

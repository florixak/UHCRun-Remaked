package me.florixak.uhcrun.teams;

import me.florixak.uhcrun.UHCRun;
import me.florixak.uhcrun.config.ConfigType;
import me.florixak.uhcrun.game.GameManager;
import me.florixak.uhcrun.player.UHCPlayer;
import me.florixak.uhcrun.utils.TextUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TeamManager {

    private GameManager gameManager;
    private FileConfiguration config, teams_config;

    private int max_size;

    private List<UHCTeam> teams;

    public TeamManager(GameManager gameManager) {
        this.gameManager = gameManager;
        this.config = gameManager.getConfigManager().getFile(ConfigType.SETTINGS).getConfig();
        this.teams_config = gameManager.getConfigManager().getFile(ConfigType.TEAMS).getConfig();

        this.max_size = config.getInt("settings.teams.max-size");

        this.teams = new ArrayList<>();
    }

    public void loadTeams() {
        if (!gameManager.isTeamMode()) return;

        if (teams_config.getConfigurationSection("teams").getKeys(false) == null) {
            UHCRun.getInstance().getLogger().info("Team file is empty!");
            return;
        }

        for (String teamName : teams_config.getConfigurationSection("teams").getKeys(false)) {
            String color = teams_config.getString("teams." + teamName);
            UHCTeam team = new UHCTeam(teamName, color, max_size);
            this.teams.add(team);
        }
    }

    public List<UHCTeam> getTeams() {
        return this.teams;
    }

    public List<UHCTeam> getLivingTeams() {
        List<UHCTeam> teams = new ArrayList<>();
        for (UHCTeam team : getTeams()) {
            if (team.getLivingMembers().size() != 0) {
                teams.add(team);
            }
        }
        return teams;
    }

    public void addTeam(UHCTeam team) {
        if (exists(team.getName()) || team == null) return;

        List<String> teams_list = teams_config.getStringList("teams");
        teams_list.add(team.getName());

        this.teams.add(team);
        teams_config.set("teams", teams_list);
        gameManager.getConfigManager().getFile(ConfigType.TEAMS).save();
    }

    public void removeTeam(String teamName) {
        if (!exists(teamName) || teamName == null) return;

        List<String> teams_list = teams_config.getStringList("teams");
        teams_list.remove(teamName);

        UHCTeam team = getTeam(teamName);

        this.teams.remove(team);
        teams_config.set("teams", teams_list);
        gameManager.getConfigManager().getFile(ConfigType.TEAMS).save();
    }

    public UHCTeam getTeam(String name) {
        for (UHCTeam team : this.teams) {
            if (team.getName().equalsIgnoreCase(name)) {
                return team;
            }
        }
        return null;
    }

    public String getTeamsList() {
        StringBuilder teams = new StringBuilder();
        List<UHCTeam> teamsList = gameManager.getTeamManager().getTeams();

        for (int i = 0; i < teamsList.size(); i++) {
            teams.append(teamsList.get(i).getName());
            if (i < teamsList.size()-1) teams.append(", ");
        }
        return teams.toString();
    }

    private UHCTeam getFreeTeam() {
        UHCTeam emptyTeam = null;
        for (UHCTeam team : this.teams) {
            if (team.getMembers().size() == 0) {
                emptyTeam = team;
                return emptyTeam;
            }
        }
        if (emptyTeam == null) {
            for (UHCTeam team : this.teams) {
                if (!team.isFull()) {
                    emptyTeam = team;
                }
            }
        }
        return emptyTeam;
    }

    public void joinRandomTeam(UHCPlayer uhcPlayer) {
        if (uhcPlayer.hasTeam()) return;
        getFreeTeam().join(uhcPlayer);
    }

    public UHCTeam getLastTeam() {
        UHCTeam winner = getLivingTeams().get(0);
        for (UHCTeam team : getLivingTeams()) {
            for (UHCPlayer member : team.getMembers()) {
                if (member.isWinner()) {
                    winner = team;
                    break;
                }
            }
        }
        return winner;
    }

    public void teleportTeamsAfterMining() {
        Location location;

        for (UHCTeam team : getLivingTeams()) {
            Player p = team.getLivingMembers().get(0).getPlayer();

            World world = gameManager.getGameWorld();
            double x = p.getLocation().getX();
            double y = 150;
            double z = p.getLocation().getZ();

            location = new Location(world, x, y, z);
            y = location.getWorld().getHighestBlockYAt(location);
            location.setY(y);

            team.teleport(location);
        }
    }

    public boolean exists(String teamName) {
        return teams_config.getConfigurationSection("teams").getKeys(false).contains(teamName);
    }

    public void onDisable() {
        this.teams.clear();
    }

}
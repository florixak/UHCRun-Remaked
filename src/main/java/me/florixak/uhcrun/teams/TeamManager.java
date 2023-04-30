package me.florixak.uhcrun.teams;

import me.florixak.uhcrun.UHCRun;
import me.florixak.uhcrun.config.ConfigType;
import me.florixak.uhcrun.config.Messages;
import me.florixak.uhcrun.game.GameManager;
import me.florixak.uhcrun.player.UHCPlayer;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

public class TeamManager {

    private GameManager gameManager;
    private FileConfiguration config, teams_config;

    private List<UHCTeam> teams;

    public TeamManager(GameManager gameManager) {
        this.gameManager = gameManager;
        this.config = gameManager.getConfigManager().getFile(ConfigType.SETTINGS).getConfig();
        this.teams_config = gameManager.getConfigManager().getFile(ConfigType.TEAMS).getConfig();

        this.teams = new ArrayList<>();
    }

    public void loadTeams() {
        if (config.getBoolean("settings.teams.team-mode", false)) return;

        if (teams_config.getConfigurationSection("teams") == null) {
            UHCRun.getInstance().getLogger().info("No teams loaded!");
            return;
        }

        for (String teamName : teams_config.getConfigurationSection("teams").getKeys(false)) {

            UHCTeam team = new UHCTeam(teamName);
            team.setSize(config.getInt("settings.teams.max-size"));
            this.teams.add(team);
        }

        UHCRun.getInstance().getLogger().info("Teams loaded! (" + getTeamsList() + ")");
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

        this.teams.add(team);
        teams_config.set("teams." + team.getName() + ".display-name", team.getName());
        gameManager.getConfigManager().getFile(ConfigType.TEAMS).save();
    }

    public void removeTeam(String teamName) {
        if (!exists(teamName) || teamName == null) return;

        UHCTeam team = getTeam(teamName);

        this.teams.remove(team);
        teams_config.set("teams." + team.getName(), null);
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
        List<UHCTeam> teamList = gameManager.getTeamManager().getTeams();

        for (int i = 0; i < teamList.size(); i++) {
            teams.append(teamList.get(i).getName());
            if (i < teamList.size()-1) teams.append(", ");
        }

        return teams.toString().isEmpty() ? Messages.TEAM_NO_TEAMS.toString() : teams.toString();
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

    public UHCTeam getGameWinner() {
        UHCTeam winner = getLivingTeams().get(0);
        for (UHCTeam team : getLivingTeams()) {
            winner = team;
        }
        return winner;
    }

    public boolean exists(String teamName) {
        return teams_config.getConfigurationSection("teams." + teamName) != null;
    }

    public void onDisable() {
        this.teams.clear();
    }

}
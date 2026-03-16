class GameAchievement {
    private Game game;
    private gameAchievement <Achievement>;

    public GameAchievement(Game game, gameAchievement <Achievement>) {
        this.game = game;
        this.gameAchievement = gameAchievement;
    }

    public compareTo(GameAchievement other) {
        return this.gameAchievement.getAchievementId() - other.gameAchievement.getAchievementId();
    }

    public Game getGame() {
        return game;
    }
    
    public gameAchievement <Achievement> getGameAchievement() {
        return gameAchievement;
    }
}
package net.everla.everlaartifacts.common.difficulty;

import net.minecraft.world.Difficulty;

/**
 * 自定义难度等级枚举
 * 包含原版的简单、普通、困难以及自定义的月狂难度
 */
public enum DifficultyLevel {
    PEACEFUL(-1, "peaceful"), // 和平模式不使用纹理
    EASY(0, "easy"),          // 纹理Y=0 (简单)
    NORMAL(1, "normal"),      // 纹理Y=32 (普通)
    HARD(2, "hard"),          // 纹理Y=64 (困难)
    LUNATIC(3, "lunatic");   // 纹理Y=96 (月狂)
    
    private final int id;
    private final String name;
    
    DifficultyLevel(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public int getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * 从原版Difficulty转换为自定义DifficultyLevel
     */
    public static DifficultyLevel fromVanillaDifficulty(Difficulty difficulty) {
        switch (difficulty) {
            case PEACEFUL: return PEACEFUL;
            case EASY: return EASY;
            case NORMAL: return NORMAL;
            case HARD: return HARD;
            default: return NORMAL;
        }
    }
    
    /**
     * 转换为原版Difficulty（月狂难度转换为困难）
     */
    public Difficulty toVanillaDifficulty() {
        switch (this) {
            case PEACEFUL: return Difficulty.PEACEFUL;
            case EASY: return Difficulty.EASY;
            case NORMAL: return Difficulty.NORMAL;
            case HARD: return Difficulty.HARD;
            case LUNATIC: return Difficulty.HARD; // 月狂暂时映射到困难
            default: return Difficulty.NORMAL;
        }
    }
    
    /**
     * 获取纹理中的Y偏移量（每32像素一个难度）
     */
    public int getTextureYOffset() {
        return this.id * 32;
    }
    
    /**
     * 是否为有效可切换难度（排除和平模式）
     */
    public boolean isValidSwitchable() {
        return this != PEACEFUL;
    }
}
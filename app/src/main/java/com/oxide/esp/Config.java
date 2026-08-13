package com.oxide.esp;

public class Config {
    // ชื่อแพ็คเกจเกม
    public static final String GAME_PACKAGE = "com.oxide.game"; // เปลี่ยนตามเวอร์ชั่นเกม

    // ค่า Base Address (จะหาให้อัตโนมัติ)
    public static long baseAddress = 0;

    // ค่าออฟเซ็ตจาก dump (ต้องปรับให้ตรงกับเวอร์ชั่นเกม)
    public static final long OFFSET_PLAYER_LIST = 0x12345678; // ตัวอย่าง ต้องหาใหม่
    public static final long OFFSET_LOCAL_PLAYER = 0x87654321; // ตัวอย่าง
    public static final long OFFSET_WEAPON = 0xABCDEF01;
    public static final long OFFSET_VIEW_MATRIX = 0x11223344;
    public static final long OFFSET_CAMERA = 0x55667788;
    public static final long OFFSET_ENTITY_LIST = 0x99AABBCC;

    // ค่าออฟเซ็ตของ Player
    public static final long OFFSET_POSITION = 0x50;
    public static final long OFFSET_HEALTH = 0x60;
    public static final long OFFSET_NAME = 0x70;
    public static final long OFFSET_BONE = 0x88;
    public static final long OFFSET_TEAM = 0x90;
    public static final long OFFSET_IS_ALIVE = 0x98;

    // ค่าเริ่มต้น
    public static int FOV = 200;
    public static int SMOOTH = 50;
    public static boolean AIMBOT_HEAD = true;
    public static boolean AIMBOT_BODY = false;
    public static boolean ESP_ENABLED = false;
    public static boolean AIMBOT_ENABLED = false;
    public static boolean WALL_CHECK = true;

    // สี ESP
    public static final int ESP_COLOR_ENEMY = 0xFFFF0000;
    public static final int ESP_COLOR_TEAM = 0xFF00FF00;
    public static final int ESP_COLOR_BOX = 0xFFFFA500;
    public static final int ESP_COLOR_HEALTH = 0xFF00FF00;
    public static final int ESP_COLOR_LINE = 0xFFFF0000;
}

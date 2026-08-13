package com.oxide.esp;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class AimAssist {

    private static final String TAG = "AimAssist";
    private MemoryReader memoryReader;

    // ข้อมูลเป้าหมาย
    private static class Target {
        long address;
        float[] position;
        float[] headPosition;
        float health;
        String name;
        int team;
        float distance;
        float screenX, screenY;
        boolean isAlive;
    }

    private List<Target> targets = new ArrayList<>();

    public AimAssist(MemoryReader reader) {
        this.memoryReader = reader;
    }

    // หาเป้าหมายทั้งหมด
    public void updateTargets() {
        targets.clear();

        // ดึง Entity List จากหน่วยความจำ
        long entityList = Config.baseAddress + Config.OFFSET_ENTITY_LIST;
        int entityCount = memoryReader.readInt(entityList);

        for (int i = 0; i < entityCount; i++) {
            long entityAddress = memoryReader.readLong(entityList + 8 + (i * 8));
            if (entityAddress == 0) continue;

            Target target = new Target();
            target.address = entityAddress;

            // อ่านตำแหน่ง
            target.position = memoryReader.readVector3(entityAddress + Config.OFFSET_POSITION);

            // อ่านพลังชีวิต
            target.health = memoryReader.readFloat(entityAddress + Config.OFFSET_HEALTH);
            if (target.health <= 0) continue;

            // อ่านชื่อ
            target.name = memoryReader.readString(entityAddress + Config.OFFSET_NAME, 32);

            // อ่านทีม
            target.team = memoryReader.readInt(entityAddress + Config.OFFSET_TEAM);

            // อ่านสถานะ
            target.isAlive = memoryReader.readInt(entityAddress + Config.OFFSET_IS_ALIVE) == 1;

            // อ่านตำแหน่งหัว (Bone)
            target.headPosition = readBonePosition(entityAddress, 6); // Bone 6 = Head

            targets.add(target);
        }

        // คำนวณระยะทาง
        float[] localPos = getLocalPlayerPosition();
        for (Target t : targets) {
            float dx = t.position[0] - localPos[0];
            float dy = t.position[1] - localPos[1];
            float dz = t.position[2] - localPos[2];
            t.distance = (float) Math.sqrt(dx*dx + dy*dy + dz*dz);

            // แปลงเป็นหน้าจอ
            float[] screen = worldToScreen(t.headPosition);
            if (screen != null) {
                t.screenX = screen[0];
                t.screenY = screen[1];
            }
        }
    }

    private float[] getLocalPlayerPosition() {
        long localPlayer = memoryReader.readLong(Config.baseAddress + Config.OFFSET_LOCAL_PLAYER);
        if (localPlayer == 0) return new float[]{0, 0, 0};
        return memoryReader.readVector3(localPlayer + Config.OFFSET_POSITION);
    }

    private float[] readBonePosition(long entityAddress, int boneIndex) {
        // Bone offset = OFFSET_BONE + (boneIndex * 48) // ขนาดของ Bone struct
        long boneAddress = entityAddress + Config.OFFSET_BONE + (boneIndex * 48);
        return memoryReader.readVector3(boneAddress);
    }

    private float[] worldToScreen(float[] worldPos) {
        // ใช้ View Matrix แปลง World Position เป็น Screen Position
        // ต้องอ่าน ViewMatrix + ProjectionMatrix จากหน่วยความจำ
        long viewMatrix = memoryReader.readLong(Config.baseAddress + Config.OFFSET_VIEW_MATRIX);
        if (viewMatrix == 0) return null;

        // TODO: คำนวณ Matrix Multiplication จริงๆ
        // float[] screen = multiplyMatrix(worldPos, viewMatrix);
        // screenX = screen[0] / screen[3];
        // screenY = screen[1] / screen[3];

        // ตัวอย่าง (ต้องแก้ให้ถูกต้อง)
        float screenX = 540; // ค่ากลางหน้าจอ
        float screenY = 960;
        return new float[]{screenX, screenY};
    }

    // หาเป้าหมายที่ดีที่สุด
    public Target getBestTarget() {
        if (targets.isEmpty()) return null;

        Target best = null;
        float bestScore = Float.MAX_VALUE;

        for (Target t : targets) {
            if (!t.isAlive || t.health <= 0) continue;
            if (t.distance > 500) continue; // ระยะไกลเกินไป

            // ตรวจสอบ Wall Check (ถ้าเปิด)
            if (Config.WALL_CHECK && !hasLineOfSight(t)) {
                continue;
            }

            float score = t.distance;
            if (Config.AIMBOT_HEAD) {
                // ถ้าเล็งหัว ให้น้ำหนักกับเป้าหมายที่ใกล้ center ของ FOV มากกว่า
                score += getFOVDistance(t) * 0.5f;
            }

            if (score < bestScore) {
                bestScore = score;
                best = t;
            }
        }

        return best;
    }

    private float getFOVDistance(Target t) {
        // คำนวณระยะห่างจาก Center ของ FOV
        float centerX = 540; // ครึ่งหน้าจอ
        float centerY = 960;
        float dx = t.screenX - centerX;
        float dy = t.screenY - centerY;
        return (float) Math.sqrt(dx*dx + dy*dy);
    }

    private boolean hasLineOfSight(Target target) {
        // ใช้ Unity Raycast ตรวจสอบว่ามีสิ่งกีดขวางหรือไม่
        // ใน Android ต้อง Hook เข้า Raycast ของ Unity
        // หรือจำลองด้วยการตรวจสอบระยะห่างและมุม
        return true;
    }

    // คำนวนค่าการเลื่อน Aim
    public float[] calculateAimSmooth(Target target, float deltaTime) {
        if (target == null) return null;

        float targetX = target.screenX;
        float targetY = target.screenY;

        float centerX = 540;
        float centerY = 960;

        float deltaX = targetX - centerX;
        float deltaY = targetY - centerY;

        float smoothFactor = Config.SMOOTH / 100.0f;
        float maxDelta = 10.0f * (1 + smoothFactor);

        if (Math.abs(deltaX) > maxDelta) {
            deltaX = (deltaX > 0) ? maxDelta : -maxDelta;
        }
        if (Math.abs(deltaY) > maxDelta) {
            deltaY = (deltaY > 0) ? maxDelta : -maxDelta;
        }

        // ใช้ Smoothing
        deltaX *= smoothFactor * 0.5f;
        deltaY *= smoothFactor * 0.5f;

        return new float[]{deltaX, deltaY};
    }

    public List<Target> getTargets() {
        return targets;
    }

    public int getTargetCount() {
        return targets.size();
    }
}

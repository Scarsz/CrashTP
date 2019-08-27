package github.scarsz.crashtp.common.util;

import org.jnbt.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class NBTUtil {

    public static void setPlayerCoordinates(File playerDat, UUID worldUuid, int environment, double x, double y, double z, float yaw, float pitch) throws IOException {
        CompoundTag tag = load(playerDat);
        Map<String, Tag> tagMap = new HashMap<>(Objects.requireNonNull(tag).getValue());

        tagMap.put("Pos", new ListTag(
                "Pos",
                DoubleTag.class,
                new LinkedList<Tag>() {{
                    add(new DoubleTag("", x));
                    add(new DoubleTag("", y));
                    add(new DoubleTag("", z));
                }}
        ));
        tagMap.put("Rotation", new ListTag(
                "Rotation",
                FloatTag.class,
                new LinkedList<Tag>() {{
                    add(new FloatTag("", yaw));
                    add(new FloatTag("", pitch));
                }}
        ));
        tagMap.put("Dimension", new IntTag("Dimension", environment));
        tagMap.put("WorldUUIDLeast", new LongTag("WorldUUIDLeast", worldUuid.getLeastSignificantBits()));
        tagMap.put("WorldUUIDMost", new LongTag("WorldUUIDMost", worldUuid.getMostSignificantBits()));

        tag = new CompoundTag(tag.getName(), tagMap); // construct new tag, applying our changes
        save(playerDat, tag);
    }

    private static CompoundTag load(File file) throws IOException {
        try (FileInputStream fileStream = new FileInputStream(file)) {
            try (NBTInputStream nbtStream = new NBTInputStream(fileStream)) {
                Tag t = nbtStream.readTag();
                return t instanceof CompoundTag ? (CompoundTag) t : null;
            }
        }
    }

    private static void save(File file, CompoundTag tag) throws IOException {
        try (FileOutputStream fileStream = new FileOutputStream(file)) {
            try (NBTOutputStream nbtStream = new NBTOutputStream(fileStream)) {
                nbtStream.writeTag(tag);
            }
        }
    }

}

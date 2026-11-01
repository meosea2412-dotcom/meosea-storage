package com.meosea.meosea_storage.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StorageImplTest {

    @Test
    void insertThenExtract() {
        final Storage storage = new StorageImpl();
        final ItemKey diamond = new ItemKey("minecraft:diamond");

        final long inserted = storage.insert(diamond, 64, Action.EXECUTE);
        assertEquals(64, inserted);
        assertEquals(64, storage.getAmount(diamond));
        assertEquals(64, storage.getStored());

        final long extracted = storage.extract(diamond, 10, Action.EXECUTE);
        assertEquals(10, extracted);
        assertEquals(54, storage.getAmount(diamond));
        assertEquals(54, storage.getStored());
    }

    @Test
    void extractMoreThanAvailableReturnsOnlyWhatExists() {
        final Storage storage = new StorageImpl();
        final ItemKey stick = new ItemKey("minecraft:stick");
        storage.insert(stick, 5, Action.EXECUTE);

        final long extracted = storage.extract(stick, 100, Action.EXECUTE);
        assertEquals(5, extracted);
        assertEquals(0, storage.getAmount(stick));
        assertEquals(0, storage.getStored());
    }

    @Test
    void extractNonExistentKeyReturnsZero() {
        final Storage storage = new StorageImpl();
        final ItemKey ghost = new ItemKey("minecraft:ghost");
        assertEquals(0, storage.extract(ghost, 1, Action.EXECUTE));
    }

    @Test
    void simulateDoesNotMutateState() {
        final Storage storage = new StorageImpl();
        final ItemKey stone = new ItemKey("minecraft:stone");
        storage.insert(stone, 10, Action.EXECUTE);

        final long simulatedInsert = storage.insert(stone, 5, Action.SIMULATE);
        assertEquals(5, simulatedInsert);
        assertEquals(10, storage.getAmount(stone));

        final long simulatedExtract = storage.extract(stone, 100, Action.SIMULATE);
        assertEquals(10, simulatedExtract);
        assertEquals(10, storage.getAmount(stone));
    }

    @Test
    void differentMetadataAreDifferentKeys() {
        final Storage storage = new StorageImpl();
        final ItemKey plain = new ItemKey("minecraft:diamond_sword");
        final ItemKey enchanted = new ItemKey("minecraft:diamond_sword", "{Enchantments:[...]}");

        storage.insert(plain, 1, Action.EXECUTE);
        storage.insert(enchanted, 1, Action.EXECUTE);

        assertEquals(1, storage.getAmount(plain));
        assertEquals(1, storage.getAmount(enchanted));
        assertEquals(2, storage.getStored());
    }
}

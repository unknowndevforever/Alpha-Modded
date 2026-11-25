import java.util.HashMap;
import java.util.Map;

/** Display names for the creative tooltips (Alpha 1.1.2 has no item names of its own). */
public class Names {
    private static final Map<Integer, String> N = new HashMap<Integer, String>();
    private static void put(int id, String n) { N.put(id, n); }

    static {
        put(1, "Stone"); put(2, "Grass Block"); put(3, "Dirt"); put(4, "Cobblestone"); put(5, "Wooden Planks");
        put(6, "Sapling"); put(7, "Bedrock"); put(12, "Sand"); put(13, "Gravel"); put(14, "Gold Ore");
        put(15, "Iron Ore"); put(16, "Coal Ore"); put(17, "Wood"); put(18, "Leaves"); put(19, "Sponge");
        put(20, "Glass"); put(27, "Powered Rail"); put(35, "Cloth"); put(37, "Flower"); put(38, "Rose");
        put(39, "Brown Mushroom"); put(40, "Red Mushroom"); put(41, "Block of Gold"); put(42, "Block of Iron");
        put(43, "Double Stone Slab"); put(44, "Stone Slab"); put(45, "Bricks"); put(46, "TNT"); put(47, "Bookshelf");
        put(48, "Mossy Cobblestone"); put(49, "Obsidian"); put(50, "Torch"); put(52, "Monster Spawner");
        put(53, "Wooden Stairs"); put(54, "Chest"); put(56, "Diamond Ore"); put(57, "Block of Diamond");
        put(58, "Workbench"); put(61, "Furnace"); put(65, "Ladder"); put(66, "Rail"); put(67, "Cobblestone Stairs");
        put(69, "Lever"); put(70, "Stone Pressure Plate"); put(72, "Wooden Pressure Plate"); put(73, "Redstone Ore");
        put(76, "Redstone Torch"); put(77, "Button"); put(78, "Snow"); put(79, "Ice"); put(80, "Snow Block");
        put(81, "Cactus"); put(82, "Clay Block"); put(84, "Jukebox"); put(85, "Fence");

        put(256, "Iron Shovel"); put(257, "Iron Pickaxe"); put(258, "Iron Axe"); put(259, "Flint and Steel");
        put(260, "Apple"); put(261, "Bow"); put(262, "Arrow"); put(263, "Coal"); put(264, "Diamond");
        put(265, "Iron Ingot"); put(266, "Gold Ingot"); put(267, "Iron Sword"); put(268, "Wooden Sword");
        put(269, "Wooden Shovel"); put(270, "Wooden Pickaxe"); put(271, "Wooden Axe"); put(272, "Stone Sword");
        put(273, "Stone Shovel"); put(274, "Stone Pickaxe"); put(275, "Stone Axe"); put(276, "Diamond Sword");
        put(277, "Diamond Shovel"); put(278, "Diamond Pickaxe"); put(279, "Diamond Axe"); put(280, "Stick");
        put(281, "Bowl"); put(282, "Mushroom Stew"); put(283, "Gold Sword"); put(284, "Gold Shovel");
        put(285, "Gold Pickaxe"); put(286, "Gold Axe"); put(287, "String"); put(288, "Feather"); put(289, "Gunpowder");
        put(290, "Wooden Hoe"); put(291, "Stone Hoe"); put(292, "Iron Hoe"); put(293, "Diamond Hoe"); put(294, "Gold Hoe");
        put(295, "Seeds"); put(296, "Wheat"); put(297, "Bread");
        put(298, "Leather Cap"); put(299, "Leather Tunic"); put(300, "Leather Pants"); put(301, "Leather Boots");
        put(302, "Chain Helmet"); put(303, "Chain Chestplate"); put(304, "Chain Leggings"); put(305, "Chain Boots");
        put(306, "Iron Helmet"); put(307, "Iron Chestplate"); put(308, "Iron Leggings"); put(309, "Iron Boots");
        put(310, "Diamond Helmet"); put(311, "Diamond Chestplate"); put(312, "Diamond Leggings"); put(313, "Diamond Boots");
        put(314, "Gold Helmet"); put(315, "Gold Chestplate"); put(316, "Gold Leggings"); put(317, "Gold Boots");
        put(318, "Flint"); put(319, "Raw Porkchop"); put(320, "Cooked Porkchop"); put(321, "Painting");
        put(322, "Golden Apple"); put(323, "Sign"); put(324, "Wooden Door"); put(325, "Bucket");
        put(326, "Water Bucket"); put(327, "Lava Bucket"); put(328, "Minecart"); put(329, "Saddle");
        put(330, "Iron Door"); put(331, "Redstone"); put(332, "Snowball"); put(333, "Boat"); put(334, "Leather");
        put(335, "Milk Bucket"); put(336, "Brick"); put(337, "Clay"); put(338, "Reeds"); put(339, "Paper");
        put(340, "Book"); put(341, "Slimeball"); put(342, "Minecart with Chest"); put(343, "Minecart with Furnace");
        put(344, "Egg"); put(345, "Compass"); put(346, "Fishing Rod");
        put(2256, "Music Disc (13)"); put(2257, "Music Disc (cat)");
    }

    public static String of(int id) {
        String n = N.get(id);
        return n != null ? n : (id < 256 ? "Block " : "Item ") + id;
    }
}

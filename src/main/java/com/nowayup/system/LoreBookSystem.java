package com.nowayup.system;

import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class LoreBookSystem {
    private LoreBookSystem() {
    }

    public static void addStartingLore(Container container) {
        addLoreBook(container, 0);
    }

    public static void addLoreBook(Container container, int loreIndex) {
        List<ItemStack> books = createLoreBooks();
        if (loreIndex >= 0 && loreIndex < books.size() && container.getContainerSize() > 0) {
            container.setItem(0, books.get(loreIndex));
        }
    }

    public static void addMessageBook(Container container, int messageIndex) {
        List<ItemStack> books = createMessageBooks();
        if (messageIndex >= 0 && messageIndex < books.size() && container.getContainerSize() > 0) {
            container.setItem(0, books.get(messageIndex));
        }
    }

    public static void giveLoreBooks(Player player) {
        for (ItemStack book : createLoreBooks()) {
            if (!player.getInventory().add(book)) {
                player.drop(book, false);
            }
        }
    }

    private static List<ItemStack> createLoreBooks() {
        return List.of(
            book(
                "Shaft 0 Survey",
                "nowayup.book.shaft0.title",
                "Elias Ward",
                List.of(
                    "nowayup.book.shaft0.page1",
                    "nowayup.book.shaft0.page2"
                )
            ),
            book(
                "The Upward Grave",
                "nowayup.book.upward_grave.title",
                "Unknown Worker",
                List.of(
                    "nowayup.book.upward_grave.page1",
                    "nowayup.book.upward_grave.page2"
                )
            ),
            book(
                "Mirror Note",
                "nowayup.book.mirror_note.title",
                "Survey Team 3",
                List.of(
                    "nowayup.book.mirror_note.page1",
                    "nowayup.book.mirror_note.page2"
                )
            ),
            book(
                "Elias Ward's Journal",
                "nowayup.book.elias_journal.title",
                "Elias Ward",
                List.of(
                    "nowayup.book.elias_journal.page1",
                    "nowayup.book.elias_journal.page2"
                )
            )
        );
    }

    private static List<ItemStack> createMessageBooks() {
        return List.of(
            book(
                "A Note With Your Name",
                "nowayup.book.name_note.title",
                "???",
                List.of(
                    "nowayup.book.name_note.page1",
                    "nowayup.book.name_note.page2"
                )
            ),
            book(
                "Do Not Open It",
                "nowayup.book.do_not_open.title",
                "???",
                List.of(
                    "nowayup.book.do_not_open.page1",
                    "nowayup.book.do_not_open.page2"
                )
            )
        );
    }

    private static ItemStack book(String title, String titleKey, String author, List<String> pageKeys) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        stack.setHoverName(Component.translatable(titleKey));
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", title);
        tag.putString("author", author);
        tag.putBoolean("resolved", true);

        ListTag pageTags = new ListTag();
        for (String pageKey : pageKeys) {
            pageTags.add(StringTag.valueOf(Component.Serializer.toJson(Component.translatable(pageKey))));
        }
        tag.put("pages", pageTags);
        return stack;
    }
}

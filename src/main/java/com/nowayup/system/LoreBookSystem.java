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
                "Elias Ward",
                List.of(
                    """
                    Shaft 0 cannot be mapped.

                    Routes alter after observation.

                    Do not leave markers you are not willing to see again.
                    """,
                    """
                    Every tunnel we marked as rising has led us lower.

                    The foreman says the instruments are broken.

                    All six of them?
                    """
                )
            ),
            book(
                "The Upward Grave",
                "Unknown Worker",
                List.of(
                    """
                    DO NOT CLIMB.

                    THE MINE LEARNED THE SKY.
                    """,
                    """
                    If the tunnel rises, turn around.

                    If the door is open, do not close it.

                    If you hear your name, do not answer.
                    """
                )
            ),
            book(
                "Mirror Note",
                "Survey Team 3",
                List.of(
                    """
                    There are no monsters here.

                    That means it already ate them.
                    """,
                    """
                    Do not climb this time.

                    The exit hates the light.

                    Your shadow left first.
                    """
                )
            ),
            book(
                "Elias Ward's Journal",
                "Elias Ward",
                List.of(
                    """
                    Day 14

                    We climbed for three hours today.

                    When we stopped, the depth gauge said we were lower than breakfast.
                    """,
                    """
                    I saw myself at the end of the rail line.

                    It did not move.

                    It was waiting where I will be.
                    """
                )
            )
        );
    }

    private static List<ItemStack> createMessageBooks() {
        return List.of(
            book(
                "A Note With Your Name",
                "???",
                List.of(
                    """
                    Why did you come back?

                    You were closer when you stopped trying to leave.
                    """,
                    """
                    The tunnel remembers your footsteps.

                    It puts them ahead of you.
                    """
                )
            ),
            book(
                "Do Not Open It",
                "???",
                List.of(
                    """
                    Do not open the door.

                    It is already open.
                    """,
                    """
                    If you see yourself, do not answer.

                    If you answer, it will know which voice to keep.
                    """
                )
            )
        );
    }

    private static ItemStack book(String title, String author, List<String> pages) {
        ItemStack stack = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("title", title);
        tag.putString("author", author);
        tag.putBoolean("resolved", true);

        ListTag pageTags = new ListTag();
        for (String page : pages) {
            pageTags.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(page.strip()))));
        }
        tag.put("pages", pageTags);
        return stack;
    }
}

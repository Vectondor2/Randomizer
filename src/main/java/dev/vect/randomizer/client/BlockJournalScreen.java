package dev.vect.randomizer.client;

import dev.vect.randomizer.network.BlockJournalPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class BlockJournalScreen
        extends Screen {

    private static final int ROW_HEIGHT =
            24;

    private static final int BACKGROUND =
            0xF20B0E14;

    private static final int PANEL =
            0xF2181D29;

    private static final int SECONDARY =
            0xF211151E;

    private static final int ROW =
            0xD9222937;

    private static final int SELECTED =
            0xFF38445C;

    private static final int ACCENT =
            0xFF72D6FF;

    private static final int TEXT =
            0xFFF2F3F7;

    private static final int MUTED =
            0xFFA8AFBD;

    private final BlockJournalPayload payload;

    private final List<
            BlockJournalPayload.Entry
            > allEntries;

    private List<
            BlockJournalPayload.Entry
            > filteredEntries;

    private BlockJournalPayload.Entry
            selected;

    private EditBox searchBox;

    private int firstVisibleRow;

    public BlockJournalScreen(
            BlockJournalPayload payload
    ) {
        super(
                Component.literal(
                        "Журнал блоков Randomizer"
                )
        );

        this.payload =
                payload;

        this.allEntries =
                List.copyOf(
                        payload.entries()
                );

        this.filteredEntries =
                this.allEntries;
    }

    @Override
    protected void init() {
        searchBox =
                new EditBox(
                        this.font,
                        left() + 12,
                        top() + 38,
                        listWidth() - 24,
                        20,
                        Component.literal(
                                "Поиск блока"
                        )
                );

        searchBox.setSuggestion(
                "Поиск..."
        );

        searchBox.setResponder(
                this::filter
        );

        addRenderableWidget(
                searchBox
        );

        filter("");
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        graphics.fill(
                0,
                0,
                width,
                height,
                BACKGROUND
        );

        graphics.fill(
                left(),
                top(),
                right(),
                bottom(),
                PANEL
        );

        graphics.fill(
                left(),
                top(),
                right(),
                top() + 2,
                ACCENT
        );

        graphics.fill(
                left(),
                top(),
                left() + listWidth(),
                bottom(),
                SECONDARY
        );

        graphics.fill(
                left() + listWidth(),
                top(),
                left() + listWidth() + 1,
                bottom(),
                0xFF303747
        );

        graphics.drawCenteredString(
                font,
                Component.literal(
                        "ЖУРНАЛ БЛОКОВ RANDOMIZER"
                ),
                (left() + right()) / 2,
                top() + 12,
                TEXT
        );

        graphics.drawString(
                font,
                Component.literal(
                        "Открыто блоков: "
                                + allEntries.size()
                ),
                left()
                        + listWidth()
                        + 18,
                top() + 42,
                MUTED
        );

        renderList(
                graphics
        );

        renderDetails(
                graphics
        );

        renderScrollbar(
                graphics
        );

        super.render(
                graphics,
                mouseX,
                mouseY,
                partialTick
        );
    }

    private void renderList(
            GuiGraphics graphics
    ) {
        int visible =
                visibleRows();

        int end =
                Math.min(
                        filteredEntries.size(),
                        firstVisibleRow
                                + visible
                );

        for (int index =
                firstVisibleRow;
             index < end;
             index++) {

            BlockJournalPayload.Entry entry =
                    filteredEntries.get(
                            index
                    );

            int y =
                    listTop()
                            + (
                            index
                                    - firstVisibleRow
                    )
                            * ROW_HEIGHT;

            graphics.fill(
                    left() + 8,
                    y,
                    left()
                            + listWidth()
                            - 8,
                    y
                            + ROW_HEIGHT
                            - 2,
                    entry == selected
                            ? SELECTED
                            : ROW
            );

            ItemStack icon =
                    blockIcon(
                            entry.blockId()
                    );

            if (!icon.isEmpty()) {
                graphics.renderItem(
                        icon,
                        left() + 12,
                        y + 3
                );
            }

            String name =
                    trim(
                            blockName(
                                    entry.blockId()
                            ),
                            listWidth() - 60
                    );

            graphics.drawString(
                    font,
                    Component.literal(
                            name
                    ),
                    left() + 34,
                    y + 8,
                    TEXT
            );
        }

        if (filteredEntries.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.literal(
                            allEntries.isEmpty()
                                    ? "Пока ничего не открыто"
                                    : "Ничего не найдено"
                    ),
                    left()
                            + listWidth() / 2,
                    listTop() + 25,
                    MUTED
            );
        }
    }

    private void renderDetails(
            GuiGraphics graphics
    ) {
        int x =
                left()
                        + listWidth()
                        + 24;

        int y =
                top() + 80;

        if (selected == null) {
            graphics.drawString(
                    font,
                    Component.literal(
                            "Выберите открытый блок."
                    ),
                    x,
                    y,
                    MUTED
            );

            return;
        }

        String blockName =
                blockName(
                        selected.blockId()
                );

        String itemName =
                itemName(
                        selected.itemId()
                );

        ItemStack sourceIcon =
                blockIcon(
                        selected.blockId()
                );

        ItemStack targetIcon =
                itemIcon(
                        selected.itemId()
                );

        if (!sourceIcon.isEmpty()) {
            graphics.renderItem(
                    sourceIcon,
                    x,
                    y
            );
        }

        graphics.drawString(
                font,
                Component.literal(
                        blockName
                ),
                x + 24,
                y + 5,
                TEXT
        );

        y += 38;

        graphics.drawString(
                font,
                Component.literal(
                        "Рандомизированный дроп"
                ),
                x,
                y,
                MUTED
        );

        y += 20;

        graphics.drawString(
                font,
                Component.literal(
                        "↓"
                ),
                x + 3,
                y + 5,
                ACCENT
        );

        y += 25;

        if (!targetIcon.isEmpty()) {
            graphics.renderItem(
                    targetIcon,
                    x,
                    y
            );
        }

        graphics.drawString(
                font,
                Component.literal(
                        itemName
                ),
                x + 24,
                y + 5,
                ACCENT
        );

        y += 46;

        graphics.drawString(
                font,
                Component.literal(
                        "Block ID:"
                ),
                x,
                y,
                MUTED
        );

        graphics.drawString(
                font,
                Component.literal(
                        selected.blockId()
                ),
                x + 65,
                y,
                0xFF7F8795
        );

        y += 16;

        graphics.drawString(
                font,
                Component.literal(
                        "Item ID:"
                ),
                x,
                y,
                MUTED
        );

        graphics.drawString(
                font,
                Component.literal(
                        selected.itemId()
                ),
                x + 65,
                y,
                0xFF7F8795
        );

        graphics.drawString(
                font,
                Component.literal(
                        "Randomizer Seed: "
                                + payload.masterSeed()
                ),
                x,
                bottom() - 24,
                0xFF606877
        );
    }

    private void filter(
            String rawQuery
    ) {
        String query =
                rawQuery
                        .strip()
                        .toLowerCase(
                                Locale.ROOT
                        );

        filteredEntries =
                allEntries.stream()
                        .filter(entry -> {
                            String block =
                                    blockName(
                                            entry.blockId()
                                    )
                                            .toLowerCase(
                                                    Locale.ROOT
                                            );

                            String item =
                                    itemName(
                                            entry.itemId()
                                    )
                                            .toLowerCase(
                                                    Locale.ROOT
                                            );

                            return query.isEmpty()
                                    || block.contains(
                                    query
                            )
                                    || item.contains(
                                    query
                            )
                                    || entry.blockId()
                                    .toLowerCase(
                                            Locale.ROOT
                                    )
                                    .contains(
                                            query
                                    );
                        })
                        .sorted(
                                Comparator.comparing(
                                        entry ->
                                                blockName(
                                                        entry.blockId()
                                                ),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                        )
                        .toList();

        firstVisibleRow = 0;

        if (selected == null
                || !filteredEntries
                .contains(selected)) {

            selected =
                    filteredEntries.isEmpty()
                            ? null
                            : filteredEntries
                            .getFirst();
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (super.mouseClicked(
                mouseX,
                mouseY,
                button
        )) {
            return true;
        }

        if (button != 0
                || !insideList(
                mouseX,
                mouseY
        )) {

            return false;
        }

        int row =
                (
                        (int) mouseY
                                - listTop()
                )
                        / ROW_HEIGHT;

        int index =
                firstVisibleRow + row;

        if (index >= 0
                && index
                < filteredEntries.size()) {

            selected =
                    filteredEntries.get(
                            index
                    );

            return true;
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY
    ) {
        if (!insideList(
                mouseX,
                mouseY
        )) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    scrollX,
                    scrollY
            );
        }

        int maximum =
                Math.max(
                        0,
                        filteredEntries.size()
                                - visibleRows()
                );

        if (scrollY > 0) {
            firstVisibleRow =
                    Math.max(
                            0,
                            firstVisibleRow - 1
                    );

        } else if (scrollY < 0) {
            firstVisibleRow =
                    Math.min(
                            maximum,
                            firstVisibleRow + 1
                    );
        }

        return true;
    }

    private void renderScrollbar(
            GuiGraphics graphics
    ) {
        int visible =
                visibleRows();

        if (filteredEntries.size()
                <= visible) {
            return;
        }

        int trackTop =
                listTop();

        int trackBottom =
                bottom() - 10;

        int trackHeight =
                trackBottom
                        - trackTop;

        int x =
                left()
                        + listWidth()
                        - 5;

        graphics.fill(
                x,
                trackTop,
                x + 2,
                trackBottom,
                0xFF303747
        );

        int thumbHeight =
                Math.max(
                        16,
                        Math.round(
                                trackHeight
                                        * (
                                        (float) visible
                                                / filteredEntries
                                                .size()
                                )
                        )
                );

        int maxScroll =
                filteredEntries.size()
                        - visible;

        float position =
                maxScroll <= 0
                        ? 0
                        : (float) firstVisibleRow
                        / maxScroll;

        int thumbY =
                trackTop
                        + Math.round(
                        (
                                trackHeight
                                        - thumbHeight
                        )
                                * position
                );

        graphics.fill(
                x - 1,
                thumbY,
                x + 3,
                thumbY + thumbHeight,
                ACCENT
        );
    }

    private boolean insideList(
            double mouseX,
            double mouseY
    ) {
        return mouseX
                >= left() + 8
                && mouseX
                <= left()
                + listWidth()
                - 8
                && mouseY
                >= listTop()
                && mouseY
                <= bottom() - 10;
    }

    private int visibleRows() {
        return Math.max(
                1,
                (
                        bottom()
                                - 10
                                - listTop()
                )
                        / ROW_HEIGHT
        );
    }

    private String blockName(
            String rawId
    ) {
        ResourceLocation id =
                ResourceLocation
                        .tryParse(rawId);

        if (id == null) {
            return rawId;
        }

        Block block =
                BuiltInRegistries.BLOCK
                        .get(id);

        if (block == null) {
            return rawId;
        }

        return block.getName()
                .getString();
    }

    private String itemName(
            String rawId
    ) {
        ResourceLocation id =
                ResourceLocation
                        .tryParse(rawId);

        if (id == null) {
            return rawId;
        }

        Item item =
                BuiltInRegistries.ITEM
                        .get(id);

        if (item == null) {
            return rawId;
        }

        return item.getDescription()
                .getString();
    }

    private ItemStack blockIcon(
            String rawId
    ) {
        ResourceLocation id =
                ResourceLocation
                        .tryParse(rawId);

        if (id == null) {
            return ItemStack.EMPTY;
        }

        Block block =
                BuiltInRegistries.BLOCK
                        .get(id);

        if (block == null) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(
                block.asItem()
        );
    }

    private ItemStack itemIcon(
            String rawId
    ) {
        ResourceLocation id =
                ResourceLocation
                        .tryParse(rawId);

        if (id == null) {
            return ItemStack.EMPTY;
        }

        Item item =
                BuiltInRegistries.ITEM
                        .get(id);

        if (item == null) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }

    private String trim(
            String value,
            int maximumWidth
    ) {
        if (font.width(value)
                <= maximumWidth) {

            return value;
        }

        String result =
                value;

        while (result.length() > 1
                && font.width(
                result + "…"
        ) > maximumWidth) {

            result =
                    result.substring(
                            0,
                            result.length() - 1
                    );
        }

        return result + "…";
    }

    private int windowWidth() {
        return Math.max(
                360,
                Math.min(
                        850,
                        width - 24
                )
        );
    }

    private int left() {
        return (
                width - windowWidth()
        ) / 2;
    }

    private int right() {
        return left()
                + windowWidth();
    }

    private int top() {
        return 16;
    }

    private int bottom() {
        return height - 16;
    }

    private int listWidth() {
        return Math.min(
                300,
                Math.max(
                        210,
                        windowWidth() / 3
                )
        );
    }

    private int listTop() {
        return top() + 68;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
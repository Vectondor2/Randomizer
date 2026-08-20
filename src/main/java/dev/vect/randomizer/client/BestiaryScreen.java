package dev.vect.randomizer.client;

import dev.vect.randomizer.network.BestiaryPayload;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class BestiaryScreen extends Screen {

    private static final int ROW_HEIGHT = 23;

    private static final int BACKGROUND = 0xF20B0E14;
    private static final int PANEL = 0xF2181D29;
    private static final int LEFT_PANEL = 0xF211151E;
    private static final int ROW = 0xD9222937;
    private static final int ROW_UNKNOWN = 0xD9191E28;
    private static final int SELECTED = 0xFF38445C;
    private static final int ACCENT = 0xFF8E79FF;
    private static final int TEXT = 0xFFF2F3F7;
    private static final int MUTED = 0xFFA8AFBD;
    private static final int SUCCESS = 0xFF79E08A;
    private static final int UNKNOWN = 0xFF8B919E;

    private final BestiaryPayload payload;
    private final List<BestiaryPayload.Entry> allEntries;

    private List<BestiaryPayload.Entry> filteredEntries;
    private BestiaryPayload.Entry selected;
    private EditBox searchBox;
    private int firstVisibleRow;

    public BestiaryScreen(
            BestiaryPayload payload
    ) {
        super(Component.literal("Бестиарий Randomizer"));
        this.payload = payload;
        this.allEntries = List.copyOf(payload.entries());
        this.filteredEntries = this.allEntries;
    }

    @Override
    protected void init() {
        searchBox = new EditBox(
                font,
                left() + 12,
                top() + 36,
                listWidth() - 24,
                20,
                Component.literal("Поиск моба")
        );

        searchBox.setSuggestion("Поиск моба...");
        searchBox.setResponder(this::filter);
        addRenderableWidget(searchBox);

        filter("");
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        /*
         * Стандартный Screen render выполняется первым.
         * Это не даёт ATM10/FancyMenu blur накрыть наш текст,
         * список и детали. SearchBox повторно рисуется поверх GUI.
         */
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.fill(0, 0, width, height, BACKGROUND);
        graphics.fill(left(), top(), right(), bottom(), PANEL);
        graphics.fill(left(), top(), left() + listWidth(), bottom(), LEFT_PANEL);
        graphics.fill(left(), top(), right(), top() + 2, ACCENT);
        graphics.fill(
                left() + listWidth(),
                top(),
                left() + listWidth() + 1,
                bottom(),
                0xFF303747
        );

        graphics.drawCenteredString(
                font,
                Component.literal("БЕСТИАРИЙ RANDOMIZER"),
                (left() + right()) / 2,
                top() + 11,
                TEXT
        );

        renderProgress(graphics);
        renderList(graphics);
        renderDetails(graphics);
        renderScrollbar(graphics);

        if (searchBox != null) {
            searchBox.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void renderProgress(
            GuiGraphics graphics
    ) {
        int discovered = 0;

        for (BestiaryPayload.Entry entry : allEntries) {
            if (entry.discovered()) {
                discovered++;
            }
        }

        int x = left() + listWidth() + 20;
        int y = top() + 38;
        int barRight = right() - 20;
        int barWidth = Math.max(1, barRight - x);

        graphics.drawString(
                font,
                Component.literal(
                        "Открыто: " + discovered + " / " + allEntries.size()
                ),
                x,
                y,
                MUTED
        );

        y += 15;

        graphics.fill(
                x,
                y,
                barRight,
                y + 5,
                0xFF282E3A
        );

        float progress = allEntries.isEmpty()
                ? 0.0F
                : (float) discovered / allEntries.size();

        graphics.fill(
                x,
                y,
                x + Math.round(barWidth * progress),
                y + 5,
                ACCENT
        );
    }

    private void renderList(
            GuiGraphics graphics
    ) {
        int visible = visibleRows();
        int end = Math.min(
                filteredEntries.size(),
                firstVisibleRow + visible
        );

        for (int index = firstVisibleRow; index < end; index++) {
            BestiaryPayload.Entry entry =
                    filteredEntries.get(index);

            int y = listTop()
                    + (index - firstVisibleRow) * ROW_HEIGHT;

            int rowColor = entry == selected
                    ? SELECTED
                    : entry.discovered()
                    ? ROW
                    : ROW_UNKNOWN;

            graphics.fill(
                    left() + 8,
                    y,
                    left() + listWidth() - 8,
                    y + ROW_HEIGHT - 2,
                    rowColor
            );

            graphics.drawString(
                    font,
                    Component.literal(
                            entry.discovered() ? "●" : "○"
                    ),
                    left() + 13,
                    y + 7,
                    entry.discovered() ? SUCCESS : UNKNOWN
            );

            graphics.drawString(
                    font,
                    Component.literal(
                            trim(
                                    entityName(entry.sourceId()),
                                    listWidth() - 58
                            )
                    ),
                    left() + 29,
                    y + 7,
                    entry.discovered() ? TEXT : MUTED
            );
        }

        if (filteredEntries.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    Component.literal("Ничего не найдено"),
                    left() + listWidth() / 2,
                    listTop() + 24,
                    MUTED
            );
        }
    }

    private void renderDetails(
            GuiGraphics graphics
    ) {
        int x = left() + listWidth() + 20;
        int y = top() + 82;
        int availableWidth = right() - x - 18;

        if (selected == null) {
            graphics.drawString(
                    font,
                    Component.literal("Выберите моба из списка."),
                    x,
                    y,
                    MUTED
            );
            return;
        }

        graphics.drawString(
                font,
                Component.literal(
                        trim(
                                entityName(selected.sourceId()),
                                availableWidth
                        )
                ),
                x,
                y,
                TEXT
        );

        y += 18;

        graphics.drawString(
                font,
                Component.literal(
                        selected.discovered()
                                ? "Открыт"
                                : "Не открыт"
                ),
                x,
                y,
                selected.discovered() ? SUCCESS : UNKNOWN
        );

        y += 20;

        graphics.drawString(
                font,
                Component.literal(
                        "Убийств игроком: " + selected.kills()
                ),
                x,
                y,
                MUTED
        );

        y += 30;

        graphics.drawString(
                font,
                Component.literal("Источник лута"),
                x,
                y,
                MUTED
        );

        y += 16;

        if (selected.discovered()
                && !selected.targetId().isEmpty()) {

            graphics.drawString(
                    font,
                    Component.literal(
                            trim(
                                    entityName(selected.targetId()),
                                    availableWidth
                            )
                    ),
                    x,
                    y,
                    ACCENT
            );

            y += 15;

            graphics.drawString(
                    font,
                    Component.literal(
                            trim(selected.targetId(), availableWidth)
                    ),
                    x,
                    y,
                    0xFF7F8795
            );
        } else {
            graphics.drawString(
                    font,
                    Component.literal("???"),
                    x,
                    y,
                    UNKNOWN
            );
        }

        y += 32;

        graphics.drawString(
                font,
                Component.literal("Entity ID:"),
                x,
                y,
                MUTED
        );

        y += 13;

        graphics.drawString(
                font,
                Component.literal(
                        trim(selected.sourceId(), availableWidth)
                ),
                x,
                y,
                0xFF7F8795
        );

        graphics.drawString(
                font,
                Component.literal(
                        "Randomizer Seed: " + payload.masterSeed()
                ),
                x,
                bottom() - 22,
                0xFF606877
        );
    }

    private void filter(
            String rawQuery
    ) {
        String query = rawQuery
                .strip()
                .toLowerCase(Locale.ROOT);

        filteredEntries = allEntries.stream()
                .filter(entry -> {
                    String sourceName = entityName(entry.sourceId())
                            .toLowerCase(Locale.ROOT);

                    String targetName = entry.discovered()
                            && !entry.targetId().isEmpty()
                            ? entityName(entry.targetId())
                            .toLowerCase(Locale.ROOT)
                            : "";

                    return query.isEmpty()
                            || sourceName.contains(query)
                            || targetName.contains(query)
                            || entry.sourceId()
                            .toLowerCase(Locale.ROOT)
                            .contains(query)
                            || entry.targetId()
                            .toLowerCase(Locale.ROOT)
                            .contains(query);
                })
                .sorted(
                        Comparator.comparing(
                                entry -> entityName(entry.sourceId()),
                                String.CASE_INSENSITIVE_ORDER
                        )
                )
                .toList();

        firstVisibleRow = 0;

        if (selected == null
                || !filteredEntries.contains(selected)) {
            selected = filteredEntries.isEmpty()
                    ? null
                    : filteredEntries.getFirst();
        }
    }

    @Override
    public boolean mouseClicked(
            double mouseX,
            double mouseY,
            int button
    ) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        if (button != 0 || !insideList(mouseX, mouseY)) {
            return false;
        }

        int row = ((int) mouseY - listTop()) / ROW_HEIGHT;
        int index = firstVisibleRow + row;

        if (index >= 0 && index < filteredEntries.size()) {
            selected = filteredEntries.get(index);
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
        if (!insideList(mouseX, mouseY)) {
            return super.mouseScrolled(
                    mouseX,
                    mouseY,
                    scrollX,
                    scrollY
            );
        }

        int maximum = Math.max(
                0,
                filteredEntries.size() - visibleRows()
        );

        if (scrollY > 0) {
            firstVisibleRow = Math.max(
                    0,
                    firstVisibleRow - 1
            );
        } else if (scrollY < 0) {
            firstVisibleRow = Math.min(
                    maximum,
                    firstVisibleRow + 1
            );
        }

        return true;
    }

    private void renderScrollbar(
            GuiGraphics graphics
    ) {
        int visible = visibleRows();

        if (filteredEntries.size() <= visible) {
            return;
        }

        int trackTop = listTop();
        int trackBottom = bottom() - 10;
        int trackHeight = trackBottom - trackTop;
        int x = left() + listWidth() - 5;

        graphics.fill(
                x,
                trackTop,
                x + 2,
                trackBottom,
                0xFF303747
        );

        int thumbHeight = Math.max(
                16,
                Math.round(
                        trackHeight
                                * ((float) visible
                                / filteredEntries.size())
                )
        );

        int maxScroll = filteredEntries.size() - visible;
        float position = maxScroll <= 0
                ? 0.0F
                : (float) firstVisibleRow / maxScroll;

        int thumbY = trackTop
                + Math.round(
                        (trackHeight - thumbHeight)
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
        return mouseX >= left() + 8
                && mouseX <= left() + listWidth() - 8
                && mouseY >= listTop()
                && mouseY <= bottom() - 10;
    }

    private int visibleRows() {
        return Math.max(
                1,
                (bottom() - 10 - listTop()) / ROW_HEIGHT
        );
    }

    private String entityName(
            String rawId
    ) {
        ResourceLocation id = ResourceLocation.tryParse(rawId);

        if (id == null) {
            return rawId;
        }

        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(id);

        if (type == null) {
            return rawId;
        }

        return type.getDescription().getString();
    }

    private String trim(
            String value,
            int maximumWidth
    ) {
        if (maximumWidth <= 0 || font.width(value) <= maximumWidth) {
            return value;
        }

        String suffix = "...";
        int suffixWidth = font.width(suffix);
        int allowed = Math.max(0, maximumWidth - suffixWidth);
        String trimmed = font.plainSubstrByWidth(value, allowed);

        return trimmed + suffix;
    }

    private int windowWidth() {
        return Math.max(360, Math.min(width - 24, 900));
    }

    private int windowHeight() {
        return Math.max(260, Math.min(height - 24, 520));
    }

    private int left() {
        return (width - windowWidth()) / 2;
    }

    private int right() {
        return left() + windowWidth();
    }

    private int top() {
        return (height - windowHeight()) / 2;
    }

    private int bottom() {
        return top() + windowHeight();
    }

    private int listWidth() {
        return Math.max(180, Math.min(330, windowWidth() * 2 / 5));
    }

    private int listTop() {
        return top() + 66;
    }
}

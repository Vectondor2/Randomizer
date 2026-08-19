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

    private static final int BACKGROUND =
            0xF20B0E14;

    private static final int PANEL =
            0xF2181D29;

    private static final int PANEL_SECONDARY =
            0xF211151E;

    private static final int ROW =
            0xD9222937;

    private static final int ROW_UNKNOWN =
            0xD9191E28;

    private static final int ROW_SELECTED =
            0xFF38445C;

    private static final int ACCENT =
            0xFF8E79FF;

    private static final int TEXT =
            0xFFF2F3F7;

    private static final int TEXT_SECONDARY =
            0xFFA8AFBD;

    private static final int SUCCESS =
            0xFF79E08A;

    private static final int UNKNOWN =
            0xFF8B919E;

    private final BestiaryPayload payload;

    private final List<BestiaryPayload.Entry>
            allEntries;

    private List<BestiaryPayload.Entry>
            filteredEntries;

    private EditBox searchBox;

    private BestiaryPayload.Entry selected;

    private int firstVisibleRow = 0;

    public BestiaryScreen(
            BestiaryPayload payload
    ) {
        super(
                Component.literal(
                        "Бестиарий Randomizer"
                )
        );

        this.payload = payload;
        this.allEntries =
                List.copyOf(payload.entries());

        this.filteredEntries =
                this.allEntries;
    }

    @Override
    protected void init() {
        int left =
                windowLeft();

        int searchWidth =
                leftPanelWidth() - 24;

        searchBox =
                new EditBox(
                        this.font,
                        left + 12,
                        windowTop() + 37,
                        searchWidth,
                        20,
                        Component.literal(
                                "Поиск"
                        )
                );

        searchBox.setSuggestion(
                "Поиск моба..."
        );

        searchBox.setResponder(
                this::applyFilter
        );

        this.addRenderableWidget(
                searchBox
        );

        applyFilter("");
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
                this.width,
                this.height,
                BACKGROUND
        );

        int left =
                windowLeft();

        int right =
                windowRight();

        int top =
                windowTop();

        int bottom =
                windowBottom();

        int divider =
                left + leftPanelWidth();

        graphics.fill(
                left,
                top,
                right,
                bottom,
                PANEL
        );

        graphics.fill(
                left,
                top,
                right,
                top + 2,
                ACCENT
        );

        graphics.fill(
                left,
                top,
                divider,
                bottom,
                PANEL_SECONDARY
        );

        graphics.fill(
                divider,
                top,
                divider + 1,
                bottom,
                0xFF303747
        );

        graphics.drawCenteredString(
                this.font,
                Component.literal(
                        "БЕСТИАРИЙ RANDOMIZER"
                ),
                (left + right) / 2,
                top + 11,
                TEXT
        );

        renderProgress(
                graphics,
                left,
                right,
                top
        );

        renderMobList(
                graphics,
                mouseX,
                mouseY
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

    private void renderProgress(
            GuiGraphics graphics,
            int left,
            int right,
            int top
    ) {
        int discovered = 0;

        for (BestiaryPayload.Entry entry :
                allEntries) {

            if (entry.discovered()) {
                discovered++;
            }
        }

        int total =
                allEntries.size();

        int barLeft =
                left + leftPanelWidth() + 18;

        int barRight =
                right - 18;

        int barY =
                top + 38;

        int barWidth =
                Math.max(
                        1,
                        barRight - barLeft
                );

        float progress =
                total == 0
                        ? 0.0F
                        : (float) discovered
                        / (float) total;

        graphics.fill(
                barLeft,
                barY,
                barRight,
                barY + 5,
                0xFF282E3A
        );

        graphics.fill(
                barLeft,
                barY,
                barLeft
                        + Math.round(
                                barWidth
                                        * progress
                        ),
                barY + 5,
                ACCENT
        );

        graphics.drawString(
                this.font,
                Component.literal(
                        "Изучено: "
                                + discovered
                                + " / "
                                + total
                ),
                barLeft,
                barY + 9,
                TEXT_SECONDARY
        );
    }

    private void renderMobList(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        int left =
                windowLeft() + 8;

        int right =
                windowLeft()
                        + leftPanelWidth()
                        - 8;

        int top =
                listTop();

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

            BestiaryPayload.Entry entry =
                    filteredEntries.get(
                            index
                    );

            int visibleIndex =
                    index - firstVisibleRow;

            int rowTop =
                    top
                            + visibleIndex
                            * ROW_HEIGHT;

            int rowBottom =
                    rowTop
                            + ROW_HEIGHT
                            - 2;

            int color;

            if (entry == selected) {
                color = ROW_SELECTED;
            } else if (entry.discovered()) {
                color = ROW;
            } else {
                color = ROW_UNKNOWN;
            }

            graphics.fill(
                    left,
                    rowTop,
                    right,
                    rowBottom,
                    color
            );

            String mobName =
                    trimToWidth(
                            entityName(
                                    entry.sourceId()
                            ),
                            right
                                    - left
                                    - 66
                    );

            graphics.drawString(
                    this.font,
                    Component.literal(
                            mobName
                    ),
                    left + 7,
                    rowTop + 7,
                    TEXT
            );

            String status =
                    entry.discovered()
                            ? "ОТКРЫТО"
                            : "???";

            int statusColor =
                    entry.discovered()
                            ? SUCCESS
                            : UNKNOWN;

            graphics.drawString(
                    this.font,
                    Component.literal(
                            status
                    ),
                    right
                            - this.font.width(
                                    status
                            )
                            - 6,
                    rowTop + 7,
                    statusColor
            );
        }

        if (filteredEntries.isEmpty()) {
            graphics.drawCenteredString(
                    this.font,
                    Component.literal(
                            "Ничего не найдено"
                    ),
                    (left + right) / 2,
                    top + 20,
                    TEXT_SECONDARY
            );
        }
    }

    private void renderDetails(
            GuiGraphics graphics
    ) {
        int left =
                windowLeft()
                        + leftPanelWidth()
                        + 20;

        int right =
                windowRight() - 20;

        int top =
                windowTop() + 72;

        if (selected == null) {
            graphics.drawCenteredString(
                    this.font,
                    Component.literal(
                            "Выберите существо слева"
                    ),
                    (left + right) / 2,
                    top + 30,
                    TEXT_SECONDARY
            );

            return;
        }

        String sourceName =
                entityName(
                        selected.sourceId()
                );

        graphics.drawString(
                this.font,
                Component.literal(
                        sourceName
                ),
                left,
                top,
                TEXT
        );

        graphics.fill(
                left,
                top + 16,
                right,
                top + 17,
                0xFF303747
        );

        int y =
                top + 30;

        graphics.drawString(
                this.font,
                Component.literal(
                        "Статус"
                ),
                left,
                y,
                TEXT_SECONDARY
        );

        graphics.drawString(
                this.font,
                Component.literal(
                        selected.discovered()
                                ? "ИЗУЧЕНО"
                                : "НЕ ИЗУЧЕНО"
                ),
                left + 92,
                y,
                selected.discovered()
                        ? SUCCESS
                        : UNKNOWN
        );

        y += 24;

        graphics.drawString(
                this.font,
                Component.literal(
                        "Убийств игроком"
                ),
                left,
                y,
                TEXT_SECONDARY
        );

        graphics.drawString(
                this.font,
                Component.literal(
                        Integer.toString(
                                selected.kills()
                        )
                ),
                left + 120,
                y,
                TEXT
        );

        y += 34;

        graphics.drawString(
                this.font,
                Component.literal(
                        "Лут перенаправлен от:"
                ),
                left,
                y,
                TEXT_SECONDARY
        );

        y += 18;

        String donorName;

        if (selected.discovered()
                && !selected.targetId()
                .isBlank()) {

            donorName =
                    entityName(
                            selected.targetId()
                    );

            graphics.drawString(
                    this.font,
                    Component.literal(
                            "→ " + donorName
                    ),
                    left,
                    y,
                    ACCENT
            );

        } else {
            graphics.drawString(
                    this.font,
                    Component.literal(
                            "→ ???"
                    ),
                    left,
                    y,
                    UNKNOWN
            );
        }

        y += 38;

        if (!selected.discovered()) {
            graphics.drawString(
                    this.font,
                    Component.literal(
                            "Убейте этого моба,"
                    ),
                    left,
                    y,
                    TEXT_SECONDARY
            );

            graphics.drawString(
                    this.font,
                    Component.literal(
                            "чтобы раскрыть его лут."
                    ),
                    left,
                    y + 12,
                    TEXT_SECONDARY
            );
        }

        int bottom =
                windowBottom();

        ResourceLocation sourceId =
                ResourceLocation.tryParse(
                        selected.sourceId()
                );

        String namespace =
                sourceId == null
                        ? "unknown"
                        : sourceId.getNamespace();

        graphics.drawString(
                this.font,
                Component.literal(
                        "Мод: " + namespace
                ),
                left,
                bottom - 48,
                0xFF717887
        );

        graphics.drawString(
                this.font,
                Component.literal(
                        "ID: "
                                + selected.sourceId()
                ),
                left,
                bottom - 34,
                0xFF5B6270
        );

        graphics.drawString(
                this.font,
                Component.literal(
                        "Randomizer Seed: "
                                + payload.masterSeed()
                ),
                left,
                bottom - 20,
                0xFF5B6270
        );
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
                windowBottom() - 10;

        int trackHeight =
                trackBottom - trackTop;

        int x =
                windowLeft()
                        + leftPanelWidth()
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
                                                / filteredEntries.size()
                                )
                        )
                );

        int maxScroll =
                filteredEntries.size()
                        - visible;

        float scrollProgress =
                maxScroll <= 0
                        ? 0.0F
                        : (float) firstVisibleRow
                        / maxScroll;

        int thumbY =
                trackTop
                        + Math.round(
                        (
                                trackHeight
                                        - thumbHeight
                        )
                                * scrollProgress
                );

        graphics.fill(
                x - 1,
                thumbY,
                x + 3,
                thumbY + thumbHeight,
                ACCENT
        );
    }

    private void applyFilter(
            String search
    ) {
        String query =
                search
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        filteredEntries =
                allEntries.stream()
                        .filter(entry -> {
                            String name =
                                    entityName(
                                            entry.sourceId()
                                    )
                                            .toLowerCase(
                                                    Locale.ROOT
                                            );

                            String id =
                                    entry.sourceId()
                                            .toLowerCase(
                                                    Locale.ROOT
                                            );

                            return query.isEmpty()
                                    || name.contains(
                                    query
                            )
                                    || id.contains(
                                    query
                            );
                        })
                        .sorted(
                                Comparator.comparing(
                                        entry ->
                                                entityName(
                                                        entry.sourceId()
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
                            : filteredEntries.get(0);
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
                || !isInsideList(
                mouseX,
                mouseY
        )) {
            return false;
        }

        int relativeY =
                (int) mouseY
                        - listTop();

        int row =
                relativeY
                        / ROW_HEIGHT;

        int index =
                firstVisibleRow
                        + row;

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
        if (!isInsideList(
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

        if (scrollY > 0.0D) {
            firstVisibleRow =
                    Math.max(
                            0,
                            firstVisibleRow - 1
                    );

        } else if (scrollY < 0.0D) {
            firstVisibleRow =
                    Math.min(
                            maximum,
                            firstVisibleRow + 1
                    );
        }

        return true;
    }

    private boolean isInsideList(
            double mouseX,
            double mouseY
    ) {
        return mouseX
                >= windowLeft() + 8

                && mouseX
                <= windowLeft()
                + leftPanelWidth()
                - 8

                && mouseY
                >= listTop()

                && mouseY
                <= windowBottom()
                - 10;
    }

    private int visibleRows() {
        return Math.max(
                1,
                (
                        windowBottom()
                                - 10
                                - listTop()
                )
                        / ROW_HEIGHT
        );
    }

    private int windowWidth() {
        return Math.max(
                300,
                Math.min(
                        820,
                        this.width - 24
                )
        );
    }

    private int windowLeft() {
        return (
                this.width
                        - windowWidth()
        ) / 2;
    }

    private int windowRight() {
        return windowLeft()
                + windowWidth();
    }

    private int windowTop() {
        return 16;
    }

    private int windowBottom() {
        return this.height - 16;
    }

    private int leftPanelWidth() {
        return Math.min(
                280,
                Math.max(
                        190,
                        windowWidth() / 3
                )
        );
    }

    private int listTop() {
        return windowTop() + 67;
    }

    private String entityName(
            String rawId
    ) {
        ResourceLocation id =
                ResourceLocation.tryParse(
                        rawId
                );

        if (id == null) {
            return rawId;
        }

        EntityType<?> type =
                BuiltInRegistries.ENTITY_TYPE
                        .get(id);

        if (type == null) {
            return rawId;
        }

        return type
                .getDescription()
                .getString();
    }

    private String trimToWidth(
            String text,
            int maximumWidth
    ) {
        if (this.font.width(text)
                <= maximumWidth) {
            return text;
        }

        String value = text;

        while (value.length() > 1
                && this.font.width(
                value + "…"
        ) > maximumWidth) {

            value =
                    value.substring(
                            0,
                            value.length() - 1
                    );
        }

        return value + "…";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
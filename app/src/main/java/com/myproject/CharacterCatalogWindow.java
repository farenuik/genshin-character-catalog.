package com.myproject;

import com.myproject.api.CharacterCatalogEntry;
import com.myproject.api.GenshinApiClient;
import com.myproject.api.GenshinDataSource;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Swing window for browsing the remote Genshin character catalog. */
public final class CharacterCatalogWindow extends JFrame {
    private static final Color BACKGROUND = new Color(25, 30, 42);
    private static final Color CARD_BACKGROUND = new Color(38, 45, 61);
    private static final Color ACCENT = new Color(218, 183, 111);
    private static final Color TEXT = new Color(239, 239, 235);
    private static final Color MUTED = new Color(177, 185, 198);
    private static final Dimension PORTRAIT_SIZE = new Dimension(112, 132);

    private final GenshinDataSource dataSource;
    private final PortraitCache portraitCache;
    private final JPanel cardsPanel = new JPanel();
    private final JTextField searchField = new JTextField(22);
    private final JLabel statusLabel = new JLabel("Готово до завантаження каталогу");
    private final JButton refreshButton = new JButton("Оновити");
    private final List<CatalogCard> cards = new ArrayList<>();
    private SwingWorker<Void, CatalogCard> catalogWorker;

    public CharacterCatalogWindow() {
        this(new GenshinApiClient(), FilePortraitCache.defaultCache());
    }

    CharacterCatalogWindow(GenshinDataSource dataSource, PortraitCache portraitCache) {
        super("Каталог персонажів Genshin Impact");
        this.dataSource = dataSource;
        this.portraitCache = portraitCache;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 560));
        setSize(980, 720);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout(0, 12));

        add(createHeader(), BorderLayout.NORTH);
        cardsPanel.setBackground(BACKGROUND);
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(cardsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
        scrollPane.getViewport().setBackground(BACKGROUND);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        add(scrollPane, BorderLayout.CENTER);

        statusLabel.setForeground(MUTED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 22, 14, 22));
        add(statusLabel, BorderLayout.SOUTH);
        refreshButton.addActionListener(this::refreshCatalog);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                filterCards();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                filterCards();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                filterCards();
            }
        });
        refreshCatalog(null);
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 12));
        header.setBackground(BACKGROUND);
        header.setBorder(BorderFactory.createEmptyBorder(20, 22, 4, 22));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Персонажі Тейвату");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 25f));
        JLabel subtitle = new JLabel("Переглядайте персонажів, стихії та спорядження");
        subtitle.setForeground(MUTED);
        subtitle.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        titleBlock.add(title);
        titleBlock.add(subtitle);
        header.add(titleBlock, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controls.setOpaque(false);
        JLabel searchLabel = new JLabel("Пошук");
        searchLabel.setForeground(TEXT);
        searchField.setToolTipText("Ім'я, стихія, тип зброї або ID");
        searchField.setPreferredSize(new Dimension(280, 34));
        searchField.setBackground(CARD_BACKGROUND);
        searchField.setForeground(TEXT);
        searchField.setCaretColor(TEXT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 82, 105)),
                BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        styleButton(refreshButton);
        controls.add(searchLabel);
        controls.add(searchField);
        controls.add(refreshButton);
        header.add(controls, BorderLayout.SOUTH);
        return header;
    }

    private void refreshCatalog(ActionEvent ignored) {
        if (catalogWorker != null && !catalogWorker.isDone()) {
            return;
        }

        cards.clear();
        cardsPanel.removeAll();
        cardsPanel.add(messageLabel("Завантаження каталогу персонажів..."));
        cardsPanel.revalidate();
        cardsPanel.repaint();
        refreshButton.setEnabled(false);
        statusLabel.setText("Отримання даних із Genshin API...");

        catalogWorker = new SwingWorker<>() {
            private int portraitFailures;

            @Override
            protected Void doInBackground() throws Exception {
                List<String> ids = dataSource.listCharacterIds();
                for (int index = 0; index < ids.size(); index++) {
                    if (isCancelled()) {
                        break;
                    }
                    String id = ids.get(index);
                    CharacterCatalogEntry entry = dataSource.getCharacterCatalogEntry(id);
                    Path portraitPath = null;
                    String portraitError = "";
                    if (!entry.imageUrl().isBlank()) {
                        try {
                            portraitPath = portraitCache.cache(entry.imageUrl());
                        } catch (IOException exception) {
                            portraitFailures++;
                            portraitError = exception.getMessage();
                        }
                    }
                    publish(new CatalogCard(entry, portraitPath, portraitError));
                    setProgress((index + 1) * 100 / Math.max(ids.size(), 1));
                }
                return null;
            }

            @Override
            protected void process(List<CatalogCard> chunks) {
                if (cards.isEmpty()) {
                    cardsPanel.removeAll();
                }
                for (CatalogCard card : chunks) {
                    cards.add(card);
                    cardsPanel.add(createCardPanel(card));
                    cardsPanel.add(Box.createVerticalStrut(12));
                }
                filterCards();
                statusLabel.setText("Завантажено персонажів: " + cards.size());
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
                try {
                    get();
                    if (cards.isEmpty()) {
                        cardsPanel.removeAll();
                        cardsPanel.add(messageLabel("Персонажів не знайдено."));
                    }
                    String message = "Завантажено персонажів: " + cards.size();
                    if (portraitFailures > 0) {
                        message += " · не вдалося завантажити портрети: " + portraitFailures;
                    }
                    statusLabel.setText(message);
                    filterCards();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showOfflineCatalog("завантаження перервано");
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    String details = cause == null ? exception.getMessage() : cause.getMessage();
                    showOfflineCatalog(details);
                }
            }
        };
        catalogWorker.execute();
    }

    private JPanel createCardPanel(CatalogCard card) {
        JPanel panel = new JPanel(new BorderLayout(18, 8));
        panel.setBackground(CARD_BACKGROUND);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(57, 67, 87)),
                BorderFactory.createEmptyBorder(12, 12, 12, 16)));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 176));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel portrait = new JLabel();
        portrait.setHorizontalAlignment(SwingConstants.CENTER);
        portrait.setVerticalAlignment(SwingConstants.CENTER);
        portrait.setPreferredSize(PORTRAIT_SIZE);
        portrait.setMinimumSize(PORTRAIT_SIZE);
        portrait.setOpaque(true);
        portrait.setBackground(new Color(51, 60, 79));
        portrait.setForeground(ACCENT);
        portrait.setBorder(BorderFactory.createLineBorder(new Color(83, 91, 109)));
        if (card.portraitPath() == null) {
            if (card.offlineDemo()) {
                portrait.setIcon(new ImageIcon(createDemoPortrait(card.entry())));
                portrait.setToolTipText("Демо-ілюстрація, не оригінальний портрет персонажа");
            } else {
                portrait.setText("Портрет відсутній");
                portrait.setFont(portrait.getFont().deriveFont(11f));
                portrait.setToolTipText(card.portraitError().isBlank() ? null : card.portraitError());
            }
        } else {
            try {
                BufferedImage image = ImageIO.read(card.portraitPath().toFile());
                if (image == null) {
                    portrait.setText("Портрет недоступний");
                } else {
                    Image scaled = image.getScaledInstance(
                            PORTRAIT_SIZE.width, PORTRAIT_SIZE.height, Image.SCALE_SMOOTH);
                    portrait.setIcon(new ImageIcon(scaled));
                }
            } catch (IOException exception) {
                portrait.setText("Портрет недоступний");
                portrait.setToolTipText(exception.getMessage());
            }
        }
        panel.add(portrait, BorderLayout.WEST);

        JPanel details = new JPanel();
        details.setOpaque(false);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(card.entry().name());
        name.setForeground(TEXT);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 19f));
        JLabel metadata = new JLabel(metadata(card.entry()));
        metadata.setForeground(ACCENT);
        metadata.setBorder(BorderFactory.createEmptyBorder(4, 0, 5, 0));
        JLabel description = new JLabel("<html><div style='width:620px'>"
                + escapeHtml(card.entry().description()) + "</div></html>");
        description.setForeground(MUTED);
        description.setFont(description.getFont().deriveFont(13f));
        description.setVerticalAlignment(SwingConstants.TOP);
        JLabel id = new JLabel("ID: " + card.entry().id()
                + " · здібностей: " + card.entry().skills().size());
        id.setForeground(new Color(143, 153, 170));
        id.setFont(id.getFont().deriveFont(11f));
        id.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        details.add(name);
        details.add(metadata);
        details.add(description);
        details.add(id);
        panel.add(details, BorderLayout.CENTER);
        return panel;
    }

    private void filterCards() {
        String query = searchField.getText().strip().toLowerCase(java.util.Locale.ROOT);
        for (int index = 0; index < cards.size(); index++) {
            CatalogCard card = cards.get(index);
            JPanel cardPanel = (JPanel) cardsPanel.getComponent(index * 2);
            String searchable = (card.entry().name() + " " + card.entry().id() + " "
                    + card.entry().element() + " " + card.entry().weaponType())
                    .toLowerCase(java.util.Locale.ROOT);
            boolean visible = searchable.contains(query);
            cardPanel.setVisible(visible);
            cardsPanel.getComponent(index * 2 + 1).setVisible(visible);
        }
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private void showOfflineCatalog(String details) {
        List<CharacterCatalogEntry> examples = List.of(
                new CharacterCatalogEntry("amber", "Amber", "Дослідниця Ордо Фавоніус із Мондштадта.", 4,
                        "Pyro", "Bow", "", List.of()),
                new CharacterCatalogEntry("kaeya", "Kaeya", "Капітан кавалерії Ордо Фавоніус.", 4,
                        "Cryo", "Sword", "", List.of()),
                new CharacterCatalogEntry("lisa", "Lisa", "Бібліотекарка Ордо Фавоніус.", 4,
                        "Electro", "Catalyst", "", List.of()),
                new CharacterCatalogEntry("diluc", "Diluc", "Власник винокурні «Рассвет».", 5,
                        "Pyro", "Claymore", "", List.of()),
                new CharacterCatalogEntry("barbara", "Barbara", "Дияконисса церкви Фавония.", 4,
                        "Hydro", "Catalyst", "", List.of()));
        cards.clear();
        cardsPanel.removeAll();
        for (CharacterCatalogEntry entry : examples) {
            CatalogCard card = new CatalogCard(entry, null, "", true);
            cards.add(card);
            cardsPanel.add(createCardPanel(card));
            cardsPanel.add(Box.createVerticalStrut(12));
        }
        statusLabel.setText("Офлайн-демо: приклади для показу; API недоступне — " + details);
        refreshButton.setEnabled(true);
        filterCards();
    }

    private static BufferedImage createDemoPortrait(CharacterCatalogEntry entry) {
        Color elementColor = switch (entry.element().toLowerCase(java.util.Locale.ROOT)) {
            case "pyro" -> new Color(195, 91, 62);
            case "cryo" -> new Color(93, 161, 197);
            case "electro" -> new Color(145, 105, 183);
            case "hydro" -> new Color(70, 135, 192);
            case "anemo" -> new Color(79, 159, 134);
            case "geo" -> new Color(184, 146, 70);
            default -> new Color(105, 119, 143);
        };
        BufferedImage image = new BufferedImage(112, 132, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setPaint(new GradientPaint(0, 0, elementColor.darker(), 112, 132, CARD_BACKGROUND));
            graphics.fillRect(0, 0, 112, 132);
            graphics.setColor(new Color(255, 255, 255, 35));
            graphics.fillOval(9, 8, 94, 94);
            graphics.setColor(new Color(239, 218, 183));
            graphics.fillOval(37, 27, 38, 43);
            graphics.setColor(new Color(45, 49, 64));
            graphics.fillArc(32, 19, 48, 47, 0, 180);
            graphics.setColor(elementColor.brighter());
            graphics.fillRoundRect(23, 72, 66, 53, 25, 25);
            graphics.setColor(Color.WHITE);
            graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 13f));
            String initials = entry.name().substring(0, Math.min(2, entry.name().length())).toUpperCase(
                    java.util.Locale.ROOT);
            int textWidth = graphics.getFontMetrics().stringWidth(initials);
            graphics.drawString(initials, (112 - textWidth) / 2, 112);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static JLabel messageLabel(String message) {
        JLabel label = new JLabel(message);
        label.setForeground(MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(16, 4, 16, 4));
        return label;
    }

    private static String metadata(CharacterCatalogEntry entry) {
        List<String> values = new ArrayList<>();
        if (entry.rarity() > 0) {
            values.add(entry.rarity() + "★");
        }
        if (!entry.element().isBlank()) {
            values.add(entry.element());
        }
        if (!entry.weaponType().isBlank()) {
            values.add(entry.weaponType());
        }
        return String.join("  ·  ", values);
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static void styleButton(JButton button) {
        button.setBackground(ACCENT);
        button.setForeground(new Color(34, 37, 45));
        button.setFocusPainted(false);
        button.setMargin(new Insets(8, 18, 8, 18));
        button.setBorder(BorderFactory.createEmptyBorder());
    }

    private record CatalogCard(
            CharacterCatalogEntry entry,
            Path portraitPath,
            String portraitError,
            boolean offlineDemo) {
        private CatalogCard(CharacterCatalogEntry entry, Path portraitPath, String portraitError) {
            this(entry, portraitPath, portraitError, false);
        }
    }
}

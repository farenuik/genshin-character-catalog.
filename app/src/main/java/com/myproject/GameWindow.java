package com.myproject;

import com.myproject.game.Ability;
import com.myproject.game.AbilityProvider;
import com.myproject.game.Character;
import com.myproject.game.DefaultAbilityProvider;
import com.myproject.game.Game;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Swing interface for playing and resuming a saved turn-based game. */
public final class GameWindow extends JFrame {
    private static final Color BACKGROUND = new Color(25, 30, 42);
    private static final Color PANEL_BACKGROUND = new Color(38, 45, 61);
    private static final Color ACCENT = new Color(218, 183, 111);
    private static final Color TEXT = new Color(239, 239, 235);
    private static final Color MUTED = new Color(177, 185, 198);

    private final AbilityProvider abilityProvider;
    private final Map<String, Character> characters = new LinkedHashMap<>();
    private final Map<Character, String> gamePlayerIds = new IdentityHashMap<>();
    private final Map<Ability, String> abilityTypes = new IdentityHashMap<>();
    private final List<String> history = new ArrayList<>();
    private final JComboBox<CharacterChoice> firstPlayer = new JComboBox<>();
    private final JComboBox<CharacterChoice> secondPlayer = new JComboBox<>();
    private final JComboBox<String> abilitySelector = new JComboBox<>();
    private final JTextArea output = new JTextArea();
    private final JLabel statusLabel = new JLabel("Створіть гру або завантажте сейв");
    private final JButton newGameButton = new JButton("Нова гра");
    private final JButton saveButton = new JButton("Зберегти");
    private final JButton loadLastButton = new JButton("Завантажити останній сейв");
    private final JButton chooseSaveButton = new JButton("Вибрати сейв...");
    private final JButton attackButton = new JButton("Атакувати");
    private final JButton defendButton = new JButton("Захищатися");
    private final JButton healButton = new JButton("Лікуватися");
    private final JButton abilityButton = new JButton("Застосувати здібність");
    private final GameSaveStore defaultSaveStore;
    private GameSaveStore activeSaveStore;
    private Game game;

    public GameWindow() throws IOException {
        this(CharacterCatalogStore.defaultStore(), GameSaveStore.defaultStore(), new DefaultAbilityProvider());
    }

    GameWindow(
            CharacterCatalogStore characterStore,
            GameSaveStore gameSaveStore,
            AbilityProvider abilityProvider) throws IOException {
        super("Текстова гра Genshin Impact");
        this.defaultSaveStore = Objects.requireNonNull(gameSaveStore, "gameSaveStore");
        this.activeSaveStore = gameSaveStore;
        this.abilityProvider = Objects.requireNonNull(abilityProvider, "abilityProvider");
        characters.putAll(Objects.requireNonNull(characterStore, "characterStore").load());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 560));
        setSize(900, 680);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BACKGROUND);
        setLayout(new BorderLayout(0, 10));

        add(createHeader(), BorderLayout.NORTH);
        configureOutput();
        add(new JScrollPane(output), BorderLayout.CENTER);
        add(createFooter(), BorderLayout.SOUTH);
        configureActions();
        populateCharacterChoices();
        updateGameControls();
        if (characters.size() < 2) {
            statusLabel.setText("Спершу створіть або імпортуйте двох персонажів у режимі --chars");
        }
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 12));
        header.setBackground(BACKGROUND);
        header.setBorder(BorderFactory.createEmptyBorder(18, 20, 2, 20));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new javax.swing.BoxLayout(titleBlock, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Покроковий бій");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitle = new JLabel("Обирайте персонажів, виконуйте ходи та продовжуйте з останнього сейву");
        subtitle.setForeground(MUTED);
        subtitle.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        titleBlock.add(title);
        titleBlock.add(subtitle);
        header.add(titleBlock, BorderLayout.NORTH);

        JPanel controls = new JPanel();
        controls.setOpaque(false);
        controls.setLayout(new javax.swing.BoxLayout(controls, javax.swing.BoxLayout.Y_AXIS));
        JPanel playersRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        playersRow.setOpaque(false);
        JPanel saveRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        saveRow.setOpaque(false);
        styleSelector(firstPlayer);
        styleSelector(secondPlayer);
        firstPlayer.setPreferredSize(new Dimension(165, 32));
        secondPlayer.setPreferredSize(new Dimension(165, 32));
        styleButton(newGameButton);
        styleButton(loadLastButton);
        styleButton(chooseSaveButton);
        styleButton(saveButton);
        JLabel firstLabel = new JLabel("Гравець 1");
        JLabel secondLabel = new JLabel("Гравець 2");
        firstLabel.setForeground(TEXT);
        secondLabel.setForeground(TEXT);
        playersRow.add(firstLabel);
        playersRow.add(firstPlayer);
        playersRow.add(secondLabel);
        playersRow.add(secondPlayer);
        playersRow.add(newGameButton);
        saveRow.add(loadLastButton);
        saveRow.add(chooseSaveButton);
        saveRow.add(saveButton);
        controls.add(playersRow);
        controls.add(saveRow);
        header.add(controls, BorderLayout.SOUTH);
        return header;
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 8));
        footer.setBackground(BACKGROUND);
        footer.setBorder(BorderFactory.createEmptyBorder(0, 20, 14, 20));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        styleButton(attackButton);
        styleButton(defendButton);
        styleButton(healButton);
        styleButton(abilityButton);
        styleSelector(abilitySelector);
        actions.add(attackButton);
        actions.add(defendButton);
        actions.add(healButton);
        actions.add(abilitySelector);
        actions.add(abilityButton);
        footer.add(actions, BorderLayout.NORTH);

        statusLabel.setForeground(MUTED);
        statusLabel.setHorizontalAlignment(SwingConstants.LEFT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(7, 0, 0, 0));
        footer.add(statusLabel, BorderLayout.SOUTH);
        return footer;
    }

    private void configureOutput() {
        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setBackground(PANEL_BACKGROUND);
        output.setForeground(TEXT);
        output.setCaretColor(TEXT);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        output.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
    }

    private void configureActions() {
        newGameButton.addActionListener(event -> startNewGame());
        saveButton.addActionListener(event -> saveGame());
        loadLastButton.addActionListener(event -> loadGame(defaultSaveStore));
        chooseSaveButton.addActionListener(event -> chooseSaveFile());
        attackButton.addActionListener(event -> performTurn(() -> game.attack(getOpponent())));
        defendButton.addActionListener(event -> performTurn(game::defend));
        healButton.addActionListener(event -> performTurn(() -> game.heal(18)));
        abilityButton.addActionListener(event -> performTurn(() ->
                game.useAbility((String) abilitySelector.getSelectedItem(), getOpponent())));
    }

    private void populateCharacterChoices() {
        characters.forEach((id, character) -> {
            CharacterChoice choice = new CharacterChoice(id, character);
            firstPlayer.addItem(choice);
            secondPlayer.addItem(choice);
        });
        if (secondPlayer.getItemCount() > 1) {
            secondPlayer.setSelectedIndex(1);
        }
    }

    private void startNewGame() {
        if (game != null && !game.isOver()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Поточна партія вже збережена. Почати нову й замінити останній сейв?",
                    "Нова гра",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }
        CharacterChoice firstChoice = (CharacterChoice) firstPlayer.getSelectedItem();
        CharacterChoice secondChoice = (CharacterChoice) secondPlayer.getSelectedItem();
        if (firstChoice == null || secondChoice == null) {
            showError("Потрібно вибрати двох персонажів.");
            return;
        }
        if (firstChoice.character() == secondChoice.character()) {
            showError("Оберіть двох різних персонажів.");
            return;
        }
        if (firstChoice.character().getHealth() <= 0 || secondChoice.character().getHealth() <= 0) {
            showError("Обидва персонажі повинні мати більше 0 HP.");
            return;
        }

        Character first = copyForGame(firstChoice.character());
        Character second = copyForGame(secondChoice.character());
        grantDefaultAbilities(first);
        grantDefaultAbilities(second);
        game = new Game();
        game.addPlayer(first);
        game.addPlayer(second);
        game.start();
        gamePlayerIds.clear();
        gamePlayerIds.put(first, firstChoice.id());
        gamePlayerIds.put(second, secondChoice.id());
        history.clear();
        history.add("Game started: " + first.getName() + " vs " + second.getName());
        activeSaveStore = defaultSaveStore;
        appendHistory();
        try {
            persistCurrentGame();
        } catch (IOException exception) {
            showError("Не вдалося зберегти нову гру: " + exception.getMessage());
        }
        updateGameControls();
        updateStatus();
    }

    private void grantDefaultAbilities(Character character) {
        Ability fireball = abilityProvider.create("attack");
        character.learn("Fireball", fireball);
        abilityTypes.put(fireball, "attack");
        Ability healingLight = abilityProvider.create("heal");
        character.learn("Healing Light", healingLight);
        abilityTypes.put(healingLight, "heal");
    }

    private Character copyForGame(Character character) {
        Character copy = new Character(
                character.getName(),
                character.getMaxHealth(),
                character.getArmor(),
                character.getAttack(),
                character.getProfile());
        copy.restoreCombatState(
                character.getHealth(), character.getArmor(), character.getAttack(), character.isDefending());
        character.getAbilities().forEach(copy::learn);
        return copy;
    }

    private void performTurn(GameAction action) {
        if (game == null || game.isOver()) {
            showError("Почніть нову гру або завантажте сейв.");
            return;
        }
        Character actor = game.getCurrentPlayer();
        try {
            String result = action.execute();
            long turnNumber = history.stream().filter(entry -> entry.startsWith("Turn ")).count() + 1;
            history.add("Turn " + turnNumber + " - " + actor.getName() + ": " + result);
            if (game.isOver()) {
                history.add("Winner: " + game.getWinner().getName());
            }
            appendHistory();
            persistCurrentGame();
            updateGameControls();
            updateStatus();
        } catch (IllegalArgumentException | IllegalStateException | IOException exception) {
            showError(exception.getMessage());
        }
    }

    private Character getOpponent() {
        Character currentPlayer = game.getCurrentPlayer();
        return game.getPlayers().stream()
                .filter(player -> player != currentPlayer)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("У грі немає суперника."));
    }

    private void saveGame() {
        if (game == null) {
            showError("Немає гри для збереження.");
            return;
        }
        try {
            persistCurrentGame();
            statusLabel.setText("Сейв збережено: " + activeSaveStorePath());
        } catch (IOException | IllegalStateException exception) {
            showError(exception.getMessage());
        }
    }

    private void persistCurrentGame() throws IOException {
        activeSaveStore.save(game, gamePlayerIds, history, abilityTypes);
    }

    private String activeSaveStorePath() {
        return activeSaveStore == defaultSaveStore ? "останній сейв" : "вибраний файл";
    }

    private void loadGame(GameSaveStore store) {
        if (game != null && !game.isOver()) {
            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Поточна партія вже збережена. Завантажити інший сейв?",
                    "Завантаження сейву",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            GameSaveStore.LoadedGame loadedGame = store.load(abilityProvider);
            game = loadedGame.game();
            gamePlayerIds.clear();
            gamePlayerIds.putAll(loadedGame.playerIds());
            history.clear();
            history.addAll(loadedGame.history());
            abilityTypes.putAll(loadedGame.abilityTypes());
            activeSaveStore = store;
            appendHistory();
            updateGameControls();
            updateStatus();
        } catch (IOException | IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void chooseSaveFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Виберіть файл збереженої гри");
        chooser.setFileFilter(new FileNameExtensionFilter("Файли сейвів (*.json)", "json"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path selectedFile = chooser.getSelectedFile().toPath();
            loadGame(new GameSaveStore(selectedFile));
        }
    }

    private void appendHistory() {
        output.setText(String.join(System.lineSeparator(), history));
        output.setCaretPosition(output.getDocument().getLength());
    }

    private void updateStatus() {
        if (game == null) {
            statusLabel.setText("Створіть гру або завантажте сейв");
            return;
        }
        String players = game.getPlayers().stream()
                .map(player -> player.getName() + " HP=" + player.getHealth())
                .reduce((first, second) -> first + "  |  " + second)
                .orElse("");
        if (game.isOver()) {
            statusLabel.setText(players + "  •  Переможець: " + game.getWinner().getName());
        } else {
            statusLabel.setText(players + "  •  Хід: " + game.getCurrentPlayer().getName());
        }
    }

    private void updateGameControls() {
        boolean canAct = game != null && !game.isOver();
        newGameButton.setEnabled(characters.size() >= 2);
        attackButton.setEnabled(canAct);
        defendButton.setEnabled(canAct);
        healButton.setEnabled(canAct);
        abilitySelector.setEnabled(canAct && abilitySelector.getItemCount() > 0);
        abilityButton.setEnabled(canAct && abilitySelector.getItemCount() > 0);
        saveButton.setEnabled(game != null);

        abilitySelector.removeAllItems();
        if (canAct) {
            game.getCurrentPlayer().getAbilities().keySet().forEach(abilitySelector::addItem);
        }
        abilitySelector.setEnabled(canAct && abilitySelector.getItemCount() > 0);
        abilityButton.setEnabled(canAct && abilitySelector.getItemCount() > 0);
    }

    private void showError(String message) {
        String details = message == null || message.isBlank() ? "Невідома помилка" : message;
        statusLabel.setText("Помилка: " + details);
        output.append(System.lineSeparator() + "Помилка: " + details);
    }

    private static void styleSelector(JComboBox<?> selector) {
        selector.setBackground(PANEL_BACKGROUND);
        selector.setForeground(TEXT);
        selector.setFocusable(true);
    }

    private static void styleButton(JButton button) {
        button.setBackground(PANEL_BACKGROUND);
        button.setForeground(ACCENT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(71, 82, 105)),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
    }

    private record CharacterChoice(String id, Character character) {
        @Override
        public String toString() {
            return character.getName() + " (" + id + ")";
        }
    }

    @FunctionalInterface
    private interface GameAction {
        String execute() throws IOException;
    }
}

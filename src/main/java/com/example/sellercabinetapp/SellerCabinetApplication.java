package com.example.sellercabinetapp;

import com.example.sellercabinetapp.client.ApiClient;
import com.example.sellercabinetapp.client.SellerApi;
import com.example.sellercabinetapp.dto.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;

import java.util.List;
import java.util.Optional;

public class SellerCabinetApplication extends Application {

    private SellerApi sellerApi;
    private ApiClient apiClient;
    private Stage primaryStage;

    private boolean isAdmin = false;
    private Long currentSellerId = null;

    private final ListView<UserDto> sellerListView = new ListView<>();
    private final TableView<ProductDto> productTableView = new TableView<>();

    private final Label lblTotalSales = new Label("Продаж: 0");
    private final Label lblStockValue = new Label("Склад на сумму: 0.00 ₽");
    private final Label lblAvgRating = new Label("Средняя оценка: 0.00");

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.apiClient = new ApiClient("http://localhost:8085/");
        this.sellerApi = new SellerApi(apiClient);

        showLoginAndStart();
    }

    private void showLoginAndStart() {
        boolean loggedIn = false;
        while (!loggedIn) {
            LoginActionResult action = showLoginDialog(apiClient);
            if (action == LoginActionResult.EXIT) {
                System.exit(0);
                return;
            } else if (action == LoginActionResult.REGISTER) {
                showRegisterDialog();
            } else if (action == LoginActionResult.SUCCESS) {
                loggedIn = true;
            }
        }

        Parent rootLayout;
        String title;

        if (isAdmin) {
            title = "Панель Администратора (Seller Cabinet)";
            rootLayout = createAdminLayout();
        } else {
            title = "Кабинет Продавца (ID: " + currentSellerId + ")";
            rootLayout = createSellerLayout();
        }

        Scene scene = new Scene(rootLayout, 1000, 650);
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void handleLogout() {
        apiClient.setJwtToken(null);
        currentSellerId = null;
        isAdmin = false;
        showLoginAndStart();
    }

    private JsonNode decodeJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) throw new IllegalArgumentException("Invalid JWT");
            byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
            return new ObjectMapper().readTree(new String(decodedBytes));
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode JWT", e);
        }
    }

    private enum LoginActionResult {
        SUCCESS, REGISTER, EXIT
    }

    private LoginActionResult showLoginDialog(ApiClient apiClient) {
        Dialog<LoginActionResult> dialog = new Dialog<>();
        dialog.setTitle("Авторизация");
        dialog.setHeaderText("Вход в систему Seller Cabinet");
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType loginButtonType = new ButtonType("Войти", ButtonBar.ButtonData.OK_DONE);
        ButtonType registerButtonType = new ButtonType("Регистрация", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, registerButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");

        grid.add(new Label("Email:"), 0, 0);
        grid.add(emailField, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(emailField::requestFocus);

        dialog.setResultConverter(button -> {
            if (button == loginButtonType) {
                try {
                    String email = emailField.getText().trim();
                    String password = passwordField.getText().trim();

                    TokenPairDto tokens = sellerApi.login(email, password);
                    apiClient.setJwtToken(tokens.accessToken());

                    JsonNode payload = decodeJwtPayload(tokens.accessToken());
                    currentSellerId = payload.get("sub").asLong();

                    JsonNode rolesNode = payload.get("roles");
                    isAdmin = false;
                    if (rolesNode != null && rolesNode.isArray()) {
                        for (JsonNode role : rolesNode) {
                            if ("ADMIN".equals(role.asText())) {
                                isAdmin = true;
                                break;
                            }
                        }
                    }
                    return LoginActionResult.SUCCESS;
                } catch (Exception ex) {
                    showAlert("Ошибка входа", ex.getMessage());
                    return null;
                }
            } else if (button == registerButtonType) {
                return LoginActionResult.REGISTER;
            }
            return LoginActionResult.EXIT;
        });

        Optional<LoginActionResult> result = dialog.showAndWait();
        return result.orElse(LoginActionResult.EXIT);
    }

    private void showRegisterDialog() {
        Dialog<RegisterRequestDto> dialog = new Dialog<>();
        dialog.setTitle("Регистрация");
        dialog.setHeaderText("Создание нового аккаунта продавца");
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType registerButtonType = new ButtonType("Зарегистрироваться", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registerButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField firstNameField = new TextField();
        firstNameField.setPromptText("Имя");
        TextField lastNameField = new TextField();
        lastNameField.setPromptText("Фамилия");
        TextField ageField = new TextField();
        ageField.setPromptText("Возраст");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Пароль");

        grid.add(new Label("Имя:"), 0, 0); grid.add(firstNameField, 1, 0);
        grid.add(new Label("Фамилия:"), 0, 1); grid.add(lastNameField, 1, 1);
        grid.add(new Label("Возраст:"), 0, 2); grid.add(ageField, 1, 2);
        grid.add(new Label("Email:"), 0, 3); grid.add(emailField, 1, 3);
        grid.add(new Label("Пароль:"), 0, 4); grid.add(passwordField, 1, 4);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(firstNameField::requestFocus);

        dialog.setResultConverter(button -> {
            if (button == registerButtonType) {
                try {
                    int age = Integer.parseInt(ageField.getText().trim());
                    return new RegisterRequestDto(
                            firstNameField.getText().trim(),
                            lastNameField.getText().trim(),
                            age,
                            emailField.getText().trim(),
                            passwordField.getText().trim()
                    );
                } catch (Exception e) {
                    showAlert("Ошибка", "Проверьте правильность введенных данных (возраст должен быть числом).");
                    return null;
                }
            }
            return null;
        });

        Optional<RegisterRequestDto> result = dialog.showAndWait();
        result.ifPresent(req -> {
            try {
                sellerApi.register(req);
                showAlert("Успех", "Аккаунт успешно создан! Теперь вы можете войти в систему.");
            } catch (Exception e) {
                showAlert("Ошибка регистрации", e.getMessage());
            }
        });
    }

    private Parent createAdminLayout() {
        VBox mainLayout = new VBox(5);

        HBox topNavBar = createTopNavBar();

        VBox sellerBox = new VBox(10);
        sellerBox.setPadding(new Insets(10));
        Label lblSellers = new Label("Все пользователи (Продавцы)");
        lblSellers.setStyle("-fx-font-weight: bold;");

        sellerListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(UserDto item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.firstName() + " " + item.lastName() + " (ID: " + item.userId() + ")");
            }
        });

        sellerListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadProductsForSeller(newVal.userId());
                loadAnalyticsForSeller(newVal.userId());
            } else {
                productTableView.getItems().clear();
                clearAnalytics();
            }
        });

        Button btnDeleteSeller = new Button("Удалить пользователя");
        btnDeleteSeller.setOnAction(e -> {
            UserDto selected = sellerListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                deleteUser(selected.userId());
            } else {
                showAlert("Ошибка", "Выберите пользователя для удаления");
            }
        });

        sellerBox.getChildren().addAll(lblSellers, sellerListView, btnDeleteSeller);
        VBox.setVgrow(sellerListView, Priority.ALWAYS);

        VBox productBox = createProductTableBase("Товары выбранного продавца");

        VBox analyticsBox = createAnalyticsCardBox("📈 Статистика выбранного продавца:");

        HBox productButtons = new HBox(5);
        Button btnDeleteProduct = new Button("Удалить выбранный товар");
        btnDeleteProduct.setOnAction(e -> {
            UserDto selectedSeller = sellerListView.getSelectionModel().getSelectedItem();
            ProductDto selectedProduct = productTableView.getSelectionModel().getSelectedItem();
            if (selectedSeller != null && selectedProduct != null) {
                deleteProduct(selectedProduct.productId(), () -> {
                    loadProductsForSeller(selectedSeller.userId());
                    loadAnalyticsForSeller(selectedSeller.userId());
                });
            } else {
                showAlert("Ошибка", "Выберите продавца и его товар");
            }
        });
        productButtons.getChildren().add(btnDeleteProduct);

        productBox.getChildren().addAll(analyticsBox, productButtons);

        SplitPane splitPane = new SplitPane(sellerBox, productBox);
        splitPane.setDividerPositions(0.35);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        mainLayout.getChildren().addAll(topNavBar, splitPane);
        loadAllUsers();
        return mainLayout;
    }

    private Parent createSellerLayout() {
        VBox mainLayout = new VBox(5);
        HBox topNavBar = createTopNavBar();

        VBox productBox = createProductTableBase("Мои товары");

        VBox analyticsBox = createAnalyticsCardBox("📈 Моя статистика:");

        HBox buttons = new HBox(5);
        Button btnAdd = new Button("Добавить товар");
        Button btnEdit = new Button("Редактировать");
        Button btnDelete = new Button("Удалить");

        btnAdd.setOnAction(e -> showProductDialog(null));
        btnEdit.setOnAction(e -> {
            ProductDto selected = productTableView.getSelectionModel().getSelectedItem();
            if (selected != null) showProductDialog(selected);
            else showAlert("Ошибка", "Выберите товар для редактирования");
        });
        btnDelete.setOnAction(e -> {
            ProductDto selected = productTableView.getSelectionModel().getSelectedItem();
            if (selected != null) deleteProduct(selected.productId(), this::loadMyProducts);
            else showAlert("Ошибка", "Выберите товар для удаления");
        });

        buttons.getChildren().addAll(btnAdd, btnEdit, btnDelete);

        productBox.getChildren().addAll(analyticsBox, buttons);
        VBox.setVgrow(productBox, Priority.ALWAYS);

        mainLayout.getChildren().addAll(topNavBar, productBox);

        loadMyProducts();

        return mainLayout;
    }

    private VBox createAnalyticsCardBox(String titleText) {
        VBox analyticsBox = new VBox(8);
        analyticsBox.setPadding(new Insets(10));
        analyticsBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5; -fx-border-color: #e9ecef; -fx-border-radius: 5;");

        Label analyticsTitle = new Label(titleText);
        analyticsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #34495e;");

        lblTotalSales.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 13px;");
        lblStockValue.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13px; -fx-font-weight: bold;");
        lblAvgRating.setStyle("-fx-text-fill: #d35400; -fx-font-size: 13px; -fx-font-weight: bold;");

        analyticsBox.getChildren().addAll(analyticsTitle, lblTotalSales, lblStockValue, lblAvgRating);
        return analyticsBox;
    }

    private HBox createTopNavBar() {
        HBox navBar = new HBox(10);
        navBar.setPadding(new Insets(10));
        navBar.setStyle("-fx-background-color: #2c3e50;");

        String analyticsButtonText = isAdmin ? "📊 Общая аналитика" : "📊 Моя аналитика";
        Button btnAnalytics = new Button(analyticsButtonText);
        btnAnalytics.setStyle("-fx-base: #27ae60; -fx-text-fill: white;");
        btnAnalytics.setOnAction(e -> showGlobalAnalyticsDialog());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnLogout = new Button("🚪 Выйти");
        btnLogout.setStyle("-fx-base: #c0392b; -fx-text-fill: white;");
        btnLogout.setOnAction(e -> handleLogout());

        navBar.getChildren().addAll(btnAnalytics, spacer, btnLogout);
        return navBar;
    }

    private void showGlobalAnalyticsDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(isAdmin ? "Общая аналитика по системе" : "Моя аналитика");
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        VBox cardsContainer = new VBox(10);
        cardsContainer.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(cardsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(500, 400);

        new Thread(() -> {
            try {
                if (isAdmin) {
                    List<SellerStatsDto> statsList = sellerApi.getAllSellerStats();
                    Platform.runLater(() -> renderAnalyticsCards(cardsContainer, statsList));
                } else {
                    SellerStatsDto myStats = sellerApi.getSellerStats(currentSellerId);
                    Platform.runLater(() -> {
                        List<SellerStatsDto> singleList = (myStats != null) ? List.of(myStats) : List.of();
                        renderAnalyticsCards(cardsContainer, singleList);
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка", "Не удалось загрузить аналитику: " + e.getMessage()));
            }
        }).start();

        String headerText = isAdmin ? "Сводная статистика по всем продавцам:" : "Ваша сводная статистика:";
        VBox mainBox = new VBox(10, new Label(headerText), scrollPane);
        mainBox.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(mainBox);
        dialog.showAndWait();
    }

    private void renderAnalyticsCards(VBox container, List<SellerStatsDto> statsList) {
        container.getChildren().clear();
        if (statsList.isEmpty()) {
            container.getChildren().add(new Label("Данные аналитики пока отсутствуют."));
            return;
        }

        for (SellerStatsDto stats : statsList) {
            VBox card = new VBox(5);
            card.setPadding(new Insets(12));
            card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 6; -fx-border-color: #dee2e6; -fx-border-radius: 6;");

            Label lblHeader = new Label("🆔 Продавец ID: " + stats.sellerId());
            lblHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

            Label lblProducts = new Label("• Всего товаров: " + stats.totalProducts());
            Label lblSales = new Label("• Всего продаж: " + stats.totalSales());
            Label lblStock = new Label("• Стоимость склада: " + stats.totalStockValue() + " ₽");
            lblStock.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");

            Label lblRating = new Label("• Средняя оценка: " + stats.averageEstimation());
            lblRating.setStyle("-fx-text-fill: #d35400; -fx-font-weight: bold;");

            card.getChildren().addAll(lblHeader, new Separator(), lblProducts, lblSales, lblStock, lblRating);
            container.getChildren().add(card);
        }
    }

    private VBox createProductTableBase(String headerText) {
        VBox box = new VBox(10);
        box.setPadding(new Insets(10));
        Label label = new Label(headerText);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TableColumn<ProductDto, Long> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().productId()));

        TableColumn<ProductDto, String> colName = new TableColumn<>("Название");
        colName.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyStringWrapper(c.getValue().name()));

        TableColumn<ProductDto, Double> colPrice = new TableColumn<>("Цена");
        colPrice.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().price()));

        TableColumn<ProductDto, Integer> colCount = new TableColumn<>("В наличии");
        colCount.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().count()));

        TableColumn<ProductDto, Integer> colSales = new TableColumn<>("Продано");
        colSales.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().countOfSales()));

        TableColumn<ProductDto, Double> colEstimation = new TableColumn<>("Оценка");
        colEstimation.setCellValueFactory(c -> new javafx.beans.property.ReadOnlyObjectWrapper<>(c.getValue().averageEstimation()));

        productTableView.getColumns().clear();
        productTableView.getColumns().addAll(colId, colName, colPrice, colCount, colSales, colEstimation);
        productTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);

        VBox.setVgrow(productTableView, Priority.ALWAYS);
        box.getChildren().addAll(label, productTableView);
        return box;
    }

    private void clearAnalytics() {
        lblTotalSales.setText("Продаж: 0");
        lblStockValue.setText("Склад на сумму: 0.00 ₽");
        lblAvgRating.setText("Средняя оценка: 0.00");
    }

    private void loadAllUsers() {
        new Thread(() -> {
            try {
                List<UserDto> users = sellerApi.getAllUsers();
                Platform.runLater(() -> sellerListView.getItems().setAll(users));
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка сети", e.getMessage()));
            }
        }).start();
    }

    private void loadProductsForSeller(Long sellerId) {
        new Thread(() -> {
            try {
                List<ProductDto> products = sellerApi.getProductsBySeller(sellerId);
                Platform.runLater(() -> productTableView.getItems().setAll(products));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    productTableView.getItems().clear();
                    showAlert("Информация", "Не удалось загрузить товары: " + e.getMessage());
                });
            }
        }).start();
    }

    private void loadAnalyticsForSeller(Long sellerId) {
        new Thread(() -> {
            try {
                SellerStatsDto stats = sellerApi.getSellerStats(sellerId);
                Platform.runLater(() -> {
                    if (stats != null) {
                        lblTotalSales.setText("Продаж: " + stats.totalSales());
                        lblStockValue.setText("Склад на сумму: " + stats.totalStockValue() + " ₽");
                        lblAvgRating.setText("Средняя оценка: " + stats.averageEstimation());
                    } else {
                        clearAnalytics();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(this::clearAnalytics);
            }
        }).start();
    }

    private void loadMyProducts() {
        loadProductsForSeller(currentSellerId);
        loadAnalyticsForSeller(currentSellerId);
    }

    private void deleteUser(Long userId) {
        new Thread(() -> {
            try {
                sellerApi.deleteUser(userId);
                Platform.runLater(this::loadAllUsers);
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка", e.getMessage()));
            }
        }).start();
    }

    private void deleteProduct(Long productId, Runnable onSuccess) {
        new Thread(() -> {
            try {
                sellerApi.deleteProduct(productId);
                Platform.runLater(onSuccess);
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка", e.getMessage()));
            }
        }).start();
    }

    private void showProductDialog(ProductDto existing) {
        Dialog<ProductRequest> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "Добавить товар" : "Редактировать товар");
        dialog.initModality(Modality.APPLICATION_MODAL);

        ButtonType saveButtonType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField();
        TextField priceField = new TextField();
        TextField countField = new TextField();
        TextField salesField = new TextField();
        TextField estimationField = new TextField();

        if (existing != null) {
            nameField.setText(existing.name());
            priceField.setText(String.valueOf(existing.price()));
            countField.setText(String.valueOf(existing.count()));
            salesField.setText(String.valueOf(existing.countOfSales()));
            estimationField.setText(String.valueOf(existing.averageEstimation()));
        }

        grid.add(new Label("Название:"), 0, 0); grid.add(nameField, 1, 0);
        grid.add(new Label("Цена:"), 0, 1); grid.add(priceField, 1, 1);
        grid.add(new Label("В наличии:"), 0, 2); grid.add(countField, 1, 2);
        grid.add(new Label("Продано:"), 0, 3); grid.add(salesField, 1, 3);
        grid.add(new Label("Оценка:"), 0, 4); grid.add(estimationField, 1, 4);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(button -> {
            if (button == saveButtonType) {
                return new ProductRequest(
                        nameField.getText().trim(),
                        parseDouble(priceField.getText()),
                        parseInt(countField.getText()),
                        parseInt(salesField.getText()),
                        parseDouble(estimationField.getText())
                );
            }
            return null;
        });

        Optional<ProductRequest> result = dialog.showAndWait();
        result.ifPresent(req -> new Thread(() -> {
            try {
                if (existing == null) sellerApi.createProduct(req);
                else sellerApi.updateProduct(existing.productId(), req);
                Platform.runLater(this::loadMyProducts);
            } catch (Exception e) {
                Platform.runLater(() -> showAlert("Ошибка сохранения", e.getMessage()));
            }
        }).start());
    }

    private double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return 0.0; }
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}